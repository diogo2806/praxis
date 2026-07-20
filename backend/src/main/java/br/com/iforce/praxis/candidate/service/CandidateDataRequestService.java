package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.DataSubjectRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.DataSubjectRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
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

/** Registra e acompanha o exercício de direitos do titular (LGPD art. 18). */
@Service
public class CandidateDataRequestService {

    private static final List<ComplianceRequestStatus> OPEN_STATUSES = List.of(
            ComplianceRequestStatus.PENDING,
            ComplianceRequestStatus.IN_PROGRESS
    );

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final DataSubjectRequestRepository dataSubjectRequestRepository;
    private final CandidateAttemptTokenResolver tokenResolver;
    private final int deadlineDays;

    public CandidateDataRequestService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            DataSubjectRequestRepository dataSubjectRequestRepository,
            CandidateAttemptTokenResolver tokenResolver,
            @Value("${praxis.privacy-request-deadline-days:15}") int deadlineDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.dataSubjectRequestRepository = dataSubjectRequestRepository;
        this.tokenResolver = tokenResolver;
        this.deadlineDays = Math.max(1, deadlineDays);
    }

    @Transactional
    public String register(String attemptToken, DataSubjectRequest request) {
        if (request == null || request.requestType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de requisição do titular é obrigatório.");
        }

        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = tokenResolver.resolve(attemptToken);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(resolved.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
        assertTokenEmpresa(resolved, attempt);

        if (dataSubjectRequestRepository.existsByAttemptIdAndRequestTypeAndStatusIn(
                attempt.getId(),
                request.requestType(),
                OPEN_STATUSES
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe uma solicitação aberta deste tipo para esta participação."
            );
        }

        Instant requestedAt = Instant.now();
        DataSubjectRequestEntity entity = new DataSubjectRequestEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEmpresaId(attempt.getEmpresaId());
        entity.setAttemptId(attempt.getId());
        entity.setRequestType(request.requestType());
        entity.setContact(normalize(request.contact()));
        entity.setDetails(normalize(request.details()));
        entity.setStatus(ComplianceRequestStatus.PENDING);
        entity.setRequestedAt(requestedAt);
        entity.setDueAt(requestedAt.plus(deadlineDays, ChronoUnit.DAYS));
        entity.setCreatedAt(requestedAt);
        entity.setUpdatedAt(requestedAt);
        dataSubjectRequestRepository.save(entity);

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.DATA_SUBJECT_REQUEST,
                "Direito do titular solicitado pelo candidato (LGPD art. 18).",
                buildMetadata(entity)
        );
        return entity.getId();
    }

    private void assertTokenEmpresa(
            CandidateAttemptTokenResolver.ResolvedAttemptToken resolved,
            CandidateAttemptEntity attempt
    ) {
        if (resolved.empresaId() != null && !resolved.empresaId().equals(attempt.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token não pertence a esta participação.");
        }
    }

    private String buildMetadata(DataSubjectRequestEntity entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", entity.getId());
        payload.put("attemptId", entity.getAttemptId());
        payload.put("source", "candidate");
        payload.put("requestType", entity.getRequestType().getDescricao());
        payload.put("status", entity.getStatus().name());
        payload.put("requestedAt", entity.getRequestedAt().toString());
        payload.put("dueAt", entity.getDueAt().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao registrar o pedido do titular.",
                    exception
            );
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
