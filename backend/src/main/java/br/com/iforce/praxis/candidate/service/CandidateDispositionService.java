package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;

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
 * Registra a decisão humana sobre um candidato (REQ-L1). É a defesa nº 1 contra a teoria de
 * agência: transforma "o algoritmo rejeitou" em "fulano decidiu em tal data, com tal
 * justificativa, tendo o score apenas como apoio". A decisão entra na trilha append-only já
 * existente, sob o agregado da tentativa, de modo a compor o relatório de evidência (REQ-L4).
 */
@Service
public class CandidateDispositionService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public CandidateDispositionService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra a decisão humana final tomada sobre o candidato.
     *
     * <p>Fluxo do processo: confere se a participação existe na empresa
     * atual, identifica quem está decidindo e grava na trilha de auditoria
     * (que nunca é apagada) a decisão tomada, a justificativa e o momento.
     * Assim, a responsabilidade fica registrada em nome de uma pessoa, e não
     * "do algoritmo" — a pontuação é apenas apoio.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request a decisão tomada e a justificativa
     */
    @Transactional
    public void register(String attemptId, RegisterDispositionRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();

        candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));

        Instant decidedAt = Instant.now();
        String reason = normalizeReason(request.reason());

        String message = "Decisão humana registrada: " + request.decision().name() + " por usuário " + userId;
        String metadata = buildMetadata(attemptId, userId, request, reason, decidedAt);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attemptId,
                AuditEventType.HUMAN_DECISION,
                message,
                metadata
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
     * Monta o detalhamento (em JSON) da decisão que será guardado na trilha
     * de auditoria: quem decidiu, qual decisão, justificativa e quando. Uso interno.
     */
    private String buildMetadata(
            String attemptId,
            String userId,
            RegisterDispositionRequest request,
            String reason,
            Instant decidedAt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("decidedByUserId", userId);
        payload.put("decision", request.decision().name());
        payload.put("reason", reason);
        payload.put("decidedAt", decidedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar a decisão.", exception);
        }
    }
}
