package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.auth.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "praxis.security.enabled=true",
        "praxis.jwt-secret=test-jwt-secret-32-characters-minimum-value"
})
@AutoConfigureMockMvc
class SpringdocDefaultSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void openApiIsUnavailableByDefaultEvenForAdministrator() throws Exception {
        String adminToken = jwtService.generateToken("admin-user", "PLATFORM", Set.of("ADMIN"));

        mockMvc.perform(get("/v3/api-docs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void swaggerUiIsUnavailableByDefaultEvenForAdministrator() throws Exception {
        String adminToken = jwtService.generateToken("admin-user", "PLATFORM", Set.of("ADMIN"));

        mockMvc.perform(get("/docs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}
