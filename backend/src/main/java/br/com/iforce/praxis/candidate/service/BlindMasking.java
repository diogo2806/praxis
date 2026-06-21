package br.com.iforce.praxis.candidate.service;

import java.util.Locale;

/**
 * Mascaramento de identidade para o Modo Cego (REQ-L3). Reduz viés demográfico na decisão humana
 * trocando o nome do candidato por um código estável derivado do attemptId. A defesa em
 * profundidade exige que o backend nem envie a PII no modo cego — por isso o mascaramento acontece
 * no servidor, e não apenas no cliente.
 */
public final class BlindMasking {

    private BlindMasking() {
    }

    public static String maskedName(String attemptId) {
        return "Candidato " + shortCode(attemptId);
    }

    public static String shortCode(String attemptId) {
        if (attemptId == null || attemptId.isBlank()) {
            return "—";
        }
        String alphanumeric = attemptId.replaceAll("[^A-Za-z0-9]", "");
        String base = alphanumeric.isEmpty() ? attemptId : alphanumeric;
        String tail = base.length() <= 6 ? base : base.substring(base.length() - 6);
        return tail.toUpperCase(Locale.ROOT);
    }
}
