package br.com.iforce.praxis.simulation.dto;

public record SimulationMonitoringResponse(
        String simulationId,
        int versionNumber,
        long attemptsCreated,
        long attemptsNotStarted,
        long attemptsInProgress,
        long attemptsPaused,
        long attemptsCompleted,
        long attemptsAbandoned,
        long attemptsExpired,
        long attemptsFailed,
        double completionRatePercent,
        double dropOffRatePercent
) {
}
