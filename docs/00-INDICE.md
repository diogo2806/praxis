# Índice de Documentação — Praxis

> Trilha para entender o produto, executar o projeto e consultar contratos técnicos sem confundir implementação local com homologação externa.

## Comece aqui

1. [README principal](../README.md) — visão do sistema, arquitetura e execução local.
2. [Documentação técnica](documentacao-tecnica.md) — componentes, ambiente, segurança e integrações.
3. [Mapa Frontend-Backend](frontend-backend-map.md) — rotas, APIs consumidas e lacunas conhecidas.
4. [Cadastro de cenários para RH](cadastro_cenarios_rh.md) — regras de produto e guardrails.
5. [Fonte canônica Gupy](GUPY-FONTE-CANONICA.md) — fonte externa autorizada e governança contra páginas duplicadas.
6. [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) — contrato implementado, payloads, estados e segurança.
7. [Homologação Gupy](HOMOLOGACAO-GUPY.md) — evidências, bloqueios e validação em vaga real.
8. [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) — retry, DLQ e entrega assíncrona.
9. [Rotas TanStack Start](../frontend/src/routes/README.md) — convenções do frontend.

## Documentação operacional atual

| Documento | Quando usar | Público | Estado |
| --- | --- | --- | --- |
| [README principal](../README.md) | Entender o produto e iniciar localmente. | Todos | Atual |
| [Documentação técnica](documentacao-tecnica.md) | Entender arquitetura, configuração e integrações. | Engenharia, DevOps | Atual |
| [Operação em produção](OPERACAO.md) | Operar, monitorar, auditar e tratar incidentes. | DevOps, Operação, Suporte | Atual |
| [Implantação do zero](IMPLANTACAO.md) | Instalar, configurar, publicar e validar. | DevOps, Engenharia | Atual |
| [Cadastro de cenários para RH](cadastro_cenarios_rh.md) | Entender regras de cadastro e produto. | RH, Produto, Compliance | Atual |
| [Mapa Frontend-Backend](frontend-backend-map.md) | Ver rotas e contratos internos. | Frontend, Backend, QA | Atual |
| [Fonte canônica Gupy](GUPY-FONTE-CANONICA.md) | Consultar a única referência externa autorizada e as regras de manutenção. | Integrações, Documentação | Atual |
| [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) | Implementar e validar `/test/**`. | Backend, Integrações | **Compatível tecnicamente; homologação pendente** |
| [Homologação Gupy](HOMOLOGACAO-GUPY.md) | Preparar evidências e validar o fluxo em vaga real. | Integrações, Operação | **Validação externa pendente** |
| [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | Operar retry, DLQ e entrega. | Backend, DevOps | Atual |
| [Rotas TanStack Start](../frontend/src/routes/README.md) | Criar e manter telas. | Frontend | Atual |

## Fontes de verdade

- Backend: `backend/src/main/java/br/com/iforce/praxis`.
- Migrações: `backend/src/main/resources/db/migration`.
- API do frontend: `frontend/src/lib/api/praxis.ts`.
- Rotas: `frontend/src/routes`.
- Menu: `frontend/src/components/app-shell.tsx`.
- Manuais contextuais: `frontend/src/lib/screen-manuals.ts`.
- Assistente: `frontend/src/lib/simulation-meta.ts`.
- Runtime: `frontend/src/lib/runtime-config.ts`, `runtime-config.server.ts` e `server.ts`.

## Fluxos por jornada

| Jornada | Ler |
| --- | --- |
| RH cria avaliação | [Cadastro de cenários](cadastro_cenarios_rh.md) e [Mapa Frontend-Backend](frontend-backend-map.md) |
| RH publica versão | [Cadastro de cenários](cadastro_cenarios_rh.md#fluxo-operacional-atual) |
| Candidato responde | [Mapa Frontend-Backend](frontend-backend-map.md#execucao-do-candidato) |
| Gupy lista avaliações | [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md#get-test) |
| Gupy cria tentativa | [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md#post-testcandidate) |
| Resultado é entregue | [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) |
| Operação reprocessa DLQ | [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md#monitoramento-e-operacao) |
| Preparar homologação Gupy | [Centro de homologação](HOMOLOGACAO-GUPY.md) |
| Revisar a fonte oficial | [Fonte canônica Gupy](GUPY-FONTE-CANONICA.md) |

## Produto e prontidão

- [P0 de prontidão](P0_PRODUCT_READINESS.md) — promessas comerciais seguras.
- [Resumo de implementação](IMPLEMENTATION_SUMMARY.md) — estado técnico resumido.
- [Pesquisa de reclamações de concorrentes](research/competitor-complaints-research-2025-2026.md) — contexto de mercado.

## Compliance e jurídico

Documentos em `docs/legal/` são minutas para validação jurídica e não constituem aconselhamento jurídico.

Principais referências:

- [RIPD / DPIA](legal/RIPD-DPIA.md)
- [Direitos do titular](legal/DIREITOS-DO-TITULAR-LGPD.md)
- [DPA](legal/DPA-ACORDO-DE-TRATAMENTO-DE-DADOS.md)
- [Política de Privacidade](legal/POLITICA-DE-PRIVACIDADE.md)
- [Termos de Uso](legal/TERMOS-DE-USO.md)
- [Política de Cookies](legal/POLITICA-DE-COOKIES.md)
- [Mitigação de viés](legal/POLITICA-MITIGACAO-VIES.md)
- [Resposta a incidentes](legal/PLANO-RESPOSTA-INCIDENTES.md)

## Histórico

- [Usabilidade — implementação](USABILIDADE_IMPLEMENTACAO.md) — registro histórico de UX.
- [Resumo de implementação](IMPLEMENTATION_SUMMARY.md) — fotografia técnica atual.

## Regras de manutenção

- Links devem apontar para arquivos reais.
- Documento histórico não deve ser usado como promessa de funcionalidade atual.
- Integração não homologada deve ser identificada explicitamente.
- Alterações de contrato público exigem atualização do documento correspondente.
- Links diretos para o portal da Gupy ficam apenas em `GUPY-FONTE-CANONICA.md`.
- Retry e DLQ pertencem ao documento de Outbox; checklist pertence ao documento de homologação.
- Execute `python3 scripts/validate_docs.py` antes do merge.

Última revisão: 18/07/2026.
