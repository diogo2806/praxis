package br.com.iforce.praxis.integration.ats.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registro central de adapters de ATS.
 * Permite resolver qual adapter usar baseado na plataforma ou tenant.
 */
@Slf4j
@Component
public class AdapterRegistry {

    private final Map<ATSAdapter.ATSPlatform, ATSAdapter> adapters;

    public AdapterRegistry(List<ATSAdapter> adapterList) {
        this.adapters = adapterList.stream()
            .collect(Collectors.toMap(ATSAdapter::type, adapter -> adapter));

        log.info("Registry inicializado com {} adapters: {}", adapters.size(), adapters.keySet());
    }

    /**
     * Obtém o adapter para uma plataforma específica.
     */
    public ATSAdapter getAdapter(ATSAdapter.ATSPlatform platform) {
        ATSAdapter adapter = adapters.get(platform);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "Nenhum adapter registrado para plataforma: " + platform +
                ". Adapters disponíveis: " + adapters.keySet()
            );
        }
        return adapter;
    }

    /**
     * Obtém o adapter Gupy como padrão.
     */
    public ATSAdapter getGupyAdapter() {
        return getAdapter(ATSAdapter.ATSPlatform.GUPY);
    }

    /**
     * Lista todas as plataformas suportadas.
     */
    public List<ATSAdapter.ATSPlatform> getSupportedPlatforms() {
        return List.copyOf(adapters.keySet());
    }
}
