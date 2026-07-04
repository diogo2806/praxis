# Notificacoes apos PR 176

Este documento complementa a prontidao P0 do produto com os detalhes entregues no fluxo de notificacoes.

## Frontend

- `/notifications` lista alertas internos.
- O menu lateral deve exibir badge com pendencias nao lidas.
- A tela permite marcar uma notificacao como lida.
- Entregas com status `dlq` devem apontar para o resultado relacionado e permitir reprocessamento.
- O dashboard deve exibir alerta quando houver entregas em `dlq`.

## API

- `GET /api/v1/notifications` lista notificacoes da empresa.
- `GET /api/v1/notifications/unread-count` retorna o total de notificacoes nao lidas.
- `POST /api/v1/notifications/{notificationId}/read` marca uma notificacao como lida.
- `POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess` reprocessa uma entrega em `dlq`.

## Regra de produto

A rota de notificacoes e o badge existem para tornar falhas operacionais descobriveis. O alerta no dashboard deve levar o usuario para `/notifications`, onde a revisao e o reprocessamento acontecem.

## Implementacao pendente

Conectar o banner visual no shell ou na rota de dashboard.
