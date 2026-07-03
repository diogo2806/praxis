package br.com.iforce.praxis.gupy.service;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;


/**
 * Deriva a chave de idempotência da participação a partir de um hash estável.
 *
 * <p>Na visão do processo: a chave que identifica de forma única a participação
 * de um candidato é montada a partir de dados que incluem <strong>dado
 * pessoal</strong> (documento/CPF e e-mail). Guardar essa composição em claro em
 * uma coluna indexada exporia o dado sem necessidade. Por isso a chave é
 * reduzida a um hash SHA-256 (hex): mantém a mesma capacidade de reconhecer o
 * mesmo candidato (mesma entrada gera o mesmo hash), sem preservar o dado
 * pessoal original.</p>
 *
 * <p><strong>Atenção:</strong> a migração {@code V68} reprocessa as chaves
 * existentes usando exatamente este algoritmo (SHA-256 dos bytes UTF-8, em hex
 * minúsculo). Qualquer mudança aqui precisa ser refletida lá.</p>
 */
public final class IdempotencyKeyHasher {

    private IdempotencyKeyHasher() {
    }

    /**
     * Devolve o SHA-256 (hex minúsculo) do valor informado.
     *
     * @param rawKey chave composta em claro (pode conter dado pessoal)
     * @return o hash hexadecimal estável da chave
     */
    public static String sha256Hex(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
