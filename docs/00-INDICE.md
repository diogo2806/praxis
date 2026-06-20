# Indice de Documentacao - Praxis

> Trilha para entender o produto, rodar o projeto e aprofundar nos contratos tecnicos.

## Comece aqui

1. [README principal](../README.md) - visao do sistema, arquitetura, execucao local, variaveis e fluxos.
2. [Mapa Frontend-Backend](frontend-backend-map.md) - rotas, APIs consumidas e lacunas conhecidas.
3. [Cadastro de cenarios para RH](cadastro_cenarios_rh.md) - especificacao atual de produto e guardrails para RH.
4. [Integracao Gupy](INTEGRACAO-GUPY-PROVEDOR.md) - contrato externo real da Gupy.
5. [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) - retry, DLQ e entrega assincrona.
6. [Rotas TanStack Start](../frontend/src/routes/README.md) - convencoes de rotas do frontend.

## Documentacao operacional atual

| Documento | Quando usar | Publico | Status |
| --- | --- | --- | --- |
| [README principal](../README.md) | Entender e rodar o sistema. | Todos | Atual |
| [Cadastro de cenarios para RH](cadastro_cenarios_rh.md) | Entender produto, limites e regras de cadastro. | RH, Produto, Compliance, Engenharia | Atual |
| [Mapa Frontend-Backend](frontend-backend-map.md) | Ver rotas, APIs e lacunas UI/backend. | Frontend, Backend, QA | Atual |
| [Integracao Gupy](INTEGRACAO-GUPY-PROVEDOR.md) | Homologar contrato `/test/**`. | Backend, Integracao, Gupy | Atual |
| [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | Operar entrega, retry e DLQ. | Backend, DevOps | Atual |
| [Rotas TanStack Start](../frontend/src/routes/README.md) | Criar/manter telas. | Frontend | Atual |

## Referencia tecnica

- Backend real: codigo em `backend/src/main/java/br/com/iforce/praxis`.
- Migracoes: `backend/src/main/resources/db/migration`.
- API frontend: `frontend/src/lib/api/praxis.ts`.
- Rotas frontend: `frontend/src/routes`.
- Configuracao runtime frontend: `frontend/src/lib/runtime-config.ts`, `frontend/src/lib/runtime-config.server.ts` e `frontend/src/server.ts`.

## Fluxos por jornada

| Jornada | Ler |
| --- | --- |
| RH cria simulacao | [Cadastro de cenarios](cadastro_cenarios_rh.md) e [Mapa Frontend-Backend](frontend-backend-map.md#fluxos-principais) |
| RH publica versao | [Cadastro de cenarios](cadastro_cenarios_rh.md#fluxo-operacional-atual) |
| Candidato responde | [README](../README.md#candidato) e [Mapa Frontend-Backend](frontend-backend-map.md#execucao-do-candidato) |
| Gupy cria tentativa | [Integracao Gupy](INTEGRACAO-GUPY-PROVEDOR.md#post-testcandidate) |
| Resultado vai para Gupy | [Integracao Gupy](INTEGRACAO-GUPY-PROVEDOR.md#entrega-assincrona-por-outbox) e [Outbox](ARQUITETURA_OUTBOX_PATTERN.md) |
| Operacao reprocessa DLQ | [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md#monitoramento-e-operacao) |

## Produto e decisoes

- [Cadastro de cenarios para RH](cadastro_cenarios_rh.md) - documento principal de produto atualizado.
- [Pesquisa de reclamacoes de concorrentes](research/competitor-complaints-research-2025-2026.md) - contexto de mercado.

## Historico

- [Usabilidade - implementacao](USABILIDADE_IMPLEMENTACAO.md) - historico de uma entrega de UX; nao e contrato atual completo.
- [Resumo de implementacao](IMPLEMENTATION_SUMMARY.md) - estado tecnico resumido e observacoes de verificacao.

## Fora do caminho principal

- [Regras Backend Java](backend-java.md) - guia amplo de estilo/geracao Java; nao e arquitetura especifica do Praxis.

## Observacoes

- O repositorio nao possui documentos chamados `GARANTIAS-PRODUCAO.md`, `ARQUITETURA-SISTEMA.md`, `OPERACIONAL-MONITORAMENTO.md`, `OPERACIONAL-GOVERNANCA.md`, `DESENVOLVIMENTO-BACKEND-JAVA.md`, `INTEGRACAO-ROUTES-TANSTACK.md` ou `RECLAMACOES-CONCORRENTES.md`.
- Links novos devem apontar para arquivos reais listados aqui.
- Documentos com conteudo historico devem ser lidos como registro de decisao, nao como promessa de feature atual.

Ultima revisao: 20/06/2026.
