package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.simulation.dto.CalibrationReportResponse;

import br.com.iforce.praxis.simulation.dto.CompetencyCalibrationDto;

import br.com.iforce.praxis.simulation.dto.OptionDiscriminationDto;

import br.com.iforce.praxis.simulation.dto.OptionDiscriminationDto.CalibrationFlag;

import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;

import java.util.Comparator;

import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Set;


/**
 * Calibração estatística automática de uma versão de simulação.
 *
 * <p>A partir das tentativas já concluídas, calcula para cada opção um índice
 * de discriminação e um índice de dificuldade, e por competência a média e o
 * desvio-padrão das notas. Não usa IA: são estatísticas determinísticas sobre
 * as respostas reais, reaproveitando a mesma pontuação final já calculada pelo
 * {@code ResultScoringService} e guardada na tentativa.</p>
 *
 * <p><b>Discriminação</b> de uma opção = proporção de quem escolheu a opção no
 * grupo de melhor desempenho (top 27% pela nota final) menos a proporção no
 * grupo de pior desempenho (bottom 27%). <b>Dificuldade</b> = proporção de quem
 * respondeu a etapa e escolheu a opção.</p>
 *
 * <p>A leitura (flag) considera se a opção é a de maior pontuação na etapa (a
 * "esperada"): para ela, discriminar bem é positivo; para um distrator,
 * discriminar bem é justamente o contrário (atrair mais quem performa pior).</p>
 */
@Service
public class SimulationCalibrationService {

    /** Amostra mínima de tentativas concluídas para liberar o relatório. */
    static final int MINIMUM_SAMPLE_REQUIRED = 30;

    /** Fração de cada extremo (melhores e piores notas) usada na discriminação. */
    private static final double EXTREME_GROUP_RATIO = 0.27;

    /** Discriminação a partir da qual uma opção esperada é considerada boa. */
    private static final double GOOD_DISCRIMINATION = 0.2;

    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentEmpresaService currentEmpresaService;

    public SimulationCalibrationService(
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentEmpresaService = currentEmpresaService;
    }

    /**
     * Gera o relatório de calibração de uma versão da prova.
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return o relatório com itens e competências, ou um relatório de amostra
     *         insuficiente quando ainda não há tentativas concluídas o bastante
     */
    @Transactional(readOnly = true)
    public CalibrationReportResponse getCalibrationReport(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity version = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(empresaId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));

        List<CandidateAttemptEntity> attempts = candidateAttemptRepository
                .findByEmpresaIdAndSimulationVersionIdAndStatus(empresaId, version.getId(), AttemptStatus.COMPLETED);

