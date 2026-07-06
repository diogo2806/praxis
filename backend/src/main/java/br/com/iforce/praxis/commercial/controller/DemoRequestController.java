package br.com.iforce.praxis.commercial.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/demo-requests")
public class DemoRequestController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final JdbcTemplate jdbcTemplate;

    public DemoRequestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DemoRequestRequest request) {
        if (request == null || !isValid(request)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe nome, empresa e um e-mail corporativo válido."));
        }
        if (notBlank(request.website())) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        jdbcTemplate.update(
                "INSERT INTO demo_requests (name, email, company, role, hiring_volume, message, source) VALUES (?, ?, ?, ?, ?, ?, ?)",
                trim(request.name()), normalizeEmail(request.email()), trim(request.company()),
                nullIfBlank(request.role()), nullIfBlank(request.hiringVolume()), nullIfBlank(request.message()), nullIfBlank(request.source())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Solicitação recebida. Nossa equipe entrará em contato para combinar a demonstração."));
    }

    private boolean isValid(DemoRequestRequest request) {
        return hasLength(request.name(), 2, 120)
                && hasLength(request.company(), 2, 160)
                && hasLength(request.email(), 5, 320)
                && EMAIL_PATTERN.matcher(request.email().trim()).matches()
                && hasMaxLength(request.role(), 120)
                && hasMaxLength(request.hiringVolume(), 80)
                && hasMaxLength(request.message(), 1200)
                && hasMaxLength(request.source(), 120);
    }

    private boolean hasLength(String value, int min, int max) { return value != null && value.trim().length() >= min && value.trim().length() <= max; }
    private boolean hasMaxLength(String value, int max) { return value == null || value.trim().length() <= max; }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value.trim(); }
    private String nullIfBlank(String value) { return notBlank(value) ? value.trim() : null; }
    private String normalizeEmail(String value) { return value.trim().toLowerCase(Locale.ROOT); }

    public record DemoRequestRequest(String name, String email, String company, String role, String hiringVolume, String message, String source, String website) {}
}
