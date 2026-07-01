package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.candidate.dto.ReviewRequest;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.util.LinkedHashMap;

import java.util.Map;


/**
 * Registra o pedido de revisão humana feito pelo candidato (REQ-L5 / LGPD art. 20). O candidato
 * acessa pelo próprio link (rota pública, sem empresa no contexto): resolvemos o empresa a partir da
 * tentativa e gravamos o pedido na trilha append-only, de onde o recrutador o vê (e ele entra no
 * relatório de evidência). A revisão em si é a decisão humana já garantida pelo REQ-L1.
 */
@Service
public class CandidateReviewRequestService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public CandidateReviewRequestService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra o pedido do candidato para que um humano revise o resultado.
     *
     * <p>Fluxo do processo: como o candidato acessa por um link público (sem
     * empresa no contexto), o sistema descobre a empresa a partir da
     * participação e grava o pedido na trilha de auditoria, de onde o
     * recrutador o vê. É o direito de revisão por pessoa natural previsto na
     * LGPD (art. 20).</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request justificativa opcional do pedido (pode ser nulo)
     */
    @Transactional
    public void register(String attemptId, ReviewRequest request) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        Instant requestedAt = Instant.now();
        String reason = normalizeReason(request == null ? null : request.reason());

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attemptId,
                AuditEventType.REVIEW_REQUESTED,
                "Revisão humana solicitada pelo candidato.",
                buildMetadata(attemptId, reason, requestedAt)
        );
    }

    /** Limpa a justificativa informada (remove espaços e trata texto vazio). Uso interno. */
    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Monta o detalhamento (em JSON) do pedido de revisão que será guardado
     * na trilha de auditoria. Uso interno.
     */
    private String buildMetadata(String attemptId, String reason, Instant requestedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("reason", reason);
        payload.put("requestedAt", requestedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o pedido de revisão.", exception);
        }
    }
}
