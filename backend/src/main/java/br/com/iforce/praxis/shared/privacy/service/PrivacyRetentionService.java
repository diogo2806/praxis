package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.time.Clock;

import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;


/**
 * Conduz a etapa de retenção e anonimização de dados pessoais dos candidatos.
 *
 * <p>Na visão do processo, este serviço é o responsável por cumprir a regra de
 * privacidade depois que uma tentativa de avaliação já não precisa mais manter
 * nome, e-mail, chave de idempotência original ou URL de retorno. Ele identifica
 * tentativas encerradas que ultrapassaram o prazo de retenção, substitui os
 * dados pessoais por valores anônimos e registra a ação na auditoria para que a
 * empresa consiga demonstrar quando e por que a anonimização aconteceu.</p>
 *
 * <p>O objetivo não é apagar a história operacional da avaliação. O sistema
 * mantém o identificador técnico da tentativa para rastreabilidade, mas remove
 * os dados que permitem reconhecer diretamente a pessoa candidata.</p>
 */
@Service
public class PrivacyRetentionService {

    private static final int BATCH_SIZE = 100;
    private static final List<AttemptStatus> CLOSED_STATUSES = List.of(
            AttemptStatus.COMPLETED,
            AttemptStatus.ABANDONED,
            AttemptStatus.EXPIRED
    );

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final Clock clock;
    private final int retentionDays;

    /**
     * Monta o serviço de retenção usando o relógio real do sistema.
     *
     * <p>Este é o caminho usado pela aplicação em produção: o sistema injeta o
     * repositório das tentativas, o serviço de auditoria e o prazo de retenção
     * configurado. Quando a configuração não informa outro valor, o processo usa
     * 180 dias como prazo padrão antes de anonimizar dados pessoais.</p>
     *
     * @param candidateAttemptRepository local onde o sistema consulta as tentativas dos candidatos
     * @param auditEventService serviço que registra a evidência de anonimização na auditoria
     * @param retentionDays quantidade de dias que os dados pessoais podem permanecer identificáveis
     */
    @Autowired
    public PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            @Value("${praxis.privacy-retention-days:180}") int retentionDays
    ) {
        this(candidateAttemptRepository, auditEventService, Clock.systemUTC(), retentionDays);
    }

    /**
     * Monta o serviço permitindo controlar o relógio usado no cálculo de vencimento.
     *
     * <p>Na prática do processo, tem o mesmo papel do construtor principal. A
     * diferença é que permite informar um relógio específico para validar cenários
     * de retenção em testes, como simular que determinada tentativa já passou do
     * prazo de anonimização.</p>
     *
     * @param candidateAttemptRepository local onde o sistema consulta as tentativas dos candidatos
     * @param auditEventService serviço que registra a evidência de anonimização na auditoria
     * @param clock referência de data e hora usada para calcular o vencimento da retenção
     * @param retentionDays quantidade de dias que os dados pessoais podem permanecer identificáveis
     */
    PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            Clock clock,
            int retentionDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /**
     * Executa a rotina de anonimização para uma empresa específica.
     *
     * <p>Na visão do usuário de negócio, este método representa a varredura de
     * LGPD de uma empresa: ele procura candidatos cujas avaliações já terminaram
     * e cujo prazo de retenção venceu. Para cada tentativa encontrada, remove os
     * dados pessoais, mantém apenas a rastreabilidade técnica e registra o evento
     * na trilha de auditoria.</p>
     *
     * <p>A rotina trabalha em lotes para proteger a estabilidade do sistema. Em
     * uma execução, trata até 100 tentativas vencidas da empresa informada; novas
     * execuções continuam processando o restante.</p>
     *
     * @param empresaId empresa cuja base de tentativas será verificada
     * @return quantidade de tentativas anonimizadas nesta execução
     */
    @Transactional
    public int anonymizeExpiredAttemptsForEmpresa(String empresaId) {
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        List<CandidateAttemptEntity> candidates = candidateAttemptRepository.findRetentionCandidatesForEmpresa(
                empresaId,
                CLOSED_STATUSES,
                cutoff,
                PageRequest.of(0, BATCH_SIZE)
        );

        Instant anonymizedAt = Instant.now(clock);
        candidates.forEach(candidate -> anonymize(candidate, anonymizedAt));
        return candidates.size();
    }

    /**
     * Aplica a anonimização em uma tentativa de candidato já selecionada pela rotina.
     *
     * <p>Este passo é o momento em que o sistema deixa a tentativa segura para
     * retenção histórica: o nome vira um texto genérico, o e-mail vira um endereço
     * técnico anônimo, a chave de idempotência passa a indicar que o registro já
     * foi tratado e a URL de webhook é removida para evitar novas entregas com
     * dados antigos.</p>
     *
     * <p>Depois da alteração, o processo grava um evento de auditoria. Assim, uma
     * pessoa de suporte, compliance ou operação consegue explicar que a tentativa
     * foi anonimizada por política de retenção, com a data aplicada e o prazo de
     * retenção usado.</p>
     *
     * @param candidate tentativa do candidato que terá os dados pessoais substituídos
     * @param anonymizedAt data e hora registradas como momento oficial da anonimização
     */
    private void anonymize(CandidateAttemptEntity candidate, Instant anonymizedAt) {
        String attemptId = candidate.getId();
        candidate.setCandidateName("Candidato anonimizado");
        candidate.setCandidateEmail("anonimizado+" + attemptId + "@privacy.local");
        candidate.setIdempotencyKey("anonymized:" + attemptId);
        candidate.setResultWebhookUrl(null);
        candidate.setAnonymizedAt(anonymizedAt);

        auditEventService.appendCandidateAttemptEvent(
                candidate.getEmpresaId(),
                attemptId,
                AuditEventType.ATTEMPT_ANONYMIZED,
                "Dados pessoais da tentativa anonimizados por politica de retencao.",
                "{\"retentionDays\":" + retentionDays + ",\"anonymizedAt\":\"" + anonymizedAt + "\"}"
        );
    }

}
