# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## 1. Integração Gupy — contrato de entrada

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT1 | Alinhar os tipos de `company_id` e `document_id` ao contrato externo da Gupy sem perder a validação de pertencimento e a idempotência. | O endpoint `POST /test/candidate` aceita os tipos definidos pelo contrato oficial, valida os valores antes de iniciar o fluxo e preserva uma chave idempotente estável para chamadas repetidas. | ⬜ Pendente |
| INT2 | Validar `candidate_type` e `previous_result` no limite de entrada conforme os enums aceitos pela Gupy. | Valores fora do contrato são rejeitados com erro de validação; ausência e `null` são tratados conforme o contrato; nenhum valor artificial como `none` é enviado ou persistido como se fosse oficial. | ⬜ Pendente |

### INT1 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | campos `companyId` e `documentId` | Ambos são declarados como `String` e validados somente com `@NotBlank`, enquanto a documentação canônica do próprio repositório registra divergência em relação ao tipo `int64` do contrato da Gupy. Esses campos também participam da resolução da empresa e da chave idempotente do fluxo. | Definir uma representação compatível com o contrato externo, aplicar validação numérica e de faixa no DTO ou em tipos de domínio e adaptar a composição da chave idempotente sem permitir colisões ou mudança de identidade entre chamadas equivalentes. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | contrato de `POST /test/candidate` | O documento registra a incompatibilidade de tipos como bloqueador de homologação. | Atualizar o documento somente depois que o fluxo real de criação/reutilização da tentativa usar os tipos corrigidos de ponta a ponta. |

### INT2 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | campos `candidateType` e `previousResult` | Os campos são `String` sem restrição Jakarta Validation ou tipo enumerado. `candidate_type` apenas documenta `internal` e `external`; `previous_result` documenta `pass`, `fail` e `none`, embora o documento canônico registre que o contrato oficial aceita `fail` ou `null`. | Substituir texto livre por enum ou validador de contrato. Rejeitar valores desconhecidos e representar ausência como `null`, sem converter ausência em `none`. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | construtor auxiliar | O construtor auxiliar encaminha os valores recebidos sem normalização ou validação adicional, permitindo que chamadas internas mantenham valores incompatíveis. | Remover o caminho alternativo ou garantir que ele passe pela mesma validação e normalização aplicada à desserialização HTTP. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | seções “Compatibilidade com o contrato oficial” e “Bloqueadores para homologação” | O documento confirma que não há validação de enum no domínio e que `previous_result` diverge do contrato oficial. | Atualizar a compatibilidade somente após o endpoint rejeitar valores inválidos e o fluxo persistir/usar apenas valores previstos pelo contrato. |

## 2. Integração Gupy — contrato de resultado

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| API1 | Remover ou isolar campos não pertencentes ao schema oficial do resultado enviado à Gupy. | A resposta de `GET /test/result/{resultId}` e o payload do webhook contêm apenas campos aceitos pelo contrato externo, ou campos adicionais são incluídos exclusivamente após confirmação formal de compatibilidade e sem alterar o schema principal. | ⬜ Pendente |

### API1 — arquivos e métodos

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/TestResultResponse.java` | record `TestResultResponse` | O DTO descrito como “corpo exato esperado pela Gupy” inclui `reliabilityLevel` e `other_informations` no topo. O documento canônico informa que esses campos não pertencem ao schema oficial publicado e ainda dependem de confirmação do provedor. | Remover os campos do DTO externo ou criar um DTO interno separado e mapear explicitamente apenas o schema homologado para consulta e webhook. Caso a Gupy confirme extensões, registrar essa confirmação e manter serialização compatível. |
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | seção “Resultado produzido” | O documento registra que os campos extras exigem validação com a Gupy. | Atualizar o documento depois que o mesmo contrato corrigido for usado tanto no endpoint de consulta quanto na entrega assíncrona. |