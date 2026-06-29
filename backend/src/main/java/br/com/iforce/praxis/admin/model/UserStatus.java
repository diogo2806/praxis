package br.com.iforce.praxis.admin.model;

/**
 * Situação de um usuário de acesso de um cliente.
 *
 * <ul>
 *     <li>{@link #ATIVO}: usuário pode autenticar normalmente.</li>
 *     <li>{@link #CONVIDADO}: usuário convidado que ainda não definiu a senha.</li>
 *     <li>{@link #BLOQUEADO}: usuário impedido de autenticar; o histórico é preservado.</li>
 * </ul>
 */
public enum UserStatus {
    ATIVO,
    CONVIDADO,
    BLOQUEADO
}
