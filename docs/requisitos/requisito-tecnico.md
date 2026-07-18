# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-17 após auditoria da branch `main`.

Commit auditado: `e61095d9d528c3146facd4f03c652b03c9122327`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

Não há pendências técnicas implementáveis comprovadas na HEAD auditada.

A correção de `ERR10` consulta diretamente uma projeção materializada com o identificador da tentativa da jornada e o identificador da etapa concluída. O advice de resposta não carrega entidades agregadas nem percorre associações JPA lazy, e `spring.jpa.open-in-view=false` permanece inalterado.

A fase 2 da homologação Gupy adiciona um centro administrativo de prontidão que consulta as mesmas fontes persistidas usadas pelos fluxos reais de token, catálogo publicado, tentativas, atividade autenticada e outbox. As etapas que dependem de vaga real e da aprovação da Gupy permanecem explicitamente externas e não são convertidas em sucesso interno.