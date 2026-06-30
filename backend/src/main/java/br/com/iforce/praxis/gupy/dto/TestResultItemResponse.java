package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.Map;


@Schema(description = "Item de resultado (major/minor)")
public record TestResultItemResponse(
        int score,
        String result_string,
        String type_result,                  // "percentage"
        String tier,                         // "major" ou "minor"
        String title,
        String description,
        String date,
        Map<String, Object> other_informations
) {
}
