# Cadastro de Cenarios para RH - Especificacao Atual

> **Proposito:** orientar RH, produto, design e engenharia sobre como o Praxis cadastra, valida, publica e aplica simulacoes situacionais.
> **Status:** alinhado ao codigo atual em 04/07/2026.
> **Escopo:** produto implementado hoje + guardrails de uso + roadmap separado.

## Resumo executivo

Praxis e um motor de avaliacao situacional para recrutamento. Ele permite criar simulacoes de julgamento situacional (SJT), aplicar a candidatos e gerar evidencias comportamentais estruturadas para apoiar triagem, entrevista e decisoes humanas documentadas.

O produto nao mede "comportamento real" no sentido literal. Ele mede como o candidato escolhe agir diante de cenarios pre-escritos, com alternativas definidas pelo RH e score calculado por regras deterministicas.

Promessa defensavel:

> Avaliacao situacional estruturada para recrutamento, sem IA julgando candidato, com criterios explicitos, auditaveis e revisaveis, score por competencias, pesos e pontuacoes cadastradas nas alternativas, trilha auditavel e integracao Gupy.

## O que esta implementado hoje

| Area | Implementado |
| --- | --- |
| Login | Rota frontend `/login` integrada ao `POST /api/v1/auth/login`. |
| Criacao de avaliacao | Rascunho por `POST /api/v1/simulations/drafts`. |
| Plano da avaliacao | Nome, descricao, situacao critica, uso do resultado, competencias, pesos e target score. |
| Editor de grafo | CRUD de nos e alternativas. |
| Midia | Upload de imagem/audio para nos e alternativas. |
| Acessibilidade | Descricao acessivel, audio descricao e multiplicador de tempo no link interno. |
| Validacao | Blockers/warnings estruturais e `qualityScore`. |
| Publicacao | Publica versao sem blockers. |
| Versionamento | Versoes publicadas ficam imutaveis; edicao usa clone para novo rascunho. |
| Arquivamento | RH arquiva avaliacao por `POST /api/v1/simulations/{id}/archive`, preservando versoes, tentativas, resultados e auditoria. |
| Candidato | Fluxo publico por token com tentativa, progresso, resposta e conclusao. |
| Score | Deterministico, por alternativa, competencia e pesos. |
| Auditoria | Eventos por tentativa e por versao de simulacao. |
| Gupy | `GET /test`, `POST /test/candidate`, `GET /test/result/{resultId}`. |
| Entrega assincrona | Outbox com status `pending`, `retrying`, `sent`, `dlq`. |
| Notificacoes/DLQ | Tela `/notifications` lista alertas internos e permite reprocessar entregas em DLQ. |
| Monitoramento | Indicadores de tentativas e entregas por simulacao/versao. |
| Empresa config | Catalogos configuraveis por empresa. |
| Talent Match | Comparacao de ate 5 tentativas contra benchmark da versao. |
| LGPD | Politica operacional com indicacoes de bases legais, retencao e canal de revisao. |
| Admin plataforma | Rotas frontend `/admin*` e backend `/api/admin/**` para operacao interna com perfil `ADMIN`. |
| Billing | Cliente consulta plano/uso/eventos em `/billing`; criacao/sincronizacao de cobrancas fica no painel ADMIN/Mercado Pago. |

## O que nao esta implementado hoje

Estes itens podem existir como visao de produto, mas nao devem ser descritos como entregues:

- Workflow formal `em revisao -> aprovado -> rejeitado`.
- Estados persistidos `in_review`, `approved`, `rejected`, `expired` para versao.
- Maturidade persistida como `piloto`, `calibrada`, `validada internamente` ou `expirada`.
- Validacao preditiva estatistica.
- Auditoria agregada de vies por grupo sensivel.
- Base legal por vaga configuravel em tela.
- Portal self-service de exportacao/exclusao de dados.
- Checkout completo self-service de plano, assinatura ou compra de creditos pelo cliente.
- Avaliacao automatica de texto livre.
- IA/LLM avaliando resposta de candidato.
- Ranking decisorio automatico.
- Endpoint separado de ativacao Gupy.

