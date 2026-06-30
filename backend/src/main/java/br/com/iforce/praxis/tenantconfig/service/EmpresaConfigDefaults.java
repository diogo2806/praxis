package br.com.iforce.praxis.tenantconfig.service;

import br.com.iforce.praxis.tenantconfig.dto.ConfigOptionDto;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;


import java.util.List;

import java.util.Map;


/**
 * Valores padrao usados quando um empresa ainda nao customizou um catalogo.
 */
public final class EmpresaConfigDefaults {

    private EmpresaConfigDefaults() {
    }

    private static final Map<EmpresaConfigType, List<ConfigOptionDto>> DEFAULTS = Map.of(
            EmpresaConfigType.COMPETENCY, List.of(
                    plain("Empatia"),
                    plain("Resolução de Conflitos"),
                    plain("Aderência à Política"),
                    plain("Comunicação"),
                    plain("Negociação"),
                    plain("Tomada de Decisão"),
                    plain("Liderança"),
                    plain("Proatividade")
            ),
            EmpresaConfigType.ANSWER_TIME_LIMIT, List.of(
                    new ConfigOptionDto("0", "Sem limite", false, false),
                    new ConfigOptionDto("30", "30 s", false, false),
                    new ConfigOptionDto("45", "45 s", false, true),
                    new ConfigOptionDto("60", "60 s", false, false)
            )
    );

    public static List<ConfigOptionDto> forType(EmpresaConfigType type) {
        return DEFAULTS.getOrDefault(type, List.of());
    }

    private static ConfigOptionDto plain(String value) {
        return new ConfigOptionDto(value, value, false, false);
    }
}
