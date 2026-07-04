# Validação em ambiente real — Billing self-service

Este roteiro valida o fluxo comercial que sai da tela `/billing`, passa pelo Mercado Pago e volta para o Práxis por webhook ou sincronização manual.

## Pré-requisitos

- Ambiente com frontend e backend apontando para o mesmo tenant.
- Integração Mercado Pago habilitada no backend.
- Planos ativos cadastrados:
  - pelo menos um pacote `AVULSO` com créditos;
  - pelo menos um plano `PROFISSIONAL` recorrente.
- Usuário autenticado como empresa cliente.
- Webhook do Mercado Pago acessível publicamente ou alternativa de sincronização manual disponível.
- Para testes sem cobrança real, usar credenciais/contas de teste do Mercado Pago.

## 1. Abrir `/billing`

1. Entrar como empresa cliente.
2. Abrir `/billing`.
3. Confirmar que a tela carrega:
   - plano atual;
   - status financeiro;
   - saldo de créditos, se AVULSO;
   - dados de assinatura, se PROFISSIONAL;
   - histórico financeiro.

Resultado esperado: a tela não exibe botão morto; as ações de cobrança levam ao bloco de self-service ou ao link do Mercado Pago.

## 2. Listar planos

1. Na tela `/billing`, verificar o bloco de self-service.
2. Confirmar que os planos vêm de `GET /api/v1/billing/plans`.
3. Para empresa AVULSO, confirmar exibição de pacotes de créditos.
4. Para empresa PROFISSIONAL, confirmar exibição de planos recorrentes ou link de autorização atual.

Resultado esperado: somente planos ativos aparecem, com preço, moeda e quantidade de créditos quando aplicável.

## 3. Gerar checkout de créditos

1. Usar uma empresa com plano `AVULSO`.
2. Clicar em um pacote de créditos.
3. Confirmar chamada para `POST /api/v1/billing/credits/checkout?planId={id}`.
4. Confirmar abertura do `initPoint` do Mercado Pago.
5. Concluir o pagamento com usuário/cartão de teste.

Resultado esperado antes da confirmação: o histórico registra criação de checkout, mas o saldo ainda não aumenta.

Resultado esperado após webhook ou sync: o pagamento aprovado gera evento financeiro e o saldo de créditos aumenta uma única vez.

## 4. Gerar checkout de assinatura

1. Usar uma empresa com plano `PROFISSIONAL` sem assinatura ativa/pending/delinquent duplicada.
2. Clicar em um plano recorrente.
3. Confirmar chamada para `POST /api/v1/billing/subscription/checkout?planId={id}`.
4. Confirmar abertura do `initPoint`/autorização do Mercado Pago.
5. Autorizar a assinatura com usuário/cartão de teste.

Resultado esperado antes da confirmação: a assinatura fica pendente e a empresa pode ficar como `PENDENTE_PAGAMENTO`.

Resultado esperado após webhook ou sync: a assinatura passa para autorizada/regular, com período vigente atualizado.

## 5. Voltar do Mercado Pago

1. Após pagar ou autorizar, voltar para a aplicação pelo `back_url` configurado.
2. Reabrir `/billing`.
3. Confirmar que a tela continua consistente mesmo se o webhook ainda não tiver chegado.

Resultado esperado: a tela mostra estado pendente quando a confirmação ainda não chegou e oferece sincronização quando aplicável.

## 6. Sincronizar assinatura

1. Em uma conta PROFISSIONAL com assinatura vinculada ao Mercado Pago, clicar em “Sincronizar assinatura”.
2. Confirmar chamada para `POST /api/v1/billing/subscription/sync`.
3. Confirmar que o backend consulta o Mercado Pago antes de alterar estado local.

Resultado esperado: status, período vigente e histórico financeiro refletem a fonte da verdade do Mercado Pago.

## 7. Conferir saldo, status e histórico

Depois de cada fluxo, conferir:

- `/billing` atualizou saldo de créditos quando pagamento AVULSO foi aprovado;
- `/billing` atualizou assinatura/status quando plano PROFISSIONAL foi autorizado ou pago;
- histórico financeiro mostra criação de checkout, aprovação, rejeição, pendência, estorno ou chargeback conforme o caso;
- repetir webhook/sync do mesmo recurso não duplica crédito nem evento aprovado idempotente;
- `/dashboard` reflete o consumo/plano após atualizar a tela.

## 8. Cenários negativos mínimos

- Empresa PROFISSIONAL tentando comprar pacote AVULSO deve receber erro de regra.
- Empresa AVULSO tentando criar assinatura self-service deve receber erro de regra.
- Plano inativo não deve gerar checkout.
- Mercado Pago desabilitado ou sem token deve retornar erro claro de serviço indisponível.
- Webhook duplicado não deve duplicar saldo.

## Evidência recomendada

Registrar no ticket/PR de release:

- empresa/tenant usado no teste;
- plano/pacote usado;
- IDs de `preference`, `payment` ou `preapproval` do Mercado Pago;
- antes/depois de saldo, status e histórico;
- resultado do webhook ou da sincronização manual;
- prints de `/billing` antes e depois.
