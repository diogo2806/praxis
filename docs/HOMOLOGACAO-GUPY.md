# Centro de homologação técnica Gupy

> **Finalidade:** mostrar o que o Práxis comprova automaticamente, registrar evidências externas auditáveis e separar essas evidências da aprovação formal feita pela Gupy e pelo cliente.

O contrato implementado está em [Integração Praxis como provedor](INTEGRACAO-GUPY-PROVEDOR.md). A fonte externa oficial está centralizada em [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md).

## Acesso

Interface:

```text
/integrations/gupy-homologacao
```

API administrativa:

```text
GET /api/v1/integrations/gupy/homologation
PUT /api/v1/integrations/gupy/homologation/evidence
```

As APIs exigem autenticação administrativa e respeitam o isolamento por empresa.

## Fluxo operacional

1. Abrir a Central de Integrações e gerar o token Gupy.
2. Publicar ao menos uma avaliação.
3. Abrir o Centro de Homologação.
4. Corrigir todos os itens marcados como bloqueio.
5. Copiar os endpoints exibidos e informar à Gupy.
6. Criar uma vaga real ou não listada de validação.
7. Executar o fluxo completo da pessoa candidata.
8. Confirmar callback, consulta de resultado e webhook.
9. Validar o percentual e as páginas separadas de empresa e candidato.
10. Registrar as evidências externas, protocolos e responsáveis na tela.
11. Copiar o JSON integral das evidências.
12. Obter e registrar a aprovação formal da Gupy e do cliente.
13. Atualizar materiais comerciais somente depois do estado `HOMOLOGATED`.

## Campos e indicadores

| Campo | Explicação |
| --- | --- |
| Estado atual | Resultado consolidado das verificações internas e externas. |
| Prontidão | Percentual das evidências técnicas mensuráveis concluídas. Não inclui as duas aprovações formais. |
| Avaliações publicadas | Itens disponíveis para `GET /test`. |
| Tentativas originadas pela Gupy | Tentativas criadas pelo contrato Gupy. |
| Tentativas concluídas | Execuções Gupy finalizadas. |
| Tentativas com webhook | Tentativas que receberam `result_webhook_url`. |
| Webhooks entregues | Entregas de resultado confirmadas. |
| Consultas de resultado | Chamadas autenticadas concluídas em `GET /test/result/{resultId}`. |
| Resultados percentuais válidos | Resultados concluídos com `normalized_score` entre 0 e 100. |
| Entregas em DLQ | Falhas permanentes ou esgotamento das tentativas. |
| Evidências externas | Confirmação do callback, páginas separadas, aprovação Gupy, aprovação do cliente e observações. |
| Endpoints | URLs públicas que devem ser configuradas na integração. |
| Checklist | Evidências internas e externas, com detalhe do bloqueio. |

## Permissões necessárias

- usuário autenticado;
- vínculo ativo com a empresa;
- permissão administrativa para consultar integrações;
- permissão administrativa para registrar evidências externas;
- acesso ao Centro Operacional para tratar DLQ;
- participação da Gupy e do cliente para executar e aprovar a validação externa.

## Estados possíveis

| Estado | Significado |
| --- | --- |
| `BLOCKED` | Há requisito interno ausente ou falha operacional. |
| `READY_FOR_EXTERNAL_VALIDATION` | A configuração interna permite iniciar a validação em vaga real, mas ainda faltam evidências. |
| `EVIDENCE_READY` | Todas as evidências técnicas e externas exigidas foram confirmadas; faltam uma ou ambas as aprovações formais. |
| `HOMOLOGATED` | Evidências completas e aprovações formais da Gupy e do cliente registradas. |

Estados dos itens do checklist:

| Estado | Significado |
| --- | --- |
| `OK` | Evidência confirmada. |
| `PENDING` | Depende de execução futura, registro externo ou aprovação. |
| `BLOCKER` | Impede avançar até a correção. |

## Evidências automáticas

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
10. Auditoria autenticada de `GET /test/result/{resultId}`.
11. Resultado concluído com percentual entre 0 e 100.

Tentativas de link direto e tentativas da Recrutei não contam como evidência Gupy.

## Evidências externas registradas

O responsável pela homologação pode registrar:

- confirmação da Gupy de que recebeu o GET no `callback_url`;
- validação das páginas separadas de resultado para empresa e candidato;
- aprovação formal da Gupy;
- aprovação formal do cliente;
- vaga utilizada, protocolo, responsáveis, links e observações.

Cada alteração grava data, usuário autenticado e evento na auditoria da integração. Marcar uma caixa não substitui o comprovante externo; o campo deve ser usado somente depois da confirmação real.

## Motivos de bloqueio ou pendência

- URL pública ausente ou sem HTTPS;
- token Gupy não criado ou inativo;
- nenhuma avaliação publicada;
- ausência de tentativa originada pela Gupy;
- tentativa sem `callback_url`;
- tentativa ainda não concluída;
- callback ainda não confirmado pela Gupy;
- ausência de `result_webhook_url`;
- webhook não entregue;
- evento em DLQ;
- resultado ainda não consultado pela Gupy;
- percentual ausente ou fora de 0 a 100;
- páginas separadas ainda não validadas;
- aprovação formal da Gupy ou do cliente ainda não registrada;
- falha de comunicação com a API administrativa.

## Exemplo de validação completa

1. A Gupy consulta `GET /test`.
2. A Gupy cria a tentativa em `POST /test/candidate`.
3. A pessoa candidata abre `test_url`.
4. A avaliação é concluída.
5. O navegador acessa `callback_url` e a Gupy confirma o recebimento.
6. O Outbox envia `TestResultResponse` para `result_webhook_url`.
7. A Gupy consulta o mesmo resultado em `GET /test/result/{resultId}`.
8. O percentual apresentado fica entre 0 e 100.
9. Empresa e candidato visualizam suas páginas separadas.
10. O responsável registra callback e páginas no Centro de Homologação.
11. O Centro passa para `EVIDENCE_READY`.
12. A Gupy e o cliente aprovam formalmente o fluxo.
13. As duas aprovações são registradas e o estado passa para `HOMOLOGATED`.

## Atalhos operacionais

- **Atualizar:** recalcula as verificações automáticas.
- **Salvar evidências:** persiste as confirmações externas com auditoria.
- **Copiar evidências:** copia o JSON integral retornado pela API.
- **Voltar para integrações:** abre a configuração dos provedores.
- **Abrir entregas e DLQ:** leva ao monitoramento e reprocessamento.
- Use `Tab` e `Shift+Tab` para navegar pelas ações.
- Use o manual contextual da tela para revisar estados e bloqueios.

## Critério de prontidão

O percentual considera verificações técnicas e validações externas do fluxo, mas exclui as aprovações formais da Gupy e do cliente. O estado `HOMOLOGATED` exige todas as evidências, nenhuma entrega em DLQ e ambas as aprovações registradas.

## Limite explícito

A documentação da Gupy não oferece sandbox para provedores externos. O Práxis não consegue gerar sozinho token, vaga, confirmação de callback, aprovação ou comprovantes do ambiente real. O registro administrativo existe para preservar evidências recebidas, não para produzi-las artificialmente.

Última revisão: 24/07/2026.
