package br.com.iforce.praxis.gupy.dto;

import jakarta.validation.constraints.Size;

/** Evidências externas registradas por um responsável após a execução real na Gupy. */
public record GupyHomologationEvidenceRequest(
        boolean callbackConfirmed,
        boolean resultPagesConfirmed,
        boolean gupyApproved,
        boolean clientApproved,
        @Size(max = 2000) String notes
) {
}
