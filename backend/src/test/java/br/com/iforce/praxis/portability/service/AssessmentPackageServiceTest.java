package br.com.iforce.praxis.portability.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ImportPackageRequest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageEnvelope;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageManifest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageValidationResponse;
import br.com.iforce.praxis.shared.model.MediaType;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssessmentPackageServiceTest {

    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private SimulationRepository simulationRepository;
    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditEventService auditEventService;

    private AssessmentPackageValidator validator;
    private AssessmentPackageService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        validator = new AssessmentPackageValidator(objectMapper);
        service = new AssessmentPackageService(
                simulationVersionRepository,
                simulationRepository,
                currentEmpresaService,
                currentUserService,
                auditEventService,
                validator,
                objectMapper
        );
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(currentUserService.requiredUserId()).thenReturn("autor-1");
    }

    @Test
    void shouldExportValidateAndImportTheSameGraphAsIndependentDraft() {
        SimulationVersionEntity source = sourceVersion();
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-1",
                "atendimento",
                3
        )).thenReturn(Optional.of(source));
        when(simulationRepository.existsById(any())).thenReturn(false);
        when(simulationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PackageEnvelope exported = service.exportPackage("atendimento", 3);
        PackageValidationResponse validation = service.validate(exported);
        var imported = service.importPackage(new ImportPackageRequest(
                exported,
                "Atendimento importado",
                true,
                true
        ));

        assertThat(validation.valid()).isTrue();
        assertThat(imported.status()).isEqualTo("DRAFT");
        assertThat(imported.sourceAssessmentId()).isEqualTo("atendimento");
        assertThat(imported.sourceVersionNumber()).isEqualTo(3);
        assertThat(imported.packageHash()).isEqualTo(exported.contentHash());

        ArgumentCaptor<SimulationEntity> captor = ArgumentCaptor.forClass(SimulationEntity.class);
        verify(simulationRepository).save(captor.capture());
        SimulationEntity saved = captor.getValue();
        SimulationVersionEntity savedVersion = saved.getVersions().iterator().next();
        assertThat(savedVersion.getStatus()).isEqualTo(SimulationVersionStatus.DRAFT);
        assertThat(savedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(savedVersion.getRootNodeId()).isEqualTo("N1");
        assertThat(savedVersion.getNodes()).hasSize(2);
        assertThat(savedVersion.getCompetencies()).singleElement()
                .extracting(SimulationCompetencyEntity::getName)
                .isEqualTo("Comunicação");

        SimulationNodeEntity savedRoot = savedVersion.getNodes().stream()
                .filter(node -> node.getNodeId().equals("N1"))
                .findFirst()
                .orElseThrow();
        assertThat(savedRoot.getOptions()).hasSize(2);
        assertThat(savedRoot.getOptions().stream()
                .flatMap(option -> option.getCompetencyScores().stream())
                .map(OptionCompetencyScoreEntity::getScore))
                .containsExactlyInAnyOrder(90, 20);
    }

    @Test
    void shouldRejectPackageWhoseManifestWasChangedAfterHashing() {
        SimulationVersionEntity source = sourceVersion();
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-1",
                "atendimento",
                3
        )).thenReturn(Optional.of(source));
        PackageEnvelope exported = service.exportPackage("atendimento", 3);
        PackageManifest changedManifest = new PackageManifest(
                exported.manifest().origin(),
                new br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.AssessmentContent(
                        "Nome alterado",
                        exported.manifest().assessment().description(),
                        exported.manifest().assessment().criticalSituation(),
                        exported.manifest().assessment().resultUse()
                ),
                exported.manifest().version(),
                exported.manifest().mediaAssets()
        );
        PackageEnvelope changed = new PackageEnvelope(
                exported.formatVersion(),
                exported.exportedAt(),
                exported.contentHash(),
                changedManifest
        );

        PackageValidationResponse validation = service.validate(changed);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.errors())
                .anyMatch(problem -> problem.code().equals("HASH_MISMATCH") && problem.path().equals("$.contentHash"));
    }

    private SimulationVersionEntity sourceVersion() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("atendimento");
        simulation.setEmpresaId("empresa-1");
        simulation.setName("Atendimento");
        simulation.setDescription("Avaliação para decisões em atendimento.");
        simulation.setCriticalSituation("Cliente insatisfeito solicita solução imediata.");
        simulation.setResultUse("SELECAO");
        simulation.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(3);
        version.setStatus(SimulationVersionStatus.PUBLISHED);
        version.setRootNodeId("N1");
        version.setCreatedAt(Instant.parse("2026-01-02T00:00:00Z"));
        simulation.getVersions().add(version);

        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName("Comunicação");
        competency.setWeight(1.0);
        competency.setTargetScore(70);
        competency.setTier(ResultTier.MAJOR);
        version.getCompetencies().add(competency);

        SimulationNodeEntity root = new SimulationNodeEntity();
        root.setSimulationVersion(version);
        root.setNodeId("N1");
        root.setTurnIndex(1);
        root.setSpeaker("Cliente");
        root.setMessage("O pedido chegou incorreto. Como você resolve?");
        root.setTimeLimitSeconds(60);
        root.setFinal(false);
        root.setPlainTextDescription("Cliente relata erro no pedido.");
        root.setMediaType(MediaType.IMAGE);
        root.setMediaUrl(null);
        root.getOptions().add(option(root, "A", "Escutar e propor solução.", "F1", 90, false));
        root.getOptions().add(option(root, "B", "Encerrar sem investigar.", "F1", 20, true));
        version.getNodes().add(root);

        SimulationNodeEntity terminal = new SimulationNodeEntity();
        terminal.setSimulationVersion(version);
        terminal.setNodeId("F1");
        terminal.setTurnIndex(2);
        terminal.setSpeaker("Sistema");
        terminal.setMessage("Fim da situação.");
        terminal.setFinal(true);
        terminal.setReportText("Resultado consolidado da decisão.");
        version.getNodes().add(terminal);
        return version;
    }

    private SimulationOptionEntity option(
            SimulationNodeEntity node,
            String id,
            String text,
            String nextNodeId,
            int scoreValue,
            boolean critical
    ) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(id);
        option.setText(text);
        option.setNextNodeId(nextNodeId);
        option.setCritical(critical);
        option.setAuditNote("Justificativa comportamental da alternativa " + id + ".");

        OptionCompetencyScoreEntity score = new OptionCompetencyScoreEntity();
        score.setSimulationOption(option);
        score.setCompetencyName("Comunicação");
        score.setScore(scoreValue);
        option.getCompetencyScores().add(score);
        return option;
    }
}