## Principios de produto

1. **Sem IA julgando candidato:** a nota vem de alternativas e pesos cadastrados.
2. **Criterios explicitos, auditaveis e revisaveis:** nao prometer ausencia absoluta de vies; prometer rastreabilidade e revisao humana.
3. **Alternativas plausiveis:** se a resposta correta e obvia, a simulacao nao mede competencia.
4. **Versao imutavel:** candidato sempre responde a versao que estava publicada quando a tentativa foi criada.
5. **Evidencia antes da decisao:** o score apoia triagem e entrevista; nao deve ser a unica base de decisao sensivel.
6. **Empresa isolado:** dados de uma empresa nao devem aparecer para outra.
7. **Explicabilidade:** todo resultado precisa ser rastreavel ate escolhas, competencias e pesos.
8. **Acessibilidade:** tempo e midia precisam ter alternativas acessiveis.

## Limites legais e de uso

O Praxis nao deve ser apresentado como:

- teste psicologico;
- garantia de ausencia de vies;
- predicao validada de performance sem estudo local;
- substituto de entrevista ou revisao humana;
- ferramenta de eliminacao automatica sem governanca;
- sistema que mede comportamento real observado no trabalho.

Uso recomendado:

- apoiar entrevista estruturada;
- comparar evidencias dentro da mesma versao de simulacao;
- identificar pontos fortes e riscos comportamentais;
- documentar criterios de decisao;
- gerar perguntas de aprofundamento para o gestor.

## Guardrails para RH

Antes de publicar uma simulacao, RH deve confirmar:

- A situacao critica representa uma tarefa real do cargo.
- As competencias estao ligadas ao comportamento esperado.
- Os pesos somam 100% ou 1.0 conforme a interface/API.
- Cada alternativa e plausivel.
- Nenhuma alternativa usa estereotipo de classe, genero, idade, regiao, sotaque, religiao ou origem.
- Erros criticos estao descritos como evidencias de risco, nao como rotulo pessoal.
- Tempo limite tem justificativa operacional.
- Ha acomodacao quando o candidato precisa de mais tempo.
- O resultado sera usado com revisao humana quando houver impacto relevante.

## Fluxo operacional atual

A entrada publica/autenticacao usa `/comecar`: com seguranca ativa, redireciona para `/login`; em modo publico de teste, redireciona para `/avaliacoes`.

Depois da autenticacao, o caminho operacional recomendado para RH e:

```text
1. Acessar Avaliacoes
   /avaliacoes -> GET /api/v1/simulations

2. Criar do zero ou por modelo
   Do zero: /nova/avaliacao -> POST /api/v1/simulations/drafts
                          -> PATCH /api/v1/simulations/{id}/versions/{n}/blueprint
   Modelo rapido: /nova/rapido -> GET/POST /api/v1/simulations/quick-start*

3. Cenario (personagem, dialogo e midias)
   /nova/personagem -> CRUD de nodes/options
   (editor de grafo detalhado tambem em /nova/dialogo)

4. Revisao (validar)
   /nova/validador -> GET /validation

5. Publicacao
   /nova/governanca -> audit, clone-draft, publish

6. Aplicar
   Gupy: POST /test/candidate
   Interno: POST /api/v1/candidate-links

7. Monitorar e explicar
   /monitoramento, /notifications, /results, /compliance, /talent-match
```

Rotas standalone como `/nova/objetivo`, `/nova/dialogo`, `/nova/mapa`, `/nova/piloto` e `/nova/gupy` continuam uteis para manutencao, power users e diagnostico, mas nao devem ser vendidas como a jornada principal de onboarding.

## Telas atuais

