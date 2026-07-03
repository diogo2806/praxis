package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;

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
 * Registra o exercício de direitos do titular feito pelo candidato (LGPD art. 18).
 *
 * <p>O candidato acessa pela própria participação (rota pública, sem empresa no
 * contexto): resolvemos a empresa a partir da tentativa e gravamos o pedido na
 * trilha append-only, de onde o controlador (empresa responsável) o atende no
 * prazo legal. O Práxis atua como operador; a decisão sobre os dados (acesso,
 * correção, eliminação, portabilidade, revogação) é do controlador, que detém a
 * base legal. O registro dá ao titular um canal in-product auditável.</p>
 */
@Service
public class CandidateDataRequestService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public CandidateDataRequestService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra a requisição de um direito do titular feita pelo candidato.
     *
     * <p>Fluxo do processo: como o candidato acessa por um link público (sem
     * empresa no contexto), o sistema descobre a empresa a partir da
     * participação e grava o pedido na trilha de auditoria, de onde o
     * controlador o vê e o atende no prazo legal (LGPD art. 18).</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request direito solicitado e dados opcionais de contato/detalhe
     */
    @Transactional
    public void register(String attemptId, DataSubjectRequest request) {
        if (request == null || request.requestType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de requisição do titular é obrigatório.");
        }

        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        Instant requestedAt = Instant.now();

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attemptId,
                AuditEventType.DATA_SUBJECT_REQUEST,
                "Direito do titular solicitado pelo candidato (LGPD art. 18).",
                buildMetadata(attemptId, request, requestedAt)
        );
    }

    /**
     * Monta o detalhamento (em JSON) do pedido do titular que será guardado na
     * trilha de auditoria. Uso interno.
     */
    private String buildMetadata(String attemptId, DataSubjectRequest request, Instant requestedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("requestType", request.requestType().getDescricao());
        payload.put("contact", normalize(request.contact()));
        payload.put("details", normalize(request.details()));
        payload.put("requestedAt", requestedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o pedido do titular.", exception);
        }
    }

    /** Limpa o texto informado (remove espaços e trata texto vazio). Uso interno. */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
