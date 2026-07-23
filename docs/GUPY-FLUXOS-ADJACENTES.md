# Matriz de aderência dos fluxos adjacentes da Gupy

> **Objetivo:** registrar o impacto de páginas gerais da Gupy sobre o Práxis sem ampliar silenciosamente o contrato atual de provedor externo de testes.

> **Fontes e aliases:** as URLs oficiais e o saneamento das cinco rotas com falha estão centralizados em [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md).

## Data e método da revisão

- Data da análise: 23/07/2026.
- Base analisada: coleta integral da documentação oficial da Gupy, com 218 páginas e referências.
- Páginas recuperadas: configuração de webhooks, atualização de vagas, admissão da pessoa contratada e admissão para sistema de folha.
- Comparação interna: contrato de provedor externo, arquitetura de Outbox, documentação de homologação e implementação em `backend/src/main/java/br/com/iforce/praxis/gupy`.

## Limite do contrato atual

O Práxis implementa o contrato de **provedor externo de testes**:

1. lista avaliações em `GET /test`;
2. registra a pessoa candidata em `POST /test/candidate`;
3. entrega ou disponibiliza o resultado em `GET /test/result/{resultId}` e `result_webhook_url`;
4. redireciona o navegador para `callback_url` após a conclusão.

Webhooks gerais de eventos da Gupy, atualização de vagas, movimentação para contratação, pré-admissão e integração com folha pertencem a outros fluxos. Eles só passam a integrar o produto após contratação, token e liberação específicos.

## URLs recuperadas

| Registro com falha | Resultado da recuperação | Decisão |
| --- | --- | --- |
| Configuração de webhooks sem hífen final | Alias da página canônica com hífen final. | Usar somente a referência registrada no documento de fonte canônica. |
| Configuração de webhooks com `copy` | Duplicata da mesma página. | Descontinuar o alias. |
| Fluxo de atualização de vagas | A página canônica usa outro slug e descreve principalmente mudança de status; atualização ampla de dados aparece como indisponível. | Não assumir suporte além do contrato publicado. |
| Pré-admissão da pessoa contratada com `copy` | Corresponde ao fluxo de admissão da pessoa contratada, acionado por webhook de contratação. | Fora do contrato de provedor externo. |
| Admissão para sistema de folha com `copy` | Corresponde à página canônica com hífen final e ao produto Gupy Admissão. | Fora do contrato de provedor externo. |

## Matriz de aderência

### Webhooks gerais recebidos pelo integrador

| Requisito encontrado | Estado no Práxis | Decisão |
| --- | --- | --- |
| Endpoint de destino HTTPS, público e acessível pelos IPs de saída da Gupy | **Dependente de contratação ou liberação pela Gupy** | O Práxis não recebe webhooks gerais no contrato atual. Criar endpoint somente quando houver integração bidirecional aprovada. |
| Responder em até 30 segundos | **Aplicável e pendente para integração futura** | O receptor futuro deve confirmar o recebimento antes do processamento. A pendência foi aberta na issue #515. |
| Retornar `200` prontamente após receber e persistir, sem aguardar o processo completo | **Aplicável e pendente para integração futura** | Processamento assíncrono obrigatório. Issue #515. |
| Tratar entrega pelo menos uma vez com idempotência | **Aplicável e pendente para integração futura** | Persistir chave ou hash de evento e impedir efeitos duplicados. Issue #515. |
| Não assumir ordem de chegada e usar a data do evento | **Aplicável e pendente para integração futura** | O consumidor deve ordenar decisões pela data do evento e tolerar eventos tardios. Issue #515. |
| Informar explicitamente `Content-Type` quando `clientHeaders` for usado | **Dependente da configuração externa** | Registrar `application/json` junto aos headers personalizados na criação do webhook. Issue #515. |
| Monitorar inativação após 100% de falhas durante sete dias | **Aplicável e pendente para integração futura** | Criar alerta, runbook de reativação e teste periódico. Issue #515. |
| Não usar serviços públicos de captura de payloads com dados reais | **Aplicável e implementado como decisão documental** | Testes devem usar ambiente controlado e dados fictícios. |
| Configurar token com permissões de criação e manutenção de webhooks | **Dependente de contratação ou liberação pela Gupy** | Não reutilizar o token do contrato de provedor externo. |

