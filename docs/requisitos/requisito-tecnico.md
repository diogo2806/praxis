# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-17 após auditoria da branch `main`.

Commit auditado: `4a55c795b93699a328a0ffba601babd4414a6bc4`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

Não há pendências técnicas implementáveis comprovadas na HEAD auditada.

A correção de `ERR10` consulta diretamente uma projeção materializada com o identificador da tentativa da jornada e o identificador da etapa concluída. O advice de resposta não carrega entidades agregadas nem percorre associações JPA lazy, e `spring.jpa.open-in-view=false` permanece inalterado.
