# Requisitos técnicos — praxis

Status: atualizado em 2026-07-22 após implementação da pendência DATA17.

Base auditada: `9343ff088bc7bef212de05e466625df1d544bc54`.

Este arquivo registra requisitos técnicos implementáveis e comprovados no sistema. Não inclui CI/CD, testes manuais, QA, métricas observacionais, publicação ou marketing.

## 1. Persistência e concorrência

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| DATA17 | Serializar a criação de etapas ramificadas por versão para impedir identificadores duplicados em requisições concorrentes. | Duas criações simultâneas na mesma versão geram nós distintos, vinculam cada alternativa ao respectivo destino e não resultam em violação de unicidade, perda de vínculo ou resposta genérica de erro. | ✅ Concluído |

### DATA17 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como ficou | Evidência de conclusão |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationBranchNodeService.java` | `createBranchNode()`, `nextTurnIndex()` e `nextNodeId()` | A versão é obtida por uma consulta exclusiva para criação de ramificações, com bloqueio pessimista mantido durante toda a transação. A alocação de `turnIndex` e `nodeId`, a inclusão do novo nó e a atualização de `sourceOption.nextNodeId` permanecem na mesma transação. | A segunda requisição concorrente só calcula os identificadores depois do commit da primeira e enxerga o nó recém-criado. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/persistence/repository/SimulationVersionRepository.java` | `findForBranchCreationByEmpresaIdAndSimulationIdAndVersionNumber()` | Foi adicionada uma consulta com `PESSIMISTIC_WRITE` que bloqueia a linha da versão e preserva o isolamento por empresa, simulação e número da versão. Os nós e alternativas são carregados dentro da mesma transação após a aquisição do bloqueio. | O bloqueio é exercitado por duas transações reais contra PostgreSQL no teste de concorrência. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/persistence/entity/SimulationNodeEntity.java` | restrição `uk_simulation_node` e campo `turnIndex` | A restrição de unicidade foi mantida como última barreira de integridade. `nodeId` e `turnIndex` agora são calculados somente dentro da seção serializada. | `SimulationBranchNodeConcurrencyTest` confirma a criação simultânea de `turno-2` e `turno-3`, sem colisão e com os dois vínculos persistidos. |
