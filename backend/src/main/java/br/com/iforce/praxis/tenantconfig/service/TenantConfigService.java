package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;
import br.com.iforce.praxis.tenantconfig.dto.TenantConfigResponse;
import br.com.iforce.praxis.tenantconfig.dto.UpdateTenantConfigRequest;
import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.persistence.entity.TenantConfigOptionEntity;
import br.com.iforce.praxis.tenantconfig.persistence.repository.TenantConfigOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolve os catalogos configuraveis por tenant. Quando o tenant nao possui opcoes
 * persistidas para um tipo, devolve os padroes embutidos ({@link TenantConfigDefaults}),
 * preservando o comportamento anterior das telas.
 */
@Service
public class TenantConfigService {

    private final TenantConfigOptionRepository repository;
    private final CurrentTenantService currentTenantService;

    public TenantConfigService(
            TenantConfigOptionRepository repository,
            CurrentTenantService currentTenantService
    ) {
        this.repository = repository;
        this.currentTenantService = currentTenantService;
    }

    @Transactional(readOnly = true)
    public TenantConfigResponse getConfig() {
        String tenantId = currentTenantService.requiredTenantId();
        return new TenantConfigResponse(
                loadOrDefault(tenantId, TenantConfigType.COMPETENCY),
                loadOrDefault(tenantId, TenantConfigType.SENIORITY_LEVEL),
                loadOrDefault(tenantId, TenantConfigType.LANGUAGE_CHECKLIST),
                loadOrDefault(tenantId, TenantConfigType.RESULT_USE),
                loadOrDefault(tenantId, TenantConfigType.ANSWER_TIME_LIMIT)
        );
    }

    @Transactional
    public List<ConfigOptionDto> updateConfig(TenantConfigType configType, UpdateTenantConfigRequest request) {
        String tenantId = currentTenantService.requiredTenantId();

        repository.deleteByTenantIdAndConfigType(tenantId, configType);
        repository.flush();

        List<TenantConfigOptionEntity> toPersist = new ArrayList<>();
        int order = 0;
        for (UpdateTenantConfigRequest.OptionInput input : request.options()) {
            TenantConfigOptionEntity entity = new TenantConfigOptionEntity();
            entity.setTenantId(tenantId);
            entity.setConfigType(configType);
            entity.setOptionValue(input.value().trim());
            entity.setOptionLabel(resolveLabel(input));
            entity.setLocked(input.locked());
            entity.setSelectedByDefault(input.selectedByDefault());
            entity.setDisplayOrder(order++);
            toPersist.add(entity);
        }

        return repository.saveAll(toPersist).stream()
                .map(TenantConfigService::toDto)
                .toList();
    }

    private List<ConfigOptionDto> loadOrDefault(String tenantId, TenantConfigType configType) {
        List<TenantConfigOptionEntity> rows =
                repository.findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(tenantId, configType);
        if (rows.isEmpty()) {
            return TenantConfigDefaults.forType(configType);
        }
        return rows.stream()
                .map(TenantConfigService::toDto)
                .toList();
    }

    private static String resolveLabel(UpdateTenantConfigRequest.OptionInput input) {
        if (input.label() == null || input.label().isBlank()) {
            return input.value().trim();
        }
        return input.label().trim();
    }

    private static ConfigOptionDto toDto(TenantConfigOptionEntity entity) {
        return new ConfigOptionDto(
                entity.getOptionValue(),
                entity.getOptionLabel(),
                entity.isLocked(),
                entity.isSelectedByDefault()
        );
    }
}
