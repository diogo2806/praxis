package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;

import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;

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
 * Gerencia a política de retenção de dados e conformidade com LGPD (Lei Geral de Proteção de Dados).
 *
 * Automaticamente anonimiza dados de candidatos após um período pré-configurado (padrão: 180 dias).
 * Quando um processo de seleção é finalizado, o candidato tem direto de esquecimento: seus dados
 * pessoais (nome, email) são removidos e substituídos por informações genéricas. Isso garante
 * que apenas dados estritamente necessários são mantidos, em conformidade com a LGPD brasileira.
 *
 * Registra todas as anonimizações no histórico de auditoria para rastreabilidade.
 */
@Service
public class PrivacyRetentionService {

    private static final int BATCH_SIZE = 100;
    private static final List<AttemptStatus> CLOSED_STATUSES = List.of(
            AttemptStatus.COMPLETED,
            AttemptStatus.ABANDONED,
            AttemptStatus.EXPIRED,
            AttemptStatus.FAILED
    );

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final MarketplaceProfessionalRepository marketplaceProfessionalRepository;
    private final AuditEventService auditEventService;
    private final Clock clock;
    private final int retentionDays;

    @Autowired
    public PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            MarketplaceProfessionalRepository marketplaceProfessionalRepository,
            AuditEventService auditEventService,
            @Value("${praxis.privacy-retention-days:180}") int retentionDays
    ) {
        this(candidateAttemptRepository, marketplaceProfessionalRepository, auditEventService, Clock.systemUTC(), retentionDays);
    }

    PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            MarketplaceProfessionalRepository marketplaceProfessionalRepository,
            AuditEventService auditEventService,
            Clock clock,
            int retentionDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.marketplaceProfessionalRepository = marketplaceProfessionalRepository;
        this.auditEventService = auditEventService;
        this.clock = clock;
        this.retentionDays = retentionDays;
    }

    /**
     * Anonimiza os dados de candidatos cuja retenção expirou.
     *
     * Busca todos os candidatos que:
     * - Tiveram seus processos de seleção finalizados (completado, abandonado, expirado ou falhou)
     * - Passaram mais tempo que o período de retenção configurado (padrão: 180 dias)
     *
     * Para cada candidato encontrado, substitui o nome e email por informações genéricas,
     * mantendo apenas o ID para rastreabilidade. Registra a anonimização no histórico para
     * conformidade regulatória.
     *
     * Processa em lotes de 100 registros para não sobrecarregar o banco de dados.
     *
     * @param empresaId A empresa para a qual executar a anonimização
     * @return Quantidade de candidatos que tiveram seus dados anonimizados
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
        int professionalCount = AuditEventService.PLATFORM_EMPRESA_ID.equals(empresaId)
                ? anonymizeExpiredMarketplaceProfessionals(cutoff, anonymizedAt)
                : 0;
        return candidates.size() + professionalCount;
    }

    /**
     * Executa a anonimização de um candidato específico.
     *
     * Remove as informações pessoais do candidato:
     * - Nome é substituído por um valor genérico
     * - Email é substituído por um identificador anônimo único
     * - URL do webhook (usada para enviar resultados) é removida
     * - ID idempotente é marcado como anonimizado
     *
     * Em seguida, registra o evento de anonimização no histórico de auditoria
     * com informações sobre quando ocorreu e qual política foi aplicada.
     *
     * @param candidate Dados do candidato a ser anonimizado
     * @param anonymizedAt Momento da anonimização
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

    private int anonymizeExpiredMarketplaceProfessionals(Instant cutoff, Instant anonymizedAt) {
        List<MarketplaceProfessionalEntity> professionals =
                marketplaceProfessionalRepository.findByVerificationStatusInAndUpdatedAtBeforeAndAnonymizedAtIsNull(
                        List.of(
                                ProfessionalVerificationStatus.REJECTED,
                                ProfessionalVerificationStatus.SUSPENDED
                        ),
                        cutoff
                );
        professionals.forEach(professional -> anonymizeProfessional(professional, anonymizedAt));
        return professionals.size();
    }

    private void anonymizeProfessional(MarketplaceProfessionalEntity professional, Instant anonymizedAt) {
        Long professionalId = professional.getId();
        professional.setDisplayName("Profissional anonimizado " + professionalId);
        professional.setDocument("anon-" + professionalId);
        professional.setProfessionalRegistration(null);
        professional.setBio(null);
        professional.getSpecialties().clear();
        professional.setLinkedinUrl(null);
        professional.setPixKey(null);
        professional.setMpSellerId(null);
        professional.setMpAccessToken(null);
        professional.setAnonymizedAt(anonymizedAt);

        auditEventService.appendUserEvent(
                AuditEventService.PLATFORM_EMPRESA_ID,
                professional.getUserId().toString(),
                AuditEventType.MARKETPLACE_PROFESSIONAL_ANONYMIZED,
                "Dados pessoais do profissional marketplace anonimizados por politica de retencao.",
                "{\"retentionDays\":" + retentionDays
                        + ",\"professionalId\":" + professionalId
                        + ",\"anonymizedAt\":\"" + anonymizedAt + "\"}"
        );
    }
}
