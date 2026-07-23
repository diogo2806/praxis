package br.com.iforce.praxis.campaign.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignCsvParserTest {

    private final CampaignCsvParser parser = new CampaignCsvParser();

    @Test
    void shouldPreviewThreeHundredValidParticipantsWithoutPersistingAnything() {
        StringBuilder csv = new StringBuilder("name,email,consent,accommodation_multiplier\n");
        for (int index = 1; index <= 300; index++) {
            csv.append("Pessoa ").append(index)
                    .append(",pessoa").append(index).append("@example.com,sim,1.5\n");
        }

        CampaignCsvParser.ParsedCsv parsed = parser.parse(csv.toString(), true);

        assertThat(parsed.headerErrors()).isEmpty();
        assertThat(parsed.rows()).hasSize(300);
        assertThat(parsed.validParticipants()).hasSize(300);
        assertThat(parsed.rows()).allMatch(row -> row.diagnostic().valid());
    }

    @Test
    void shouldReportDuplicateInvalidEmailAndMissingConsentByRow() {
        String csv = """
                nome;e-mail;consentimento;tempo_extra
                Ana;ana@example.com;sim;1,5
                Ana duplicada;ana@example.com;sim;1
                Sem email;email-invalido;sim;1
                Sem consentimento;sem.consentimento@example.com;nao;1
                """;

        CampaignCsvParser.ParsedCsv parsed = parser.parse(csv, true);

        assertThat(parsed.validParticipants()).hasSize(1);
        assertThat(parsed.rows()).hasSize(4);
        assertThat(parsed.rows().get(1).diagnostic().errors()).contains("E-mail duplicado no arquivo.");
        assertThat(parsed.rows().get(2).diagnostic().errors()).contains("E-mail inválido.");
        assertThat(parsed.rows().get(3).diagnostic().errors())
                .contains("Consentimento obrigatório não confirmado.");
    }

    @Test
    void shouldRejectMissingConsentHeaderWhenCampaignRequiresIt() {
        CampaignCsvParser.ParsedCsv parsed = parser.parse(
                "name,email\nPessoa,pessoa@example.com\n",
                true
        );

        assertThat(parsed.headerErrors())
                .contains("Cabeçalho obrigatório ausente para esta campanha: consent.");
        assertThat(parsed.validParticipants()).isEmpty();
    }
}
