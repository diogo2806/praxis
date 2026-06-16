package br.com.iforce.praxis.shared.model;

/**
 * Resolução genérica de enums {@link DescribedEnum} a partir de uma string, aceitando tanto o
 * {@code name()} quanto a descrição exposta no JSON. Centraliza a lógica antes duplicada nos
 * métodos {@code fromString} anotados com {@code @JsonCreator}.
 */
public final class DescribedEnums {

    private DescribedEnums() {
    }

    public static <E extends Enum<E> & DescribedEnum> E fromValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }

        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value) || constant.getDescricao().equalsIgnoreCase(value)) {
                return constant;
            }
        }

        throw new IllegalArgumentException(
                "Valor invalido para " + type.getSimpleName() + ": '" + value + "'");
    }
}
