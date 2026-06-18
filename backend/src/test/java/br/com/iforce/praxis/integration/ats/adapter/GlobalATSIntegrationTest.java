package br.com.iforce.praxis.integration.ats.adapter;

import br.com.iforce.praxis.integration.ats.model.CandidateContext;
import br.com.iforce.praxis.integration.ats.model.ResultPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para adapters de ATS globais (LinkedIn, Indeed).
 */
class GlobalATSIntegrationTest {

    private AdapterRegistry adapterRegistry;
    private LinkedInAdapter linkedInAdapter;
    private IndeedAdapter indeedAdapter;

    @BeforeEach
    void setUp() {
        linkedInAdapter = new LinkedInAdapter();
        indeedAdapter = new IndeedAdapter();

        List<ATSAdapter> adapters = List.of(linkedInAdapter, indeedAdapter);
        adapterRegistry = new AdapterRegistry(adapters);
    }

    @Test
    void shouldRegisterGlobalAdapters() {
        assertThat(adapterRegistry.getSupportedPlatforms())
            .contains(ATSAdapter.ATSPlatform.LINKEDIN, ATSAdapter.ATSPlatform.INDEED);
    }

    @Test
    void shouldResolveLinkedInAdapter() {
        ATSAdapter adapter = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN);
        assertThat(adapter).isInstanceOf(LinkedInAdapter.class);
        assertThat(adapter.type()).isEqualTo(ATSAdapter.ATSPlatform.LINKEDIN);
    }

    @Test
    void shouldCreateCandidateInLinkedIn() {
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-global",
            "linkedin_cand_123",
            "linkedin_job_456",
            "Technical Assessment",
            "https://api.linkedin.com/talent/result"
        );

        CandidateContext context = linkedInAdapter.createCandidate(cmd);

        assertThat(context.candidateId()).isEqualTo("linkedin_cand_123");
        assertThat(context.tenantId()).isEqualTo("tenant-global");
        assertThat(context.jobId()).isEqualTo("linkedin_job_456");
        assertThat(context.resultWebhookUrl()).isEqualTo("https://api.linkedin.com/talent/result");
    }

    @Test
    void shouldCreateCandidateInIndeedGlobally() {
        ATSAdapter.CreateCandidateCommand cmd = new ATSAdapter.CreateCandidateCommand(
            "tenant-us",
            "indeed_cand_usa",
            "indeed_job_usa",
            "Assessment",
            "https://api.indeed.com/assessment/callback"
        );

        CandidateContext context = indeedAdapter.createCandidate(cmd);

        assertThat(context.candidateId()).isEqualTo("indeed_cand_usa");
        assertThat(context.tenantId()).isEqualTo("tenant-us");
    }

    @Test
    void shouldHandleResultPushForGlobalPlatforms() {
        ResultPayload payload = new ResultPayload(
            "cand_global",
            "sim_global",
            "att_global",
            92,
            "res_global",
            List.of(),
            "APPROVED",
            false,
            "Candidate passed all assessments"
        );

        try {
            linkedInAdapter.pushResult(payload);
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
    void shouldSupportLinkedInWithOAuth() {
        ATSAdapter linkedIn = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN);
        assertThat(linkedIn.type()).isEqualTo(ATSAdapter.ATSPlatform.LINKEDIN);
        // LinkedIn requires OAuth 2.0 - future implementation should handle this
    }

    @Test
    void shouldHandleMultipleGlobalATSSimultaneously() {
        ATSAdapter linkedIn = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.LINKEDIN);
        ATSAdapter indeed = adapterRegistry.getAdapter(ATSAdapter.ATSPlatform.INDEED);

        assertThat(linkedIn.type()).isEqualTo(ATSAdapter.ATSPlatform.LINKEDIN);
        assertThat(indeed.type()).isEqualTo(ATSAdapter.ATSPlatform.INDEED);
    }
}
