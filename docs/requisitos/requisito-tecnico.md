# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `7439c3747cf9e3870dbb3119d30b50d6afb85749`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox.
- Áreas revisadas nesta rodada: criação idempotente de tentativas Gupy, execução pública do candidato, montagem de `TestResult`, callbacks, tokens públicos, eventos de engajamento, entrega Gupy, entrega `CUSTOM_API`, retry e DLQ.

## 1. Integração Gupy e destinos de webhook

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT11 | Impedir o envio de eventos proprietários de engajamento para o `result_webhook_url` reservado ao `TestResult` da Gupy. | O endereço recebido em `result_webhook_url` recebe exclusivamente `POST` com `TestResult`; `ATTEMPT_STARTED`, `ATTEMPT_ABANDONED` e outros eventos internos são publicados somente para uma integração genérica explicitamente configurada, sem reutilizar o destino Gupy. | ⬜ Pendente |
| INT12 | Não converter tentativas abandonadas ou expiradas em resultados Gupy concluídos com pontuações artificiais. | `ABANDONED` e `EXPIRED` possuem representação contratual que não se confunde com conclusão válida; resultados por competência só são enviados quando o cálculo foi concluído, e uma tentativa não finalizada nunca aparece como `done` com notas zero. | ⬜ Pendente |
| INT13 | Tornar categoria e nível do catálogo Gupy derivados de dados reais ou omiti-los quando não configurados. | Cada teste publicado anuncia metadados coerentes com sua configuração; não há valores fixos `Situational Judgment` e `advanced` aplicados indistintamente a todas as avaliações sem fonte no domínio. | ⬜ Pendente |
| INT14 | Alinhar a URL de resultado da pessoa candidata ao conteúdo e à duração prometidos no contrato externo. | O campo enviado como página de resultado abre conteúdo que corresponda à finalidade declarada, preservando a política de privacidade, e continua acessível pelo período necessário ou dispõe de mecanismo seguro de renovação; um JWT padrão de sete dias não torna resultados históricos permanentemente inacessíveis. | ⬜ Pendente |

### INT11 — separação entre webhook de resultado Gupy e eventos internos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `publishAttemptEngagementEvent()` | Lê `CandidateAttemptEntity.resultWebhookUrl`, grava esse endereço em `webhookUrl` e publica `ATTEMPT_STARTED` ou `ATTEMPT_ABANDONED` com payload proprietário. | Remover o `resultWebhookUrl` desse fluxo; resolver um destino genérico próprio por empresa e publicar o evento somente quando essa integração estiver ativa. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `processAttemptEngagementEvent()` | Valida o mesmo endereço e envia `eventPayload` arbitrário por `ResultWebhookClient.postPayload()`. | Rotear eventos de engajamento exclusivamente ao contrato genérico e reservar `postResult()`/destino Gupy para `TestResult`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java` | `resultWebhookUrl` | O campo de destino Gupy é usado também como destino de eventos internos. | Preservar o campo somente para entrega de resultado ou introduzir campo/integração distinta quando houver necessidade real de eventos proprietários. |

### INT12 — estados terminais sem resultado válido

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `toGupyStatus()` | Mapeia `COMPLETED`, `ABANDONED` e `EXPIRED` para `done`. | Definir representação contratual específica para abandono/expiração ou bloquear a emissão de `TestResult` até haver conclusão real; não declarar `done` para fluxos não concluídos. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `toResponse()` | Serializa todos os itens persistidos independentemente do estado da tentativa. | Incluir itens de resultado somente quando a pontuação final tiver sido calculada e validada. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | criação de `CandidateAttempt` | Inicializa competências com pontuação zero antes de qualquer resposta. | Manter o estado interno inicial, mas impedir que esses zeros provisórios atravessem o contrato externo como resultado legítimo. |

### INT13 — metadados reais do catálogo Gupy

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/SimulationCatalogService.java` | montagem do catálogo externo | Anuncia categoria `Situational Judgment` e nível `advanced` de forma fixa para todas as avaliações. | Derivar os campos de configuração persistida da simulação ou omiti-los quando o contrato permitir e o domínio não possuir essa informação. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/model/PublishedSimulation.java` | categoria/nível | Não há fonte comprovada para distinguir os metadados anunciados por avaliação. | Adicionar campos de domínio e persistência somente se os metadados forem obrigatórios; caso contrário, não fabricar valores. |

