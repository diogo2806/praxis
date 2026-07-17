# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-17 após auditoria da branch `main`.

Commit auditado: `d75ad0c133203efd4d749dd16aea3533b43c20b8`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Jornada do candidato

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| ERR10 | Carregar a etapa vinculada antes de gerar o redirecionamento automático da jornada | A conclusão de uma avaliação vinculada a uma jornada retorna a URL da próxima etapa sem acessar coleção JPA lazy fora de transação e sem produzir erro 500 | ⬜ Pendente |

### ERR10 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/journey/controller/CandidateJourneyRedirectAdvice.java` | `resolveJourneyRedirect(ServerHttpRequest)` | O advice consulta `AssessmentJourneyAttemptEntity` e depois percorre `journey.getSteps()`. A associação `steps` é lazy por padrão, o projeto usa `spring.jpa.open-in-view=false` e o acesso ocorre durante a escrita da resposta, fora do contexto transacional do repositório, podendo lançar `LazyInitializationException` e substituir a conclusão bem-sucedida por erro 500. | Não carregar a entidade agregada para procurar a etapa. Consultar diretamente uma projeção contendo `journeyAttemptId` e `stepId`, ou criar um método transacional em serviço que resolva esses identificadores enquanto a sessão está aberta. O advice deve apenas montar a URL com dados já materializados. |
| `backend/src/main/java/br/com/iforce/praxis/journey/persistence/repository/AssessmentJourneyAttemptRepository.java` | `findDistinctByEmpresaIdAndStepsCandidateAttemptIdOrderByCreatedAtDesc(...)` | O método filtra pela associação, mas não garante fetch da coleção `steps`; retornar a entidade não inicializa `getSteps()` com Open Session in View desativado. | Substituir por consulta que retorne diretamente a etapa correspondente e a jornada, sem depender da inicialização posterior da coleção. Novo método sugerido: `findJourneyRedirectTarget(String empresaId, String candidateAttemptId)`. |
| `backend/src/main/resources/application.properties` | `spring.jpa.open-in-view=false` | A configuração corretamente encerra o contexto de persistência antes da serialização e evidencia que o advice não pode navegar em associações lazy. | Manter a configuração desativada; corrigir o fluxo de consulta em vez de reativar Open Session in View. |
