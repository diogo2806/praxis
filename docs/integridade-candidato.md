# Integridade técnica na aplicação do candidato

## Finalidade

Esta implementação adapta a governança do AIA Evidence Engine ao fluxo público de avaliações do Práxis.

O módulo observa somente as condições técnicas da execução. Ele não altera respostas, pontuação, competências, regras da avaliação, resultado enviado à Gupy ou decisão sobre a candidatura.

## Escopo implementado

### Sessão técnica única

Cada tentativa pode manter uma única sessão técnica ativa.

Fluxo:

1. a pessoa lê as instruções e aceita o aviso de privacidade;
2. o navegador solicita a abertura da sessão técnica;
3. o backend valida o token da tentativa;
4. a mesma aba pode retomar a sessão já aberta;
5. outra sessão ativa recebe HTTP 409 com mensagem neutra;
6. a sessão expira após 90 segundos sem heartbeat;
7. uma nova sessão pode ser aberta após a expiração ou o encerramento da anterior.

A trava usa o banco de dados nesta primeira versão. A restrição parcial `uq_integrity_active_attempt` garante no PostgreSQL que não existam duas sessões `ACTIVE` para a mesma tentativa, inclusive em condição de corrida.

### Telemetria registrada

Os eventos aceitos do navegador são:

- visibilidade da aba;
- mudança do modo de entrada entre ponteiro, toque e teclado;
- apresentação de uma etapa;
- carregamento de imagem ou áudio;
- início de estímulo em áudio;
- seleção e confirmação de resposta, sem texto ou identificador da alternativa.

O backend também registra abertura, retomada, heartbeat, expiração e encerramento da sessão.

### Fila de revisão técnica

A Central operacional sincroniza uma fila restrita a partir de regras determinísticas e retroativas. A regra apenas encaminha a tentativa para revisão humana; não classifica intenção e não produz score de risco.

Regras iniciais:

| Código | Regra de encaminhamento | Explicação neutra |
|---|---|---|
| `SESSION_INTERRUPTION` | uma ou mais sessões expiradas | interrupção compatível com conexão instável, suspensão do navegador ou pausa operacional |
| `VISIBILITY_CHANGES` | três ou mais eventos `TAB_HIDDEN` | alternância recorrente de visibilidade, sem inferência sobre intenção |
| `SESSION_RESUMPTIONS` | duas ou mais retomadas | recargas, perdas de conexão ou retomadas operacionais repetidas |
| `INPUT_MODE_CHANGES` | oito ou mais mudanças de entrada | alternância entre teclado, toque e ponteiro, sem inferir deficiência ou comportamento |

A revisão aceita somente os pareceres:

- `NO_IMPACT` — Sem impacto;
- `TECHNICAL_ISSUE_CONFIRMED` — Problema técnico confirmado;
- `REAPPLICATION_RECOMMENDED` — Reaplicação recomendada;
- `PRIVACY_COMPLIANCE_REVIEW` — Análise de privacidade/compliance.

Todo parecer exige justificativa textual e registra responsável, data, decisão anterior, nova decisão e opção de compartilhamento. Cada abertura do detalhe também gera uma entrada de acesso às evidências.

### Separação da pontuação

As informações ficam exclusivamente em:

- `candidate_integrity_sessions`;
- `candidate_integrity_events`;
- `candidate_integrity_reviews`;
- `candidate_integrity_review_audit`.

Não foram adicionados campos de integridade em:

- `candidate_attempts`;
- respostas da tentativa;
- itens de resultado;
- payload enviado à Gupy.

Nenhum serviço de pontuação consulta as tabelas de integridade. O parecer técnico não chama `save` da tentativa e não altera score, competência, resultado da integração ou decisão de contratação.

Quando o revisor autoriza o compartilhamento, o relatório empresarial recebe somente o status neutro e a data. Evidências, justificativa e identidade do responsável permanecem restritas.

## Dados e proteção

| Campo | Finalidade | Proteção |
|---|---|---|
| `client_session_id` | Retomar a mesma sessão do navegador | Valor aleatório, sem dado pessoal |
| `ip_hash` | Correlacionar tecnicamente acessos quando necessário | HMAC-SHA-256; IP bruto não é persistido nem devolvido pela API de revisão |
| `user_agent_category` | Distinguir categoria geral do dispositivo | Somente desktop, tablet, mobile ou desconhecido |
| `input_mode` | Interpretar corretamente uso de teclado, toque ou ponteiro | Não representa deficiência e não gera pontuação |
| `visibility_state` | Registrar visibilidade da aba | Somente `VISIBLE` ou `HIDDEN` |
| `event_type` | Construir linha do tempo técnica | Lista fechada de eventos permitidos |
| `detail` | Identificar categoria de mídia | Somente `IMAGE`, `AUDIO` ou `OTHER` |
| `justification` | Fundamentar o parecer humano | Restrita aos perfis autorizados e preservada na auditoria |
| `share_with_company` | Autorizar status neutro no relatório | Não libera evidências, justificativa ou responsável |

