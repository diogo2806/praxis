package br.com.iforce.praxis.jobpreview;

import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewReactionRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewContentRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RealisticJobPreviewContractTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresBalancedAndAccessibleContent() {
        PreviewContentRequest content = new PreviewContentRequest(
                "Atender clientes e registrar ocorrências.",
                "Pode priorizar a fila dentro das regras definidas.",
                "",
                "Contato contínuo com clientes e supervisão.",
                "",
                "Turnos com variação de demanda.",
                "",
                "Aprendizado e possibilidade de crescimento.",
                "",
                List.of(),
                List.of()
        );

        Set<String> invalidFields = validator.validate(content).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(invalidFields).contains(
                "pressureContext",
                "criticalSituations",
                "workConditions",
                "alternativeText"
        );
    }

    @Test
    void candidateContractsDoNotExposeScoreWeightOrExpectedAnswer() {
        Set<String> fields = List.of(CandidatePreviewResponse.class.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Set<String> reactionFields = List.of(CandidatePreviewReactionRequest.class.getRecordComponents()).stream()
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        assertThat(fields).noneMatch(name ->
                name.contains("score") || name.contains("weight") || name.contains("answer"));
        assertThat(reactionFields).containsExactlyInAnyOrder(
                "acknowledged",
                "voluntarywithdrawal",
                "clarityscore",
                "realismscore",
                "expectationcompatibilityscore"
        );
    }

    @Test
    void persistenceKeepsPresentedVersionAndNeverUpdatesCandidateScore() throws IOException {
        String migration;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V1108__create_realistic_job_previews.sql")) {
            assertThat(input).isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(migration)
                .contains("preview_version_id uuid not null")
                .contains("unique (candidate_attempt_id, preview_version_id, display_stage)")
                .contains("status in ('draft', 'published', 'archived')")
                .doesNotContain("update candidate_attempts")
                .doesNotContain("candidate_attempts set score");
    }
}
