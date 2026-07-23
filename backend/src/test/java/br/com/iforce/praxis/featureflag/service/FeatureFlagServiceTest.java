package br.com.iforce.praxis.featureflag.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.UpsertRequest;
import br.com.iforce.praxis.featureflag.persistence.entity.FeatureFlagEntity;
import br.com.iforce.praxis.featureflag.persistence.repository.FeatureFlagMetricRepository;
import br.com.iforce.praxis.featureflag.persistence.repository.FeatureFlagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository featureFlagRepository;
    @Mock
    private FeatureFlagMetricRepository metricRepository;
    @Mock
    private AuditEventService auditEventService;

    private FeatureFlagService service;

    @BeforeEach
    void setUp() {
        service = new FeatureFlagService(
                featureFlagRepository,
                metricRepository,
                auditEventService,
                new ObjectMapper(),
                "test"
        );
    }

    @Test
    void keepsPercentageRolloutStableForSameIdentifier() {
        FeatureFlagEntity flag = flag("flag-1", "nova.experiencia", 37);
        when(featureFlagRepository.findById("flag-1")).thenReturn(Optional.of(flag));
        EvaluationRequest context = new EvaluationRequest(
                "empresa-1",
                "enterprise",
                "user-1",
                Set.of("TEAM_MANAGER"),
                "test",
                "candidate-42"
        );

        EvaluationResponse first = service.evaluate("flag-1", context);
        EvaluationResponse second = service.evaluate("flag-1", context);

        assertThat(first.rolloutBucket()).isBetween(0, 9_999);
        assertThat(second.rolloutBucket()).isEqualTo(first.rolloutBucket());
        assertThat(second.enabled()).isEqualTo(first.enabled());
        assertThat(second.reason()).isEqualTo("ROLLOUT");
    }

    @Test
    void killSwitchAlwaysDisablesFeature() {
        FeatureFlagEntity flag = flag("flag-2", "integracao.nova", 100);
        flag.setKillSwitch(true);
        when(featureFlagRepository.findById("flag-2")).thenReturn(Optional.of(flag));

        EvaluationResponse result = service.evaluate("flag-2", new EvaluationRequest(
                "empresa-piloto",
                null,
                "user-1",
                Set.of("ADMIN"),
                "test",
                "stable"
        ));

        assertThat(result.enabled()).isFalse();
        assertThat(result.reason()).isEqualTo("KILL_SWITCH");
    }

    @Test
    void refusesFlagsThatCanChangeScoring() {
        UpsertRequest request = new UpsertRequest(
                "scoring.experimental",
                "Não deve ser aceita",
                "produto",
                false,
                null,
                true,
                false,
                false,
                true,
                Instant.now().plus(30, ChronoUnit.DAYS),
                "Remover após o piloto.",
                Set.of("test"),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                10,
                true
        );

        assertThatThrownBy(() -> service.create("admin-1", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pontuação");
    }

    private static FeatureFlagEntity flag(String id, String key, int rolloutPercentage) {
        FeatureFlagEntity flag = new FeatureFlagEntity();
        flag.setId(id);
        flag.setKey(key);
        flag.setDescription("Flag de teste");
        flag.setOwner("produto");
        flag.setActive(true);
        flag.setDefaultEnabled(false);
        flag.setRolloutPercentage(rolloutPercentage);
        flag.setCreatedBy("admin");
        flag.setUpdatedBy("admin");
        flag.setCreatedAt(Instant.now());
        flag.setUpdatedAt(Instant.now());
        return flag;
    }
}
