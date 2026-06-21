package br.com.iforce.praxis.recrutei.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item de resultado de competência.")
public record RecruteiTestResultItemResponse(
        String title,
        String description,
        int score,
        String type,
        String tier,
        String date
) {
}