### INT14 — página de resultado e validade do acesso

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | `candidateResultPageUrl()` | Gera a URL externa com o mesmo JWT temporário usado no fluxo de tentativa. | Usar credencial específica de resultado com política de validade compatível ou implementar renovação segura sem expor identificadores crus. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | `attemptLinkTtlHours` | O valor padrão de 168 horas também limita a página de resultado enviada à Gupy. | Separar TTL de execução e TTL de consulta de resultado, documentando e aplicando a política adequada a cada finalidade. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateResultController.java` | resposta pública de resultado | A página identificada como resultado retorna apenas metadados operacionais e URL de retorno, sem um conteúdo de resultado definido. | Definir explicitamente o contrato da página: apresentar informação permitida à pessoa candidata ou deixar de anunciá-la externamente como página de resultado quando a política do produto proibir essa exibição. |

## 2. Outbox, retry e confirmação de entrega

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ASYNC10 | Tornar a entrega `CUSTOM_API` confirmável e retentável sem repetir uma entrega Gupy já concluída. | Falha no webhook genérico não é convertida em `SENT`; cada destino possui estado, tentativas, erro e confirmação independentes, de modo que a Gupy não seja reenviada quando apenas o `CUSTOM_API` falhar e o evento genérico não seja perdido. | ⬜ Pendente |

### ASYNC10 — fan-out com estado independente por destino

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `processResultReadyEvent()` | Entrega primeiro à Gupy e depois chama `deliverGenericWebhookBestEffort()`. | Criar entregas independentes por destino, com idempotência e status próprios, ou eventos separados para Gupy e `CUSTOM_API`. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/service/OutboxProcessor.java` | `deliverGenericWebhookBestEffort()` e `deliverAndFinalize()` | Captura e ignora qualquer falha genérica; o método superior termina normalmente e marca o evento como `SENT`. | Não confirmar a entrega genérica em caso de falha; persistir retry/DLQ do destino sem provocar reenvio do destino já confirmado. |
| `backend/src/main/java/br/com/iforce/praxis/shared/integration/service/GenericWebhookDeliveryService.java` | `deliverResultReady()` | A entrega é tratada como melhor esforço e não possui fila ou retentativa independente comprovada. | Propagar resultado de entrega ou gravar uma entrega durável própria com tentativas, erro final e reprocessamento. |
| `backend/src/main/java/br/com/iforce/praxis/shared/outbox/persistence/entity/OutboxEventEntity.java` | status único do evento | Um único `SENT/RETRYING/DLQ` representa múltiplos destinos com resultados diferentes. | Introduzir granularidade por destino ou separar os eventos antes do processamento. |

## 3. API pública do candidato e proteção do fluxo

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| SEC11 | Remover da API pública os identificadores que revelam a topologia futura da avaliação. | A pessoa candidata recebe somente a etapa atual e identificadores opacos necessários para responder; alternativas e timeout não revelam IDs de próximas etapas, convergências ou regras de navegação antes da escolha ser processada no servidor. | ⬜ Pendente |
| SEC12 | Restringir callbacks externos e tornar o retorno pós-conclusão coerente com a origem Gupy. | `callback_url` passa por política explícita de origem/provedor, não aceita destinos arbitrários apenas por serem HTTP(S), e o fluxo de conclusão registra de forma confiável a entrega ou o redirecionamento esperado sem depender silenciosamente de 1,2 segundo de JavaScript. | ⬜ Pendente |
| SEC13 | Remover o bypass que aceita `attemptId` cru quando o JWT público é inválido ou expirou. | Todos os endpoints públicos exigem token assinado válido ou uma credencial de migração explicitamente limitada e revogável; conhecer `att_...` não permite acessar a tentativa nem contornar o TTL. | ⬜ Pendente |

### SEC11 — topologia interna exposta ao navegador

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/candidate/dto/RespostaResponse.java` | `proximaEtapaId` | Cada alternativa informa antecipadamente o identificador da próxima etapa. | Remover o campo do DTO público; a transição deve ser resolvida no servidor após a resposta. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/dto/EtapaAtualResponse.java` | `proximaEtapaTempoEsgotadoId` | A resposta expõe o destino técnico do timeout. | Remover o campo do contrato público e manter a regra apenas no domínio/backend. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptMapper.java` | `toEtapaAtualResponse()` | Copia IDs internos de `ScenarioOption.nextNodeId()` e `ScenarioNode.timeoutNextNodeId()` para os DTOs públicos. | Mapear somente IDs opacos da etapa/resposta atual e conteúdo visual necessário. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateAttemptController.java` | contrato/documentação do endpoint | Declara não expor identificadores internos ou regras técnicas, mas a resposta contém os destinos da árvore. | Fazer a implementação corresponder ao contrato e remover a afirmação otimista enquanto a exposição existir. |

