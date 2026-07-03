package br.com.iforce.praxis.simulation.dto;

public record SimulationMonitoringResponse(
        String simulationId,
        int versionNumber,
        long attemptsCreated,
        long attemptsNotStarted,
        long attemptsInProgress,
        long attemptsCompleted,
        long attemptsAbandoned,
        long attemptsExpired,
        double completionRatePercent,
        double dropOffRatePercent
) {
}
