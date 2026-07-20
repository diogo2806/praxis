package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.candidate.service.CandidateAttemptTokenResolver;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.persistence.entity.CandidateNoticeAcceptanceEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.CandidateNoticeAcceptanceRepository;
import br.com.iforce.praxis.shared.security.Sha256;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CandidatePrivacyNoticeService {

    private final CandidateAttemptTokenResolver tokenResolver;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EmpresaRepository empresaRepository;
    private final CandidateNoticeAcceptanceRepository acceptanceRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final int defaultRetentionDays;
    private final String fallbackControllerName;
    private final String fallbackServiceEmail;
    private final String fallbackServiceUrl;
    private final String fallbackDpoContact;
    private final String fallbackLegalBasis;
    private final String fallbackNoticeVersion;
    private final boolean enforceReadiness;

    public CandidatePrivacyNoticeService(
            CandidateAttemptTokenResolver tokenResolver,
            CandidateAttemptRepository candidateAttemptRepository,
            EmpresaRepository empresaRepository,
            CandidateNoticeAcceptanceRepository acceptanceRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            @Value("${praxis.privacy-retention-days:180}") int defaultRetentionDays,
            @Value("${praxis.privacy.controller-name:Empresa responsável pelo processo seletivo}") String fallbackControllerName,
            @Value("${praxis.privacy.service-email:}") String fallbackServiceEmail,
            @Value("${praxis.privacy.service-url:}") String fallbackServiceUrl,
            @Value("${praxis.privacy.dpo-contact:}") String fallbackDpoContact,
            @Value("${praxis.privacy.legal-basis:Base legal definida e documentada pelo controlador}") String fallbackLegalBasis,
            @Value("${praxis.privacy.notice-version:2026-07-20}") String fallbackNoticeVersion,
            @Value("${praxis.privacy.enforce-readiness:false}") boolean enforceReadiness
    ) {
        this.tokenResolver = tokenResolver;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.empresaRepository = empresaRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.defaultRetentionDays = defaultRetentionDays;
        this.fallbackControllerName = fallbackControllerName;
        this.fallbackServiceEmail = fallbackServiceEmail;
        this.fallbackServiceUrl = fallbackServiceUrl;
        this.fallbackDpoContact = fallbackDpoContact;
        this.fallbackLegalBasis = fallbackLegalBasis;
        this.fallbackNoticeVersion = fallbackNoticeVersion;
        this.enforceReadiness = enforceReadiness;
    }

    @Transactional(readOnly = true)
    public CandidatePrivacyNoticeResponse getNotice(String attemptToken) {
        ResolvedContext context = resolve(attemptToken);
        return noticeFor(context.empresa());
    }

    @Transactional
    public void acknowledge(String attemptToken, CandidatePrivacyNoticeAcknowledgementRequest request) {
        if (request == null || isBlank(request.language())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O idioma do aviso é obrigatório.");
        }
        ResolvedContext context = resolve(attemptToken);
        CandidatePrivacyNoticeResponse notice = noticeFor(context.empresa());
        assertReadiness(notice);
        if (!notice.noticeVersion().equals(request.noticeVersion()) || !notice.noticeHash().equals(request.noticeHash())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O aviso de privacidade mudou. Recarregue a página antes de continuar."
            );
        }

        Instant now = Instant.now();
        acceptanceRepository.findByAttemptIdAndNoticeVersion(context.attempt().getId(), notice.noticeVersion())
                .orElseGet(() -> {
                    CandidateNoticeAcceptanceEntity entity = new CandidateNoticeAcceptanceEntity();
                    entity.setEmpresaId(context.attempt().getEmpresaId());
                    entity.setAttemptId(context.attempt().getId());
                    entity.setNoticeVersion(notice.noticeVersion());
                    entity.setNoticeLanguage(request.language().trim());
                    entity.setNoticeHash(notice.noticeHash());
                    entity.setAcknowledgedAt(now);
                    entity.setCreatedAt(now);
                    return acceptanceRepository.save(entity);
                });

        context.attempt().setPrivacyNoticeAcknowledgedAt(now);
        context.attempt().setPrivacyNoticeVersion(notice.noticeVersion());
        context.attempt().setPrivacyNoticeLanguage(request.language().trim());
        context.attempt().setPrivacyNoticeHash(notice.noticeHash());
        candidateAttemptRepository.save(context.attempt());

        auditEventService.appendCandidateAttemptEvent(
                context.attempt().getEmpresaId(),
                context.attempt().getId(),
                AuditEventType.PRIVACY_NOTICE_ACKNOWLEDGED,
                "Ciência do aviso de privacidade registrada antes do início da avaliação.",
                metadata(notice, request.language(), now)
        );
    }

    @Transactional(readOnly = true)
    public void assertAcknowledged(String attemptToken) {
        ResolvedContext context = resolve(attemptToken);
        CandidatePrivacyNoticeResponse notice = noticeFor(context.empresa());
        assertReadiness(notice);
        boolean accepted = acceptanceRepository
                .findByAttemptIdAndNoticeVersion(context.attempt().getId(), notice.noticeVersion())
                .filter(value -> notice.noticeHash().equals(value.getNoticeHash()))
                .isPresent();
        if (!accepted) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "Registre a ciência do aviso de privacidade antes de iniciar a avaliação."
            );
        }
    }

    private ResolvedContext resolve(String attemptToken) {
        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = tokenResolver.resolve(attemptToken);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(resolved.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
        if (resolved.empresaId() != null && !resolved.empresaId().equals(attempt.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token não pertence a esta participação.");
        }
        EmpresaEntity empresa = empresaRepository.findById(attempt.getEmpresaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa responsável não encontrada."));
        return new ResolvedContext(attempt, empresa);
    }

    private CandidatePrivacyNoticeResponse noticeFor(EmpresaEntity empresa) {
        String controllerName = valueOrFallback(empresa.getPrivacyControllerName(), fallbackControllerName);
        String controllerTaxId = normalize(empresa.getPrivacyControllerTaxId());
        String serviceEmail = valueOrFallback(empresa.getPrivacyServiceEmail(), fallbackServiceEmail);
        String serviceUrl = valueOrFallback(empresa.getPrivacyServiceUrl(), fallbackServiceUrl);
        String dpoContact = valueOrFallback(empresa.getPrivacyDpoContact(), fallbackDpoContact);
        String legalBasis = valueOrFallback(empresa.getPrivacyLegalBasis(), fallbackLegalBasis);
        int retentionDays = empresa.getPrivacyRetentionDays() == null
                ? defaultRetentionDays
                : empresa.getPrivacyRetentionDays();
        String version = valueOrFallback(empresa.getPrivacyNoticeVersion(), fallbackNoticeVersion);
        boolean configured = !isBlank(empresa.getPrivacyControllerName())
                && (!isBlank(empresa.getPrivacyServiceEmail()) || !isBlank(empresa.getPrivacyServiceUrl()))
                && !isBlank(empresa.getPrivacyLegalBasis())
                && empresa.getPrivacyRetentionDays() != null
                && !isBlank(empresa.getPrivacyNoticeVersion())
                && !isBlank(empresa.getPrivacyNoticeHash());
        String hash = configured
                ? empresa.getPrivacyNoticeHash().toLowerCase()
                : Sha256.hex(controllerName + "|" + controllerTaxId + "|" + serviceEmail + "|" + serviceUrl
                        + "|" + dpoContact + "|" + legalBasis + "|" + retentionDays + "|" + version);
        return new CandidatePrivacyNoticeResponse(
                controllerName,
                controllerTaxId,
                serviceEmail,
                serviceUrl,
                dpoContact,
                legalBasis,
                retentionDays,
                version,
                hash,
                configured
        );
    }

    private void assertReadiness(CandidatePrivacyNoticeResponse notice) {
        if (enforceReadiness && !notice.configured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A empresa responsável ainda não configurou o canal e o aviso de privacidade."
            );
        }
    }

    private String metadata(CandidatePrivacyNoticeResponse notice, String language, Instant acknowledgedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("noticeVersion", notice.noticeVersion());
        payload.put("noticeHash", notice.noticeHash());
        payload.put("language", language.trim());
        payload.put("acknowledgedAt", acknowledgedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar a ciência.", exception);
        }
    }

    private String valueOrFallback(String value, String fallback) {
        return isBlank(value) ? normalize(fallback) : value.trim();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResolvedContext(CandidateAttemptEntity attempt, EmpresaEntity empresa) {
    }

    public record CandidatePrivacyNoticeResponse(
            String controllerName,
            String controllerTaxId,
            String serviceEmail,
            String serviceUrl,
            String dpoContact,
            String legalBasis,
            int retentionDays,
            String noticeVersion,
            String noticeHash,
            boolean configured
    ) {
    }

    public record CandidatePrivacyNoticeAcknowledgementRequest(
            String noticeVersion,
            String noticeHash,
            String language
    ) {
    }
}
