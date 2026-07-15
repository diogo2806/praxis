# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-15 após auditoria da branch main.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

## Contexto da auditoria

- Commit auditado da branch principal: `2cbe682a1c82c2b11fa7b08523b28805bc790e33`.
- Finalidade identificada: plataforma de avaliações situacionais para recrutamento, com critérios explícitos, score determinístico, trilha auditável e integração com ATS.
- Stack principal: Java 21, Spring Boot 3.5, Spring Security, JPA, PostgreSQL/Flyway, React 19, TanStack Start/Router e TypeScript.
- Arquitetura predominante: frontend React consumindo API Spring Boot, persistência PostgreSQL, autenticação JWT nas rotas internas, Bearer token nas integrações e entrega assíncrona por outbox.
- Fluxos revisados: catálogo Gupy, criação idempotente de tentativas, execução pública do candidato, cálculo e entrega de resultado, callback, página pública de resultado, outbox por destino e documentação de compatibilidade.

## 1. Contrato e catálogo da integração Gupy

| ID | Tarefa técnica | Critério de conclusão | Status |
|---|---|---|---|
| INT13 | Tornar categoria e nível do catálogo Gupy derivados de dados reais ou omiti-los quando não configurados. | Cada teste publicado anuncia metadados coerentes com sua configuração; não há valores fixos aplicados indistintamente a todas as avaliações sem fonte no domínio. | ⬜ Pendente |
| INT14 | Alinhar a URL de resultado da pessoa candidata ao conteúdo e à duração prometidos no contrato externo. | A URL enviada como página de resultado abre conteúdo coerente com a finalidade declarada e usa credencial com validade própria ou renovação segura, sem reutilizar o TTL curto de execução. | ⬜ Pendente |
| INT15 | Manter uma matriz explícita de compatibilidade Gupy sem declarar homologação antes da validação contratual. | A documentação distingue schema confirmado, extensões do Praxis, exemplos contraditórios e decisões ainda pendentes, sem afirmar compatibilidade absoluta ou homologação concluída. | ⬜ Pendente |

### INT13 — metadados reais do catálogo Gupy

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestCatalogMapper.java` | montagem de categoria e nível | O contrato externo continua preenchendo categoria e nível com valores fixos sem fonte comprovada na simulação publicada. | Derivar os valores de configuração persistida da avaliação ou omiti-los quando o contrato permitir. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/model/PublishedSimulation.java` | metadados de catálogo | O domínio publicado não possui campos que distingam categoria e nível por avaliação. | Adicionar campos somente quando forem realmente configuráveis e obrigatórios; caso contrário, não fabricar valores no mapper externo. |
| `backend/src/main/java/br/com/iforce/praxis/simulation/service/SimulationMapperService.java` | `toPublishedSimulation()` | O mapeamento da versão publicada não fornece metadados de categoria ou nível ao catálogo Gupy. | Propagar os campos persistidos caso sejam introduzidos ou manter o contrato externo sem esses valores artificiais. |

### INT14 — página de resultado e validade do acesso

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `backend/src/main/java/br/com/iforce/praxis/gupy/service/GupyTestResultMapper.java` | URL pública de resultado | A URL externa continua vinculada ao token da tentativa usado no fluxo de execução. | Emitir credencial específica de consulta de resultado com escopo e validade próprios ou implementar renovação segura. |
| `backend/src/main/java/br/com/iforce/praxis/config/PraxisProperties.java` | TTL do link da tentativa | A mesma política temporal ainda governa execução e consulta histórica do resultado. | Separar a configuração de TTL de execução da configuração de consulta de resultado. |
| `backend/src/main/java/br/com/iforce/praxis/candidate/controller/CandidateResultController.java` | conteúdo da página pública | O endpoint identificado como página de resultado não possui um contrato de conteúdo que corresponda claramente ao que é anunciado ao provedor. | Exibir somente o resultado permitido pela política do produto ou deixar de anunciar esse endpoint como página de resultado. |

### INT15 — matriz de compatibilidade e prontidão Gupy

| Caminho completo | Método/campo/contrato | Como está | O que fazer |
|---|---|---|---|
| `docs/INTEGRACAO-GUPY-PROVEDOR.md` | status de compatibilidade | A documentação precisa permanecer alinhada às divergências funcionais ainda abertas, especialmente catálogo e página de resultado. | Manter tabela por campo e endpoint com situação confirmada, decisão adotada, risco e evidência, sem declarar homologação concluída. |
| `backend/src/main/java/br/com/iforce/praxis/gupy/dto/CreateCandidateRequest.java` | campos adicionais e interpretações | O Praxis exige dados adicionais e adota interpretações específicas para valores contraditórios nos exemplos externos. | Documentar formalmente cada extensão e interpretação até confirmação do contrato efetivamente aceito pelo provedor. |
| `README.md` | apresentação da integração | O README já evita afirmar homologação, mas depende do documento técnico para o estado detalhado. | Preservar essa ressalva e garantir que o documento técnico não contradiga a implementação real. |

## Ordem recomendada

1. `INT14` — corrigir semântica e validade da página de resultado.
2. `INT13` — publicar somente metadados de catálogo sustentados pelo domínio.
3. `INT15` — consolidar a matriz de compatibilidade com as decisões contratuais reais.
