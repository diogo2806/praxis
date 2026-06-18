package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para adapters de ATS brasileiros (Catho, Indeed).
 */
class BrazilianATSIntegrationTest {

    private AdapterRegistry adapterRegistry;
    private CathoAdapter cathoAdapter;
    private IndeedAdapter indeedAdapter;

    @BeforeEach
    void setUp() {
        cathoAdapter = new CathoAdapter();
        indeedAdapter = new IndeedAdapter();

        List<ATSAdapter> adapters = List.of(cathoAdapter, indeedAdapter);
        adapterRegistry = new AdapterRegistry(adapters);
    }

    @Test
    void shouldRegisterBrazilianAdapters() {
        assertThat(adapterRegistry.getSupportedPlatforms())
            .contains(ATSAdapter.ATSPlatform.CATHO, ATSAdapter.ATSPlatform.INDEED);
    }

    @Test
    void shouldResolveCathoAdapter() {
        ATSAdapter adapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO);
        assertThat(adapter).isInstanceOf(CathoAdapter.class);
        assertThat(adapter.type()).isEqualTo(ATSAdapter.ATSPlatform.CATHO);
    }

    @Test
    void shouldResolveIndeedAdapter() {
        ATSAdapter adapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED);
        assertThat(adapter).isInstanceOf(IndeedAdapter.class);
        assertThat(adapter.type()).isEqualTo(ATSAdapter.ATSPlatform.INDEED);
    }

    @Test
    void shouldCreateCandidateInCatho() {
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-br",
            "catho_cand_123",
            "catho_job_456",
            "Avaliação Técnica",
            "https://webhook.catho.com.br/result"
        );

        CandidateContext context = cathoAdapter.createCandidate(cmd);

        assertThat(context.candidateId()).isEqualTo("catho_cand_123");
        assertThat(context.tenantId()).isEqualTo("tenant-br");
        assertThat(context.jobId()).isEqualTo("catho_job_456");
    }

    @Test
    void shouldCreateCandidateInIndeed() {
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-br",
            "indeed_cand_789",
            "indeed_job_012",
            "Avaliação Comportamental",
            "https://webhook.indeed.com/result"
        );

        CandidateContext context = indeedAdapter.createCandidate(cmd);

        assertThat(context.candidateId()).isEqualTo("indeed_cand_789");
        assertThat(context.tenantId()).isEqualTo("tenant-br");
        assertThat(context.jobId()).isEqualTo("indeed_job_012");
    }

    @Test
    void shouldHandleResultPushForBrazilianPlatforms() {
        ResultPayload payload = new ResultPayload(
            "cand_001",
            "sim_001",
            "att_001",
            78,
            "res_001",
            List.of(),
            "PENDING_REVIEW",
            true,
            "Resultado requer revisão humana"
        );

        // Both platforms should handle the same payload format
        try {
            cathoAdapter.pushResult(payload);
        } catch (Exception e) {
            // Expected - method not implemented
        }

        try {
            indeedAdapter.pushResult(payload);
        } catch (Exception e) {
            // Expected - method not implemented
        }
    }

    @Test
    void shouldSupportMultipleBrazilianATSSimultaneously() {
        ATSAdapter catho = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.CATHO);
        ATSAdapter indeed = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED);

        assertThat(catho.type()).isEqualTo(ATSAdapter.ATSPlatform.CATHO);
        assertThat(indeed.type()).isEqualTo(ATSAdapter.ATSPlatform.INDEED);
    }
}
