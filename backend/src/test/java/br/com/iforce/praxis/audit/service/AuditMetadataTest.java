package br.com.iforce.praxis.audit.service;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;


import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


class AuditMetadataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditMetadata auditMetadata = new AuditMetadata(objectMapper);

    @Test
    void serializesMetadataWithEscapedStrings() throws Exception {
        String json = auditMetadata.of(
                "nodeId", "node\"with\\chars\nnext",
                "optionId", "option-1",
                "timedOut", true
        );

        Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(parsed)
                .containsEntry("nodeId", "node\"with\\chars\nnext")
                .containsEntry("optionId", "option-1")
                .containsEntry("timedOut", true);
    }

    @Test
    void rejectsOddKeyValueArguments() {
        assertThatThrownBy(() -> auditMetadata.of("nodeId"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Estado interno inválido.");
    }
}
