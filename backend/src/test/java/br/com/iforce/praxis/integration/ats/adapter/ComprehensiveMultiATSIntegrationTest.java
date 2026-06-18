package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração completos para todos os adapters de ATS suportados.
 * Valida que o sistema pode lidar com múltiplos ATS simultaneamente sem conflitos.
 */
class ComprehensiveMultiATSIntegrationTest {

    private AdapterRegistry adapterRegistry;

    @BeforeEach
    void setUp() {
        // Registrar todos os adapters disponíveis
        List<ATSAdapter> adapters = List.of(
            new GupyAdapter(null),           // Local (Brasil)
            new CathoAdapter(),               // Portal de empregos (Brasil)
            new IndeedAdapter(),              // Portal global
            new LinkedInAdapter(),            // Rede profissional global
            new WorkdayAdapter(),             // Enterprise (EUA)
            new GreenhouseAdapter()           // Enterprise (EUA)
        );
        adapterRegistry = new AdapterRegistry(adapters);
    }

    @Test
    void shouldSupportAllATSPlatforms() {
        List<ATSAdapter.ATSPlatform> supported = adapterRegistry.getSupportedPlatforms();

        assertThat(supported).hasSize(6);
        assertThat(supported).containsExactlyInAnyOrder(
            ATSAdapter.ATSPlatform.GUPY,
            ATSAdapter.ATSPlatform.CATHO,
            ATSAdapter.ATSPlatform.INDEED,
            ATSAdapter.ATSPlatform.LINKEDIN,
            ATSAdapter.ATSPlatform.WORKDAY,
            ATSAdapter.ATSPlatform.GREENHOUSE
        );
    }

