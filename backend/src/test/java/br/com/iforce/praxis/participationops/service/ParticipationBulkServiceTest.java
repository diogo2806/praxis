package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringResponse;
import br.com.iforce.praxis.candidate.service.ParticipationMonitoringQueryService;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkFilter;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationRef;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkItemRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkJobRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationTagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationBulkServiceTest {

    @Mock
    private ParticipationMonitoringQueryService queryService;
    @Mock
    private ParticipationBulkJobRepository jobRepository;
    @Mock
    private ParticipationBulkItemRepository itemRepository;
    @Mock
    private ParticipationTagRepository tagRepository;
    @Mock
    private ParticipationBulkWorker worker;
    @Mock
    private ParticipationBulkAuditService auditService;

    private ParticipationBulkService service;

    @BeforeEach
    void setUp() {
        EmpresaContextHolder.set("empresa-1");
        service = new ParticipationBulkService(
                queryService,
                jobRepository,
                itemRepository,
                tagRepository,
                worker,
                auditService,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        EmpresaContextHolder.clear();
    }

    @Test
    void previewSeparatesEligibleAndIncompatibleItems() {
        ParticipationMonitoringResponse eligible = participation(
                "journey-1",
                "journey",
                "created",
                "active",
                true,
                true,
                true
        );
        ParticipationMonitoringResponse incompatible = participation(
                "individual-1",
                "individual",
                "completed",
                "active",
                false,
                false,
                false
        );
        when(queryService.search(0, 100, null, null)).thenReturn(
                new ParticipationMonitoringPageResponse(List.of(eligible, incompatible), 0, 100, 2, 1)
        );

        BulkPreviewResponse preview = service.preview(new BulkPreviewRequest(
                "RESEND",
                "EXPLICIT",
                List.of(
                        new ParticipationRef("journey", "journey-1"),
                        new ParticipationRef("individual", "individual-1")
                ),
                new BulkFilter(null, null, null, null, null),
                null,
                null,
                null
        ));

        assertThat(preview.selectedCount()).isEqualTo(2);
        assertThat(preview.eligibleCount()).isEqualTo(1);
        assertThat(preview.excludedCount()).isEqualTo(1);
        assertThat(preview.excluded().getFirst().participationId()).isEqualTo("individual-1");
    }

    @Test
    void cancelRequiresJustificationBeforeProcessing() {
        BulkPreviewRequest request = new BulkPreviewRequest(
                "CANCEL",
                "EXPLICIT",
                List.of(new ParticipationRef("journey", "journey-1")),
                new BulkFilter(null, null, null, null, null),
                null,
                null,
                "  "
        );

        assertThatThrownBy(() -> service.preview(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("justificativa");
    }

    private static ParticipationMonitoringResponse participation(
            String id,
            String type,
            String status,
            String linkStatus,
            boolean canResend,
            boolean canExtend,
            boolean canCancel
    ) {
        Instant now = Instant.now();
        return new ParticipationMonitoringResponse(
                id,
                type,
                "Pessoa teste",
                "teste@example.com",
                "sim-1",
                "Avaliação teste",
                1,
                "journey-1",
                "Jornada teste",
                "principal",
                status,
                0,
                1,
                0,
                0,
                now,
                true,
                "/candidate/" + id,
                now.plusSeconds(86_400),
                linkStatus,
                1,
                canResend,
                canExtend,
                canCancel,
                null,
                now
        );
    }
}
