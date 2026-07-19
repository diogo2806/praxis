# DATA-16 — Auditoria universal das tabelas

## Objetivo

Garantir rastreabilidade uniforme para todas as tabelas do schema da aplicação, identificando quando e por quem cada registro foi criado ou alterado e preservando uma trilha cronológica das operações de escrita.

## Campos obrigatórios

A migration `V1011__universal_table_auditing` adiciona às tabelas que ainda não possuírem:

- `created_at`: instante de criação;
- `created_by`: ator responsável pela criação;
- `updated_at`: instante da última alteração;
- `updated_by`: ator responsável pela última alteração.

Registros já existentes recebem `SYSTEM_MIGRATION` nos campos de autoria adicionados pela migration. Novas operações utilizam o usuário autenticado, `ANONYMOUS` para requisições públicas, `SYSTEM` para tarefas internas ou o usuário padrão quando a segurança está desabilitada.

## Histórico de alterações

A tabela append-only `data_change_history` registra automaticamente:

- schema e tabela;
- chave primária do registro;
- operação `INSERT`, `UPDATE` ou `DELETE`;
- usuário responsável;
- identificador da requisição;
- campos alterados;
- data e hora;
- representações anterior e nova com conteúdo sensível mascarado.

Campos relacionados a senhas, tokens, credenciais, payloads, dados pessoais e textos livres são substituídos por `[REDACTED]` no histórico. Os dados originais permanecem somente nas tabelas de domínio conforme as regras de retenção existentes.

## Integridade

No PostgreSQL, cada tabela recebe:

- `trg_praxis_audit_columns`, responsável por preencher e preservar os campos de auditoria;
- `trg_praxis_row_history`, responsável por gravar a trilha de alterações.

A tabela `data_change_history` rejeita `UPDATE` e `DELETE` por trigger, permanecendo append-only.

O `UniversalAuditSchemaValidator` verifica no startup se todas as tabelas possuem os quatro campos e, no PostgreSQL, os dois gatilhos obrigatórios. Uma migration futura que criar tabela sem auditoria impedirá a inicialização da aplicação até ser corrigida.

## Contexto do usuário

O `DatabaseAuditContextAspect` executa dentro da transação e publica no PostgreSQL:

- `praxis.actor_user_id`;
- `praxis.request_id`.

Os valores permanecem locais à transação e são consumidos pelos gatilhos, evitando vazamento de contexto quando conexões retornam ao pool.

## Validação automatizada

`UniversalTableAuditingIntegrationTest` confirma que:

1. todas as tabelas possuem os campos obrigatórios;
2. criação e alteração registram o usuário correto;
3. o histórico contém as operações esperadas;
4. valores sensíveis são mascarados.
