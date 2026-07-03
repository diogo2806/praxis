# Cadastro de Cenarios para RH - Especificacao Atual

> **Proposito:** orientar RH, produto, design e engenharia sobre como o Praxis cadastra, valida, publica e aplica simulacoes situacionais.
> **Status:** alinhado ao codigo atual em 20/06/2026.
> **Escopo:** produto implementado hoje + guardrails de uso + roadmap separado.

## Resumo executivo

Praxis e um motor de avaliacao situacional para recrutamento. Ele permite criar simulacoes de julgamento situacional (SJT), aplicar a candidatos e gerar evidencias comportamentais estruturadas para apoiar triagem, entrevista e decisoes humanas documentadas.

O produto nao mede "comportamento real" no sentido literal. Ele mede como o candidato escolhe agir diante de cenarios pre-escritos, com alternativas definidas pelo RH e score calculado por regras deterministicas.

Promessa defensavel:

> Avaliacao situacional estruturada para recrutamento, sem IA julgando candidato, com score por competencias, pesos e pontuacoes cadastradas nas alternativas, trilha auditavel e integracao Gupy.

## O que esta implementado hoje

| Area | Implementado |
| --- | --- |
| Criacao de avaliacao | Rascunho por `POST /api/v1/simulations/drafts`. |
| Plano da avaliacao | Nome, descricao, situacao critica, uso do resultado, competencias, pesos e target score. |
| Editor de grafo | CRUD de nos e alternativas. |
| Midia | Upload de imagem/audio para nos e alternativas. |
| Acessibilidade | Descricao acessivel, audio descricao e multiplicador de tempo no link interno. |
| Validacao | Blockers/warnings estruturais e `qualityScore`. |
| Publicacao | Publica versao sem blockers. |
| Versionamento | Versoes publicadas ficam imutaveis; edicao usa clone para novo rascunho. |
| Candidato | Fluxo publico por token com tentativa, progresso, resposta e conclusao. |
| Score | Deterministico, por alternativa, competencia e pesos. |
| Auditoria | Eventos por tentativa e por versao de simulacao. |
| Gupy | `GET /test`, `POST /test/candidate`, `GET /test/result/{resultId}`. |
| Entrega assincrona | Outbox com status `pending`, `retrying`, `sent`, `dlq`. |
| Monitoramento | Indicadores de tentativas e entregas por simulacao/versao. |
| Empresa config | Catalogos configuraveis por empresa. |
| Talent Match | Comparacao de ate 5 tentativas contra benchmark da versao. |
| LGPD | Politica operacional com indicacoes de bases legais, retencao e canal de revisao. |

## O que nao esta implementado hoje

Estes itens podem existir como visao de produto, mas nao devem ser descritos como entregues:

- Workflow formal `em revisao -> aprovado -> rejeitado`.
- Estados persistidos `in_review`, `approved`, `rejected`, `expired` para versao.
- Maturidade persistida como `piloto`, `calibrada`, `validada internamente` ou `expirada`.
- Validacao preditiva estatistica.
- Auditoria agregada de vies por grupo sensivel.
- Base legal por vaga configuravel em tela.
- Portal self-service de exportacao/exclusao de dados.
- Avaliacao automatica de texto livre.
- IA/LLM avaliando resposta de candidato.
- Ranking decisorio automatico.
- Administracao global `ADMIN`.
- Endpoint separado de ativacao Gupy.

## Principios de produto

1. **Sem IA julgando candidato:** a nota vem de alternativas e pesos cadastrados.
2. **Alternativas plausiveis:** se a resposta correta e obvia, a simulacao nao mede competencia.
3. **Versao imutavel:** candidato sempre responde a versao que estava publicada quando a tentativa foi criada.
4. **Evidencia antes da decisao:** o score apoia triagem e entrevista; nao deve ser a unica base de decisao sensivel.
5. **Empresa isolado:** dados de uma empresa nao devem aparecer para outra.
6. **Explicabilidade:** todo resultado precisa ser rastreavel ate escolhas, competencias e pesos.
7. **Acessibilidade:** tempo e midia precisam ter alternativas acessiveis.

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

```text
1. Criar rascunho
   /nova/avaliacao -> POST /api/v1/simulations/drafts

2. Ajustar plano
   /nova/objetivo -> PATCH /api/v1/simulations/{id}/versions/{n}/blueprint

3. Montar personagem e dialogo
   /nova/personagem
   /nova/dialogo -> CRUD de nodes/options

4. Validar
   /nova/validador -> GET /validation

5. Revisar mapa e governanca
   /nova/mapa
   /nova/governanca -> audit, clone-draft, publish

6. Conferir integracao Gupy
   /nova/gupy -> GET /gupy-preflight e entregas

7. Aplicar
   Gupy: POST /test/candidate
   Interno: POST /api/v1/candidate-links

8. Monitorar e explicar
   /monitoramento, /defensabilidade, /lgpd, /talent-match
```

