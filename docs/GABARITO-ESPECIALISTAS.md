# Gabarito por especialistas do cargo

## Finalidade

O fluxo transforma a pontuação autoral de uma avaliação situacional em um gabarito revisado, justificável e versionado. A publicação de uma versão em rascunho fica bloqueada até que especialistas independentes avaliem todas as alternativas, o consenso mínimo seja atingido e um aprovador distinto conclua a rodada.

## Fluxo operacional

1. Abra uma versão em rascunho e acesse `/nova/gabarito?simulationId={id}&versionNumber={versao}`.
2. Crie a rodada informando quantidade mínima de especialistas e consenso mínimo.
3. Convide usuários como `EXPERT` e designe um usuário diferente como `APPROVER`.
4. Para cada cenário não final, registre tarefa representada, risco, competência, indicador comportamental e peso da evidência.
5. Cada especialista classifica todas as alternativas de 0 a 100, informa os escores por competência e registra uma justificativa comportamental.
6. O especialista conclui a própria revisão. Apenas revisões concluídas entram no cálculo agregado.
7. O sistema calcula média, dispersão e consenso por alternativa. Ausência de avaliações ou consenso abaixo do limite bloqueia a aprovação; dispersão relevante gera aviso de ambiguidade.
8. O aprovador revisa bloqueios e avisos, aprova a rodada e exporta o relatório técnico.
9. A publicação valida o fingerprint do conteúdo aprovado. Alterações em cenário, alternativa, destino, criticidade, justificativa autoral, pontuação ou matriz de evidências exigem nova rodada.

## Campos

- **Quantidade mínima de especialistas:** de 2 a 20 revisores concluídos por alternativa.
- **Consenso mínimo:** de 50% a 100%. O indicador é calculado por `1 - dispersão/100`, limitado entre 0 e 1.
- **Papel:** `EXPERT` classifica alternativas; `APPROVER` confirma a rodada.
- **Tarefa e risco:** demonstram a relação do cenário com incidentes críticos do trabalho.
- **Competência e indicador:** identificam qual comportamento a situação permite observar.
- **Peso:** importância daquela evidência na matriz técnica.
- **Eficácia:** julgamento do especialista sobre a adequação da alternativa.
- **Justificativa comportamental:** fundamento obrigatório para o julgamento.
- **Escores por competência:** leitura do especialista para cada dimensão declarada na versão.

## Permissões

- Autores e gestores da empresa criam rodadas, registram a matriz e convidam revisores.
- O perfil restrito `PARTNER_SPECIALIST` pode acessar os endpoints de revisão sem receber acesso administrativo, financeiro, a resultados ou integrações.
- Apenas usuário designado como `EXPERT` registra e conclui avaliações.
- Apenas usuário designado como `APPROVER` aprova a rodada.
- A publicação continua indisponível ao especialista parceiro pelo controle de acesso existente.

## Estados

- `DRAFT`: rodada criada, ainda sem distribuição completa.
- `IN_REVIEW`: especialistas ou aprovadores foram designados.
- `CHANGES_REQUESTED`: reservado para devolução formal em evolução posterior.
- `APPROVED`: rodada imutável, com fingerprint e aprovação registrados.

As designações passam por `INVITED`, `IN_PROGRESS`, `SUBMITTED` e `APPROVED`.

## Motivos de bloqueio

- versão diferente de rascunho;
- usuário não designado para o papel solicitado;
- especialista sem revisar todas as alternativas;
- quantidade de especialistas concluídos abaixo do mínimo;
- cenário não final sem tarefa, risco e indicador vinculados;
- alternativa sem quantidade mínima de avaliações;
- consenso abaixo do mínimo;
- tentativa de alterar uma rodada aprovada;
- conteúdo ou pontuação alterados após a aprovação;
- publicação sem rodada aprovada.

## Exemplos

Uma avaliação para atendimento possui três cenários e quatro alternativas em cada cenário. A empresa exige três especialistas e 75% de consenso. Os três revisores avaliam as doze alternativas e justificam cada julgamento. Uma alternativa obtém dispersão de 32 pontos, portanto consenso de 68%; a aprovação fica bloqueada até que o texto, o indicador ou a pontuação sejam revistos em nova rodada.

Quando uma versão já publicada precisa mudar, o usuário clona a versão para um novo rascunho. A versão publicada e seus resultados permanecem imutáveis, enquanto o novo rascunho passa por outra rodada de especialistas.

## Atalhos e processo completo

- Use **Validador estrutural** para corrigir caminhos, alternativas e pesos antes da aprovação científica.
- Use **Governança** para consultar auditoria e publicar depois da aprovação.
- Use **Relatório técnico** para exportar a matriz cenário × alternativa × competência × indicador × peso, os indicadores de consenso e o histórico.
- Processo completo de autoria: `docs/fluxo-autoria-avaliacoes.md`.
- Plano científico: `docs/VALIDACAO-CIENTIFICA.md`.

## Contratos HTTP

Base: `/api/v1/simulations/{simulationId}/versions/{versionNumber}/answer-key-review`

- `POST /rounds`
- `GET /rounds/latest`
- `GET /rounds/{roundId}`
- `POST /rounds/{roundId}/assignments`
- `PUT /rounds/{roundId}/evidence/{nodeId}`
- `PUT /rounds/{roundId}/options/{nodeId}/{optionId}/reviews/me`
- `POST /rounds/{roundId}/submit`
- `POST /rounds/{roundId}/approve`
- `GET /rounds/{roundId}/report.csv`
