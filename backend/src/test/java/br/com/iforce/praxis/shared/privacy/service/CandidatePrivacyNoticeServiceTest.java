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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatePrivacyNoticeServiceTest {

    private static final String NOTICE_HASH = "a".repeat(64);
    private static final String TERMS_HASH =
            "dd1a4872abd133eddde7e14d635429025d4786b06cb39c07c8c6c0378de86037";

    @Mock
    private CandidateAttemptTokenResolver tokenResolver;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CandidateNoticeAcceptanceRepository acceptanceRepository;
    @Mock
    private AuditEventService auditEventService;

    private CandidatePrivacyNoticeService service;

    @BeforeEach
    void setUp() {
        service = new CandidatePrivacyNoticeService(
                tokenResolver,
                candidateAttemptRepository,
                empresaRepository,
                acceptanceRepository,
                auditEventService,
                new ObjectMapper(),
                180,
                "Empresa responsável",
                "privacidade@empresa.com",
                "",
                "",
                "Execução do processo seletivo",
                "2026-07-20",
                true,
                true,
                "1.1",
                TERMS_HASH
        );
    }

    @Test
    void recordsTermsAcceptanceAndPrivacyAcknowledgementTogether() {
        CandidateAttemptEntity attempt = attempt();
        EmpresaEntity empresa = configuredEmpresa();
        when(tokenResolver.resolve("token-1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", "attempt-1"));
        when(candidateAttemptRepository.findById("attempt-1")).thenReturn(Optional.of(attempt));
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));
        when(acceptanceRepository.findByAttemptIdAndNoticeVersionAndTermsVersion(
                "attempt-1", "2026-07-20", "1.1"))
                .thenReturn(Optional.empty());
        when(acceptanceRepository.save(any(CandidateNoticeAcceptanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.acknowledge(
                "token-1",
                new CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest(
                        "2026-07-20",
                        NOTICE_HASH,
                        "1.1",
                        TERMS_HASH,
                        true,
                        true,
                        "pt-BR"
                )
        );

        ArgumentCaptor<CandidateNoticeAcceptanceEntity> acceptanceCaptor =
                ArgumentCaptor.forClass(CandidateNoticeAcceptanceEntity.class);
        verify(acceptanceRepository).save(acceptanceCaptor.capture());
        CandidateNoticeAcceptanceEntity acceptance = acceptanceCaptor.getValue();
        assertThat(acceptance.getNoticeVersion()).isEqualTo("2026-07-20");
        assertThat(acceptance.getNoticeHash()).isEqualTo(NOTICE_HASH);
        assertThat(acceptance.getTermsVersion()).isEqualTo("1.1");
        assertThat(acceptance.getTermsHash()).isEqualTo(TERMS_HASH);
        assertThat(acceptance.getAcknowledgedAt()).isNotNull();
        assertThat(acceptance.getTermsAcceptedAt()).isEqualTo(acceptance.getAcknowledgedAt());

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-1"),
                eq("attempt-1"),
                eq(AuditEventType.PRIVACY_NOTICE_ACKNOWLEDGED),
                anyString(),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getValue())
                .contains("\"noticeVersion\":\"2026-07-20\"")
                .contains("\"termsVersion\":\"1.1\"")
                .contains("\"termsHash\":\"" + TERMS_HASH + "\"");
    }

    @Test
    void rejectsRequestWithoutExplicitTermsAcceptance() {
        assertThatThrownBy(() -> service.acknowledge(
                "token-1",
                new CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest(
                        "2026-07-20",
                        NOTICE_HASH,
                        "1.1",
                        TERMS_HASH,
                        true,
                        false,
                        "pt-BR"
                )
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400")
                .hasMessageContaining("Termos de Uso");

        verifyNoInteractions(
                tokenResolver,
                candidateAttemptRepository,
                empresaRepository,
                acceptanceRepository,
                auditEventService
        );
    }

    @Test
    void blocksAssessmentWhenCombinedAcceptanceIsMissing() {
        CandidateAttemptEntity attempt = attempt();
        EmpresaEntity empresa = configuredEmpresa();
        when(tokenResolver.resolve("token-1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", "attempt-1"));
        when(candidateAttemptRepository.findById("attempt-1")).thenReturn(Optional.of(attempt));
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));
        when(acceptanceRepository.findByAttemptIdAndNoticeVersionAndTermsVersion(
                "attempt-1", "2026-07-20", "1.1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertAcknowledged("token-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("428")
                .hasMessageContaining("Termos de Uso");
    }

    private CandidateAttemptEntity attempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("attempt-1");
        attempt.setEmpresaId("empresa-1");
        return attempt;
    }

    private EmpresaEntity configuredEmpresa() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("empresa-1");
        empresa.setPrivacyControllerName("Empresa responsável");
        empresa.setPrivacyServiceEmail("privacidade@empresa.com");
        empresa.setPrivacyLegalBasis("Execução do processo seletivo");
        empresa.setPrivacyRetentionDays(180);
        empresa.setPrivacyNoticeVersion("2026-07-20");
        empresa.setPrivacyNoticeHash(NOTICE_HASH);
        return empresa;
    }
}