| Tela | Rota | Objetivo |
| --- | --- | --- |
| Landing/entrada | `/` | Entrada inicial publica. |
| Comecar | `/comecar` | Redirecionamento de entrada: `/login` com seguranca ativa ou `/avaliacoes` em modo publico de teste. |
| Login | `/login` | Autenticacao da empresa. |
| Dashboard | `/dashboard` | Painel inicial de indicadores da empresa. |
| Avaliacoes | `/avaliacoes` | Ver, editar e arquivar avaliacoes; lista status, metricas e acoes. |
| Teste (passo 1) | `/nova/avaliacao` | Cria rascunho com nome, descricao, situacao critica, competencias, pesos e uso do resultado. |
| Cenario (passo 2) | `/nova/personagem` | Define personagem, turnos, alternativas e midias acessiveis. |
| Revisao (passo 3) | `/nova/validador` | Mostra blockers, warnings e qualidade. |
| Publicacao (passo 4) | `/nova/governanca` | Publica, clona e consulta auditoria. |
| Rapido | `/nova/rapido` | Cria avaliacao pre-preenchida a partir de um modelo pronto (quick-start). |
| Dialogo | `/nova/dialogo` | Editor de grafo detalhado (standalone, fora dos 4 passos). |
| Mapa | `/nova/mapa` | Visualiza grafo, destinos, criticidade e pesos (standalone). |
| Piloto | `/nova/piloto` | Exibe sinais de monitoramento da versao (standalone). |
| Gupy | `/nova/gupy` | Preflight e monitoramento de entregas (standalone). |
| Competencias | `/competencias` | Configura catalogos da empresa. |
| Enviar link | `/enviar-link` | Cria links internos e acompanha tentativas ao vivo. |
| Resultados | `/results` | Lista resultados; `/results/{attemptId}` detalha e registra a decisao do recrutador. |
| Monitoramento | `/monitoramento` | Indicadores e entregas. |
| Notificacoes | `/notifications` | Alertas internos e reprocessamento de DLQ. |
| Jornadas | `/jornadas` | Encadeia varias avaliacoes em um funil unico por candidato. |
| Talent Match | `/talent-match` | Comparacao por competencias, nao ranking automatico. |
| Integracoes | `/integrations` | Central de integracoes (Gupy, Recrutei e API propria). |
| Compliance | `/compliance` | Defensabilidade, evidencias, privacidade e explicabilidade (substitui `/defensabilidade` e `/lgpd`). |
| Plano | `/billing` | Plano, uso e historico de cobranca. |
| Admin plataforma | `/admin*` | Operacao interna da plataforma por perfil `ADMIN`. |
| Candidato | `/candidato` e `/candidato/$token` | Fluxo publico de resposta. |

## Modelo conceitual de uma simulacao

```text
Simulation
  id
  name
  description
  empresaId

SimulationVersion
  simulationId
  versionNumber
  status: draft | published | archived
  criticalSituation
  resultUse
  competencies[]
  nodes[]

Node
  id
  turnIndex
  speaker
  clientMessage
  timeLimitSeconds
  timeoutNextNodeId
  mediaUrl
  audioDescriptionUrl
  plainTextDescription
  options[]

Option
  id
  text
  competencyLevels
  isCritical
  nextNodeId
  mediaUrl
  audioDescriptionUrl
  plainTextDescription
```

## Estados reais

### Versao de simulacao

| Estado | Significado |
| --- | --- |
| `draft` | Editavel. |
| `published` | Aplicavel e imutavel. |
| `archived` | Fora de uso, preservada para historico. |

### Tentativa de candidato

| Estado | Significado |
| --- | --- |
| `notStarted` | Criada, ainda nao iniciada. |
| `inProgress` | Em andamento. |
| `paused` | Pausada. |
| `completed` | Finalizada com resultado. |
| `abandoned` | Abandonada. |
| `expired` | Expirada. |
| `failed` | Falhou por erro operacional ou tecnico. |

### Entrega de resultado

| Estado | Significado |
| --- | --- |
| `pending` | Aguardando envio. |
| `retrying` | Falhou e sera tentada novamente. |
| `sent` | Enviada com sucesso. |
| `dlq` | Esgotou tentativas e precisa de atuacao operacional/reprocessamento. |
