/**
 * Cobrança e faturamento — como a empresa paga pela plataforma e como o consumo é controlado.
 *
 * <p>Este é o pacote do "dinheiro". Enquanto os outros pacotes cuidam de criar avaliações,
 * aplicar a prova e apurar resultados, aqui se responde a uma pergunta diferente: <em>o cliente
 * está em dia e pode continuar usando o sistema?</em> Todo o ciclo financeiro passa por aqui —
 * da criação de uma cobrança até a confirmação do pagamento, o controle de saldo e o bloqueio de
 * quem deixa de pagar.</p>
 *
 * <p>Existem três formas de contratar (o "plano comercial" do cliente):</p>
 * <ul>
 *   <li><b>AVULSO (pré-pago)</b> — o cliente compra pacotes de créditos e cada avaliação
 *       concluída gasta 1 crédito. Sem crédito, não inicia novas avaliações.</li>
 *   <li><b>PROFISSIONAL (assinatura)</b> — o cliente paga uma mensalidade recorrente que renova
 *       sozinha. Se um pagamento falha, entra em carência e, persistindo, é suspenso.</li>
 *   <li><b>ENTERPRISE (contrato)</b> — condição negociada manualmente; aqui o pacote apenas
 *       mostra a situação, sem cobrar automaticamente.</li>
 * </ul>
 *
 * <p>Um princípio governa tudo: <b>o Mercado Pago é a fonte da verdade financeira</b>. Nenhum
 * pagamento é dado como aprovado por decisão manual de alguém — a confirmação sempre vem de uma
 * consulta à API do Mercado Pago, disparada por uma notificação (webhook) ou por uma
 * sincronização manual. Cada efeito financeiro (compra, cobrança, consumo de crédito) vira um
 * registro que só acrescenta e nunca apaga (append-only), o que dá uma trilha auditável de tudo
 * o que aconteceu com o dinheiro do cliente.</p>
 *
 * <p>O ciclo, na visão de quem opera a cobrança:</p>
 * <ol>
 *   <li><b>Criar a cobrança</b> — o painel ADMIN gera um checkout de créditos (AVULSO) ou uma
 *       assinatura (PROFISSIONAL) e devolve o link de pagamento do Mercado Pago.</li>
 *   <li><b>Confirmar o pagamento</b> — o Mercado Pago avisa a plataforma por webhook; ela
 *       confere a autenticidade do aviso, consulta o pagamento real e só então aplica o efeito
 *       (soma créditos, ativa a assinatura, reativa o cliente).</li>
 *   <li><b>Controlar o consumo</b> — a cada avaliação concluída, um crédito é debitado do cliente
 *       AVULSO; ao zerar o saldo, ele fica "sem crédito" e é impedido de começar novas provas.</li>
 *   <li><b>Recarregar sozinho (AVULSO)</b> — o cliente pré-pago pode ligar a <em>recarga
 *       automática</em>: quando o saldo cai abaixo de um nível crítico que ele define, a plataforma
 *       cobra o cartão salvo no Mercado Pago e libera um novo lote de créditos sem intervenção
 *       humana — com trava contra cobrança dupla, chave de idempotência e janela de espera entre
 *       tentativas.</li>
 *   <li><b>Cobrar quem atrasa</b> — assinaturas com pagamento recusado entram em carência e, se a
 *       carência vencer sem regularização, o cliente é suspenso automaticamente.</li>
 *   <li><b>Mostrar a situação</b> — o próprio cliente vê seu plano, saldo, uso e histórico; o
 *       ADMIN vê a visão consolidada de qualquer cliente.</li>
 * </ol>
 *
 * <p>Segurança e isolamento: as credenciais do Mercado Pago vivem apenas no backend, nunca no
 * navegador; o webhook público é validado por assinatura antes de ser aceito; e toda leitura é
 * restrita ao cliente logado, de modo que uma empresa nunca enxerga a cobrança de outra.</p>
 */
package br.com.iforce.praxis.billing;
