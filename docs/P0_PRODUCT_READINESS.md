# P0 - Prontidao de Produto antes de venda ou demonstracao

> Fonte de verdade complementar para README, `frontend-backend-map.md` e `cadastro_cenarios_rh.md`.
> Objetivo: deixar claro o que esta pronto para demonstracao, o que e operacao interna e quais promessas comerciais sao seguras.

## Posicionamento seguro

Frase principal recomendada:

> Praxis adiciona ao ATS uma camada de avaliacao situacional estruturada, com criterios explicitos, pesos cadastrados, score deterministico e trilha auditavel — sem IA julgando candidato.

Evitar em venda, tela ou documentacao publica:

- "sem vies" como promessa absoluta;
- "livre de discriminacao" sem estudo local;
- "prediz performance" sem validacao estatistica da empresa;
- "decisao automatica" ou ranking eliminatorio.

Usar no lugar:

- criterios explicitos, auditaveis e revisaveis;
- score calculado por regra cadastrada;
- apoio a triagem e entrevista estruturada;
- decisao final humana documentada;
- sem IA/LLM julgando resposta de candidato.

## Estado real do produto

### Producao / demonstracao principal

| Area | Estado | Observacao |
| --- | --- | --- |
| Login | Implementado | Ha rota frontend `/login` integrada ao `POST /api/v1/auth/login`. |
| Entrada `/comecar` | Implementado como redirecionamento | Com seguranca ativa, leva para `/login`; em modo publico de teste, leva para `/avaliacoes`. Nao deve ser descrita como tela de autoria. |
| Dashboard | Implementado | Consolida indicadores e acoes recomendadas da empresa. |
| Avaliacoes | Implementado | Lista avaliacoes, mostra status, competencias, tentativas e arquiva em vez de incentivar exclusao definitiva. |
| Criacao do zero | Implementado | `/nova/avaliacao` inicia o assistente de 4 passos. |
| Modelo rapido | Implementado | `/nova/rapido` cria rascunho a partir de modelo pronto. |
| Assistente de 4 passos | Implementado | Caminho canonico: Teste -> Cenario -> Revisao -> Publicacao. |
| Enviar link | Implementado | Gera links internos e acompanha tentativas ao vivo. |
| Gupy | Implementado | Contrato externo `/test/**`, preflight e entregas por outbox. |
| Resultados | Implementado | Lista/detalha tentativa e registra decisao humana. |
| Compliance | Implementado | Reune defensabilidade, LGPD e explicabilidade operacional. |
| Notificacoes/DLQ | Implementado | Rota `/notifications` lista alertas internos e permite reprocessar entregas em DLQ. |

### Operacao/admin

| Area | Estado | Observacao |
| --- | --- | --- |
| Admin plataforma | Implementado | Rotas `/admin*` no frontend e `/api/admin/**` no backend exigem perfil `ADMIN`. |
| Billing | Parcialmente self-service | Cliente consulta plano/uso/eventos em `/billing`; criacao/sincronizacao de cobranca segue no painel ADMIN/Mercado Pago. Botoes sem URL operacional nao devem ser vendidos como checkout completo. |
| Health Score / CS | Implementado como operacao interna | Fila admin de empresas em risco, nao fluxo principal de RH. |
| Engagement mensal | Implementado como worker | Apoia retencao e percepcao de valor, nao altera o fluxo de avaliacao. |

### Rotas standalone secundarias

Estas rotas continuam uteis para manutencao, power users ou diagnostico, mas nao devem ser apresentadas como caminho principal de onboarding:

- `/nova/objetivo`
- `/nova/dialogo`
- `/nova/mapa`
- `/nova/piloto`
- `/nova/gupy`

O caminho recomendado para RH autenticado e:

```text
Avaliacoes -> Nova avaliacao do zero ou Modelo rapido -> 4 passos -> Publicar -> Enviar link/Gupy -> Ver resultados
```

## Lacunas P0 fechadas

1. **Notificacoes operacionais:** tela dedicada em `/notifications` consome `GET /api/v1/notifications`.
2. **Reprocessamento de DLQ:** a mesma tela lista entregas com status `dlq` e chama `POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess`.
3. **Arquivamento seguro:** a lista de avaliacoes usa arquivamento, preservando historico e auditoria, em vez de incentivar exclusao definitiva.
4. **Mensagem de produto:** este documento consolida a promessa segura: criterios explicitos, auditaveis e revisaveis, sem IA julgando candidato.

## Proxima revisao de documentacao

Ao editar README, `frontend-backend-map.md` ou `cadastro_cenarios_rh.md`, manter esta verdade:

- `/login` existe.
- `/comecar` e redirecionamento de entrada/autenticacao, nao uma tela de autoria.
- `/admin*` existe e e operacao de plataforma, nao fluxo de RH.
- `/billing` e leitura de plano/uso e historico para cliente; checkout completo ainda depende das acoes administrativas disponiveis.
- `/notifications` e a tela dedicada para alertas internos e DLQ.
- O assistente de 4 passos e o fluxo principal de criacao; as rotas standalone sao secundarias.
- Nunca prometer ausencia absoluta de vies; prometer criterio explicito, auditavel e revisavel.
