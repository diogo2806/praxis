package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.gupy.model.ResultTier;

import br.com.iforce.praxis.simulation.dto.CalibrationReportResponse;

import br.com.iforce.praxis.simulation.dto.CompetencyCalibrationDto;

import br.com.iforce.praxis.simulation.dto.OptionDiscriminationDto;

import br.com.iforce.praxis.simulation.dto.OptionDiscriminationDto.CalibrationFlag;

import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import org.junit.jupiter.api.Test;


import java.util.ArrayList;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;


class SimulationCalibrationServiceTest {

    private final SimulationCalibrationService service = new SimulationCalibrationService(null, null, null);

    @Test
    void reportsInsufficientSampleBelowMinimum() {
        SimulationVersionEntity version = twoOptionVersion();
        List<CandidateAttemptEntity> attempts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            attempts.add(attempt(i + 1, "opcao-a", 70, 50));
        }

        CalibrationReportResponse report = service.calibrate(version, attempts);

        assertThat(report.sufficientSample()).isFalse();
        assertThat(report.sampleSize()).isEqualTo(6);
        assertThat(report.minimumSampleRequired()).isEqualTo(30);
        assertThat(report.items()).isEmpty();
        assertThat(report.competencies()).isEmpty();
    }

    @Test
    void flagsHealthyDiscriminationAsOk() {
        SimulationVersionEntity version = twoOptionVersion();
        List<CandidateAttemptEntity> attempts = new ArrayList<>();
        // Scores 1..40 garantem ordenação determinística: bottom = 1..10, top = 31..40.
        for (int score = 1; score <= 40; score++) {
            // Quem performa melhor escolhe a opção esperada (opcao-a); quem performa pior, o distrator.
            String choice = score > 20 ? "opcao-a" : "opcao-b";
            attempts.add(attempt(score, choice, 70, score <= 20 ? 40 : 60));
        }

        CalibrationReportResponse report = service.calibrate(version, attempts);

        assertThat(report.sufficientSample()).isTrue();
        assertThat(report.sampleSize()).isEqualTo(40);

        OptionDiscriminationDto expected = item(report, "opcao-a");
        assertThat(expected.discriminationIndex()).isEqualTo(1.0);
        assertThat(expected.flag()).isEqualTo(CalibrationFlag.OK);

        OptionDiscriminationDto distractor = item(report, "opcao-b");
        assertThat(distractor.discriminationIndex()).isEqualTo(-1.0);
        // Distrator que repele quem performa melhor está calibrado corretamente.
        assertThat(distractor.flag()).isEqualTo(CalibrationFlag.OK);
    }

    @Test
    void flagsInvertedExpectedOptionAsRevisar() {
        SimulationVersionEntity version = twoOptionVersion();
        List<CandidateAttemptEntity> attempts = new ArrayList<>();
        for (int score = 1; score <= 40; score++) {
            // Inversão: quem performa melhor escolhe o distrator e quem performa pior, a opção esperada.
            String choice = score > 20 ? "opcao-b" : "opcao-a";
            attempts.add(attempt(score, choice, 70, 50));
        }

        CalibrationReportResponse report = service.calibrate(version, attempts);

        OptionDiscriminationDto expected = item(report, "opcao-a");
        assertThat(expected.discriminationIndex()).isLessThan(0);
        assertThat(expected.flag()).isEqualTo(CalibrationFlag.REVISAR);

        OptionDiscriminationDto distractor = item(report, "opcao-b");
        assertThat(distractor.flag()).isEqualTo(CalibrationFlag.REVISAR);
    }

    @Test
    void computesCompetencyMeanAndStandardDeviation() {
        SimulationVersionEntity version = twoOptionVersion();
        List<CandidateAttemptEntity> attempts = new ArrayList<>();
        for (int score = 1; score <= 40; score++) {
            // Empatia constante (70) e Resolução alternando 40/60 → média 50, desvio populacional 10.
            attempts.add(attempt(score, "opcao-a", 70, score % 2 == 0 ? 40 : 60));
        }

        CalibrationReportResponse report = service.calibrate(version, attempts);

        CompetencyCalibrationDto empatia = competency(report, "Empatia");
        assertThat(empatia.averageScore()).isEqualTo(70.0);
        assertThat(empatia.stdDeviation()).isEqualTo(0.0);

        CompetencyCalibrationDto resolucao = competency(report, "Resolucao");
        assertThat(resolucao.averageScore()).isEqualTo(50.0);
        assertThat(resolucao.stdDeviation()).isEqualTo(10.0);
    }

    private OptionDiscriminationDto item(CalibrationReportResponse report, String optionId) {
        return report.items().stream()
                .filter(item -> item.optionId().equals(optionId))
                .findFirst()
                .orElseThrow();
    }

    private CompetencyCalibrationDto competency(CalibrationReportResponse report, String name) {
        return report.competencies().stream()
                .filter(item -> item.competencyName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private SimulationVersionEntity twoOptionVersion() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId("sim-test");
        simulation.setName("Sim Test");

        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setSimulation(simulation);
        version.setVersionNumber(1);
        version.setRootNodeId("turno-1");
        version.getCompetencies().add(competencyEntity(version, "Empatia", 0.5));
        version.getCompetencies().add(competencyEntity(version, "Resolucao", 0.5));

        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setSimulationVersion(version);
        node.setNodeId("turno-1");
        node.setTurnIndex(1);
        node.setSpeaker("Cliente");
        node.setMessage("Mensagem");
        node.getOptions().add(optionEntity(node, "opcao-a", 90, 90));
        node.getOptions().add(optionEntity(node, "opcao-b", 10, 10));
        version.getNodes().add(node);

        SimulationNodeEntity finalNode = new SimulationNodeEntity();
        finalNode.setSimulationVersion(version);
        finalNode.setNodeId("fim");
        finalNode.setTurnIndex(2);
        finalNode.setSpeaker("Cliente");
        finalNode.setMessage("Fim");
        finalNode.setFinal(true);
        version.getNodes().add(finalNode);
        return version;
    }

    private SimulationCompetencyEntity competencyEntity(SimulationVersionEntity version, String name, double weight) {
        SimulationCompetencyEntity competency = new SimulationCompetencyEntity();
        competency.setSimulationVersion(version);
        competency.setName(name);
        competency.setWeight(weight);
        return competency;
    }

    private SimulationOptionEntity optionEntity(SimulationNodeEntity node, String optionId, int empatia, int resolucao) {
        SimulationOptionEntity option = new SimulationOptionEntity();
        option.setSimulationNode(node);
        option.setOptionId(optionId);
        option.setText("Texto " + optionId);
        option.setNextNodeId("fim");
        option.setAuditNote("");
        option.getCompetencyScores().add(scoreEntity(option, "Empatia", empatia));
        option.getCompetencyScores().add(scoreEntity(option, "Resolucao", resolucao));
        return option;
    }

    private OptionCompetencyScoreEntity scoreEntity(SimulationOptionEntity option, String name, int score) {
        OptionCompetencyScoreEntity entity = new OptionCompetencyScoreEntity();
        entity.setSimulationOption(option);
        entity.setCompetencyName(name);
        entity.setScore(score);
        return entity;
    }

    private CandidateAttemptEntity attempt(int finalScore, String turno1Choice, int empatia, int resolucao) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setScore(finalScore);
        attempt.getAnswers().add(answer(attempt, "turno-1", turno1Choice));
        attempt.getResultItems().add(resultItem(attempt, "Empatia", empatia));
        attempt.getResultItems().add(resultItem(attempt, "Resolucao", resolucao));
        return attempt;
    }

    private AttemptAnswerEntity answer(CandidateAttemptEntity attempt, String nodeId, String optionId) {
        AttemptAnswerEntity answer = new AttemptAnswerEntity();
        answer.setCandidateAttempt(attempt);
        answer.setNodeId(nodeId);
        answer.setOptionId(optionId);
        return answer;
    }

    private ResultItemEntity resultItem(CandidateAttemptEntity attempt, String name, int score) {
        ResultItemEntity item = new ResultItemEntity();
        item.setCandidateAttempt(attempt);
        item.setName(name);
        item.setScore(score);
        item.setTier(ResultTier.MAJOR);
        return item;
    }
}
