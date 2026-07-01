package br.com.iforce.praxis.marketplace.model;

/**
 * Situação de verificação de um profissional de psicometria no marketplace.
 *
 * <p>O cadastro é gratuito e nasce em {@link #PENDING_VERIFICATION}; a liberação para publicar
 * depende de verificação manual pela moderação ({@code ROLE ADMIN}).</p>
 */
public enum ProfessionalVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    SUSPENDED
}
