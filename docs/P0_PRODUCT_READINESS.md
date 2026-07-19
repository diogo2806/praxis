# P0 — Prontidão de produto antes de venda ou demonstração

> Fonte de verdade complementar para README, `frontend-backend-map.md` e `cadastro_cenarios_rh.md`.

> Objetivo: deixar claro o que está pronto para demonstração, o que é operação interna e quais promessas comerciais são seguras.

## Posicionamento seguro

Frase recomendada:

> Praxis adiciona ao ATS uma camada de avaliação situacional estruturada, com critérios explícitos, pesos cadastrados, score determinístico e trilha auditável — sem IA julgando candidato.

Evitar:

- “sem viés” como promessa absoluta;
- “livre de discriminação” sem estudo local;
- “prediz performance” sem validação estatística;
- “decisão automática” ou ranking eliminatório;
- “integração Gupy homologada” antes da aprovação oficial.

Usar:

- critérios explícitos, auditáveis e revisáveis;
- score calculado por regra cadastrada;
- apoio à triagem e entrevista estruturada;
- decisão final humana documentada;
- integração Gupy tecnicamente compatível e aguardando validação formal, enquanto esse for o estado real.

## Estado real do produto

### Produção e demonstração principal

| Área | Estado | Observação |
| --- | --- | --- |
| Login | Implementado | `/login` integrado a `POST /api/v1/auth/login`. |
| Dashboard | Implementado | Indicadores e ações recomendadas. |
| Avaliações | Implementado | Lista, edição, publicação e arquivamento com preservação de histórico. |
| Começar | Implementado | Entrada recomendada para RH. |
| Modelo rápido | Implementado | `/nova/rapido` cria rascunho por modelo. |
| Assistente de 4 passos | Implementado | Teste → Cenário → Revisão → Publicação. |
| Enviar link | Implementado | Links internos e acompanhamento de tentativas. |
| Gupy | **Compatibilidade técnica implementada** | Contrato `/test/**`, callback, páginas de resultado, outbox e centro de evidências implementados; homologação em vaga real pendente. |
| Recrutei | Implementado tecnicamente | Validar homologação comercial separadamente. |
| API própria | Implementada | Central de integrações, token e webhook próprios. |
| Resultados | Implementado | Lista, detalhe e decisão humana. |
| Compliance | Implementado | Defensabilidade, LGPD e explicabilidade operacional. |
| Notificações/DLQ | Implementado | `/notifications` lista alertas e reprocessa DLQ. |
| Billing | Implementado | Plano, uso, checkout e sincronização conforme permissões do fluxo atual. |

### Operação e administração

| Área | Estado | Observação |
| --- | --- | --- |
| Admin da plataforma | Implementado | Rotas `/admin*` e `/api/admin/**`, role `ADMIN`. |
| Billing admin | Implementado | Visão consolidada e suporte operacional. |
| Health Score / CS | Operação interna | Não é fluxo principal de RH. |
| Engagement mensal | Worker interno | Apoia retenção; não altera avaliação. |
| Homologação Gupy | Operação técnica | `/integrations/gupy-homologacao` consolida prontidão, bloqueios e evidências. |

### Rotas secundárias

Não apresentar como caminho principal de onboarding:

- `/nova/objetivo`;
- `/nova/dialogo`;
- `/nova/mapa`;
- `/nova/piloto`;
- `/nova/gupy`.

Caminho recomendado:

```text
Começar → Modelo rápido ou Do zero → 4 passos → Publicar → Enviar link ou preparar integração → Ver resultados
```

## Regra comercial para Gupy

Pode afirmar:

- o Praxis implementa o contrato de provedor externo da Gupy;
- há autenticação por token individual da empresa;
- `company_id` e `document_id` seguem o formato `int64`;
- há idempotência por candidato, avaliação e vaga;
- o candidato recebe `test_url` e retorna pelo `callback_url`;
- empresa e candidato possuem URLs de resultado separadas;
- consulta e webhook usam o mesmo contrato externo;
- há resultado percentual, monitoramento, retry e DLQ;
- existe diagnóstico local em `/nova/gupy`;
- existe centro de evidências em `/integrations/gupy-homologacao`.

Não pode afirmar ainda:

- que a integração está homologada ou certificada pela Gupy;
- que a Gupy aprovou comercialmente o Práxis como parceiro;
- que existe sandbox oficial de homologação;
- que qualquer cliente pode ativar sem validação conjunta;
- que evidência técnica interna substitui a aprovação externa.

## Documentos responsáveis

- [Fonte canônica da integração Gupy](GUPY-FONTE-CANONICA.md);
- [Contrato implementado](INTEGRACAO-GUPY-PROVEDOR.md);
- [Centro de homologação](HOMOLOGACAO-GUPY.md);
- [Arquitetura de Outbox](ARQUITETURA_OUTBOX_PATTERN.md).

## Verdades que devem permanecer sincronizadas

- `/login` existe.
- `/admin*` é operação de plataforma.
- `/notifications` é a tela dedicada de alertas e DLQ.
- O assistente de 4 passos é o fluxo principal.
- Rotas standalone são secundárias.
- Nunca prometer ausência absoluta de viés.
- Compatibilidade técnica não significa homologação formal.
- O callback é executado pelo navegador uma única vez.
- A aprovação Gupy depende de token, vaga e ambiente reais.

Última revisão: 18/07/2026.
