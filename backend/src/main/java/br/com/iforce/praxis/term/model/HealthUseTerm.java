package br.com.iforce.praxis.term.model;

/**
 * Termo de uso na vertical de saúde, aceito pelo recrutador/administrador antes de publicar uma
 * avaliação quando o empresa opera nessa vertical (Minuta C — uso educativo, LGPD dado sensível).
 *
 * <p>É um termo-resumo exibido no produto, complementar ao {@link ResponsibilityTerm}. Os termos
 * contratuais vinculantes (Termos de Uso/DPA) são tratados à parte com advogado; ao mudar este
 * texto, incremente {@link #VERSION} para exigir novo aceite.</p>
 */
public final class HealthUseTerm {

    public static final String TYPE = "HEALTH_USE";
    public static final String VERSION = "2026-06-01";
    public static final String TEXT =
            "Ao usar a Práxis na área de saúde, você declara que: (1) usa a ferramenta como apoio "
                    + "educativo/treinamento de decisão, sem fins de diagnóstico, prescrição ou conduta "
                    + "clínica; (2) é o controlador dos dados dos participantes e define finalidade, base "
                    + "legal e retenção, sendo a Práxis operadora nos limites do contrato e do DPA; (3) "
                    + "declara possuir base legal válida para dado sensível de saúde e coleta o consentimento "
                    + "específico quando aplicável, inclusive para menores e vulneráveis; (4) não usa o "
                    + "resultado, isoladamente, para decisão automatizada sobre tratamento, atendimento ou "
                    + "acesso a serviços de saúde, mantendo revisão humana; (5) responde pelo conteúdo "
                    + "dos cenários, sem prometer cura ou resultado clínico; e (6) disponibiliza política "
                    + "de privacidade e canal do Encarregado (DPO) aos participantes.";

    private HealthUseTerm() {
    }
}
