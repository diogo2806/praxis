# Centro de homologação técnica Gupy

## Objetivo

O Centro de Homologação separa o que o Práxis consegue comprovar automaticamente do que depende do ambiente real da Gupy e do cliente.

A existência de evidências técnicas não equivale à aprovação formal da homologação.

## Acesso

Interface:

```text
/integrations/gupy-homologacao
```

API administrativa:

```text
GET /api/v1/integrations/gupy/homologation
```

A API exige a autenticação administrativa normal do Práxis e respeita o isolamento por empresa.

## Estados gerais

| Estado | Significado |
| --- | --- |
| `BLOCKED` | Há requisito interno ausente, como HTTPS, token ou avaliação publicada. |
| `READY_FOR_EXTERNAL_VALIDATION` | O Práxis está configurado para iniciar o teste em vaga real. |
| `EVIDENCE_READY` | Existem evidências de tentativa concluída e webhook entregue, mas a aprovação externa continua pendente. |

## Evidências verificadas

O centro consulta fontes reais do sistema:

1. `praxis.public-base-url` usando HTTPS.
2. Token Gupy cadastrado em `integration_tokens`.
3. Versões publicadas disponíveis para `GET /test`.
4. Atividade autenticada registrada pela integração Gupy.
5. Tentativas criadas por payload com `callback_url`, obrigatório no contrato Gupy.
6. Tentativas Gupy concluídas.
7. Presença de `result_webhook_url` nas tentativas.
8. Eventos `RESULT_READY` entregues ou enviados para DLQ.

Tentativas de link direto não possuem `callback_url` e não entram nas métricas. A integração Recrutei cria a requisição compartilhada sem `callback_url`, portanto também não é contabilizada como evidência Gupy.

## Endpoints apresentados para configuração

O centro monta os endereços usando `praxis.public-base-url`:

```text
GET  {baseUrl}/test
POST {baseUrl}/test/candidate
GET  {baseUrl}/test/result/{resultId}
```

Todos exigem:

```text
Authorization: Bearer <token-gerado-no-praxis>
```

## Critério de prontidão

O percentual considera oito verificações mensuráveis. A etapa de aprovação formal da Gupy permanece sempre externa e não é usada para inflar o percentual técnico.

Uma entrega em DLQ é mostrada como bloqueio operacional. A correção e o reprocessamento permanecem disponíveis no Centro Operacional.

## Fluxo para concluir a homologação

1. Corrigir todos os bloqueios internos.
2. Gerar o token Gupy no Práxis.
3. Publicar ao menos uma avaliação.
4. Configurar os três endpoints no processo de homologação da Gupy.
5. Executar `GET /test` no ambiente integrado.
6. Criar uma tentativa por `POST /test/candidate` com `callback_url` e `result_webhook_url` reais.
7. Abrir `test_url` e concluir a avaliação.
8. Confirmar o redirecionamento do navegador para `callback_url`.
9. Confirmar a entrega de `TestResult` para `result_webhook_url`.
10. Consultar o mesmo resultado por `GET /test/result/{resultId}`.
11. Copiar as evidências do centro e encaminhar para validação da Gupy e do cliente.

## Limite explícito

O Práxis não consegue executar sozinho a etapa final porque a documentação da Gupy não oferece sandbox para provedores externos. Token, vaga, callback, webhook e aprovação precisam vir do ambiente real.