        return calibrate(version, attempts);
    }

    /**
     * Calcula o relatório a partir de uma versão e de uma amostra de tentativas
     * concluídas. Separado do acesso a banco para facilitar os testes com
     * amostras sintéticas conhecidas.
     */
    CalibrationReportResponse calibrate(SimulationVersionEntity version, List<CandidateAttemptEntity> attempts) {
        long sampleSize = attempts.size();
        if (sampleSize < MINIMUM_SAMPLE_REQUIRED) {
            return CalibrationReportResponse.insufficient(sampleSize, MINIMUM_SAMPLE_REQUIRED);
        }

        List<AttemptView> views = attempts.stream().map(AttemptView::of).toList();
        List<AttemptView> orderedByScore = new ArrayList<>(views);
        orderedByScore.sort(Comparator.comparingInt(AttemptView::finalScore));

        int extremeSize = Math.max(1, (int) Math.floor(sampleSize * EXTREME_GROUP_RATIO));
        List<AttemptView> bottomGroup = orderedByScore.subList(0, extremeSize);
        List<AttemptView> topGroup = orderedByScore.subList(orderedByScore.size() - extremeSize, orderedByScore.size());

        List<OptionDiscriminationDto> items = new ArrayList<>();
        for (SimulationNodeEntity node : version.getNodes()) {
            if (node.isFinal() || node.getOptions().isEmpty()) {
                continue;
            }
            int maxPointTotal = node.getOptions().stream()
                    .mapToInt(SimulationCalibrationService::pointTotal)
                    .max()
                    .orElse(0);
            for (SimulationOptionEntity option : node.getOptions()) {
                items.add(scoreOption(node, option, maxPointTotal, views, topGroup, bottomGroup));
            }
        }

        List<CompetencyCalibrationDto> competencies = new ArrayList<>();
        for (SimulationCompetencyEntity competency : version.getCompetencies()) {
            competencies.add(scoreCompetency(competency.getName(), views));
        }

        return new CalibrationReportResponse(sampleSize, MINIMUM_SAMPLE_REQUIRED, true, items, competencies);
    }

    private OptionDiscriminationDto scoreOption(
            SimulationNodeEntity node,
            SimulationOptionEntity option,
            int maxPointTotal,
            List<AttemptView> all,
            List<AttemptView> topGroup,
            List<AttemptView> bottomGroup
    ) {
        String nodeId = node.getNodeId();
        String optionId = option.getOptionId();

        long answeredAll = all.stream().filter(view -> view.answered(nodeId)).count();
        long choseAll = all.stream().filter(view -> view.chose(nodeId, optionId)).count();
        double difficulty = answeredAll == 0 ? 0.0 : (double) choseAll / answeredAll;

        double pTop = selectionRate(topGroup, nodeId, optionId);
        double pBottom = selectionRate(bottomGroup, nodeId, optionId);
        double discrimination = pTop - pBottom;

        boolean isExpected = maxPointTotal > 0 && pointTotal(option) == maxPointTotal;
        return new OptionDiscriminationDto(
                nodeId,
                optionId,
                option.getText(),
                round(discrimination, 2),
                round(difficulty, 2),
                flagFor(discrimination, isExpected)
        );
    }

    /**
     * Para a opção esperada (a de maior pontuação na etapa), discriminar bem é
     * positivo. Para um distrator, o esperado é discriminação não-positiva
     * (quem performa pior tende a escolhê-lo mais), então discriminação positiva
     * forte é sinal de calibração invertida.
     */
    private CalibrationFlag flagFor(double discrimination, boolean isExpected) {
        if (isExpected) {
            if (discrimination >= GOOD_DISCRIMINATION) {
                return CalibrationFlag.OK;
            }
            return discrimination < 0 ? CalibrationFlag.REVISAR : CalibrationFlag.FRACO;
        }
        if (discrimination <= 0) {
            return CalibrationFlag.OK;
        }
        return discrimination > GOOD_DISCRIMINATION ? CalibrationFlag.REVISAR : CalibrationFlag.FRACO;
    }

    private CompetencyCalibrationDto scoreCompetency(String competencyName, List<AttemptView> views) {
        List<Integer> scores = views.stream()
                .map(view -> view.competencyScore(competencyName))
                .filter(java.util.Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return new CompetencyCalibrationDto(competencyName, 0.0, 0.0);
        }
        double mean = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = scores.stream()
                .mapToDouble(score -> (score - mean) * (score - mean))
                .average()
                .orElse(0.0);
        return new CompetencyCalibrationDto(competencyName, round(mean, 1), round(Math.sqrt(variance), 1));
    }

    private static double selectionRate(List<AttemptView> group, String nodeId, String optionId) {
        long answered = group.stream().filter(view -> view.answered(nodeId)).count();
        if (answered == 0) {
            return 0.0;
        }
        long chose = group.stream().filter(view -> view.chose(nodeId, optionId)).count();
        return (double) chose / answered;
    }

    private static int pointTotal(SimulationOptionEntity option) {
        return option.getCompetencyScores().stream()
                .mapToInt(OptionCompetencyScoreEntity::getScore)
                .sum();
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    /** Projeção imutável de uma tentativa com o necessário para a calibração. */
    private record AttemptView(
            int finalScore,
            Set<String> answeredNodes,
            Map<String, String> chosenOptionByNode,
            Map<String, Integer> competencyScores
    ) {

        static AttemptView of(CandidateAttemptEntity attempt) {
            Set<String> answered = new HashSet<>();
            Map<String, String> chosen = new HashMap<>();
            for (AttemptAnswerEntity answer : attempt.getAnswers()) {
                answered.add(answer.getNodeId());
                if (answer.getOptionId() != null) {
                    chosen.put(answer.getNodeId(), answer.getOptionId());
                }
            }
            Map<String, Integer> competencyScores = new HashMap<>();
            for (ResultItemEntity item : attempt.getResultItems()) {
                competencyScores.put(item.getName().toLowerCase(Locale.ROOT), item.getScore());
            }
            int score = attempt.getScore() == null ? 0 : attempt.getScore();
            return new AttemptView(score, answered, chosen, competencyScores);
        }

        boolean answered(String nodeId) {
            return answeredNodes.contains(nodeId);
        }

        boolean chose(String nodeId, String optionId) {
            return optionId.equals(chosenOptionByNode.get(nodeId));
        }

        Integer competencyScore(String competencyName) {
            return competencyScores.get(competencyName.toLowerCase(Locale.ROOT));
        }
    }
}
