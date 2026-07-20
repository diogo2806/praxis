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

### Separação da pontuação

As informações ficam exclusivamente em:

- `candidate_integrity_sessions`;
- `candidate_integrity_events`.

Não foram adicionados campos de integridade em:

- `candidate_attempts`;
- respostas da tentativa;
- itens de resultado;
- relatório empresarial;
- payload enviado à Gupy.

Nenhum serviço de pontuação consulta as tabelas de integridade.

## Dados e proteção

| Campo | Finalidade | Proteção |
|---|---|---|
| `client_session_id` | Retomar a mesma sessão do navegador | Valor aleatório, sem dado pessoal |
| `ip_hash` | Correlacionar tecnicamente acessos quando necessário | HMAC-SHA-256; IP bruto não é persistido |
| `user_agent_category` | Distinguir categoria geral do dispositivo | Somente desktop, tablet, mobile ou desconhecido |
| `input_mode` | Interpretar corretamente uso de teclado, toque ou ponteiro | Não representa deficiência e não gera pontuação |
| `visibility_state` | Registrar visibilidade da aba | Somente `VISIBLE` ou `HIDDEN` |
| `event_type` | Construir linha do tempo técnica | Lista fechada de eventos permitidos |
| `detail` | Identificar categoria de mídia | Somente `IMAGE`, `AUDIO` ou `OTHER` |

O cliente não pode enviar pontos, gabarito, classificação, score de risco, justificativa de revisão ou conteúdo textual da resposta por esses endpoints.

## Endpoints

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

## Estados da sessão

| Estado | Significado |
|---|---|
| `ACTIVE` | Sessão autorizada e renovada por heartbeat |
| `CLOSED` | Navegador encerrou ou a avaliação terminou |
| `EXPIRED` | Heartbeat não foi recebido dentro do limite |

Esses estados são internos e não representam aprovação, reprovação ou validade cognitiva da avaliação.

## Comunicação ao candidato

Antes de iniciar, a pessoa é informada de que:

- eventos técnicos da aplicação são registrados;
- o monitoramento não altera a pontuação;
- não existe eliminação automática;
- qualquer consequência depende de revisão humana;
- é possível solicitar revisão humana e exercer direitos sobre os dados.

Quando outra sessão está ativa, a interface apresenta somente orientação operacional para fechar a outra janela ou aguardar a expiração. Não são usados termos acusatórios.

As mensagens da barreira técnica estão disponíveis em português, inglês e espanhol, acompanhando o idioma selecionado pela pessoa participante.

## Manual da tela

A rota `/candidato/{token}` já utiliza o ícone global de manual. O processo completo correspondente é **Experiência da pessoa participante**, contendo:

- finalidade da tela;
- fluxo operacional;
- explicação dos campos;
- permissões necessárias;
- estados possíveis;
- motivos de bloqueio;
- exemplos;
- atalhos;
- link para a central completa em `/manual#experiencia-participante`.

## Configuração

```properties
praxis.integrity.heartbeat-seconds=30
praxis.integrity.session-timeout-seconds=90
praxis.integrity.hash-secret=
```

Quando `praxis.integrity.hash-secret` não é informado, o backend usa `praxis.jwt-secret` como chave do HMAC. Em produção, é recomendado configurar uma chave própria e rotacionável.

## Limites desta entrega

Esta entrega cria a fundação do fluxo do candidato. Permanecem como próximas fases independentes:

1. painel interno de revisão com permissões específicas;
2. entidades de alerta, evidência, revisão e trilha de acesso individual;
3. regras determinísticas retroativas sobre dados existentes;
4. status neutro revisado para empresa e candidato;
5. retenção e descarte automatizados das evidências;
6. implementação Redis `SET NX EX` quando a infraestrutura disponibilizar Redis ao backend;
7. modelos estatísticos somente depois de existir base rotulada por revisão humana.

Nenhuma dessas fases futuras deve ser implementada dentro das tabelas ou serviços de pontuação.
