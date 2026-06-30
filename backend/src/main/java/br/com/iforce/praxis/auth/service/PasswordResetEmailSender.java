package br.com.iforce.praxis.auth.service;

/**
 * Envia o e-mail de recuperação de senha com o link temporário.
 *
 * <p>A abstração isola o fluxo de recuperação do mecanismo concreto de entrega: hoje o sistema
 * ainda não possui um provedor SMTP configurado, então a implementação padrão apenas registra o
 * envio em log. Quando um provedor real for adicionado, basta fornecer outra implementação deste
 * contrato sem alterar o serviço de recuperação.</p>
 */
public interface PasswordResetEmailSender {

    /**
     * Envia a mensagem de redefinição de senha.
     *
     * @param recipientEmail e-mail cadastrado do usuário
     * @param userName       nome do usuário, usado na saudação
     * @param resetLink      link temporário completo para redefinição
     * @param ttlHours       validade do link em horas
     */
    void sendPasswordResetEmail(String recipientEmail, String userName, String resetLink, int ttlHours);
}
