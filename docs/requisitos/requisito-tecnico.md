# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-17 após implementação de `ERR10`.

Não há pendências técnicas implementáveis registradas neste documento.

A correção de `ERR10` passou a consultar diretamente uma projeção materializada com o identificador da tentativa da jornada e o identificador da etapa concluída. O advice de resposta não carrega entidades agregadas nem percorre associações JPA lazy, e `spring.jpa.open-in-view=false` permanece inalterado.
