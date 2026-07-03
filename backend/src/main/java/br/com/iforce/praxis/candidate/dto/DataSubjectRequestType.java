package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.shared.model.DescribedEnum;

import br.com.iforce.praxis.shared.model.DescribedEnums;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Direitos do titular passíveis de requisição no fluxo público do candidato,
 * mapeados ao art. 18 da LGPD.
 *
 * <p>O candidato aciona um desses direitos pela própria participação (rota
 * pública, sem empresa no contexto). O pedido é registrado na trilha de
 * auditoria para que o controlador (empresa responsável) o atenda no prazo
 * legal, já que é ele quem detém a base legal e a decisão sobre os dados.</p>
 */
public enum DataSubjectRequestType implements DescribedEnum {

    /** Confirmação de existência do tratamento e acesso aos dados (art. 18, I e II). */
    CONFIRMATION_ACCESS("confirmationAccess"),

    /** Correção de dados incompletos, inexatos ou desatualizados (art. 18, III). */
    RECTIFICATION("rectification"),

    /** Anonimização, bloqueio ou eliminação de dados desnecessários ou excessivos (art. 18, IV). */
    ANONYMIZATION_DELETION("anonymizationDeletion"),

    /** Portabilidade dos dados a outro fornecedor (art. 18, V). */
    PORTABILITY("portability"),

    /** Eliminação dos dados pessoais tratados com base em consentimento (art. 18, VI). */
    DELETION_CONSENT("deletionConsent"),

    /** Informação sobre entidades com as quais os dados foram compartilhados (art. 18, VII). */
    INFORMATION_SHARING("informationSharing"),

    /** Revogação do consentimento anteriormente fornecido (art. 18, IX). */
    CONSENT_REVOCATION("consentRevocation");

    private final String descricao;

    DataSubjectRequestType(String descricao) {
        this.descricao = descricao;
    }

    @JsonValue
    @Override
    public String getDescricao() {
        return descricao;
    }

    @JsonCreator
    public static DataSubjectRequestType fromString(String valor) {
        return DescribedEnums.fromValue(DataSubjectRequestType.class, valor);
    }
}
