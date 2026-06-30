package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;


import java.time.Instant;

import java.util.List;


public record ResultDetailResponse(
        String attemptId,
        Candidate candidate,
        Simulation simulation,
        AttemptStatus status,
        Instant startedAt,
        Instant finishedAt,
        Integer overallScore,
        List<Competency> competencies,
        List<Answer> answers,
        HumanDecision humanDecision
) {
    public record Candidate(String name, String email, String externalId) {
    }

    public record Simulation(String id, String title, Integer versionNumber) {
    }

    public record Competency(String name, int score, String level, String summary) {
    }

    public record Answer(String stepTitle, String question, String answer, Integer score) {
    }

    public record HumanDecision(String status, String decidedBy, Instant decidedAt, String note) {
    }
}
