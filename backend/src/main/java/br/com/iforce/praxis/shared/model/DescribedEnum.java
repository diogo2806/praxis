package br.com.iforce.praxis.shared.model;

/**
 * Enums expostos no contrato REST carregam uma descrição estável (camelCase) usada na
 * serialização JSON, mantida separada do {@code name()} interno. Implementar esta interface
 * permite reaproveitar o parsing genérico de {@link DescribedEnums} e remover o boilerplate de
 * {@code fromString} repetido em cada enum.
 */
public interface DescribedEnum {

    String getDescricao();
}
