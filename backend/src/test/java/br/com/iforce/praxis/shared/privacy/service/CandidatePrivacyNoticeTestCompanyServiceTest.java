package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatePrivacyNoticeTestCompanyServiceTest {

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
    void allowsFallbackNoticeAcknowledgementForTestCompany() {
        CandidateAttemptEntity attempt = attempt();
        EmpresaEntity empresa = empresa(EmpresaStatus.EM_TESTE);
        mockContext(attempt, empresa);

        CandidatePrivacyNoticeService.CandidatePrivacyNoticeResponse notice = service.getNotice("token-1");
        assertThat(notice.configured()).isFalse();

        when(acceptanceRepository.findByAttemptIdAndNoticeVersionAndTermsVersion(
                "attempt-1", notice.noticeVersion(), notice.termsVersion()))
                .thenReturn(Optional.empty());
        when(acceptanceRepository.save(any(CandidateNoticeAcceptanceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest request = request(notice);

        assertThatCode(() -> service.acknowledge("token-1", request))
                .doesNotThrowAnyException();

        verify(acceptanceRepository).save(any(CandidateNoticeAcceptanceEntity.class));
        verify(candidateAttemptRepository).save(attempt);
    }

    @Test
    void keepsBlockingFallbackNoticeForActiveCompany() {
        CandidateAttemptEntity attempt = attempt();
        EmpresaEntity empresa = empresa(EmpresaStatus.ATIVO);
        mockContext(attempt, empresa);

        CandidatePrivacyNoticeService.CandidatePrivacyNoticeResponse notice = service.getNotice("token-1");

        assertThatThrownBy(() -> service.acknowledge("token-1", request(notice)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503")
                .hasMessageContaining("aviso de privacidade");

        verify(acceptanceRepository, never()).save(any(CandidateNoticeAcceptanceEntity.class));
        verify(candidateAttemptRepository, never()).save(any(CandidateAttemptEntity.class));
    }

    private void mockContext(CandidateAttemptEntity attempt, EmpresaEntity empresa) {
        when(tokenResolver.resolve("token-1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-1", "attempt-1"));
        when(candidateAttemptRepository.findById("attempt-1")).thenReturn(Optional.of(attempt));
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));
    }

    private CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest request(
            CandidatePrivacyNoticeService.CandidatePrivacyNoticeResponse notice
    ) {
        return new CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest(
                notice.noticeVersion(),
                notice.noticeHash(),
                notice.termsVersion(),
                notice.termsHash(),
                true,
                true,
                "pt-BR"
        );
    }

    private CandidateAttemptEntity attempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("attempt-1");
        attempt.setEmpresaId("empresa-1");
        return attempt;
    }

    private EmpresaEntity empresa(EmpresaStatus status) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("empresa-1");
        empresa.setStatus(status);
        return empresa;
    }
}