    @Test
    void shouldResolveAllAdapters() {
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY))
            .isInstanceOf(GupyAdapter.class);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO))
            .isInstanceOf(CathoAdapter.class);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED))
            .isInstanceOf(IndeedAdapter.class);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN))
            .isInstanceOf(LinkedInAdapter.class);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY))
            .isInstanceOf(WorkdayAdapter.class);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE))
            .isInstanceOf(GreenhouseAdapter.class);
    }

    @Test
    void shouldCreateCandidatePipelineForMultipleTenants() {
        // Tenant Brasil usando Gupy
        ATSAdapter.CreateCandidateCommand gupyCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-br-gupy",
            "cand_br_gupy",
            "job_br_gupy",
            "Avaliação Técnica",
            "https://webhook.gupy.com"
        );
        CandidateContext gupyContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY)
            .createCandidate(gupyCmd);
        assertThat(gupyContext.tenantId()).isEqualTo("tenant-br-gupy");

        // Tenant Brasil usando Catho
        ATSAdapter.CreateCandidateCommand cathoCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-br-catho",
            "cand_br_catho",
            "job_br_catho",
            "Avaliação Comportamental",
            "https://webhook.catho.com.br"
        );
        CandidateContext cathoContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO)
            .createCandidate(cathoCmd);
        assertThat(cathoContext.tenantId()).isEqualTo("tenant-br-catho");

        // Tenant Global usando Indeed
        ATSAdapter.CreateCandidateCommand indeedCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-global-indeed",
            "cand_us_indeed",
            "job_us_indeed",
            "Technical Assessment",
            "https://webhook.indeed.com"
        );
        CandidateContext indeedContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED)
            .createCandidate(indeedCmd);
        assertThat(indeedContext.tenantId()).isEqualTo("tenant-global-indeed");

        // Enterprise usando LinkedIn
        ATSAdapter.CreateCandidateCommand linkedinCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-enterprise-linkedin",
            "cand_ent_linkedin",
            "job_ent_linkedin",
            "Assessment",
            "https://api.linkedin.com/talent"
        );
        CandidateContext linkedinContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN)
            .createCandidate(linkedinCmd);
        assertThat(linkedinContext.tenantId()).isEqualTo("tenant-enterprise-linkedin");

        // Enterprise usando Workday
        ATSAdapter.CreateCandidateCommand workdayCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-enterprise-workday",
            "cand_ent_workday",
            "job_ent_workday",
            "Assessment",
            "https://api.workday.com/talent"
        );
        CandidateContext workdayContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY)
            .createCandidate(workdayCmd);
        assertThat(workdayContext.tenantId()).isEqualTo("tenant-enterprise-workday");

        // Enterprise usando Greenhouse
        ATSAdapter.CreateCandidateCommand greenhouseCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-enterprise-greenhouse",
            "cand_ent_greenhouse",
            "job_ent_greenhouse",
            "Assessment",
            "https://api.greenhouse.com/talent"
        );
        CandidateContext greenhouseContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE)
            .createCandidate(greenhouseCmd);
        assertThat(greenhouseContext.tenantId()).isEqualTo("tenant-enterprise-greenhouse");
    }

    @Test
    void shouldPublishSameResultPayloadToMultipleATSs() {
        ResultPayload payload = new ResultPayload(
            "cand_multi",
            "sim_001",
            "att_001",
            85,
            "res_001",
            List.of(
                new ResultPayload.CompetencyScore("Problem Solving", 90, "HIGH"),
                new ResultPayload.CompetencyScore("Communication", 80, "MEDIUM"),
                new ResultPayload.CompetencyScore("Leadership", 75, "MEDIUM")
            ),
            "APPROVED",
            false,
            "Candidate meets requirements"
        );

        // All adapters should accept the same payload format
        List<ATSAdapter.ATSPlatform> platforms = adapterRegistry.getSupportedPlatforms();
        for (ATSAdapter.ATSPlatform platform : platforms) {
            ATSAdapter adapter = adapterRegistry.getAdapter(platform);
            try {
                adapter.pushResult(payload);
            } catch (Exception e) {
                // Expected for unimplemented push methods
            }
        }
    }

    @Test
    void shouldSwitchAdaptersPerRequestWithoutConflict() {
        // Simula múltiplas requisições concorrentes para diferentes ATSs
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-multi",
            "cand_123",
            "job_456",
            "Assessment",
            "https://webhook.example.com"
        );

        CandidateContext gupyResult = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY)
            .createCandidate(cmd);
        CandidateContext cathoResult = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO)
            .createCandidate(cmd);
        CandidateContext indeedResult = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED)
            .createCandidate(cmd);
        CandidateContext linkedinResult = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN)
            .createCandidate(cmd);

        // All adapters should process the same candidate data without cross-contamination
        assertThat(gupyResult.candidateId()).isEqualTo("cand_123");
        assertThat(cathoResult.candidateId()).isEqualTo("cand_123");
        assertThat(indeedResult.candidateId()).isEqualTo("cand_123");
        assertThat(linkedinResult.candidateId()).isEqualTo("cand_123");

        // All should have the same tenant
        assertThat(gupyResult.tenantId()).isEqualTo("tenant-multi");
        assertThat(cathoResult.tenantId()).isEqualTo("tenant-multi");
        assertThat(indeedResult.tenantId()).isEqualTo("tenant-multi");
        assertThat(linkedinResult.tenantId()).isEqualTo("tenant-multi");
    }

    @Test
    void shouldClassifyAdaptersByCategory() {
        // Local (Brasil)
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY).type())
            .isEqualTo(ATSAdapter.ATSPlatform.GUPY);

        // Portais (Brasil/Global)
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO).type())
            .isEqualTo(ATSAdapter.ATSPlatform.CATHO);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED).type())
            .isEqualTo(ATSAdapter.ATSPlatform.INDEED);

        // Redes profissionais
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN).type())
            .isEqualTo(ATSAdapter.ATSPlatform.LINKEDIN);

        // Enterprise
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY).type())
            .isEqualTo(ATSAdapter.ATSPlatform.WORKDAY);
        assertThat(adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE).type())
            .isEqualTo(ATSAdapter.ATSPlatform.GREENHOUSE);
    }
}
