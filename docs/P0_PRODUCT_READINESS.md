# P0 — Prontidão de produto antes de venda ou demonstração

> Fonte de verdade complementar para README, `frontend-backend-map.md` e `cadastro_cenarios_rh.md`.
>
> Objetivo: deixar claro o que está pronto para demonstração, o que é operação interna e quais promessas comerciais são seguras.

## Posicionamento seguro

Frase recomendada:

> Praxis adiciona ao ATS uma camada de avaliação situacional estruturada, com critérios explícitos, pesos cadastrados, score determinístico e trilha auditável — sem IA julgando candidato.

Evitar:

- “sem viés” como promessa absoluta;
- “livre de discriminação” sem estudo local;
- “prediz performance” sem validação estatística;
- “decisão automática” ou ranking eliminatório;
- “integração Gupy homologada” antes da validação oficial.

Usar:

- critérios explícitos, auditáveis e revisáveis;
- score calculado por regra cadastrada;
- apoio à triagem e entrevista estruturada;
- decisão final humana documentada;
- integração técnica em preparação para homologação, quando esse for o estado real.

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
| Gupy | **Implementação técnica parcial** | `/test/**`, preflight e outbox existem, mas callback, redirecionamento e assinatura oficial do resultado ainda bloqueiam homologação. |
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

- o Praxis possui endpoints de provedor externo;
- há autenticação por token de empresa;
- há idempotência, resultado percentual e entrega assíncrona;
- há monitoramento, retry e DLQ;
- existe diagnóstico local em `/nova/gupy`.

Não pode afirmar ainda:

- que a integração está homologada pela Gupy;
- que o candidato retorna automaticamente à Gupy;
- que o contrato oficial é atendido integralmente;
- que existe sandbox oficial de homologação;
- que qualquer cliente pode ativar sem validação conjunta.

Documento técnico: [INTEGRACAO-GUPY-PROVEDOR.md](INTEGRACAO-GUPY-PROVEDOR.md).

## Verdades que devem permanecer sincronizadas

- `/login` existe.
- `/admin*` é operação de plataforma.
- `/notifications` é a tela dedicada de alertas e DLQ.
- O assistente de 4 passos é o fluxo principal.
- Rotas standalone são secundárias.
- Nunca prometer ausência absoluta de viés.
- Nunca promover Gupy como homologada enquanto houver bloqueadores no contrato oficial.

Última revisão: 12/07/2026.
