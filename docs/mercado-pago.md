# Cobrança Mercado Pago (Parte B)

A cobrança é opcional e desligada por padrão (`MP_ENABLED=false`). As credenciais ficam
**apenas no backend**, lidas de variáveis de ambiente, e **nunca** são commitadas no repositório
nem enviadas ao frontend.

## Variáveis de ambiente

| Variável            | Descrição                                                        |
| ------------------- | ---------------------------------------------------------------- |
| `MP_ENABLED`        | `true` para habilitar as chamadas ao Mercado Pago                |
| `MP_ACCESS_TOKEN`   | Access Token (segredo, somente backend)                          |
| `MP_PUBLIC_KEY`     | Public Key                                                       |
| `MP_WEBHOOK_SECRET` | Segredo para validar a assinatura `x-signature` do webhook       |
| `MP_BASE_URL`       | Padrão `https://api.mercadopago.com`                             |
| `MP_GRACE_PERIOD_DAYS` | Carência de inadimplência antes da suspensão (padrão `7`)     |
| `MP_BACK_URL`       | URL de retorno do checkout                                       |
| `MP_NOTIFICATION_URL` | URL pública do webhook (`.../api/webhooks/mercado-pago`)       |
| `MP_DELINQUENCY_CRON` | Cron do job de inadimplência (padrão de hora em hora)          |

Exemplo (ambiente de teste — preencha com as suas credenciais, não as commite):

```bash
export MP_ENABLED=true
export MP_ACCESS_TOKEN="APP_USR-xxxxxxxx..."
export MP_PUBLIC_KEY="APP_USR-xxxxxxxx..."
export MP_WEBHOOK_SECRET="seu-segredo-de-webhook"
export MP_NOTIFICATION_URL="https://SEU_DOMINIO/api/webhooks/mercado-pago"
```

> O `MP_WEBHOOK_SECRET` é obtido no painel do Mercado Pago, na configuração de webhooks/notificações
> (não é o Access Token). Sem ele configurado, o webhook aceita notificações sem verificar a
> assinatura (apenas para desenvolvimento local) e registra um aviso.

## Modelo

- **AVULSO** — crédito pré-pago. Compra gera checkout; o webhook confirma o pagamento (consultando
  a API) e adiciona créditos via ledger. Cada avaliação concluída consome 1 crédito; sem saldo o
  cliente fica `SEM_CREDITO` e não inicia nova avaliação.
- **PROFISSIONAL** — assinatura recorrente mensal (preapproval). Pagamento recusado inicia
  inadimplência (`INADIMPLENTE`) com carência configurável; após a carência o cliente é suspenso
  automaticamente.
- **ENTERPRISE** — contrato manual; sem automação de cobrança no MVP.

## Princípios

- O Mercado Pago é a fonte da verdade financeira: nenhum pagamento é aprovado por ação manual.
  A confirmação vem sempre de consulta à API (via webhook ou sincronização manual).
- Webhook valida assinatura, é idempotente (por tópico + id do recurso) e salva o payload bruto.
- Eventos financeiros e o ledger de créditos são append-only.
