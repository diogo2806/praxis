package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.auth.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "praxis.security.enabled=true",
        "praxis.jwt-secret=test-jwt-secret-32-characters-minimum-value"
})
@AutoConfigureMockMvc
class AdminTenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken() {
        return jwtService.generateToken("admin-operator-1", "PLATFORM", Set.of("ADMIN"));
    }

    private JsonNode createTenant(String name, boolean healthVertical) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "tradeName": "%s LTDA",
                  "taxId": "12345678000199",
                  "corporateEmail": "contato@%s.com",
                  "healthVertical": %s,
                  "commercialPlanType": "PROFISSIONAL",
                  "initialStatus": "EM_TESTE",
                  "responsibleName": "Maria Responsável",
                  "responsibleEmail": "maria@%s.com",
                  "sendInvite": true
                }
                """.formatted(name, name, name.toLowerCase(), healthVertical, name.toLowerCase());

        MvcResult result = mockMvc.perform(post("/api/admin/tenants")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String empresaToken = jwtService.generateToken("u1", "tenant-1", Set.of("EMPRESA"));

        mockMvc.perform(get("/api/admin/tenants"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/tenants")
                        .header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/tenants")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void createTenantProvisionsClientAndEmpresaUser() throws Exception {
        JsonNode response = createTenant("AcmeHealth", true);
        String tenantId = response.get("tenant").get("tenantId").asText();

        assertThat(response.get("inviteUrl").asText()).contains("/convite/");

        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow();
        assertThat(tenant.isHealthVertical()).isTrue();
        assertThat(tenant.getCompanyId()).isNotBlank();
        assertThat(tenant.getCommercialPlanType().name()).isEqualTo("PROFISSIONAL");

        // O usuário responsável é EMPRESA, nunca ADMIN.
        UserEntity responsible = userRepository.findByTenantIdOrderByCreatedAtAsc(tenantId).get(0);
        assertThat(responsible.getRoles()).containsExactly("EMPRESA");
        assertThat(responsible.getStatus()).isEqualTo(UserStatus.CONVIDADO);
    }

    @Test
    void suspendBlocksLoginAndProtectedApisThenReactivateRestores() throws Exception {
        JsonNode response = createTenant("SuspendCo", false);
        String tenantId = response.get("tenant").get("tenantId").asText();

        // Usuário com senha conhecida para testar o bloqueio de login.
        UserEntity user = new UserEntity();
        user.setTenantId(tenantId);
        user.setEmail("login@suspendco.com");
        user.setName("Login User");
        user.setPasswordHash(passwordEncoder.encode("senha-super-secreta"));
        user.setRoles(Set.of("EMPRESA"));
        user.setStatus(UserStatus.ATIVO);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String empresaToken = jwtService.generateToken(user.getId().toString(), tenantId, Set.of("EMPRESA"));

        // Antes da suspensão: API protegida acessível e login válido.
        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
        login(tenantId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isOk());

        // Suspende exigindo motivo.
        mockMvc.perform(post("/api/admin/tenants/" + tenantId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inadimplência operacional\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENSO"));

        // Suspenso: login bloqueado e API protegida bloqueada mesmo com token válido.
        login(tenantId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isForbidden());

        // Reativa para ATIVO.
        mockMvc.perform(post("/api/admin/tenants/" + tenantId + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Regularizado\",\"targetStatus\":\"ATIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
        login(tenantId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isOk());
    }

    @Test
    void sensitiveActionsRequireReason() throws Exception {
        JsonNode response = createTenant("ReasonCo", false);
        String tenantId = response.get("tenant").get("tenantId").asText();

        mockMvc.perform(post("/api/admin/tenants/" + tenantId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelKeepsTenantAndRecordsAuditWithActor() throws Exception {
        JsonNode response = createTenant("CancelCo", false);
        String tenantId = response.get("tenant").get("tenantId").asText();

        mockMvc.perform(post("/api/admin/tenants/" + tenantId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Encerramento contratual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        // Tenant preservado.
        assertThat(tenantRepository.findById(tenantId)).isPresent();

        // Auditoria registra ator e tipo de evento, é somente leitura.
        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/audit")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType=='adminTenantCanceled')].actorUserId")
                        .value(org.hamcrest.Matchers.hasItem("admin-operator-1")));
    }

    @Test
    void planChangeIsAudited() throws Exception {
        JsonNode response = createTenant("PlanCo", false);
        String tenantId = response.get("tenant").get("tenantId").asText();

        mockMvc.perform(patch("/api/admin/tenants/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialPlanType\":\"AVULSO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPlanType").value("AVULSO"));

        mockMvc.perform(get("/api/admin/tenants/" + tenantId + "/audit")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType=='adminCommercialPlanChanged')]")
                        .isNotEmpty());
    }

    @Test
    void dashboardReturnsCounts() throws Exception {
        createTenant("DashCo", false);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTenants").isNumber())
                .andExpect(jsonPath("$.trialTenants").isNumber());
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String tenantId, String email, String password) throws Exception {
        String body = """
                {"tenantId":"%s","email":"%s","password":"%s"}
                """.formatted(tenantId, email, password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
