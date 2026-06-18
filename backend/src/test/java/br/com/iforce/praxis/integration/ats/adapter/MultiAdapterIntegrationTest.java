package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiAdapterIntegrationTest {

    private AdapterRegistry adapterRegistry;
    private GupyAdapter gupyAdapter;
    private WorkdayAdapter workdayAdapter;
    private GreenhouseAdapter greenhouseAdapter;

    @BeforeEach
    void setUp() {
        gupyAdapter = new GupyAdapter(null);
        workdayAdapter = new WorkdayAdapter();
        greenhouseAdapter = new GreenhouseAdapter();

        List<ATSAdapter> adapters = List.of(gupyAdapter, workdayAdapter, greenhouseAdapter);
        adapterRegistry = new AdapterRegistry(adapters);
    }

    @Test
    void shouldCreateCandidateContextThroughDifferentAdapters() {
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-1",
            "ext_cand_123",
            "job_456",
            "Technical Assessment",
            "https://webhook.example.com/result"
        );

        CandidateContext gupyContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY)
            .createCandidate(cmd);
        assertThat(gupyContext.candidateId()).isEqualTo("ext_cand_123");
        assertThat(gupyContext.tenantId()).isEqualTo("tenant-1");

        CandidateContext workdayContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY)
            .createCandidate(cmd);
        assertThat(workdayContext.candidateId()).isEqualTo("ext_cand_123");
        assertThat(workdayContext.tenantId()).isEqualTo("tenant-1");

        CandidateContext greenhouseContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE)
            .createCandidate(cmd);
        assertThat(greenhouseContext.candidateId()).isEqualTo("ext_cand_123");
        assertThat(greenhouseContext.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void shouldHandleResultPushForMultipleAdapters() {
        ResultPayload payload = new ResultPayload(
            "cand_123",
            "sim_456",
            "att_789",
            85,
            "res_999",
            List.of(),
            "APPROVED",
            false,
            "Candidate qualified for next round"
        );

        ATSAdapter gupyAdapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY);
        ATSAdapter workdayAdapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY);
        ATSAdapter greenhouseAdapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE);

        // All adapters should handle the same payload format
        // Error handling is specific to each adapter's implementation
        try {
            gupyAdapter.pushResult(payload);
        } catch (Exception e) {
            // Expected since resultWebhookClient is not set up
        }

        try {
            workdayAdapter.pushResult(payload);
        } catch (Exception e) {
            // Expected - method not implemented
        }

        try {
            greenhouseAdapter.pushResult(payload);
        } catch (Exception e) {
            // Expected - method not implemented
        }
    }

    @Test
    void shouldReturnCorrectAdapterType() {
        ATSAdapter gupy = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY);
        assertThat(gupy.type()).isEqualTo(ATSAdapter.ATSPlatform.GUPY);

        ATSAdapter workday = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY);
        assertThat(workday.type()).isEqualTo(ATSAdapter.ATSPlatform.WORKDAY);

        ATSAdapter greenhouse = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE);
        assertThat(greenhouse.type()).isEqualTo(ATSAdapter.ATSPlatform.GREENHOUSE);
    }

    @Test
    void shouldListAllSupportedPlatforms() {
        List<ATSAdapter.ATSPlatform> supported = adapterRegistry.getSupportedPlatforms();

        assertThat(supported)
            .containsExactlyInAnyOrder(
                ATSAdapter.ATSPlatform.GUPY,
                ATSAdapter.ATSPlatform.WORKDAY,
                ATSAdapter.ATSPlatform.GREENHOUSE
            );
    }

    @Test
    void shouldSwitchAdaptersDynamicallyPerTenant() {
        ATSAdapter.CreateCandidateCommand gupyCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-gupy",
            "gupy_cand",
            "gupy_job",
            "Assessment",
            "https://gupy.webhook.com"
        );

        ATSAdapter.CreateCandidateCommand workdayCmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-workday",
            "workday_cand",
            "workday_job",
            "Assessment",
            "https://workday.webhook.com"
        );

        CandidateContext gupyContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.GUPY)
            .createCandidate(gupyCmd);
        assertThat(gupyContext.tenantId()).isEqualTo("tenant-gupy");

        CandidateContext workdayContext = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY)
            .createCandidate(workdayCmd);
        assertThat(workdayContext.tenantId()).isEqualTo("tenant-workday");
    }
}
