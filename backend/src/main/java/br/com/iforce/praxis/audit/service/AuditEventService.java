package br.com.iforce.praxis.audit.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;

import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.util.List;


/**
 * Registra o histórico de auditoria de todas as ações importantes no sistema.
 *
 * Mantém um log completo e imutável de tudo que acontece com candidatos, provas
 * e versões de provas. Cada ação registrada inclui o que aconteceu, quando,
 * por quem, e detalhes adicionais.
 *
 * Esse histórico serve para:
 * - Conformidade regulatória (rastreabilidade de dados pessoais)
 * - Investigação de problemas (entender como uma tentativa chegou a um estado)
 * - Segurança (identificar acessos não autorizados ou alterações suspeitas)
 *
 * O histórico nunca é apagado (append-only), garantindo que não há como
 * falsificar o registro de eventos.
 */
@Service
public class AuditEventService {

    public static final String CANDIDATE_ATTEMPT_AGGREGATE = "CandidateAttempt";
    public static final String SIMULATION_AGGREGATE = "Simulation";
    public static final String SIMULATION_VERSION_AGGREGATE = "SimulationVersion";
    public static final String EMPRESA_AGGREGATE = "Empresa";
    public static final String USER_AGGREGATE = "User";
    public static final String ASSESSMENT_JOURNEY_AGGREGATE = "AssessmentJourney";
    public static final String ASSESSMENT_JOURNEY_ATTEMPT_AGGREGATE = "AssessmentJourneyAttempt";
    public static final String INTEGRATION_AGGREGATE = "Integration";

    /** Empresa técnico usado quando uma ação administrativa não tem cliente alvo. */
    public static final String PLATFORM_EMPRESA_ID = "PLATFORM";

    private final AuditEventRepository auditEventRepository;
    private final CurrentEmpresaService currentEmpresaService;

    public AuditEventService(AuditEventRepository auditEventRepository, CurrentEmpresaService currentEmpresaService) {
        this.auditEventRepository = auditEventRepository;
        this.currentEmpresaService = currentEmpresaService;
    }

