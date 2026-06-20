package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;
import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;

import java.util.List;
import java.util.Map;

/**
 * Valores padrao usados quando um tenant ainda nao customizou um catalogo.
 * Espelham as listas que antes ficavam fixas no frontend, garantindo que tenants
 * existentes mantenham exatamente o comportamento anterior sem precisar de seed.
 */
public final class TenantConfigDefaults {

    private TenantConfigDefaults() {
    }

    private static final Map<TenantConfigType, List<ConfigOptionDto>> DEFAULTS = Map.of(
            TenantConfigType.COMPETENCY, List.of(
                    plain("Empatia"),
                    plain("Resolução de Conflitos"),
                    plain("Aderência à Política"),
                    plain("Comunicação"),
                    plain("Negociação"),
                    plain("Tomada de Decisão"),
                    plain("Liderança"),
                    plain("Proatividade")
            ),
            TenantConfigType.SENIORITY_LEVEL, List.of(
                    plain("Júnior"),
                    selected("Pleno"),
                    plain("Sênior")
            ),
            TenantConfigType.LANGUAGE_CHECKLIST, List.of(
                    plain("Evita regionalismo desnecessario"),
                    plain("Nao usa estereotipo de classe"),
                    plain("Sem marcador de genero sem necessidade"),
                    plain("Sem referencia a idade, sotaque, origem ou crenca"),
                    plain("Linguagem compativel com o cargo avaliado")
            ),
            TenantConfigType.RESULT_USE, List.of(
                    selected("Triagem"),
                    plain("Ranking"),
                    plain("Apoio à entrevista"),
                    locked("Decisão final")
            ),
            TenantConfigType.ANSWER_TIME_LIMIT, List.of(
                    new ConfigOptionDto("0", "Sem limite", false, false, true),
                    new ConfigOptionDto("30", "30 s", false, false, true),
                    new ConfigOptionDto("45", "45 s", false, true, true),
                    new ConfigOptionDto("60", "60 s", false, false, true)
            )
    );

    public static List<ConfigOptionDto> forType(TenantConfigType type) {
        return DEFAULTS.getOrDefault(type, List.of());
    }

    private static ConfigOptionDto plain(String value) {
        return new ConfigOptionDto(value, value, false, false, true);
    }

    private static ConfigOptionDto selected(String value) {
        return new ConfigOptionDto(value, value, false, true, true);
    }

    private static ConfigOptionDto locked(String value) {
        return new ConfigOptionDto(value, value, true, false, true);
    }
}
