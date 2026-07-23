# Campanhas de participação em lote

## Finalidade

O módulo organiza convites em lote para avaliações publicadas, com pré-validação do CSV, verificação de duplicidade e capacidade, agendamento do primeiro envio, até três lembretes condicionais, processamento por outbox, retries e acompanhamento operacional.

A campanha não altera o domínio do resultado. Pausar ou cancelar uma campanha controla somente mensagens e participações ainda não concluídas. Resultados já finalizados permanecem válidos e auditáveis.

## Formato do CSV

Cabeçalhos reconhecidos:

- `name`: nome da pessoa participante;
- `email`: e-mail normalizado e usado para idempotência;
- `consent`: obrigatório quando a campanha exige consentimento; aceita `true`, `1`, `sim`, `yes`, `y` ou `s`;
- `accommodation_multiplier`: multiplicador entre `1.00` e `3.00` para acomodação de tempo.

Também são aceitos aliases em português, como `nome`, `e-mail`, `consentimento` e `tempo_extra`. O separador pode ser vírgula ou ponto e vírgula. Campos com separador ou quebra de linha devem usar aspas conforme CSV.

A pré-validação limita o arquivo a 5.000 pessoas e retorna diagnóstico por linha sem criar tentativas e sem consumir créditos.

## Validações antes da confirmação

- avaliação publicada existente na empresa atual;
- cabeçalhos obrigatórios;
- nome e e-mail válidos;
- e-mail único dentro do lote;
- consentimento quando obrigatório;
- multiplicador de acomodação válido;
- ausência de aplicação anterior para a mesma empresa, pessoa, avaliação e ciclo, salvo reaproveitamento explícito;
- capacidade disponível no plano medido por créditos;
- assunto e corpo com variáveis permitidas;
- presença de `{{link}}` no corpo;
- até três lembretes com índices únicos.

Para planos `AVULSO` e `PROFISSIONAL`, a capacidade prévia corresponde ao saldo menos tentativas ativas. A criação real da tentativa continua usando `CreditService.assertCanStartNewAttempt`, evitando corrida entre campanhas simultâneas.

## Fluxo operacional

1. Informe avaliação, ciclo e configuração de consentimento.
2. Envie ou cole o CSV.
3. Execute `POST /preview-csv`.
4. Corrija todas as linhas inválidas e resolva o limite do plano.
5. Configure o modelo inicial e pré-visualize com dados fictícios.
6. Configure a data do primeiro envio, validade do link, retenção e até três lembretes.
7. Confirme a campanha com uma chave de idempotência.
8. A campanha grava participantes e mensagens na outbox, mas ainda não cria links quando o envio é futuro.
9. No horário devido, o worker cria ou reaproveita a tentativa pela idempotência existente, ajusta a validade do token, renderiza a mensagem e envia pelo `EmailDeliveryService`.
10. Falhas recebem até três tentativas com backoff. Depois disso, a mensagem entra em `DEAD_LETTER` e a pessoa aparece com falha operacional.
11. O painel sincroniza o estado da tentativa e apresenta o funil.
12. Depois do prazo de retenção e com campanha concluída ou cancelada, nome, e-mail, URL e conteúdo das mensagens são removidos, preservando hashes, identificadores e totais de auditoria.

## Variáveis de mensagem

Permitidas:

- `{{name}}`;
- `{{email}}`;
- `{{link}}`;
- `{{campaign}}`;
- `{{simulation}}`;
- `{{expiresAt}}`.

Variáveis desconhecidas são rejeitadas. Não há execução de expressão, código, HTML dinâmico ou acesso a propriedades arbitrárias. O corpo precisa conter `{{link}}`.

## Lembretes

Cada campanha pode possuir até três lembretes. A condição é avaliada no momento do processamento:

- `NOT_OPENED`: não existe evento de abertura e a participação ainda não foi concluída;
- `NOT_STARTED`: a tentativa permanece `NOT_STARTED`;
- `IN_PROGRESS`: a tentativa permanece `IN_PROGRESS`.

Quando a condição deixou de ser verdadeira, a mensagem recebe `SKIPPED`, evitando envio tardio ou duplicado.

## Idempotência

Existem três níveis:

1. `participation_campaigns.idempotency_key`: repetir a confirmação com a mesma chave devolve a campanha existente;
2. tentativa da pessoa: utiliza a chave já definida pelo `CompanyCandidateLinkService` para empresa, e-mail, avaliação e ciclo;
3. outbox: chave única por empresa, campanha, participante, tipo e índice do lembrete.

A concorrência de workers é controlada pela transição atômica de `PENDING` ou `FAILED` para `PROCESSING`.

## Estados

Campanha:

- `DRAFT`;
- `SCHEDULED`;
- `RUNNING`;
- `PAUSED`;
- `COMPLETED`;
- `CANCELLED`.

Participação operacional:

- `PENDING`;
- `LINK_CREATED`;
- `NOT_STARTED`;
- `IN_PROGRESS`;
- `COMPLETED`;
- `EXPIRED`;
- `CANCELLED`;
- `FAILED`.

Comunicação:

- `PENDING`;
- `QUEUED`;
- `DELIVERED`;
- `FAILED`;
- `BOUNCED`;
- `OPENED`;
- `SKIPPED`.

Outbox:

- `PENDING`;
- `PROCESSING`;
- `SENT`;
- `SKIPPED`;
- `FAILED`;
- `DEAD_LETTER`;
- `CANCELLED`.

## Eventos do provedor

Quando o provedor de e-mail disponibilizar webhook, os eventos podem ser registrados em:

`POST /api/v1/participation-campaigns/{campaignId}/participants/{participantId}/communication-event`

Valores aceitos: `DELIVERED`, `BOUNCED` e `OPENED`. O SMTP atual já informa se o provedor aceitou a mensagem. Bounce e abertura dependem do retorno do provedor.

## Cancelamento e pausa

Pausar impede que mensagens de outbox sejam processadas e mantém links e resultados existentes. Retomar reativa o processamento.

Cancelar:

- marca mensagens pendentes como `CANCELLED`;
- marca participações não concluídas como `CANCELLED`;
- mantém participações `COMPLETED` e seus resultados;
- não exclui tentativas nem histórico.

## API

Base: `/api/v1/participation-campaigns`

- `POST /preview-csv`;
- `POST /preview-message`;
- `POST /`;
- `GET /`;
- `GET /{campaignId}`;
- `POST /{campaignId}/pause`;
- `POST /{campaignId}/resume`;
- `POST /{campaignId}/cancel`;
- `POST /{campaignId}/participants/{participantId}/communication-event`;
- `GET /{campaignId}/export.csv`.

## Configuração

- `praxis.campaign.outbox-delay-ms`: intervalo do worker, padrão 60 segundos;
- `praxis.campaign.retention-cron`: cron de anonimização, padrão diário às 03:30.

## Auditoria e privacidade

As tabelas recebem colunas e gatilhos da auditoria universal por meio de `V1105__refresh_universal_table_auditing`. A tela mascara e-mails. O CSV operacional exportado também usa e-mail mascarado.

O hash do e-mail permanece após a retenção para preservar idempotência e totais sem manter o endereço pessoal em texto claro.