### Resultado assíncrono do provedor externo

| Requisito | Estado no Práxis | Evidência |
| --- | --- | --- |
| Enviar resultado para `result_webhook_url` | **Aplicável e implementado** | O resultado é persistido no Outbox na transação de conclusão. |
| Não bloquear a conclusão da avaliação durante a entrega externa | **Aplicável e implementado** | A entrega é assíncrona. |
| Retry, backoff, DLQ e reprocessamento | **Aplicável e implementado** | Responsabilidade documentada em `ARQUITETURA_OUTBOX_PATTERN.md`. |
| HTTPS e proteção contra destinos internos | **Aplicável e implementado** | URLs externas passam por validação contra SSRF e HTTPS é obrigatório em produção. |
| Idempotência da criação da tentativa | **Aplicável e implementado** | A identidade usa empresa, `company_id`, `document_id`, teste e vaga. |

Os requisitos de recebimento de webhooks gerais não devem ser confundidos com o envio do resultado do Práxis para a URL pública fornecida pela Gupy.

### Atualização de vagas

| Requisito ou capacidade | Estado no Práxis | Decisão |
| --- | --- | --- |
| Alterar status de vaga por transições permitidas | **Não aplicável ao modelo de provedor externo** | O Práxis recebe `job_id` apenas como contexto da tentativa. |
| Atualizar dados cadastrais da vaga | **Dependente de publicação ou liberação pela Gupy** | A página analisada informa disponibilidade futura. Não criar contrato especulativo. |
| Sincronizar vaga, etapas e responsáveis | **Dependente de contratação ou liberação pela Gupy** | Somente após definição de escopo bidirecional, permissões e fonte de verdade. |

### Admissão da pessoa contratada

| Requisito ou capacidade | Estado no Práxis | Decisão |
| --- | --- | --- |
| Receber evento de pessoa candidata contratada | **Não aplicável ao modelo de provedor externo** | O resultado do teste não admite nem movimenta a pessoa candidata. |
| Criar webhook para evento de contratação | **Dependente de contratação ou liberação pela Gupy** | Exige token e configuração próprios. |
| Escrever tag ou comentário na candidatura | **Não aplicável ao contrato atual** | O Práxis devolve resultado pelo contrato de testes, sem alterar timeline. |
| Distinguir contratação em R&S de admissão no produto Admissão | **Aplicável e implementado como decisão documental** | Os fluxos permanecem separados nesta documentação. |

### Admissão para sistema de folha

| Requisito ou capacidade | Estado no Práxis | Decisão |
| --- | --- | --- |
| Receber `pre-employee.moved` | **Não aplicável ao modelo de provedor externo** | Pertence ao produto Gupy Admissão. |
| Filtrar etapa de interesse e campos customizados de admissão | **Não aplicável ao modelo de provedor externo** | Não faz parte da avaliação situacional. |
| Integrar dados com sistema de folha | **Não aplicável ao produto Práxis** | Não será incorporado ao domínio do produto. |
| Usar URL pública, sem VPN, e tolerar entrega fora de ordem | **Dependente de integração futura específica** | Regras cobertas na issue #515 apenas se um fluxo bidirecional for aprovado. |

## Pendência técnica criada

- Issue #515: receptor idempotente para webhooks bidirecionais, bloqueado até aprovação comercial e liberação externa.

Não foram abertas issues para atualização de vagas, contratação ou folha porque esses fluxos não pertencem ao contrato atual e não existe autorização para implementá-los. Criá-los agora produziria endpoints especulativos e ampliaria indevidamente o escopo do Práxis.

## Decisões de governança

1. A issue #477 continua responsável somente pela homologação formal do Práxis como provedor de testes.
2. O contrato público implementado continua descrito em `INTEGRACAO-GUPY-PROVEDOR.md`.
3. A matriz deste documento é informativa e não habilita recursos em produção.
4. Nenhum token geral de R&S ou Admissão deve ser confundido com o token individual do provedor externo.
5. Qualquer nova tela decorrente da issue #515 deve possuir manual contextual completo.
6. Novas páginas da Gupy devem ser saneadas primeiro em `GUPY-FONTE-CANONICA.md`.

Última revisão: 23/07/2026.
