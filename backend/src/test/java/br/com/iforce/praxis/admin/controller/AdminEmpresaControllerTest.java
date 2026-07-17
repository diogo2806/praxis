package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
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
class AdminEmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken() {
        return jwtService.generateToken("admin-operator-1", "PLATFORM", Set.of("ADMIN"));
    }

    private JsonNode createEmpresa(String name, boolean healthVertical) throws Exception {
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

        MvcResult result = mockMvc.perform(post("/api/admin/empresas")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String empresaToken = jwtService.generateToken("u1", "empresa-1", Set.of("EMPRESA"));

        mockMvc.perform(get("/api/admin/empresas/page"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/empresas/page")
                        .header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/empresas/page")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/empresas")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void createEmpresaProvisionsClientAndEmpresaUser() throws Exception {
        JsonNode response = createEmpresa("AcmeHealth", true);
        String empresaId = response.get("empresa").get("empresaId").asText();

        assertThat(response.get("inviteUrl").asText()).contains("/convite/");

        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElseThrow();
        assertThat(empresa.isHealthVertical()).isTrue();
        assertThat(empresa.getCompanyId()).isNotBlank();
        assertThat(empresa.getCommercialPlanType().name()).isEqualTo("PROFISSIONAL");

        UserEntity responsible = userRepository.findByEmpresaIdOrderByCreatedAtAsc(empresaId).get(0);
        assertThat(responsible.getRoles()).containsExactly("EMPRESA");
        assertThat(responsible.getStatus()).isEqualTo(UserStatus.CONVIDADO);
        assertThat(responsible.getInviteTokenLookupHash()).isNotBlank();
    }

    @Test
    void suspendBlocksLoginAndProtectedApisThenReactivateRestores() throws Exception {
        JsonNode response = createEmpresa("SuspendCo", false);
        String empresaId = response.get("empresa").get("empresaId").asText();

        UserEntity user = new UserEntity();
        user.setEmpresaId(empresaId);
        user.setEmail("login@suspendco.com");
        user.setName("Login User");
        user.setPasswordHash(passwordEncoder.encode("senha-super-secreta"));
        user.setRoles(Set.of("EMPRESA"));
        user.setStatus(UserStatus.ATIVO);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);

        String empresaToken = jwtService.generateToken(user.getId().toString(), empresaId, Set.of("EMPRESA"));

        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
        login(empresaId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Inadimplência operacional\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENSO"));

        login(empresaId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Regularizado\",\"targetStatus\":\"ATIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATIVO"));

        mockMvc.perform(get("/api/v1/simulations").header("Authorization", "Bearer " + empresaToken))
                .andExpect(status().isOk());
        login(empresaId, "login@suspendco.com", "senha-super-secreta").andExpect(status().isOk());
    }

    @Test
    void sensitiveActionsRequireReason() throws Exception {
        JsonNode response = createEmpresa("ReasonCo", false);
        String empresaId = response.get("empresa").get("empresaId").asText();

        mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelKeepsEmpresaAndRecordsAuditWithActor() throws Exception {
        JsonNode response = createEmpresa("CancelCo", false);
        String empresaId = response.get("empresa").get("empresaId").asText();

        mockMvc.perform(post("/api/admin/empresas/" + empresaId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Encerramento contratual\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(empresaRepository.findById(empresaId)).isPresent();

        mockMvc.perform(get("/api/admin/empresas/" + empresaId + "/audit")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType=='adminEmpresaCanceled')].actorUserId")
                        .value(org.hamcrest.Matchers.hasItem("admin-operator-1")));
    }

    @Test
    void planChangeIsAudited() throws Exception {
        JsonNode response = createEmpresa("PlanCo", false);
        String empresaId = response.get("empresa").get("empresaId").asText();

        mockMvc.perform(patch("/api/admin/empresas/" + empresaId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commercialPlanType\":\"AVULSO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialPlanType").value("AVULSO"));

        mockMvc.perform(get("/api/admin/empresas/" + empresaId + "/audit")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventType=='adminCommercialPlanChanged')]")
                        .isNotEmpty());
    }

    @Test
    void dashboardReturnsCounts() throws Exception {
        createEmpresa("DashCo", false);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmpresas").isNumber())
                .andExpect(jsonPath("$.trialEmpresas").isNumber());
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String empresaId,
            String email,
            String password
    ) throws Exception {
        String body = """
                {"empresaId":"%s","email":"%s","password":"%s"}
                """.formatted(empresaId, email, password);
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