## Telas atuais

| Tela | Rota | Objetivo |
| --- | --- | --- |
| Avaliações | `/avaliacoes` | Ver e editar avaliacoes: lista, status, metricas e acoes. |
| Começar | `/comecar` | Entrada para criar nova avaliacao. |
| Avaliacao | `/nova/avaliacao` | Cria rascunho com nome, descricao, situacao critica e competencias (`/nova/blueprint` apenas redireciona para ca). |
| Competencias | `/nova/competencias` | Configura catalogos do empresa. |
| Objetivo | `/nova/objetivo` | Ajusta plano da avaliacao, pesos e uso do resultado. |
| Personagem | `/nova/personagem` | Define primeiro no/personagem e midias acessiveis. |
| Dialogo | `/nova/dialogo` | Edita grafo, alternativas, score e ramificacoes. |
| Validador | `/nova/validador` | Mostra blockers, warnings e qualidade. |
| Piloto | `/nova/piloto` | Exibe sinais de monitoramento da versao. |
| Mapa | `/nova/mapa` | Visualiza grafo, destinos, criticidade e pesos. |
| Governanca | `/nova/governanca` | Publica, clona e consulta auditoria. |
| Gupy | `/nova/gupy` | Preflight e monitoramento de entregas. |
| Enviar link | `/enviar-link` | Cria links internos e acompanha tentativas ao vivo. |
| Monitoramento | `/monitoramento` | Indicadores e entregas. |
| Talent Match | `/talent-match` | Comparacao por competencias, nao ranking automatico. |
| Defensabilidade | `/defensabilidade` | Explica evidencias e sustentacao do score. |
| LGPD | `/lgpd` | Exibe politica de privacidade e explicabilidade. |
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
| `archived` | Versao antiga, preservada para historico. |

### Tentativa de candidato

| Estado | Significado |
| --- | --- |
| `notStarted` | Criada, ainda nao iniciada. |
| `inProgress` | Em andamento. |
| `paused` | Pausada. |
| `completed` | Finalizada com resultado. |
| `abandoned` | Abandonada. |
| `expired` | Expirada. |
| `failed` | Falhou por erro operacional. |

### Entrega Gupy/outbox

| Estado | Significado |
| --- | --- |
| `pending` | Aguardando primeira tentativa. |
| `retrying` | Falhou e sera reprocessada. |
| `sent` | Enviada com sucesso. |
| `dlq` | Falha permanente ou limite de tentativas atingido. |

## Regras de cadastro

### Blueprint

Campos esperados:

- nome da avaliacao;
- descricao;
- situacao critica;
- uso do resultado;
- competencias;
- peso por competencia;
- target score por competencia, quando aplicavel.

Boas praticas:

- escrever a situacao como problema concreto do cargo;
- evitar historia dramatica sem relacao com decisao de trabalho;
- limitar competencias para manter interpretabilidade;
- justificar qualquer uso de tempo limite.

### Personagem e primeiro turno

O personagem deve existir para dar contexto, nao para criar estereotipo.

Evite:

- sotaque escrito;
- classe social caricata;
- idade como marcador de incapacidade;
- genero sem relevancia;
- nomes ou descricoes que induzam vies.

### Dialogo e alternativas

Cada turno deve ter de 2 a 4 alternativas plausiveis.

Alternativa ruim:

```text
A. Ser educado e resolver tudo.
B. Ignorar o cliente.
C. Ofender o cliente.
```

Alternativas melhores:

```text
A. Acolhe bem, mas promete excecao sem validar politica.
B. Segue o processo corretamente, mas comunica de forma fria.
C. Acolhe, coleta dados minimos e explica o proximo passo.
D. Resolve rapido, mas deixa de registrar evidencias.
```

## Score e interpretacao

O score e deterministico. A mesma sequencia de respostas na mesma versao deve produzir o mesmo resultado.

Cuidados:

- Compare candidatos apenas dentro da mesma simulacao e versao.
- Nao trate diferenca pequena de score como verdade absoluta.
- Use o resultado por competencia para orientar entrevista.
- Se uma alternativa critica foi escolhida, registre evidencia e revise contexto.
- Talent Match e comparacao com benchmark; nao e ranking decisorio automatico.

## Validador

O validador atual e estrutural. Ele ajuda a bloquear publicacao insegura, mas nao substitui revisao humana.

Exemplos de checagens:

- existencia de no inicial;
- competencias e pesos validos;
- grafo navegavel;
- alternativas com score;
- blockers e warnings;
- quality score.

Nao faz hoje:

- analise semantica de vies;
- validacao preditiva;
- deteccao automatica de resposta obvia por NLP;
- revisao juridica.

## Gupy

### Endpoints que o Praxis implementa

```text
GET  /test
POST /test/candidate
GET  /test/result/{resultId}?company_id={companyId}
```

### Criacao de tentativa

A Gupy chama `POST /test/candidate`. O Praxis cria ou reutiliza uma tentativa idempotente e devolve `test_url` e `test_result_id`.

Body real esperado:

```json
{
  "company_id": "empresa-123",
  "document_id": "candidate-document-456",
  "test_id": "sim-atendimento",
  "name": "Candidato Teste",
  "email": "candidato@example.com",
  "result_webhook_url": "https://integracao.gupy.example/webhook"
}
```

O candidato acessa o link publico e responde no Praxis.

### Resultado

O resultado pode chegar a Gupy de duas formas:

- consulta por `GET /test/result/{resultId}`;
- envio assincrono para `result_webhook_url`, quando configurado.

Nao documentar como comportamento atual obrigatorio:

- redirect final de volta para a Gupy;
- chamada de callback de retorno ao finalizar;
- endpoint de ativacao Gupy separado.

## Outbox e entregas

Eventos de resultado e engajamento sao gravados em outbox e processados de forma assincrona.

Backoff atual:

| Tentativa | Delay |
| --- | --- |
| 1 | 1 segundo |
| 2 | 4 segundos |
| 3 | 16 segundos |
| 4 | 64 segundos |
| 5+ | DLQ |

Operacao:

```text
GET  /api/v1/gupy/result-deliveries
GET  /api/v1/gupy/result-deliveries/ready
POST /api/v1/gupy/result-deliveries/process-ready
POST /api/v1/gupy/result-deliveries/{deliveryId}/reprocess
```

## LGPD e explicabilidade

Implementado hoje:

- endpoint `/api/v1/privacy/compliance`;
- politica de retencao configuravel por `PRAXIS_PRIVACY_RETENTION_DAYS`;
- indicacao de bases legais;
- canal de revisao;
- flag de decisao automatizada sem revisao;
- auditoria operacional de eventos.

Guardrails recomendados:

- informar ao candidato que a avaliacao e situacional;
- manter revisao humana em decisoes sensiveis;
- registrar por que a simulacao e relevante para o cargo;
- evitar uso eliminatorio quando a simulacao nao foi validada internamente;
- oferecer canal de revisao;
- preservar versao e evidencia usadas na decisao.

Roadmap para uso enterprise:

- base legal configuravel por vaga;
- exportacao de dados por candidato;
- anonimizacao/exclusao self-service;
- controle granular de quem acessou resultado;
- relatorio de impacto e vies agregado;
- fluxo formal de contestacao.

## Criterios de aceite para cadastro de uma simulacao

- [ ] Nome e descricao explicam cargo/contexto.
- [ ] Situacao critica e realista e ligada ao cargo.
- [ ] Competencias estao definidas e ponderadas.
- [ ] Pesos respeitam tolerancia configurada no backend.
- [ ] Grafo possui no inicial.
- [ ] Todos os caminhos relevantes chegam a conclusao ou proximo no valido.
- [ ] Alternativas sao plausiveis.
- [ ] Opcoes criticas sao justificadas.
- [ ] Midias possuem descricao acessivel quando usadas.
- [ ] Tempo limite tem justificativa e acomodacao quando necessario.
- [ ] Validador nao apresenta blockers.
- [ ] Resultado sera usado com revisao humana quando houver impacto relevante.

## Roadmap separado

### P1

- Workflow formal de revisao e aprovacao.
- Tela de maturidade da simulacao.
- Relatorios de calibracao com amostra de colaboradores.
- Politica LGPD configuravel por empresa/vaga.
- Revisao formal de candidato.

### P2

- Estudos de validade preditiva.
- Fairness monitoring agregado.
- Banco de familias de cenarios anti-vazamento.
- Comparacao longitudinal de versoes.
- Integrações ATS adicionais, apenas com contrato real.

## Decisoes editoriais

Este documento substitui a especificacao antiga, que misturava:

- manifesto de produto;
- wireframes em ASCII;
- contrato tecnico alvo;
- features futuras;
- endpoints antigos.

A regra atual e simples: o que for capacidade entregue fica em "implementado hoje"; o que for direcao de produto fica em "roadmap"; o que for risco de uso fica em "guardrails".

Ultima revisao: 20/06/2026.