O cliente não pode enviar pontos, gabarito, classificação, score de risco ou conteúdo textual da resposta pelos endpoints de telemetria.

## Endpoints

### Fluxo público do candidato

```text
POST /candidate/attempts/{attemptToken}/integrity/session
POST /candidate/attempts/{attemptToken}/integrity/heartbeat
POST /candidate/attempts/{attemptToken}/integrity/events
POST /candidate/attempts/{attemptToken}/integrity/close
```

Todos exigem o mesmo token público assinado usado no fluxo do candidato.

Após a abertura, o identificador opaco retornado em `sessionId` deve acompanhar as operações protegidas no cabeçalho:

```text
X-Praxis-Integrity-Session: {sessionId}
```

O cabeçalho é obrigatório em produção para:

```text
GET  /candidate/attempts/{attemptToken}
POST /candidate/attempts/{attemptToken}/answers
```

Assim, ocultar ou bloquear a tela no React não é a fronteira de segurança: o backend também recusa o carregamento da etapa e o envio de resposta sem uma sessão técnica ativa pertencente à mesma tentativa.

### Revisão interna restrita

```text
GET  /api/v1/integrity-reviews?page=0&size=25
GET  /api/v1/integrity-reviews/{attemptId}
POST /api/v1/integrity-reviews/{attemptId}/decision
```

A fila e as evidências exigem `TEAM_MANAGER`, `PARTNER_MANAGER` ou `OPERATIONS_MANAGER` da empresa autenticada.

### Status neutro no relatório

```text
GET /api/v1/results/{attemptId}/integrity-status
```

O endpoint devolve HTTP 204 enquanto não existir um parecer decidido e explicitamente autorizado para compartilhamento. Quando autorizado, retorna apenas `decision` e `decidedAt`.

## Estados

### Sessão

| Estado | Significado |
|---|---|
| `ACTIVE` | Sessão autorizada e renovada por heartbeat |
| `CLOSED` | Navegador encerrou ou a avaliação terminou |
| `EXPIRED` | Heartbeat não foi recebido dentro do limite |

### Revisão

| Estado | Significado |
|---|---|
| `PENDING` | Evidências aguardam parecer humano |
| `DECIDED` | Parecer neutro foi registrado com justificativa e responsável |

Esses estados não representam aprovação, reprovação ou validade cognitiva da avaliação.

## Retenção e descarte

A rotina diária descarta sessões `CLOSED` e `EXPIRED` cujo encerramento ultrapassou o prazo configurado. A exclusão da sessão remove os eventos técnicos relacionados por chave estrangeira com `ON DELETE CASCADE`.

Quando a última sessão da tentativa é descartada:

- o parecer neutro é preservado;
- a justificativa e a trilha de auditoria são preservadas;
- `evidence_discarded_at` é preenchido;
- a tela deixa de exibir sessões e eventos;
- o descarte é auditado.

## Comunicação ao candidato

Antes de iniciar, a pessoa é informada de que:

- eventos técnicos da aplicação são registrados;
- o monitoramento não altera a pontuação;
- não existe eliminação automática;
- qualquer consequência depende de revisão humana;
- é possível solicitar revisão humana e exercer direitos sobre os dados.

Quando outra sessão está ativa, a interface apresenta somente orientação operacional para fechar a outra janela ou aguardar a expiração. Não são usados termos acusatórios.

As mensagens da barreira técnica estão disponíveis em português, inglês e espanhol, acompanhando o idioma selecionado pela pessoa participante.

## Manual das telas

A rota `/candidato/{token}` usa o processo **Experiência da pessoa participante** em `/manual#experiencia-participante`.

A rota `/monitoramento` usa o processo **Central operacional e revisão técnica** em `/manual#central-operacional-fila`, contendo:

- finalidade da tela;
- fluxo operacional;
- explicação dos campos;
- permissões necessárias;
- estados possíveis;
- motivos de bloqueio;
- exemplos;
- atalhos;
- link para o processo completo na Central de manuais.

## Configuração

```properties
praxis.integrity.heartbeat-seconds=30
praxis.integrity.session-timeout-seconds=90
praxis.integrity.hash-secret=
praxis.integrity.evidence-retention-days=180
praxis.integrity.evidence-retention-enabled=true
praxis.integrity.evidence-retention-cron=0 0 4 * * *
```

Quando `praxis.integrity.hash-secret` não é informado, o backend usa `praxis.jwt-secret` como chave do HMAC. Em produção, é recomendado configurar uma chave própria e rotacionável.

## Limites e próximas evoluções independentes

1. implementação Redis `SET NX EX` quando a infraestrutura disponibilizar Redis ao backend;
2. modelos estatísticos somente depois de existir base rotulada por revisão humana;
3. qualquer modelo futuro deve permanecer explicável, auditável e sem penalidade automática.

Nenhuma evolução futura deve ser implementada dentro das tabelas ou serviços de pontuação.
