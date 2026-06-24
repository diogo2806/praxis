package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;
import br.com.iforce.praxis.tenantconfig.dto.TenantConfigResponse;
import br.com.iforce.praxis.tenantconfig.dto.UpdateTenantConfigRequest;
import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.persistence.entity.TenantConfigOptionEntity;
import br.com.iforce.praxis.tenantconfig.persistence.repository.TenantConfigOptionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantConfigServiceTest {

    private final TenantConfigOptionRepository repository = mock(TenantConfigOptionRepository.class);
    private final CurrentTenantService currentTenantService = mock(CurrentTenantService.class);
    private final TenantConfigService service = new TenantConfigService(repository, currentTenantService);

    @Test
    void getConfigFallsBackToActiveDefaultsWhenTenantHasNoRows() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(repository.findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("tenant-1"), any()))
                .thenReturn(List.of());

        TenantConfigResponse config = service.getConfig();

        assertThat(config.competencies()).extracting(ConfigOptionDto::value)
                .containsExactly(
                        "Empatia",
                        "Resolução de Conflitos",
                        "Aderência à Política",
                        "Comunicação",
                        "Negociação",
                        "Tomada de Decisão",
                        "Liderança",
                        "Proatividade"
                );
        assertThat(config.answerTimeLimits()).filteredOn(ConfigOptionDto::selectedByDefault)
                .extracting(ConfigOptionDto::value)
                .containsExactly("45");
    }

    @Test
    void getConfigReturnsTenantRowsWhenPresent() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(repository.findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("tenant-1"), eq(TenantConfigType.COMPETENCY)))
                .thenReturn(List.of(option("Pensamento Crítico", "Pensamento Crítico", false, false, 0)));
        when(repository.findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("tenant-1"), eq(TenantConfigType.ANSWER_TIME_LIMIT)))
                .thenReturn(List.of());

        TenantConfigResponse config = service.getConfig();

        assertThat(config.competencies()).extracting(ConfigOptionDto::value)
                .containsExactly("Pensamento Crítico");
        assertThat(config.answerTimeLimits()).extracting(ConfigOptionDto::value)
                .containsExactly("0", "30", "45", "60");
    }

    @Test
    void updateConfigReplacesExistingRowsAndPreservesOrder() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTenantConfigRequest request = new UpdateTenantConfigRequest(List.of(
                new UpdateTenantConfigRequest.OptionInput("Empatia", null, false, false),
                new UpdateTenantConfigRequest.OptionInput("Foco no Cliente", "Foco no Cliente", false, true)
        ));

        List<ConfigOptionDto> result = service.updateConfig(TenantConfigType.COMPETENCY, request);

        verify(repository).deleteByTenantIdAndConfigType("tenant-1", TenantConfigType.COMPETENCY);
        verify(repository).flush();
        assertThat(result).extracting(ConfigOptionDto::label)
                .containsExactly("Empatia", "Foco no Cliente");
        assertThat(result).extracting(ConfigOptionDto::selectedByDefault)
                .containsExactly(false, true);
    }

    private static TenantConfigOptionEntity option(
            String value,
            String label,
            boolean locked,
            boolean selected,
            int order
    ) {
        TenantConfigOptionEntity entity = new TenantConfigOptionEntity();
        entity.setTenantId("tenant-1");
        entity.setConfigType(TenantConfigType.COMPETENCY);
        entity.setOptionValue(value);
        entity.setOptionLabel(label);
        entity.setLocked(locked);
        entity.setSelectedByDefault(selected);
        entity.setDisplayOrder(order);
        return entity;
    }
}