### SEC12 — callback externo e conclusão dependente do navegador

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `validateCallbackUrl()` | Aceita qualquer host com esquema HTTP ou HTTPS. | Validar callback conforme provedor e ambiente, com lista de hosts/padrões autorizados e proteção contra redirecionamento externo indevido. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `redirectUrl()` | Apenas devolve a URL na resposta quando o status é `COMPLETED`; não registra confirmação do retorno. | Definir um mecanismo confiável e observável para o handoff de conclusão, preservando a semântica exigida pelo provedor. |
| `frontend/src/routes/candidato/$attemptToken.tsx` | redirecionamento após conclusão | Aguarda no navegador e chama `window.location.assign()`, portanto o retorno desaparece se a aba fechar, JavaScript falhar ou a conexão cair. | Usar o frontend apenas como experiência complementar; o backend deve manter evidência e estratégia de recuperação coerentes para o callback. |

### SEC13 — fallback de token público para identificador cru

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `resolveAttemptId()` e `isLegacyAttemptId()` | Após falha ao validar o JWT, aceita qualquer texto no padrão `att_[A-Za-z0-9]{16,64}` como identificador autorizado. | Eliminar o fallback ou restringi-lo a migração com prazo, segredo adicional e registro explícito; JWT inválido/expirado deve resultar em `401`. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/security/CandidateAttemptTokenFilter.java` | resolução da empresa pelo token | Também consulta a tentativa usando o identificador cru quando o token assinado não é válido. | Exigir claims verificadas antes de definir o contexto da empresa e não usar `attemptId` público como credencial. |

## 4. Idempotência e consistência da tentativa

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| API2 | Impedir que uma repetição idempotente com conteúdo divergente transforme a tentativa em objeto híbrido. | Uma chave já existente preserva integralmente a primeira requisição ou retorna `409` quando campos relevantes divergem; destinos, identidade, acomodação e demais dados não são atualizados seletivamente. | ⬜ Pendente |

### API2 — repetição idempotente com mutação parcial

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/CandidateAttemptService.java` | `createOrReuse()` | Ao encontrar a mesma chave, sobrescreve `gupyJobId`, `callbackUrl` e `resultWebhookUrl`, mas preserva nome, e-mail e acomodação da criação original. | Comparar uma impressão canônica dos campos relevantes; retornar a tentativa original somente quando o conteúdo for equivalente ou responder `409` em divergência. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/persistence/entity/CandidateAttemptEntity.java` | dados da requisição Gupy | Não há evidência de snapshot/hash completo da solicitação que permita validar repetição equivalente. | Persistir os campos necessários à comparação ou um hash canônico versionado da requisição. |

## 5. Compatibilidade declarada e documentação interna

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT15 | Substituir a declaração de compatibilidade absoluta com a Gupy por uma matriz explícita de divergências e decisões contratuais. | A documentação interna distingue schema, exemplos contraditórios, extensões exigidas pelo Praxis e itens ainda não validados; não afirma prontidão para homologação enquanto `INT11`, `INT12`, `ASYNC10`, `SEC11` e demais bloqueadores permanecerem. | ⬜ Pendente |

### INT15 — contrato oficial contraditório e prontidão superestimada

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | status de compatibilidade e bloqueadores | Trata o contrato como alinhado e concentra o bloqueio restante em validação real, apesar das divergências funcionais e contratuais presentes na implementação. | Registrar cada divergência com decisão, risco e evidência; marcar homologação como bloqueada até as correções aplicáveis e validação com o provedor. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | obrigatoriedade de `company_id` e `test_id` | O Praxis exige campos adicionais ao conjunto mínimo apresentado no schema de referência. | Documentar formalmente a extensão ou adaptar o endpoint após validação do contrato efetivamente aceito pela Gupy. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | `previous_result` | Rejeita a string `"null"` e aceita JSON `null`, enquanto exemplos externos podem usar representação textual contraditória. | Não declarar compatibilidade absoluta antes de confirmar o comportamento real; registrar a interpretação adotada e tratar compatibilidade somente se necessária e segura. |

## Ordem recomendada

1. `INT11` — não enviar eventos proprietários ao webhook de resultado Gupy.
2. `ASYNC10` — impedir perda silenciosa do `CUSTOM_API` e separar confirmação por destino.
3. `INT12` — não publicar abandono/expiração como conclusão com nota zero.
4. `SEC11` — remover topologia interna da API pública.
5. `SEC13` — eliminar o bypass do JWT por `attemptId` cru.
6. `API2` — tornar a idempotência integral e consistente.
7. `SEC12` — restringir callback e tornar o handoff recuperável.
8. `INT14` — corrigir semântica e validade da página de resultado.
9. `INT13` — publicar metadados reais do catálogo.
10. `INT15` — corrigir a declaração interna de compatibilidade e homologação.
