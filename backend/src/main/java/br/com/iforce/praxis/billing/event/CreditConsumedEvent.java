package br.com.iforce.praxis.billing.event;

/**
 * Aviso interno de que um cliente pré-pago (AVULSO) acabou de consumir 1 crédito.
 *
 * <p>Na visão do processo, é a {@code CreditService} anunciando "o saldo deste cliente mudou e
 * agora está em X" logo depois de debitar uma avaliação concluída. Quem cuida da recarga
 * automática escuta este aviso e decide, conforme a preferência do cliente, se é hora de comprar
 * um novo lote de créditos. O aviso é disparado apenas após a transação do consumo ser confirmada,
 * de modo que quem o recebe já enxerga o saldo definitivo.</p>
 *
 * @param empresaId identificador do cliente que consumiu o crédito
 * @param balance saldo de créditos restante logo após o consumo
 */
public record CreditConsumedEvent(String empresaId, int balance) {
}
