package br.com.iforce.praxis.shared.web;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exceção de negócio que carrega, além do status e da mensagem, um mapa de
 * detalhes ({@code fields}) que é exposto no corpo padronizado de erro da API.
 *
 * <p>Use quando a resposta de erro precisa explicar <em>quais</em> verificações
 * falharam, em vez de apenas uma mensagem genérica.
 */
public class ApiException extends RuntimeException {

    private final transient HttpStatus status;
    private final transient Map<String, String> fields;

    public ApiException(HttpStatus status, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.fields = fields == null || fields.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
