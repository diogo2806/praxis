package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "praxis.security.enabled=true",
        "praxis.integration-token=test-gupy-token-32-characters-minimum",
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
                        .header("Authorization", "Bearer test-gupy-token-32-characters-minimum"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void empresaJwtCanAccessCompanyEndpoints() throws Exception {
        String empresaToken = jwtService.generateToken("empresa-user", "tenant-1", Set.of("EMPRESA"));

        mockMvc.perform(get("/api/v1/simulations")
                        .header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
    }

    @Test
    void gupyTokenCanAccessOnlyGupyEndpoints() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer test-gupy-token-32-characters-minimum"))
                .andExpect(status().isOk());
    }
}
