package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "praxis.security.enabled=true",
        "praxis.jwt-secret=test-jwt-secret-32-characters-minimum-value"
})
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void gupyTokenCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/simulations")
                        .header("Authorization", "Bearer empresa1-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void empresaJwtCanAccessCompanyEndpoints() throws Exception {
        String empresaToken = jwtService.generateToken("empresa-user", "empresa-1", Set.of("EMPRESA"));

        mockMvc.perform(get("/api/v1/simulations")
                        .header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
    }

    @Test
    void gupyTokenCanAccessOnlyGupyEndpoints() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer empresa1-token"))
                .andExpect(status().isOk());
    }

    @Test
    @Sql(statements = {
            "DELETE FROM integration_tokens WHERE empresa_id = 'empresa-1' AND provider = 'recrutei'",
            "INSERT INTO integration_tokens (empresa_id, provider, token_hash) VALUES "
                    + "('empresa-1', 'recrutei', 'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk')",
            "DELETE FROM empresa_integrations WHERE empresa_id = 'empresa-1' AND provider = 'RECRUTEI'",
            "INSERT INTO empresa_integrations "
                    + "(empresa_id, provider, type, status, credentials_hash, configured_at, created_at, updated_at) "
                    + "VALUES ('empresa-1', 'RECRUTEI', 'ATS', 'PENDENTE', "
                    + "'HQkNHnvADAg8tGffiSYY9Lx094NTkUZ9OLLSNKjSTGk', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
    })
    void recruteiOpaqueTokenReachesProviderAuthenticationInsteadOfJwtParser() throws Exception {
        mockMvc.perform(get("/recrutei/test")
                        .header("Authorization", "Bearer empresa1-token"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfProtectionRemainsEnabledOutsideStatelessContracts() throws Exception {
        mockMvc.perform(post("/browser/session-action"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfDoesNotBlockBearerAuthenticatedApiRequests() throws Exception {
        String empresaToken = jwtService.generateToken("empresa-user", "empresa-1", Set.of("EMPRESA"));

        mockMvc.perform(post("/api/v1/simulations")
                        .header("Authorization", "Bearer " + empresaToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
