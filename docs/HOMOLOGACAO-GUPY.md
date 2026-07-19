# Centro de homologação técnica Gupy

> **Finalidade:** mostrar o que o Práxis comprova automaticamente e separar essas evidências da aprovação formal feita pela Gupy e pelo cliente.

O contrato implementado está em [Integração Praxis como provedor](INTEGRACAO-GUPY-PROVEDOR.md). A fonte externa oficial está centralizada em [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md).

## Acesso

Interface:

```text
/integrations/gupy-homologacao
```

API administrativa:

```text
GET /api/v1/integrations/gupy/homologation
```

A API exige autenticação administrativa e respeita o isolamento por empresa.

## Fluxo operacional

1. Abrir a Central de Integrações e gerar o token Gupy.
2. Publicar ao menos uma avaliação.
3. Abrir o Centro de Homologação.
4. Corrigir todos os itens marcados como bloqueio.
5. Copiar os endpoints exibidos e informar à Gupy.
6. Criar uma vaga real de validação.
7. Executar o fluxo completo da pessoa candidata.
8. Confirmar callback, consulta de resultado e webhook.
9. Copiar as evidências da tela.
10. Encaminhar as evidências para aprovação da Gupy e do cliente.

## Campos e indicadores

| Campo | Explicação |
| --- | --- |
| Estado atual | Resultado consolidado das verificações internas. |
| Prontidão | Percentual das evidências técnicas mensuráveis concluídas. |
| Avaliações publicadas | Itens disponíveis para `GET /test`. |
| Tentativas originadas pela Gupy | Tentativas criadas pelo contrato Gupy. |
| Tentativas concluídas | Execuções Gupy finalizadas. |
| Tentativas com webhook | Tentativas que receberam `result_webhook_url`. |
| Webhooks entregues | Entregas de resultado confirmadas. |
| Entregas em DLQ | Falhas permanentes ou esgotamento das tentativas. |
| Endpoints | URLs públicas que devem ser configuradas na integração. |
| Checklist | Evidências internas e externas, com detalhe do bloqueio. |

## Permissões necessárias

- usuário autenticado;
- vínculo ativo com a empresa;
- permissão administrativa para consultar integrações;
- acesso ao Centro Operacional para tratar DLQ;
- participação da Gupy e do cliente para executar a validação externa.

## Estados possíveis

| Estado | Significado |
| --- | --- |
| `BLOCKED` | Há requisito interno ausente ou falha operacional. |
| `READY_FOR_EXTERNAL_VALIDATION` | A configuração interna permite iniciar a validação em vaga real. |
| `EVIDENCE_READY` | Tentativa, conclusão e entrega produziram evidências; a aprovação formal ainda está pendente. |

Estados dos itens do checklist:

| Estado | Significado |
| --- | --- |
| `OK` | Evidência confirmada. |
| `PENDING` | Depende de execução futura ou ambiente externo. |
| `BLOCKER` | Impede avançar até a correção. |

## Evidências verificadas

O serviço consulta fontes reais:

1. `praxis.public-base-url` configurada com HTTPS.
2. Token Gupy ativo em `integration_tokens`.
3. Avaliação publicada disponível para o catálogo.
4. Atividade autenticada registrada para a integração.
5. Tentativa criada com `callback_url`.
6. Tentativa Gupy concluída.
7. Presença de `result_webhook_url`.
8. Evento `RESULT_READY` entregue.
9. Ausência de entrega Gupy bloqueada em DLQ.

Tentativas de link direto e tentativas da Recrutei não contam como evidência Gupy.

## Motivos de bloqueio

- URL pública ausente ou sem HTTPS;
- token Gupy não criado ou inativo;
- nenhuma avaliação publicada;
- ausência de tentativa originada pela Gupy;
- tentativa sem `callback_url`;
- tentativa ainda não concluída;
- ausência de `result_webhook_url`;
- webhook não entregue;
- evento em DLQ;
- falha de comunicação com a API administrativa;
- ambiente real, vaga ou aprovação externa ainda indisponíveis.

## Exemplo de validação completa

1. A Gupy consulta `GET /test`.
2. A Gupy cria a tentativa em `POST /test/candidate`.
3. A pessoa candidata abre `test_url`.
4. A avaliação é concluída.
5. O navegador acessa `callback_url`.
6. O Outbox envia `TestResultResponse` para `result_webhook_url`.
7. A Gupy consulta o mesmo resultado em `GET /test/result/{resultId}`.
8. O Centro de Homologação passa para `EVIDENCE_READY`.
9. A Gupy e o cliente aprovam formalmente o fluxo.

## Atalhos operacionais

- **Atualizar:** recalcula as verificações.
- **Copiar evidências:** copia o JSON integral retornado pela API.
- **Voltar para integrações:** abre a configuração dos provedores.
- **Abrir entregas e DLQ:** leva ao monitoramento e reprocessamento.
- Use `Tab` e `Shift+Tab` para navegar pelas ações.
- Use o manual contextual da tela para revisar estados e bloqueios.

## Critério de prontidão

O percentual considera apenas verificações mensuráveis pelo Práxis. Aprovação comercial, contratual ou formal da Gupy nunca aumenta artificialmente a prontidão técnica.

## Limite explícito

A documentação da Gupy não oferece sandbox para provedores externos. O Práxis não consegue gerar sozinho token, vaga, callback, webhook ou aprovação do ambiente real.

Última revisão: 18/07/2026.
