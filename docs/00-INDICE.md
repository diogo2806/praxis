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
| [Operacao em producao](OPERACAO.md) | Operar, monitorar, auditar, atualizar e tratar incidentes. | DevOps, Operacao, Suporte, Admin | Atual |
| [Implantacao do zero](IMPLANTACAO.md) | Instalar, configurar variaveis, deploy, SSL e checklist pos-deploy. | DevOps, Engenharia | Atual |
| [Cadastro de cenarios para RH](cadastro_cenarios_rh.md) | Entender produto, limites e regras de cadastro. | RH, Produto, Compliance, Engenharia | Atual |
| [Mapa Frontend-Backend](frontend-backend-map.md) | Ver rotas, APIs e lacunas UI/backend. | Frontend, Backend, QA | Atual |
| [Integracao Gupy](INTEGRACAO-GUPY-PROVEDOR.md) | Homologar contrato `/test/**`. | Backend, Integracao, Gupy | Atual |
| [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | Operar entrega, retry e DLQ. | Backend, DevOps | Atual |
| [Rotas TanStack Start](../frontend/src/routes/README.md) | Criar/manter telas. | Frontend | Atual |

## Referencia tecnica

- Backend real: codigo em `backend/src/main/java/br/com/iforce/praxis` (dominios: `simulation`, `journey`, `results`, `dashboard`, `gupy`, `recrutei`, `shared.integration`, `billing`, `admin`, `team`, `account`, `companyprofile`, `term`, `candidate`, `audit`, `tenantconfig`, `media`, `privacy`).
- Migracoes: `backend/src/main/resources/db/migration`.
- API frontend: `frontend/src/lib/api/praxis.ts`.
- Rotas frontend: `frontend/src/routes`; menu principal em `frontend/src/components/app-shell.tsx`.
- Passos do assistente de criacao: `frontend/src/lib/simulation-meta.ts` (`avaliacao -> personagem -> validador -> governanca`).
- Documentacao in-app da integracao por API propria: rota `/docs/integracao-api-propria`.
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

## Compliance e juridico

> Documentos em `docs/legal/` sao **minutas para validacao juridica** e nao
> constituem aconselhamento juridico. Enderecam os riscos juridicos criticos e altos.

Riscos criticos:

- [Minuta de consentimento e termos para a vertical de Saude](PROPOSTA-CONSENTIMENTO-SAUDE.md) - propostas de texto (LGPD dado sensivel, uso educativo) para validacao juridica antes de habilitar a vertical.
- [RIPD / DPIA](legal/RIPD-DPIA.md) - relatorio de impacto do tratamento automatizado (LGPD art. 20 - risco critico #1).
- [Direitos do titular (LGPD art. 18)](legal/DIREITOS-DO-TITULAR-LGPD.md) - canal in-product `data-request`, prazos e retencao (risco critico #2).
- [DPA - Acordo de Tratamento de Dados](legal/DPA-ACORDO-DE-TRATAMENTO-DE-DADOS.md) - controlador x operador e mapa de suboperadores (LGPD art. 39 - risco critico #3).
- [Politica de Privacidade](legal/POLITICA-DE-PRIVACIDADE.md), [Termos de Uso](legal/TERMOS-DE-USO.md) e [Politica de Cookies](legal/POLITICA-DE-COOKIES.md) - documentos publicos da plataforma (risco critico #4).

Riscos altos:

- [Politica de mitigacao de vies](legal/POLITICA-MITIGACAO-VIES.md) - guardrails antidiscriminacao, job-relatedness e monitoramento de impacto (risco alto #5).
- [Vertical de Saude - ANVISA](legal/VERTICAL-SAUDE-ANVISA.md) - enquadramento SaMD e checklist de habilitacao (risco alto #6).
- Fail-fast de seguranca em producao: `SecurityStartupGuard` recusa subir com perfil `prod` e `PRAXIS_SECURITY_ENABLED=false` (risco alto #7; ver `IMPLANTACAO.md`).

Riscos medios:

- Hash do dado pessoal na chave de idempotencia: `IdempotencyKeyHasher` + migracao `V68` deixam de guardar CPF/e-mail em claro na coluna `idempotency_key` (risco medio #10).
- [Plano de resposta a incidentes](legal/PLANO-RESPOSTA-INCIDENTES.md) - runbook LGPD arts. 46-48 (risco medio #11).
- [Exposicao regulatoria internacional](legal/EXPOSICAO-REGULATORIA-INTERNACIONAL.md) - EU AI Act, NYC LL144, GDPR e recomendacao de geo-restricao (risco medio #9).

## Historico

- [Usabilidade - implementacao](USABILIDADE_IMPLEMENTACAO.md) - historico de uma entrega de UX; nao e contrato atual completo.
- [Resumo de implementacao](IMPLEMENTATION_SUMMARY.md) - estado tecnico resumido e observacoes de verificacao.

## Fora do caminho principal

- [Regras Backend Java](backend-java.md) - guia amplo de estilo/geracao Java; nao e arquitetura especifica do Praxis.

## Observacoes

- O repositorio nao possui documentos chamados `GARANTIAS-PRODUCAO.md`, `ARQUITETURA-SISTEMA.md`, `OPERACIONAL-MONITORAMENTO.md`, `OPERACIONAL-GOVERNANCA.md`, `DESENVOLVIMENTO-BACKEND-JAVA.md`, `INTEGRACAO-ROUTES-TANSTACK.md` ou `RECLAMACOES-CONCORRENTES.md`.
- Links novos devem apontar para arquivos reais listados aqui.
- Documentos com conteudo historico devem ser lidos como registro de decisao, nao como promessa de feature atual.

Ultima revisao: 03/07/2026.
