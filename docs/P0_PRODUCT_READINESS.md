# P0 - Prontidão de Produto antes de venda ou demonstração

> Fonte de verdade complementar para README, `frontend-backend-map.md` e `cadastro_cenarios_rh.md`.
> Objetivo: deixar claro o que está pronto para demonstração, o que é operação interna e quais promessas comerciais são seguras.

## Posicionamento seguro

Frase principal recomendada:

> Praxis adiciona ao ATS uma camada de avaliação situacional estruturada, com critérios explícitos, pesos cadastrados, score determinístico e trilha auditável — sem IA julgando candidato.

Evitar em venda, tela ou documentação pública:

- "sem viés" como promessa absoluta;
- "livre de discriminação" sem estudo local;
- "prediz performance" sem validação estatística da empresa;
- "decisão automática" ou ranking eliminatório.

Usar no lugar:

- critérios explícitos, auditáveis e revisáveis;
- score calculado por regra cadastrada;
- apoio à triagem e entrevista estruturada;
- decisão final humana documentada;
- sem IA/LLM julgando resposta de candidato.

## Estado real do produto

### Produção / demonstração principal

| Área | Estado | Observação |
| --- | --- | --- |
| Login | Implementado | Há rota frontend `/login` integrada ao `POST /api/v1/auth/login`. |
| Dashboard | Implementado | Consolida indicadores e ações recomendadas da empresa. |
| Avaliações | Implementado | Lista avaliações, mostra status, competências, tentativas e agora arquiva em vez de excluir definitivamente. |
| Começar | Implementado | Entrada recomendada para RH antes de criar uma avaliação. |
| Modelo rápido | Implementado | `/nova/rapido` cria rascunho a partir de modelo pronto. |
| Assistente de 4 passos | Implementado | Caminho canônico: Teste -> Cenário -> Revisão -> Publicação. |
| Enviar link | Implementado | Gera links internos e acompanha tentativas ao vivo. |
| Gupy | Implementado | Contrato externo `/test/**`, preflight e entregas por outbox. |
| Resultados | Implementado | Lista/detalha tentativa e registra decisão humana. |
| Compliance | Implementado | Reúne defensabilidade, LGPD e explicabilidade operacional. |
| Notificações/DLQ | Implementado | Nova rota `/notifications` lista alertas internos e permite reprocessar entregas em DLQ. |
| Billing self-service | Implementado nesta entrega | `/billing` lista planos, gera checkout de créditos/assinatura no Mercado Pago e permite sincronizar a assinatura do próprio cliente. |

### Operação/admin

| Área | Estado | Observação |
| --- | --- | --- |
| Admin plataforma | Implementado | Rotas `/admin*` no frontend e `/api/admin/**` no backend exigem perfil `ADMIN`. |
| Billing admin | Implementado | ADMIN continua com visão consolidada, criação assistida de cobranças e sincronização manual como operação de suporte. |
| Health Score / CS | Implementado como operação interna | Fila admin de empresas em risco, não fluxo principal de RH. |
| Engagement mensal | Implementado como worker | Apoia retenção e percepção de valor, não altera o fluxo de avaliação. |

### Rotas standalone secundárias

Estas rotas continuam úteis para manutenção, power users ou diagnóstico, mas não devem ser apresentadas como caminho principal de onboarding:

- `/nova/objetivo`
- `/nova/dialogo`
- `/nova/mapa`
- `/nova/piloto`
- `/nova/gupy`

O caminho recomendado para RH é:

```text
Começar -> Modelo rápido ou Do zero -> 4 passos -> Publicar -> Enviar link/Gupy -> Ver resultados
```

## Lacunas P0 fechadas nesta entrega

1. **Notificações operacionais:** nova tela dedicada em `/notifications` consome `GET /api/v1/notifications`.
2. **Reprocessamento de DLQ:** a mesma tela lista entregas com status `dlq` e chama `POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess`.
3. **Arquivamento seguro:** a lista de avaliações passou a usar arquivamento, preservando histórico e auditoria, em vez de incentivar exclusão definitiva.
4. **Mensagem de produto:** este documento consolida a promessa segura: critérios explícitos, auditáveis e revisáveis, sem IA julgando candidato.
5. **Billing self-service:** o cliente passa a gerar checkout de créditos/assinatura e sincronizar a própria assinatura sem depender do ADMIN para a ação operacional básica.

## Próxima revisão de documentação

Ao editar README, `frontend-backend-map.md` ou `cadastro_cenarios_rh.md`, manter esta verdade:

- `/login` existe.
- `/admin*` existe e é operação de plataforma, não fluxo de RH.
- `/billing` é o self-service do cliente para plano, uso, histórico, checkout de créditos/assinatura e sincronização da assinatura atual.
- `/api/admin/**` continua sendo a operação de suporte para visão consolidada, criação assistida e sincronização manual quando necessário.
- `/notifications` é a tela dedicada para alertas internos e DLQ.
- O assistente de 4 passos é o fluxo principal; as rotas standalone são secundárias.
- Nunca prometer ausência absoluta de viés; prometer critério explícito, auditável e revisável.
