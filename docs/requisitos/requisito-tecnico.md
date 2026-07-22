# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-22 após auditoria da branch `main`.

Commit auditado: `12247851c7a4a52e7cf9a278b3aaa491b2737ee2`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Persistência e concorrência

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA17 | Serializar a criação de etapas ramificadas por versão para impedir identificadores duplicados em requisições concorrentes. | Duas criações simultâneas na mesma versão geram nós distintos, vinculam cada alternativa ao respectivo destino e não resultam em violação de unicidade, perda de vínculo ou resposta genérica de erro. | ⬜ Pendente |

### DATA17 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationBranchNodeService.java` | `createBranchNode()`, `nextTurnIndex()` e `nextNodeId()` | O serviço carrega a versão sem bloqueio, calcula o próximo `turnIndex` pelo maior valor observado e deriva `nodeId` como `turno-N`. Requisições concorrentes podem ler o mesmo estado e tentar persistir o mesmo identificador; a restrição `uk_simulation_node` rejeita uma gravação depois de o fluxo já ter calculado e vinculado o destino em memória. | Executar a alocação e a persistência sob serialização por versão. A leitura usada pelo fluxo deve bloquear a linha da versão até o término da transação, ou a identificação deve ser alocada por mecanismo atômico equivalente. Manter a criação do nó e a atualização de `sourceOption.nextNodeId` na mesma transação. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/persistence/repository/SimulationVersionRepository.java` | novo método sugerido: `findForBranchCreationByEmpresaIdAndSimulationIdAndVersionNumber()` | O método usado atualmente possui apenas `@EntityGraph`; não aplica `PESSIMISTIC_WRITE`, versionamento otimista com retry seguro nem outro mecanismo que serialize duas criações na mesma versão. | Criar uma consulta específica para mutação com bloqueio de escrita sobre a versão e carregamento dos nós e alternativas necessários, preservando o isolamento por empresa. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/persistence/entity/SimulationNodeEntity.java` | restrição `uk_simulation_node` e campo `turnIndex` | Existe unicidade para `(simulation_version_id, node_id)`, mas ela atua apenas como última barreira e não evita a corrida de alocação. `turnIndex` também é derivado por leitura não serializada. | Manter a restrição como proteção de integridade e garantir que `nodeId` e `turnIndex` sejam definidos dentro da seção serializada. Caso a estratégia adotada use conflito de banco, tratar especificamente a colisão com nova alocação segura, sem converter outras violações em sucesso. |
