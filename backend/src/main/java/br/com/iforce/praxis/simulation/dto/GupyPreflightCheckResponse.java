package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.simulation.model.GupyPreflightCheckCode;

import br.com.iforce.praxis.simulation.model.GupyPreflightCheckStatus;


public record GupyPreflightCheckResponse(
        GupyPreflightCheckCode code,
        GupyPreflightCheckStatus status,
        String message
) {
}