    /**
     * Registra uma ação sensível executada por um operador ADMIN sobre um cliente (empresa).
     *
     * Diferente dos eventos operacionais, a ação administrativa precisa identificar
     * explicitamente o ator (operador ADMIN) e o empresa alvo, já que o ADMIN não pertence
     * ao fluxo normal de empresa. Para ações sem cliente alvo, use {@link #PLATFORM_EMPRESA_ID}.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente,
     * garantindo que a ação e o registro de auditoria sejam aplicados juntos. A trilha é
     * append-only: não há caminho para editar ou excluir o evento depois de gravado.
     *
     * @param actorUserId    ID do operador ADMIN que executou a ação
     * @param targetEmpresaId empresa alvo; {@code null} é tratado como {@link #PLATFORM_EMPRESA_ID}
     * @param eventType      tipo do evento administrativo
     * @param message        descrição em português do que aconteceu
     * @param metadata       informações adicionais em JSON (motivo, valores anteriores, etc.)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void auditAdminAction(
            String actorUserId,
            String targetEmpresaId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        String empresaId = (targetEmpresaId == null || targetEmpresaId.isBlank())
                ? PLATFORM_EMPRESA_ID
                : targetEmpresaId;

        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setActorUserId(actorUserId);
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(EMPRESA_AGGREGATE);
        auditEventEntity.setAggregateId(empresaId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Registra um evento no histórico de um usuário de acesso (ex.: recuperação de senha).
     *
     * <p>Diferente da ação administrativa, o ator é o próprio usuário e não há operador ADMIN
     * envolvido. A trilha permanece append-only e preserva o isolamento por empresa. Nunca
     * registre o token, a senha ou seus hashes nos metadados.</p>
     *
     * <p>Este método DEVE ser chamado dentro de uma transação de banco de dados existente.</p>
     *
     * @param empresaId  empresa (ou {@link #PLATFORM_EMPRESA_ID} para ADMIN) do usuário
     * @param userId    identificador do usuário alvo do evento
     * @param eventType tipo do evento (ex.: PASSWORD_RESET_REQUESTED, PASSWORD_RESET_COMPLETED)
     * @param message   descrição em português do que aconteceu
     * @param metadata  informações adicionais em JSON (horário, IP quando disponível, etc.)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendUserEvent(
            String empresaId,
            String userId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(USER_AGGREGATE);
        auditEventEntity.setAggregateId(userId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Registra um evento no histórico de uma tentativa de candidato.
     *
     * Cada vez que algo importante acontece com a prova de um candidato
     * (começou, terminou, foi anonimizado, etc), este método é chamado
     * para deixar um registro permanente no histórico.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente.
     * Se algo der errado, o banco inteiro volta atrás (rollback), garantindo que
     * a ação e seu registro no histórico acontecem juntos.
     *
     * @param empresaId A empresa que o candidato pertence
     * @param attemptId ID único da tentativa da prova
     * @param eventType Tipo de evento (ex: STARTED, COMPLETED, ANONYMIZED)
     * @param message Descrição em português do que aconteceu
     * @param metadata Informações adicionais em JSON (contexto do evento)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendCandidateAttemptEvent(
            String empresaId,
            String attemptId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(CANDIDATE_ATTEMPT_AGGREGATE);
        auditEventEntity.setAggregateId(attemptId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Registra um evento no histórico de uma simulação (prova).
     *
     * Cada vez que uma prova é criada, editada ou publicada, este método
     * registra o evento para rastreabilidade.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente.
     *
     * @param empresaId A empresa que a simulação pertence
     * @param simulationId ID único da simulação
     * @param eventType Tipo de evento (ex: CREATED, UPDATED, PUBLISHED)
     * @param message Descrição em português do que aconteceu
     * @param metadata Informações adicionais em JSON (quem editou, quais campos mudaram, etc)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendSimulationEvent(
            String empresaId,
            String simulationId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(SIMULATION_AGGREGATE);
        auditEventEntity.setAggregateId(simulationId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Registra um evento no histórico de uma versão específica de uma simulação.
     *
     * Provas podem ter múltiplas versões (v1, v2, v3, etc). Cada versão tem
     * seu próprio histórico. Este método registra eventos nesse histórico de versão.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente.
     *
     * @param empresaId A empresa que a simulação pertence
     * @param simulationId ID único da simulação
     * @param versionNumber Número da versão (1, 2, 3, etc)
     * @param eventType Tipo de evento (ex: CREATED, PUBLISHED)
     * @param message Descrição em português do que aconteceu
     * @param metadata Informações adicionais em JSON
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendSimulationVersionEvent(
            String empresaId,
            String simulationId,
            int versionNumber,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(SIMULATION_VERSION_AGGREGATE);
        auditEventEntity.setAggregateId(simulationVersionAggregateId(simulationId, versionNumber));
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Recupera o histórico completo de uma tentativa de candidato.
     *
     * Retorna uma lista cronológica de todos os eventos que ocorreram com
     * a prova de um candidato, do início ao fim. Inclui quando começou,
     * quando terminou, se foi anonimizado, etc.
     *
     * Útil para investigar problemas ou fazer auditoria de uma tentativa específica.
     *
     * @param attemptId ID único da tentativa
     * @return Lista dos eventos ordenados por data (do mais antigo para o mais recente)
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> listCandidateAttemptEvents(String attemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return auditEventRepository
                .findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        empresaId,
                        CANDIDATE_ATTEMPT_AGGREGATE,
                        attemptId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Recupera o histórico de uma versão específica de uma simulação.
     *
     * Provas podem ser criadas, editadas e republicadas em múltiplas versões.
     * Este método retorna o histórico de mudanças de uma versão em particular.
     *
     * @param simulationId ID único da simulação
     * @param versionNumber Número da versão (1, 2, 3, etc)
     * @return Lista dos eventos daquela versão ordenados por data
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> listSimulationVersionEvents(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return auditEventRepository
                .findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        empresaId,
                        SIMULATION_VERSION_AGGREGATE,
                        simulationVersionAggregateId(simulationId, versionNumber)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Registra um evento no histórico de uma Jornada de Avaliação.
     *
     * <p>Cada criação, atualização, publicação ou arquivamento da jornada fica
     * registrada de forma append-only, preservando o empresa, o ator (quando
     * houver), a jornada e os metadados relevantes.</p>
     *
     * <p>Este método DEVE ser chamado dentro de uma transação de banco existente.</p>
     *
     * @param empresaId empresa dona da jornada
     * @param actorUserId usuário ator que executou a ação, quando houver
     * @param journeyId identificador da jornada
     * @param eventType tipo de evento (ex.: CREATED, PUBLISHED, ARCHIVED)
     * @param message descrição em português do que aconteceu
     * @param metadata informações adicionais em JSON
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAssessmentJourneyEvent(
            String empresaId,
            String actorUserId,
            String journeyId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setActorUserId(actorUserId);
        auditEventEntity.setAggregateType(ASSESSMENT_JOURNEY_AGGREGATE);
        auditEventEntity.setAggregateId(journeyId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Registra um evento no histórico da tentativa de um candidato em uma jornada.
     *
     * <p>Cobre o ciclo da tentativa da jornada (criada, iniciada, etapas
     * iniciadas/concluídas e conclusão), de forma append-only, preservando o
     * empresa, a tentativa da jornada e os metadados relevantes.</p>
     *
     * <p>Este método DEVE ser chamado dentro de uma transação de banco existente.</p>
     *
     * @param empresaId empresa dona da jornada
     * @param journeyAttemptId identificador da tentativa da jornada
     * @param eventType tipo de evento (ex.: ATTEMPT_CREATED, STEP_COMPLETED)
     * @param message descrição em português do que aconteceu
     * @param metadata informações adicionais em JSON
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void appendAssessmentJourneyAttemptEvent(
            String empresaId,
            String journeyAttemptId,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setAggregateType(ASSESSMENT_JOURNEY_ATTEMPT_AGGREGATE);
        auditEventEntity.setAggregateId(journeyAttemptId);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void appendIntegrationEvent(
            String empresaId,
            String actorUserId,
            String provider,
            AuditEventType eventType,
            String message,
            String metadata
    ) {
        AuditEventEntity auditEventEntity = new AuditEventEntity();
        auditEventEntity.setEmpresaId(empresaId);
        auditEventEntity.setActorUserId(actorUserId);
        auditEventEntity.setAggregateType(INTEGRATION_AGGREGATE);
        auditEventEntity.setAggregateId(provider);
        auditEventEntity.setEventType(eventType);
        auditEventEntity.setMessage(message);
        auditEventEntity.setMetadata(metadata);
        auditEventEntity.setCreatedAt(Instant.now());

        auditEventRepository.save(auditEventEntity);
    }

    /**
     * Recupera o histórico de auditoria de uma Jornada de Avaliação.
     *
     * @param journeyId identificador da jornada
     * @return os eventos da jornada ordenados por data (do mais antigo ao mais recente)
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAssessmentJourneyEvents(String journeyId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return auditEventRepository
                .findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        empresaId,
                        ASSESSMENT_JOURNEY_AGGREGATE,
                        journeyId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Recupera o histórico de auditoria da tentativa de um candidato em uma jornada.
     *
     * @param journeyAttemptId identificador da tentativa da jornada
     * @return os eventos da tentativa ordenados por data
     */
    @Transactional(readOnly = true)
    public List<AuditEventResponse> listAssessmentJourneyAttemptEvents(String journeyAttemptId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return auditEventRepository
                .findByEmpresaIdAndAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
                        empresaId,
                        ASSESSMENT_JOURNEY_ATTEMPT_AGGREGATE,
                        journeyAttemptId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String simulationVersionAggregateId(String simulationId, int versionNumber) {
        return simulationId + ":v" + versionNumber;
    }

    private AuditEventResponse toResponse(AuditEventEntity auditEventEntity) {
        return new AuditEventResponse(
                auditEventEntity.getId(),
                displayAggregateType(auditEventEntity.getAggregateType()),
                auditEventEntity.getAggregateId(),
                auditEventEntity.getEventType(),
                auditEventEntity.getMessage(),
                auditEventEntity.getMetadata(),
                auditEventEntity.getCreatedAt()
        );
    }

    private String displayAggregateType(String aggregateType) {
        return switch (aggregateType) {
            case CANDIDATE_ATTEMPT_AGGREGATE -> "Tentativa do candidato";
            case SIMULATION_AGGREGATE -> "Simulação";
            case SIMULATION_VERSION_AGGREGATE -> "Versão da simulação";
            case USER_AGGREGATE -> "Usuário";
            case ASSESSMENT_JOURNEY_AGGREGATE -> "Jornada de avaliação";
            case ASSESSMENT_JOURNEY_ATTEMPT_AGGREGATE -> "Tentativa da jornada";
            case INTEGRATION_AGGREGATE -> "Integração";
            default -> "Item";
        };
    }
}
