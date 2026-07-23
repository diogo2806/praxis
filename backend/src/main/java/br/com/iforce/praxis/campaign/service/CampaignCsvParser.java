package br.com.iforce.praxis.campaign.service;

import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignParticipantInput;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvRowDiagnostic;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CampaignCsvParser {

    private static final int MAX_ROWS = 5_000;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> ACCEPTED_CONSENT = Set.of("true", "1", "sim", "yes", "y", "s");

    public ParsedCsv parse(String content, boolean consentRequired) {
        if (content == null || content.isBlank()) {
            return new ParsedCsv(List.of(), List.of(), List.of());
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        List<String> records = splitRecords(normalized);
        if (records.isEmpty()) {
            return new ParsedCsv(List.of(), List.of(), List.of());
        }
        char separator = detectSeparator(records.getFirst());
        List<String> rawHeaders = parseRecord(records.getFirst(), separator);
        List<String> headers = rawHeaders.stream().map(this::normalizeHeader).toList();
        Map<String, Integer> indexes = headerIndexes(headers);
        List<String> headerErrors = validateHeaders(indexes, consentRequired);
        if (!headerErrors.isEmpty()) {
            return new ParsedCsv(rawHeaders, List.of(), headerErrors);
        }
        if (records.size() - 1 > MAX_ROWS) {
            return new ParsedCsv(
                    rawHeaders,
                    List.of(),
                    List.of("O arquivo excede o limite de " + MAX_ROWS + " participantes por campanha.")
            );
        }

        List<ParsedRow> rows = new ArrayList<>();
        Set<String> emails = new LinkedHashSet<>();
        for (int recordIndex = 1; recordIndex < records.size(); recordIndex++) {
            if (records.get(recordIndex).isBlank()) continue;
            int rowNumber = recordIndex + 1;
            List<String> columns = parseRecord(records.get(recordIndex), separator);
            String name = value(columns, indexes.get("name"));
            String email = value(columns, indexes.get("email")).toLowerCase(Locale.ROOT);
            String consentValue = value(columns, indexes.get("consent"));
            String accommodationValue = value(columns, indexes.get("accommodation_multiplier"));
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            if (name.isBlank()) errors.add("Nome obrigatório.");
            if (name.length() > 180) errors.add("Nome excede 180 caracteres.");
            if (email.isBlank()) {
                errors.add("E-mail obrigatório.");
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.add("E-mail inválido.");
            } else if (!emails.add(email)) {
                errors.add("E-mail duplicado no arquivo.");
            }
            boolean consent = ACCEPTED_CONSENT.contains(consentValue.toLowerCase(Locale.ROOT));
            if (consentRequired && !consent) {
                errors.add("Consentimento obrigatório não confirmado.");
            }
            BigDecimal accommodation = parseAccommodation(accommodationValue, errors);
            if (!consentRequired && consentValue.isBlank()) {
                warnings.add("Consentimento não informado; a campanha foi configurada sem exigência obrigatória.");
            }
            rows.add(new ParsedRow(
                    new CampaignParticipantInput(rowNumber, name, email, consent, accommodation),
                    new CsvRowDiagnostic(rowNumber, name, email, errors.isEmpty(), List.copyOf(errors), List.copyOf(warnings))
            ));
        }
        return new ParsedCsv(rawHeaders, List.copyOf(rows), List.of());
    }

    private List<String> validateHeaders(Map<String, Integer> indexes, boolean consentRequired) {
        List<String> errors = new ArrayList<>();
        if (!indexes.containsKey("name")) errors.add("Cabeçalho obrigatório ausente: name.");
        if (!indexes.containsKey("email")) errors.add("Cabeçalho obrigatório ausente: email.");
        if (consentRequired && !indexes.containsKey("consent")) {
            errors.add("Cabeçalho obrigatório ausente para esta campanha: consent.");
        }
        return errors;
    }

    private Map<String, Integer> headerIndexes(List<String> headers) {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            indexes.putIfAbsent(headers.get(index), index);
        }
        return indexes;
    }

    private String normalizeHeader(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "nome", "candidate_name", "candidato" -> "name";
            case "e-mail", "mail", "candidate_email" -> "email";
            case "consentimento", "lgpd", "consent_confirmed" -> "consent";
            case "accommodation", "multiplicador", "tempo_extra" -> "accommodation_multiplier";
            default -> normalized;
        };
    }

    private BigDecimal parseAccommodation(String value, List<String> errors) {
        if (value == null || value.isBlank()) return BigDecimal.ONE;
        try {
            BigDecimal parsed = new BigDecimal(value.trim().replace(',', '.'));
            if (parsed.compareTo(BigDecimal.ONE) < 0 || parsed.compareTo(new BigDecimal("3.00")) > 0) {
                errors.add("O multiplicador de acomodação deve estar entre 1,00 e 3,00.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.add("Multiplicador de acomodação inválido.");
            return BigDecimal.ONE;
        }
    }

    private char detectSeparator(String header) {
        int commas = countOutsideQuotes(header, ',');
        int semicolons = countOutsideQuotes(header, ';');
        return semicolons > commas ? ';' : ',';
    }

    private int countOutsideQuotes(String value, char separator) {
        boolean quoted = false;
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < value.length() && value.charAt(index + 1) == '"') {
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && current == separator) {
                count++;
            }
        }
        return count;
    }

    private List<String> splitRecords(String content) {
        List<String> records = new ArrayList<>();
        StringBuilder record = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (current == '"') {
                record.append(current);
                if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
                    record.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == '\n' && !quoted) {
                records.add(record.toString());
                record.setLength(0);
            } else {
                record.append(current);
            }
        }
        if (!record.isEmpty()) records.add(record.toString());
        return records;
    }

    private List<String> parseRecord(String record, char separator) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < record.length(); index++) {
            char current = record.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < record.length() && record.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == separator && !quoted) {
                values.add(value.toString().trim());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString().trim());
        return values;
    }

    private String value(List<String> columns, Integer index) {
        if (index == null || index < 0 || index >= columns.size()) return "";
        return columns.get(index).trim();
    }

    public record ParsedRow(CampaignParticipantInput participant, CsvRowDiagnostic diagnostic) {
    }

    public record ParsedCsv(List<String> headers, List<ParsedRow> rows, List<String> headerErrors) {
        public List<CampaignParticipantInput> validParticipants() {
            return rows.stream()
                    .filter(row -> row.diagnostic().valid())
                    .map(ParsedRow::participant)
                    .toList();
        }
    }
}
