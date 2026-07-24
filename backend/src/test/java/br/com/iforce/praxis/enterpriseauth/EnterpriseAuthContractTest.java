package br.com.iforce.praxis.enterpriseauth;

import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.SaveIdentityProviderRequest;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.StartLoginResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EnterpriseAuthContractTest {

    @Test
    void configurationNeverAcceptsRawClientSecret() {
        Set<String> fields = Arrays.stream(SaveIdentityProviderRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(fields)
                .contains("clientsecretenvvar", "requiremfa", "enforcesso", "allowedemaildomains")
                .doesNotContain("clientsecret", "secretvalue", "password");
    }

    @Test
    void migrationPersistsStateNoncePkceAndMfaEvidenceWithoutSecretValue() throws IOException {
        String migration;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V1112__create_enterprise_sso_and_mfa.sql")) {
            assertThat(input).isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(migration)
                .contains("state_hash varchar(64) not null unique")
                .contains("nonce_hash varchar(64) not null")
                .contains("pkce_verifier varchar(180) not null")
                .contains("require_mfa boolean not null default true")
                .contains("last_mfa_verified_at")
                .contains("client_secret_env_var")
                .doesNotContain("client_secret varchar")
                .doesNotContain("client_secret text");
    }

    @Test
    void publicStartResponseDoesNotExposeStateNonceVerifierOrSecret() {
        Set<String> fields = Arrays.stream(StartLoginResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(fields).containsExactlyInAnyOrder("authorizationurl", "expiresat");
    }
}
