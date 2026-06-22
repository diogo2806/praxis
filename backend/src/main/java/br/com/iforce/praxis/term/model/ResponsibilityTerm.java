package br.com.iforce.praxis.term.model;

/**
 * Termo de responsabilidade que o recrutador aceita antes de usar/publicar um teste (REQ-L5).
 *
 * <p>Este é o texto-resumo exibido no produto, que reforça a postura de "ferramenta de apoio, não
 * decisora". Os termos contratuais vinculantes (Termos de Uso/DPA) são tratados à parte com
 * advogado (REQ-L6); ao mudar qualquer um deles, incremente {@link #VERSION} para exigir novo
 * aceite.</p>
 */
public final class ResponsibilityTerm {

    public static final String TYPE = "RESPONSIBILITY";
    public static final String VERSION = "2026-06-01";
    public static final String TEXT =
            "Você é responsável pelo conteúdo do teste, pela decisão final e pelo uso lícito da "
                    + "avaliação. A Práxis é ferramenta de apoio: fornece evidência auditável e não "
                    + "decide pela sua empresa. A decisão sobre cada candidato é tomada por uma pessoa, "
                    + "e o candidato tem direito a revisão humana.";

    private ResponsibilityTerm() {
    }
}
