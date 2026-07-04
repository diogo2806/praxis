package br.com.iforce.praxis.term.model;

/**
 * Termo de uso na vertical de saúde, aceito pelo recrutador ou administrador antes de publicar
 * uma avaliação quando a empresa opera nessa vertical (Minuta C — uso educativo, LGPD dado sensível).
 *
 * <p>É um termo-resumo exibido no produto, complementar ao {@link ResponsibilityTerm}. Os termos
 * contratuais vinculantes (Termos de Uso/DPA) são tratados à parte com advogado; ao mudar este
 * texto, incremente {@link #VERSION} para exigir novo aceite.</p>
 */
public final class HealthUseTerm {

    /** Identificador interno do termo de uso em saúde no histórico de aceites. */
    public static final String TYPE = "HEALTH_USE";

    /** Versão vigente exibida ao usuário; deve mudar quando o texto do termo mudar. */
    public static final String VERSION = "2026-06-01";

    /** Texto apresentado antes de seguir com o fluxo de publicação na vertical de saúde. */
    public static final String TEXT =
            "Ao usar a Práxis na área de saúde, você declara que: (1) usa a ferramenta como apoio "
                    + "educativo/treinamento de decisão, sem fins de diagnóstico, prescrição ou conduta "
                    + "clínica; (2) é o controlador dos dados dos participantes e define finalidade, base "
                    + "legal e retenção, sendo a Práxis operadora nos limites do contrato e do DPA; (3) "
                    + "declara possuir base legal válida para dado sensível de saúde e coleta o consentimento "
                    + "específico quando aplicável, inclusive para meno"
                    + "res e vulneráveis; (4) não usa o "
                    + "resultado, isoladamente, para decisão automatizada sobre tratamento, atendimento ou "
                    + "acesso a serviços de saúde, mantendo revisão humana; (5) responde pelo conteúdo "
                    + "dos cenários, sem prometer cura ou resultado clínico; e (6) disponibiliza política "
                    + "de privacidade e canal do Encarregado (DPO) aos participantes.";

    /**
     * Impede a criação de objetos desta classe.
     *
     * <p>Ela funciona apenas como fonte oficial do texto, do tipo e da versão do termo
     * usados pelo processo de aceite na vertical de saúde.</p>
     */
    private HealthUseTerm() {
    }
}
