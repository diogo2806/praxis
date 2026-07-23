# Fluxo de autoria de avaliações

## Objetivo

O Práxis possui um único percurso oficial para criar, revisar e publicar uma avaliação. As telas com responsabilidades específicas não representam assistentes concorrentes: elas são subetapas do mesmo contexto de avaliação e versão.

## Fluxo oficial

Os quatro passos exibidos pelo assistente são:

1. **Teste** — `/nova/avaliacao`
2. **Cenário** — `/nova/personagem`
3. **Revisão** — `/nova/validador`
4. **Publicação** — `/nova/governanca`

O passo **Cenário** possui três subetapas com responsabilidades distintas e navegação compartilhada:

1. **Personagem** — `/nova/personagem`: contexto e mensagem inicial.
2. **Diálogo** — `/nova/dialogo`: etapas, alternativas, mídia, pontuação e desfechos.
3. **Mapa do fluxo** — `/nova/mapa`: conexões, ramificações e organização visual.

Diálogo e Mapa não são novos assistentes. O componente `WizardStepper` sempre os apresenta como partes do passo 2 e preserva `simulationId`, `versionNumber` e `nodeId` durante a navegação.

## Rotas de compatibilidade

As URLs abaixo não mantêm telas próprias:

- `/nova/objetivo` redireciona para `/nova/avaliacao`;
- `/simulations/new` redireciona para `/nova/avaliacao`.

Os redirecionamentos preservam os parâmetros de avaliação e versão quando eles forem informados.

## Começar rápido

`/nova/rapido` continua disponível como entrada por modelo pronto. Depois de criar o rascunho, o usuário é encaminhado para `/nova/avaliacao` com a avaliação e a versão recém-criadas, seguindo o mesmo percurso oficial de uma criação iniciada do zero.

## Operação pós-publicação

As telas abaixo preservam funcionalidades exclusivas, mas não aparecem como passos do assistente de autoria:

- `/nova/piloto`: indicadores reais, abandono, conclusão e calibração;
- `/nova/gupy`: preflight técnico de uma versão publicada antes da ativação na Gupy.

Essas telas são contextuais e retornam para Governança, Integrações ou Monitoramento. Elas não criam nem mantêm uma segunda sequência de edição.

## Fonte única

As rotas e sua ordem são declaradas em `frontend/src/lib/authoring-flow.ts`. Os quatro passos de interface usam essa definição em `frontend/src/lib/simulation-meta.ts`.

O teste `frontend/scripts/test-authoring-flow.mjs` impede regressões como:

- recriar conteúdo próprio em uma rota de compatibilidade;
- fazer o início rápido pular diretamente para a revisão;
- apresentar Diálogo ou Mapa como outro passo do assistente;
- recolocar Piloto ou Gupy dentro do `WizardStepper`;
- reintroduzir o manual específico da rota `/nova/objetivo`.
