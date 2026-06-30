package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;

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
 * Registra o consentimento do participante (paciente) para tratamento de dado sensível de saúde na
 * vertical educativa (Minuta A — LGPD, arts. 11 e 14). Rota pública: o participante acessa pelo
 * próprio link, sem empresa no contexto; resolvemos o empresa a partir da tentativa e gravamos o
 * consentimento na trilha append-only, capturando o que foi exibido, quando e por quem.
 */
@Service
public class CandidateHealthConsentService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public CandidateHealthConsentService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra o consentimento de saúde dado pelo próprio participante.
     *
     * <p>Fluxo do processo: como o participante acessa por um link público
     * (sem empresa no contexto), o sistema descobre a empresa a partir da
     * participação e grava na trilha de auditoria o consentimento — incluindo
     * a versão do aviso exibido e o momento. Isso atende às exigências da
     * LGPD (arts. 11 e 14) antes de iniciar a atividade.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request dados do consentimento (versão do aviso, se é em nome de menor, etc.)
     */
    @Transactional
    public void register(String attemptId, HealthConsentRequest request) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        Instant consentedAt = Instant.now();

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attemptId,
                AuditEventType.HEALTH_CONSENT_RECORDED,
                "Consentimento de saúde registrado pelo participante.",
                buildMetadata(attemptId, request, consentedAt)
        );
    }

    /**
     * Monta o detalhamento (em JSON) do consentimento que será guardado na
     * trilha de auditoria. Uso interno.
     */
    private String buildMetadata(String attemptId, HealthConsentRequest request, Instant consentedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("noticeVersion", request.version());
        payload.put("onBehalfOfMinor", request.onBehalfOfMinor());
        payload.put("consentedAt", consentedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o consentimento.", exception);
        }
    }
}
