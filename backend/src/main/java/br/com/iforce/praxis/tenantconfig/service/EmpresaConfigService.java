package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;

import br.com.iforce.praxis.tenantconfig.dto.EmpresaConfigResponse;

import br.com.iforce.praxis.tenantconfig.dto.UpdateEmpresaConfigRequest;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;

import br.com.iforce.praxis.tenantconfig.persistence.entity.EmpresaConfigOptionEntity;

import br.com.iforce.praxis.tenantconfig.persistence.repository.EmpresaConfigOptionRepository;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;

import java.util.List;


/**
 * Resolve os catalogos configuraveis por empresa. Quando o empresa nao possui opcoes
 * persistidas para um tipo, devolve os padroes embutidos ({@link EmpresaConfigDefaults}),
 * preservando o comportamento anterior das telas.
 */
@Service
public class EmpresaConfigService {

    private final EmpresaConfigOptionRepository repository;
    private final CurrentEmpresaService currentEmpresaService;

    public EmpresaConfigService(
            EmpresaConfigOptionRepository repository,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.repository = repository;
        this.currentEmpresaService = currentEmpresaService;
    }

    /**
     * Devolve os catálogos configuráveis da empresa logada.
     *
     * <p>Traz as competências e os limites de tempo; quando a empresa não
     * personalizou um tipo, devolve o catálogo padrão.</p>
     *
     * @return os catálogos da empresa para as telas de autoria
     */
    @Transactional(readOnly = true)
    public EmpresaConfigResponse getConfig() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return new EmpresaConfigResponse(
                loadOrDefault(empresaId, EmpresaConfigType.COMPETENCY),
                loadOrDefault(empresaId, EmpresaConfigType.ANSWER_TIME_LIMIT)
        );
    }

    /**
     * Substitui por completo as opções de um catálogo da empresa.
     *
     * <p>Fluxo do processo: apaga as opções atuais daquele tipo e grava a
     * nova lista informada, preservando a ordem. Devolve o catálogo
     * resultante.</p>
     *
     * @param configType o tipo de catálogo a personalizar
     * @param request a nova lista completa de opções
     * @return a lista de opções resultante
     */
    @Transactional
    public List<ConfigOptionDto> updateConfig(EmpresaConfigType configType, UpdateEmpresaConfigRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();

        repository.deleteByEmpresaIdAndConfigType(empresaId, configType);
        repository.flush();

        List<EmpresaConfigOptionEntity> toPersist = new ArrayList<>();
        int order = 0;
        for (UpdateEmpresaConfigRequest.OptionInput input : request.options()) {
            EmpresaConfigOptionEntity entity = new EmpresaConfigOptionEntity();
            entity.setEmpresaId(empresaId);
            entity.setConfigType(configType);
            entity.setOptionValue(input.value().trim());
            entity.setOptionLabel(resolveLabel(input));
            entity.setLocked(input.locked());
            entity.setSelectedByDefault(input.selectedByDefault());
            entity.setDisplayOrder(order++);
            toPersist.add(entity);
        }

        return repository.saveAll(toPersist).stream()
                .map(EmpresaConfigService::toDto)
                .toList();
    }

    /**
     * Carrega as opções de um catálogo da empresa; se não houver
     * personalização, devolve os valores padrão embutidos. Uso interno.
     */
    private List<ConfigOptionDto> loadOrDefault(String empresaId, EmpresaConfigType configType) {
        List<EmpresaConfigOptionEntity> rows =
                repository.findByEmpresaIdAndConfigTypeOrderByDisplayOrderAscIdAsc(empresaId, configType);
        if (rows.isEmpty()) {
            return EmpresaConfigDefaults.forType(configType);
        }
        return rows.stream()
                .map(EmpresaConfigService::toDto)
                .toList();
    }

    private static String resolveLabel(UpdateEmpresaConfigRequest.OptionInput input) {
        if (input.label() == null || input.label().isBlank()) {
            return input.value().trim();
        }
        return input.label().trim();
    }

    private static ConfigOptionDto toDto(EmpresaConfigOptionEntity entity) {
        return new ConfigOptionDto(
                entity.getOptionValue(),
                entity.getOptionLabel(),
                entity.isLocked(),
                entity.isSelectedByDefault()
        );
    }
}
