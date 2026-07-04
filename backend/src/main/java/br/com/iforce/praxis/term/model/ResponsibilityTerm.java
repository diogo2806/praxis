package br.com.iforce.praxis.term.model;

/**
 * Termo de responsabilidade que o recrutador aceita antes de usar ou publicar um teste (REQ-L5).
 *
 * <p>Este é o texto-resumo exibido no produto, que reforça a postura de "ferramenta de apoio,
 * não decisora". Os termos contratuais vinculantes (Termos de Uso/DPA) são tratados à parte com
 * advogado (REQ-L6); ao mudar qualquer um deles, incremente {@link #VERSION} para exigir novo
 * aceite.</p>
 */
public final class ResponsibilityTerm {

    /** Identificador interno do termo de responsabilidade no histórico de aceites. */
    public static final String TYPE = "RESPONSIBILITY";

    /** Versão vigente exibida ao usuário; deve mudar quando o texto do termo mudar. */
    public static final String VERSION = "2026-06-01";

    /** Texto apresentado ao recrutador antes da confirmação de aceite. */
    public static final String TEXT =
            "Você é responsável pelo conteúdo do teste, pela decisão final e pelo uso lícito da "
                    + "avaliação. A Práxis é ferramenta de apoio: fornece evidência auditável e não "
                    + "decide pela sua empresa. A decisão sobre cada candidato é tomada por uma pessoa, "
                    + "e o candidato tem direito a revisão humana.";

    /**
     * Impede a criação de objetos desta classe.
     *
     * <p>Ela funciona apenas como fonte oficial do texto, do tipo e da versão do termo
     * usados pelo processo de aceite.</p>
     */
    private ResponsibilityTerm() {
    }
}
