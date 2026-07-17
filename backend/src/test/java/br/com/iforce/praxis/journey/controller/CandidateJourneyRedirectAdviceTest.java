package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import br.com.iforce.praxis.journey.persistence.repository.JourneyRedirectTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateJourneyRedirectAdviceTest {

    @Mock private JwtService jwtService;
    @Mock private AssessmentJourneyAttemptRepository journeyAttemptRepository;
    @Mock private ServerHttpRequest request;

    private CandidateJourneyRedirectAdvice advice;

    @BeforeEach
    void setUp() {
        PraxisProperties properties = new PraxisProperties(
                "https://api.praxis.test",
                "https://praxis.test/",
                168,
                8,
                720,
                70,
                15,
                0.001
        );
        advice = new CandidateJourneyRedirectAdvice(jwtService, properties, journeyAttemptRepository);
        EmpresaContextHolder.set("empresa-1");
    }

    @AfterEach
    void tearDown() {
        EmpresaContextHolder.clear();
    }

    @Test
    void addsJourneyRedirectToCompletedCandidateAttemptUsingMaterializedTarget() {
        when(request.getURI()).thenReturn(
                URI.create("https://api.praxis.test/candidate/attempts/token-1/answers")
        );
        when(jwtService.parseCandidateAttemptToken("token-1"))
                .thenReturn(new JwtService.CandidateAttemptToken("empresa-1", "att_1"));
        when(journeyAttemptRepository.findJourneyRedirectTarget("empresa-1", "att_1"))
                .thenReturn(Optional.of(new JourneyRedirectTarget("jatt_1", 42L)));

        RegistrarRespostaResponse response = completedResponse(null);

        RegistrarRespostaResponse enriched = write(response);

        assertThat(enriched.redirectUrl())
                .isEqualTo("https://praxis.test/jornada/jatt_1?completedStepId=42");
    }

    @Test
    void preservesCompletedResponseWhenJourneyTargetDoesNotExist() {
        when(request.getURI()).thenReturn(
                URI.create("https://api.praxis.test/candidate/attempts/token-1/answers")
        );
        when(jwtService.parseCandidateAttemptToken("token-1"))
                .thenReturn(new JwtService.CandidateAttemptToken("empresa-1", "att_1"));
        when(journeyAttemptRepository.findJourneyRedirectTarget("empresa-1", "att_1"))
                .thenReturn(Optional.empty());

        RegistrarRespostaResponse enriched = write(completedResponse(null));

        assertThat(enriched.redirectUrl()).isNull();
    }

    @Test
    void preservesExistingProviderCallback() {
        RegistrarRespostaResponse enriched = write(completedResponse("https://gupy.test/callback"));

        assertThat(enriched.redirectUrl()).isEqualTo("https://gupy.test/callback");
    }

    private RegistrarRespostaResponse completedResponse(String redirectUrl) {
        return new RegistrarRespostaResponse(
                "pub_attempt",
                "concluida",
                false,
                true,
                redirectUrl,
                null,
                null
        );
    }

    private RegistrarRespostaResponse write(RegistrarRespostaResponse response) {
        return (RegistrarRespostaResponse) advice.beforeBodyWrite(
                response,
                null,
                null,
                null,
                request,
                null
        );
    }
}
