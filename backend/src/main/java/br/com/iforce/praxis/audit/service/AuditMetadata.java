package br.com.iforce.praxis.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AuditMetadata {

    private final ObjectMapper objectMapper;

    public AuditMetadata(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String of(Object... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Estado interno inválido.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            metadata.put(String.valueOf(kv[i]), kv[i + 1]);
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Estado interno inválido.", exception);
        }
    }
}
