# Índice de Documentação — Praxis

> Trilha para entender o produto, executar o projeto e consultar contratos técnicos sem confundir implementação local com homologação externa.

## Comece aqui

1. [README principal](../README.md) — visão do sistema, arquitetura e execução local.
2. [Documentação técnica](documentacao-tecnica.md) — componentes, ambiente, segurança e integrações.
3. [Mapa Frontend-Backend](frontend-backend-map.md) — rotas, APIs consumidas e lacunas conhecidas.
4. [Cadastro de cenários para RH](cadastro_cenarios_rh.md) — regras de produto e guardrails.
5. [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) — contrato implementado, comparação oficial e bloqueadores de homologação.
6. [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) — retry, DLQ e entrega assíncrona.
7. [Rotas TanStack Start](../frontend/src/routes/README.md) — convenções do frontend.

## Documentação operacional atual

| Documento | Quando usar | Público | Estado |
| --- | --- | --- | --- |
| [README principal](../README.md) | Entender o produto e iniciar localmente. | Todos | Atual |
| [Documentação técnica](documentacao-tecnica.md) | Entender arquitetura, configuração e integrações. | Engenharia, DevOps | Atual |
| [Operação em produção](OPERACAO.md) | Operar, monitorar, auditar e tratar incidentes. | DevOps, Operação, Suporte | Atual |
| [Implantação do zero](IMPLANTACAO.md) | Instalar, configurar, publicar e validar. | DevOps, Engenharia | Atual |
| [Cadastro de cenários para RH](cadastro_cenarios_rh.md) | Entender regras de cadastro e produto. | RH, Produto, Compliance | Atual |
| [Mapa Frontend-Backend](frontend-backend-map.md) | Ver rotas e contratos internos. | Frontend, Backend, QA | Atual |
| [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md) | Implementar e homologar `/test/**`. | Backend, Integrações | **Compatibilidade parcial; homologação pendente** |
| [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | Operar retry, DLQ e entrega. | Backend, DevOps | Atual |
| [Rotas TanStack Start](../frontend/src/routes/README.md) | Criar e manter telas. | Frontend | Atual |

## Fontes de verdade

- Backend: `backend/src/main/java/br/com/iforce/praxis`.
- Migrações: `backend/src/main/resources/db/migration`.
- API do frontend: `frontend/src/lib/api/praxis.ts`.
- Rotas: `frontend/src/routes`.
- Menu: `frontend/src/components/app-shell.tsx`.
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
| Resultado é entregue | [Integração Gupy](INTEGRACAO-GUPY-PROVEDOR.md#outbox-e-entrega-assincrona) |
| Operação reprocessa DLQ | [Arquitetura Outbox](ARQUITETURA_OUTBOX_PATTERN.md) |
| Preparar homologação Gupy | [Bloqueadores](INTEGRACAO-GUPY-PROVEDOR.md#bloqueadores-para-homologacao) |

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

Última revisão: 12/07/2026.
