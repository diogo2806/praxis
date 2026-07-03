package br.com.iforce.praxis.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.boot.context.properties.bind.ConstructorBinding;


/**
 * Configuração do Mercado Pago. Todas as credenciais ficam apenas no backend e nunca são
 * enviadas ao frontend nem commitadas no repositório: são lidas de variáveis de ambiente
 * ({@code MP_ACCESS_TOKEN}, {@code MP_PUBLIC_KEY}, {@code MP_WEBHOOK_SECRET}).
 */
@ConfigurationProperties(prefix = "mp")
public record MercadoPagoProperties(
        boolean enabled,
        String baseUrl,
        String accessToken,
        String publicKey,
        String webhookSecret,
        int gracePeriodDays,
        String backUrl,
        String notificationUrl
) {

    @ConstructorBinding
    public MercadoPagoProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.mercadopago.com";
        }
        if (gracePeriodDays <= 0) {
            gracePeriodDays = 7;
        }
    }

    /** Mascara o token para logs (nunca registrar credencial em claro). */
    public String maskedAccessToken() {
        if (accessToken == null || accessToken.length() < 8) {
            return "***";
        }
        return accessToken.substring(0, 4) + "***" + accessToken.substring(accessToken.length() - 4);
    }
}
