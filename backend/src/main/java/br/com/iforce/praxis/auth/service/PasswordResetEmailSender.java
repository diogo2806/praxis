package br.com.iforce.praxis.auth.service;

/** Envia mensagens de acesso da plataforma. */
public interface PasswordResetEmailSender {

    /**
     * Envia a mensagem de redefinição de senha.
     *
     * @param recipientEmail e-mail cadastrado do usuário
     * @param userName       nome do usuário, usado na saudação
     * @param resetLink      endereço temporário completo para redefinição
     * @param ttlHours       validade em horas
     */
    void sendPasswordResetEmail(String recipientEmail, String userName, String resetLink, int ttlHours);

    /** Envia a mensagem de convite para uma pessoa entrar na equipe. */
    default void sendTeamInviteEmail(String recipientEmail, String userName, String inviteUrl, int ttlHours) {
        sendPasswordResetEmail(recipientEmail, userName, inviteUrl, ttlHours);
    }
}
