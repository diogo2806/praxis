package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.billing.model.DunningStage;


import java.time.Instant;


/**
 * Dados de um toque educativo de cobrança (retry) a ser entregue ao cliente por e-mail e/ou SMS.
 *
 * <p>Carrega para onde enviar ({@code corporateEmail}, {@code phone}), em que etapa da régua o
 * cliente está ({@code stage}), a situação financeira atual ({@code status}) e até quando vai a
 * carência ({@code graceUntil}, {@code null} quando não houver prazo definido) — informação que o
 * canal usa para montar a mensagem de retry antes da suspensão dura.</p>
 */
public record DunningNotice(
        String empresaId,
        String corporateEmail,
        String phone,
        DunningStage stage,
        EmpresaStatus status,
        Instant graceUntil
) {
}
