package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;

import org.flywaydb.core.api.migration.Context;


import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;

import java.sql.Connection;

import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.util.ArrayList;

import java.util.HexFormat;

import java.util.List;


/**
 * Reprocessa as chaves de idempotência já gravadas em {@code candidate_attempts}
 * para não guardar dado pessoal em claro.
 *
 * <p>Até aqui a coluna {@code idempotency_key} armazenava uma composição em claro
 * que incluía documento/CPF (fluxo Gupy) ou e-mail (link interno). A partir de
 * agora a aplicação grava o SHA-256 (hex) dessa composição
 * ({@code IdempotencyKeyHasher.sha256Hex}). Esta migração aplica o <strong>mesmo
 * algoritmo</strong> às linhas existentes, para que o reconhecimento do mesmo
 * candidato continue funcionando (a mesma entrada gera o mesmo hash) sem manter o
 * dado pessoal original.</p>
 *
 * <p>Linhas já anonimizadas ({@code idempotency_key} começando com
 * {@code anonymized:}) são preservadas. Em um banco recém-criado a tabela está
 * vazia e a migração é um no-op.</p>
 */
public class V68__hash_candidate_attempt_idempotency_keys extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();

        List<String[]> pending = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT id, idempotency_key FROM candidate_attempts "
                        + "WHERE idempotency_key IS NOT NULL AND idempotency_key NOT LIKE 'anonymized:%'");
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                pending.add(new String[]{rs.getString("id"), rs.getString("idempotency_key")});
            }
        }

        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE candidate_attempts SET idempotency_key = ? WHERE id = ?")) {
            for (String[] row : pending) {
                update.setString(1, sha256Hex(row[1]));
                update.setString(2, row[0]);
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    /**
     * SHA-256 (hex minúsculo) dos bytes UTF-8. Deve permanecer idêntico a
     * {@code br.com.iforce.praxis.gupy.service.IdempotencyKeyHasher.sha256Hex}.
     */
    private static String sha256Hex(String rawKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
