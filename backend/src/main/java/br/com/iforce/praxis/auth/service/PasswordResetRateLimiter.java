package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;


import java.time.Duration;

import java.time.Instant;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Limitador de tentativas em memória para o fluxo de recuperação de senha.
 *
 * <p>Protege os endpoints públicos contra abuso (varredura de e-mails e força bruta de tokens)
 * aplicando uma janela fixa de contagem por chave — tipicamente o IP de origem ou a identidade do
 * usuário ({@code empresa|email}). O bloqueio nunca revela se uma conta existe: a contagem é feita
 * sobre o que foi enviado, não sobre o resultado da busca.</p>
 *
 * <p>A contagem é propositalmente simples e local ao processo; para múltiplas instâncias, troque por
 * um backend compartilhado. Entradas expiradas são removidas de forma preguiçosa a cada acesso.</p>
 */
@Component
public class PasswordResetRateLimiter {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    public PasswordResetRateLimiter(
            @Value("${praxis.auth.password-reset-max-attempts:5}") int maxAttempts,
            @Value("${praxis.auth.password-reset-window-minutes:15}") int windowMinutes
    ) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.window = Duration.ofMinutes(Math.max(1, windowMinutes));
    }

    /**
     * Registra mais uma tentativa para a chave e indica se ela está dentro do limite.
     *
     * @param key identificador da origem (IP ou {@code empresa|email})
     * @return {@code true} se a tentativa é permitida; {@code false} se o limite foi excedido
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isBlank()) {
            return true;
        }
        Instant now = Instant.now();
        Window updated = counters.compute(key, (ignored, current) -> {
            if (current == null || now.isAfter(current.resetAt())) {
                return new Window(1, now.plus(window));
            }
            return new Window(current.count() + 1, current.resetAt());
        });
        return updated.count() <= maxAttempts;
    }

    private record Window(int count, Instant resetAt) {
    }
}
