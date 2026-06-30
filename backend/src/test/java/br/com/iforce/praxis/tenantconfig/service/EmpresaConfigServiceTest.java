package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;

import br.com.iforce.praxis.tenantconfig.dto.EmpresaConfigResponse;

import br.com.iforce.praxis.tenantconfig.dto.UpdateEmpresaConfigRequest;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;

import br.com.iforce.praxis.tenantconfig.persistence.entity.EmpresaConfigOptionEntity;

import br.com.iforce.praxis.tenantconfig.persistence.repository.EmpresaConfigOptionRepository;

import org.junit.jupiter.api.Test;


import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


class EmpresaConfigServiceTest {

    private final EmpresaConfigOptionRepository repository = mock(EmpresaConfigOptionRepository.class);
    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final EmpresaConfigService service = new EmpresaConfigService(repository, currentEmpresaService);

    @Test
    void getConfigFallsBackToActiveDefaultsWhenEmpresaHasNoRows() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(repository.findByEmpresaIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("empresa-1"), any()))
                .thenReturn(List.of());

        EmpresaConfigResponse config = service.getConfig();

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
    void getConfigReturnsEmpresaRowsWhenPresent() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(repository.findByEmpresaIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("empresa-1"), eq(EmpresaConfigType.COMPETENCY)))
                .thenReturn(List.of(option("Pensamento Crítico", "Pensamento Crítico", false, false, 0)));
        when(repository.findByEmpresaIdAndConfigTypeOrderByDisplayOrderAscIdAsc(eq("empresa-1"), eq(EmpresaConfigType.ANSWER_TIME_LIMIT)))
                .thenReturn(List.of());

        EmpresaConfigResponse config = service.getConfig();

        assertThat(config.competencies()).extracting(ConfigOptionDto::value)
                .containsExactly("Pensamento Crítico");
        assertThat(config.answerTimeLimits()).extracting(ConfigOptionDto::value)
                .containsExactly("0", "30", "45", "60");
    }

    @Test
    void updateConfigReplacesExistingRowsAndPreservesOrder() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEmpresaConfigRequest request = new UpdateEmpresaConfigRequest(List.of(
                new UpdateEmpresaConfigRequest.OptionInput("Empatia", null, false, false),
                new UpdateEmpresaConfigRequest.OptionInput("Foco no Cliente", "Foco no Cliente", false, true)
        ));

        List<ConfigOptionDto> result = service.updateConfig(EmpresaConfigType.COMPETENCY, request);

        verify(repository).deleteByEmpresaIdAndConfigType("empresa-1", EmpresaConfigType.COMPETENCY);
        verify(repository).flush();
        assertThat(result).extracting(ConfigOptionDto::label)
                .containsExactly("Empatia", "Foco no Cliente");
        assertThat(result).extracting(ConfigOptionDto::selectedByDefault)
                .containsExactly(false, true);
    }

    private static EmpresaConfigOptionEntity option(
            String value,
            String label,
            boolean locked,
            boolean selected,
            int order
    ) {
        EmpresaConfigOptionEntity entity = new EmpresaConfigOptionEntity();
        entity.setEmpresaId("empresa-1");
        entity.setConfigType(EmpresaConfigType.COMPETENCY);
        entity.setOptionValue(value);
        entity.setOptionLabel(label);
        entity.setLocked(locked);
        entity.setSelectedByDefault(selected);
        entity.setDisplayOrder(order);
        return entity;
    }
}
