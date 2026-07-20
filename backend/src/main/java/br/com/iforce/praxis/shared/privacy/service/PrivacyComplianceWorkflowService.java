package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.DataSubjectRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.entity.PrivacyIncidentEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.DataSubjectRequestRepository;
import br.com.iforce.praxis.shared.privacy.persistence.repository.HumanReviewRequestRepository;
import br.com.iforce.praxis.shared.privacy.persistence.repository.PrivacyIncidentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PrivacyComplianceWorkflowService {

    private static final Pattern SHA_256 = Pattern.compile("^[a-fA-F0-9]{64}$");

    private final DataSubjectRequestRepository dataSubjectRequestRepository;
    private final HumanReviewRequestRepository humanReviewRequestRepository;
    private final PrivacyIncidentRepository privacyIncidentRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EmpresaRepository empresaRepository;
    private final PrivacyRetentionService privacyRetentionService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public PrivacyComplianceWorkflowService(
            DataSubjectRequestRepository dataSubjectRequestRepository,
            HumanReviewRequestRepository humanReviewRequestRepository,
            PrivacyIncidentRepository privacyIncidentRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            EmpresaRepository empresaRepository,
            PrivacyRetentionService privacyRetentionService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.dataSubjectRequestRepository = dataSubjectRequestRepository;
        this.humanReviewRequestRepository = humanReviewRequestRepository;
        this.privacyIncidentRepository = privacyIncidentRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.empresaRepository = empresaRepository;
        this.privacyRetentionService = privacyRetentionService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DataSubjectRequestResponse> listDataSubjectRequests() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return dataSubjectRequestRepository.findByEmpresaIdOrderByRequestedAtDesc(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DataSubjectRequestResponse updateDataSubjectRequest(
            String requestId,
            ResolveDataSubjectRequestRequest request
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        DataSubjectRequestEntity entity = dataSubjectRequestRepository.findByIdAndEmpresaId(requestId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada."));

        ComplianceRequestStatus targetStatus = requireStatus(request.status());
        if (targetStatus == ComplianceRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use IN_PROGRESS, RESOLVED ou REJECTED.");
        }
        if (targetStatus == ComplianceRequestStatus.REJECTED && isBlank(request.denialReason())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O motivo da recusa é obrigatório.");
        }
        if (targetStatus == ComplianceRequestStatus.RESOLVED && isBlank(request.resolution())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A resolução é obrigatória.");
        }

        Instant now = Instant.now();
        entity.setStatus(targetStatus);
        entity.setAssignedUserId(actorUserId);
        entity.setResolution(normalize(request.resolution()));
        entity.setDenialReason(normalize(request.denialReason()));
        entity.setUpdatedAt(now);
        if (targetStatus == ComplianceRequestStatus.RESOLVED || targetStatus == ComplianceRequestStatus.REJECTED) {
            entity.setRespondedAt(now);
            entity.setClosedAt(now);
        }

        if (Boolean.TRUE.equals(request.executeAnonymization())) {
            if (targetStatus != ComplianceRequestStatus.RESOLVED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A anonimização só pode ser executada ao resolver a solicitação."
                );
            }
            privacyRetentionService.anonymizeAttemptNow(
                    empresaId,
                    entity.getAttemptId(),
                    actorUserId,
                    normalize(request.resolution())
            );
        }

        dataSubjectRequestRepository.save(entity);
        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                entity.getAttemptId(),
                AuditEventType.PRIVACY_REQUEST_STATUS_CHANGED,
                "Situação da solicitação de direito do titular atualizada.",
                json(Map.of(
                        "requestId", entity.getId(),
                        "status", entity.getStatus().name(),
                        "actorUserId", actorUserId
                ))
        );
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<HumanReviewRequestResponse> listHumanReviewRequests() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return humanReviewRequestRepository.findByEmpresaIdOrderByRequestedAtDesc(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HumanReviewRequestResponse resolveHumanReview(String requestId, ResolveHumanReviewRequest request) {
        if (request == null || isBlank(request.resolution())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A conclusão da revisão é obrigatória.");
        }

        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        HumanReviewRequestEntity entity = humanReviewRequestRepository.findByIdAndEmpresaId(requestId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido de revisão não encontrado."));
        CandidateAttemptEntity attempt = candidateAttemptRepository
                .findByEmpresaIdAndIdForUpdate(empresaId, entity.getAttemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        Instant now = Instant.now();
        entity.setStatus(ComplianceRequestStatus.RESOLVED);
        entity.setAssignedUserId(actorUserId);
        entity.setResolution(request.resolution().trim());
        entity.setResolvedAt(now);
        entity.setUpdatedAt(now);
        humanReviewRequestRepository.save(entity);

        attempt.setHumanReviewRequired(false);
        attempt.setHumanReviewCompletedAt(now);
        attempt.setHumanReviewedBy(actorUserId);
        attempt.setHumanReviewResolution(request.resolution().trim());
        candidateAttemptRepository.save(attempt);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attempt.getId(),
                AuditEventType.HUMAN_DECISION,
                "Revisão humana concluída por usuário autorizado.",
                json(Map.of(
                        "reviewRequestId", entity.getId(),
                        "actorUserId", actorUserId,
                        "resolvedAt", now.toString()
                ))
        );
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PrivacyConfigurationResponse getConfiguration() {
        return toResponse(requireCurrentEmpresa());
    }

    @Transactional
    public PrivacyConfigurationResponse updateConfiguration(PrivacyConfigurationRequest request) {
        validateConfiguration(request);
        EmpresaEntity empresa = requireCurrentEmpresa();
        empresa.setPrivacyControllerName(request.controllerName().trim());
        empresa.setPrivacyControllerTaxId(normalize(request.controllerTaxId()));
        empresa.setPrivacyServiceEmail(normalize(request.serviceEmail()));
        empresa.setPrivacyServiceUrl(normalize(request.serviceUrl()));
        empresa.setPrivacyDpoContact(normalize(request.dpoContact()));
        empresa.setPrivacyLegalBasis(request.legalBasis().trim());
        empresa.setPrivacyRetentionDays(request.retentionDays());
        empresa.setPrivacyNoticeVersion(request.noticeVersion().trim());
        empresa.setPrivacyNoticeHash(request.noticeHash().toLowerCase());
        empresa.setUpdatedAt(Instant.now());
        empresaRepository.save(empresa);

        auditEventService.auditAdminAction(
                currentUserService.requiredUserId(),
                empresa.getId(),
                AuditEventType.ADMIN_EMPRESA_UPDATED,
                "Configuração de privacidade da empresa atualizada.",
                json(Map.of("noticeVersion", empresa.getPrivacyNoticeVersion()))
        );
        return toResponse(empresa);
    }

    @Transactional
    public PrivacyConfigurationResponse approveHealthCompliance() {
        EmpresaEntity empresa = requireCurrentEmpresa();
        PrivacyConfigurationResponse configuration = toResponse(empresa);
        if (!configuration.readyForRealProcesses()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A configuração de privacidade deve estar completa antes da aprovação da vertical de saúde."
            );
        }
        String actorUserId = currentUserService.requiredUserId();
        Instant now = Instant.now();
        empresa.setHealthComplianceApprovedAt(now);
        empresa.setHealthComplianceApprovedBy(actorUserId);
        empresa.setUpdatedAt(now);
        empresaRepository.save(empresa);
        auditEventService.auditAdminAction(
                actorUserId,
                empresa.getId(),
                AuditEventType.HEALTH_COMPLIANCE_APPROVED,
                "Aprovação formal da vertical de saúde registrada.",
                json(Map.of("approvedAt", now.toString()))
        );
        return toResponse(empresa);
    }

    @Transactional(readOnly = true)
    public List<PrivacyIncidentResponse> listIncidents() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return privacyIncidentRepository.findByEmpresaIdOrderByDiscoveredAtDesc(empresaId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PrivacyIncidentResponse createIncident(PrivacyIncidentRequest request) {
        validateIncident(request);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        Instant now = Instant.now();
        Instant discoveredAt = request.discoveredAt() == null ? now : request.discoveredAt();

        PrivacyIncidentEntity entity = new PrivacyIncidentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEmpresaId(empresaId);
        entity.setSeverity(request.severity());
        entity.setStatus(PrivacyIncidentEntity.Status.OPEN);
        entity.setDiscoveredAt(discoveredAt);
        entity.setConfirmedAt(request.confirmedAt());
        entity.setDescription(request.description().trim());
        entity.setAffectedData(normalize(request.affectedData()));
        entity.setAffectedSubjectsEstimate(request.affectedSubjectsEstimate());
        entity.setRiskAssessment(normalize(request.riskAssessment()));
        entity.setOwnerUserId(actorUserId);
        entity.setRetentionUntil(discoveredAt.plus(5 * 366L, ChronoUnit.DAYS));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        privacyIncidentRepository.save(entity);

        auditEventService.auditAdminAction(
                actorUserId,
                empresaId,
                AuditEventType.PRIVACY_INCIDENT_RECORDED,
                "Incidente de privacidade registrado.",
                json(Map.of("incidentId", entity.getId(), "severity", entity.getSeverity().name()))
        );
        return toResponse(entity);
    }

    @Transactional
    public PrivacyIncidentResponse updateIncident(String incidentId, PrivacyIncidentUpdateRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actorUserId = currentUserService.requiredUserId();
        PrivacyIncidentEntity entity = privacyIncidentRepository.findByIdAndEmpresaId(incidentId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente não encontrado."));
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.confirmedAt() != null) {
            entity.setConfirmedAt(request.confirmedAt());
        }
        if (request.riskAssessment() != null) {
            entity.setRiskAssessment(normalize(request.riskAssessment()));
        }
        if (request.controllerNotifiedAt() != null) {
            entity.setControllerNotifiedAt(request.controllerNotifiedAt());
        }
        if (request.anpdNotifiedAt() != null) {
            entity.setAnpdNotifiedAt(request.anpdNotifiedAt());
        }
        if (request.subjectsNotifiedAt() != null) {
            entity.setSubjectsNotifiedAt(request.subjectsNotifiedAt());
        }
        entity.setOwnerUserId(actorUserId);
        entity.setUpdatedAt(Instant.now());
        privacyIncidentRepository.save(entity);

        auditEventService.auditAdminAction(
                actorUserId,
                empresaId,
                AuditEventType.PRIVACY_INCIDENT_UPDATED,
                "Incidente de privacidade atualizado.",
                json(Map.of("incidentId", entity.getId(), "status", entity.getStatus().name()))
        );
        return toResponse(entity);
    }

    private EmpresaEntity requireCurrentEmpresa() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));
    }

    private ComplianceRequestStatus requireStatus(ComplianceRequestStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A situação é obrigatória.");
        }
        return status;
    }

    private void validateConfiguration(PrivacyConfigurationRequest request) {
        if (request == null || isBlank(request.controllerName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do controlador é obrigatório.");
        }
        if (isBlank(request.serviceEmail()) && isBlank(request.serviceUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe e-mail ou URL do canal de privacidade.");
        }
        if (isBlank(request.legalBasis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A base legal documentada é obrigatória.");
        }
        if (request.retentionDays() == null || request.retentionDays() < 1 || request.retentionDays() > 3650) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A retenção deve ficar entre 1 e 3650 dias.");
        }
        if (isBlank(request.noticeVersion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A versão do aviso é obrigatória.");
        }
        if (isBlank(request.noticeHash()) || !SHA_256.matcher(request.noticeHash()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O hash do aviso deve ser SHA-256 hexadecimal.");
        }
    }

    private void validateIncident(PrivacyIncidentRequest request) {
        if (request == null || request.severity() == null || isBlank(request.description())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Severidade e descrição são obrigatórias.");
        }
        if (request.affectedSubjectsEstimate() != null && request.affectedSubjectsEstimate() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A estimativa de titulares não pode ser negativa.");
        }
    }

    private DataSubjectRequestResponse toResponse(DataSubjectRequestEntity entity) {
        return new DataSubjectRequestResponse(
                entity.getId(), entity.getAttemptId(), entity.getRequestType(), entity.getContact(), entity.getDetails(),
                entity.getStatus(), entity.getRequestedAt(), entity.getDueAt(), entity.getAssignedUserId(),
                entity.getResolution(), entity.getDenialReason(), entity.getRespondedAt(), entity.getClosedAt()
        );
    }

    private HumanReviewRequestResponse toResponse(HumanReviewRequestEntity entity) {
        return new HumanReviewRequestResponse(
                entity.getId(), entity.getAttemptId(), entity.getReason(), entity.getStatus(), entity.getRequestedAt(),
                entity.getDueAt(), entity.getAssignedUserId(), entity.getResolution(), entity.getResolvedAt()
        );
    }

    private PrivacyConfigurationResponse toResponse(EmpresaEntity empresa) {
        boolean channelConfigured = !isBlank(empresa.getPrivacyServiceEmail()) || !isBlank(empresa.getPrivacyServiceUrl());
        boolean ready = !isBlank(empresa.getPrivacyControllerName())
                && channelConfigured
                && !isBlank(empresa.getPrivacyLegalBasis())
                && empresa.getPrivacyRetentionDays() != null
                && empresa.getPrivacyRetentionDays() > 0
                && !isBlank(empresa.getPrivacyNoticeVersion())
                && empresa.getPrivacyNoticeHash() != null
                && SHA_256.matcher(empresa.getPrivacyNoticeHash()).matches();
        return new PrivacyConfigurationResponse(
                empresa.getPrivacyControllerName(), empresa.getPrivacyControllerTaxId(), empresa.getPrivacyServiceEmail(),
                empresa.getPrivacyServiceUrl(), empresa.getPrivacyDpoContact(), empresa.getPrivacyLegalBasis(),
                empresa.getPrivacyRetentionDays(), empresa.getPrivacyNoticeVersion(), empresa.getPrivacyNoticeHash(),
                ready, empresa.getHealthComplianceApprovedAt(), empresa.getHealthComplianceApprovedBy()
        );
    }

    private PrivacyIncidentResponse toResponse(PrivacyIncidentEntity entity) {
        return new PrivacyIncidentResponse(
                entity.getId(), entity.getSeverity(), entity.getStatus(), entity.getDiscoveredAt(), entity.getConfirmedAt(),
                entity.getDescription(), entity.getAffectedData(), entity.getAffectedSubjectsEstimate(),
                entity.getRiskAssessment(), entity.getOwnerUserId(), entity.getControllerNotifiedAt(),
                entity.getAnpdNotifiedAt(), entity.getSubjectsNotifiedAt(), entity.getRetentionUntil(), entity.getUpdatedAt()
        );
    }

    private String json(Map<String, ?> values) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(values));
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar auditoria.", exception);
        }
    }

    private String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DataSubjectRequestResponse(
            String id,
            String attemptId,
            br.com.iforce.praxis.candidate.dto.DataSubjectRequestType requestType,
            String contact,
            String details,
            ComplianceRequestStatus status,
            Instant requestedAt,
            Instant dueAt,
            String assignedUserId,
            String resolution,
            String denialReason,
            Instant respondedAt,
            Instant closedAt
    ) {
    }

    public record ResolveDataSubjectRequestRequest(
            ComplianceRequestStatus status,
            String resolution,
            String denialReason,
            Boolean executeAnonymization
    ) {
    }

    public record HumanReviewRequestResponse(
            String id,
            String attemptId,
            String reason,
            ComplianceRequestStatus status,
            Instant requestedAt,
            Instant dueAt,
            String assignedUserId,
            String resolution,
            Instant resolvedAt
    ) {
    }

    public record ResolveHumanReviewRequest(String resolution) {
    }

    public record PrivacyConfigurationRequest(
            String controllerName,
            String controllerTaxId,
            String serviceEmail,
            String serviceUrl,
            String dpoContact,
            String legalBasis,
            Integer retentionDays,
            String noticeVersion,
            String noticeHash
    ) {
    }

    public record PrivacyConfigurationResponse(
            String controllerName,
            String controllerTaxId,
            String serviceEmail,
            String serviceUrl,
            String dpoContact,
            String legalBasis,
            Integer retentionDays,
            String noticeVersion,
            String noticeHash,
            boolean readyForRealProcesses,
            Instant healthComplianceApprovedAt,
            String healthComplianceApprovedBy
    ) {
    }

    public record PrivacyIncidentRequest(
            PrivacyIncidentEntity.Severity severity,
            Instant discoveredAt,
            Instant confirmedAt,
            String description,
            String affectedData,
            Integer affectedSubjectsEstimate,
            String riskAssessment
    ) {
    }

    public record PrivacyIncidentUpdateRequest(
            PrivacyIncidentEntity.Status status,
            Instant confirmedAt,
            String riskAssessment,
            Instant controllerNotifiedAt,
            Instant anpdNotifiedAt,
            Instant subjectsNotifiedAt
    ) {
    }

    public record PrivacyIncidentResponse(
            String id,
            PrivacyIncidentEntity.Severity severity,
            PrivacyIncidentEntity.Status status,
            Instant discoveredAt,
            Instant confirmedAt,
            String description,
            String affectedData,
            Integer affectedSubjectsEstimate,
            String riskAssessment,
            String ownerUserId,
            Instant controllerNotifiedAt,
            Instant anpdNotifiedAt,
            Instant subjectsNotifiedAt,
            Instant retentionUntil,
            Instant updatedAt
    ) {
    }
}
