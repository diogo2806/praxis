# Resumo de Implementacao

## Integracao Frontend/Backend

- As telas operacionais usam a camada HTTP em `frontend/src/lib/api/praxis.ts`.
- Catalogos de tenant sao carregados por `GET /api/v1/tenant-config`.
- A criacao de rascunho usa `POST /api/v1/simulations/drafts`.
- Edicao de fluxo, validacao, governanca, monitoramento, LGPD, envio de link e candidato consomem endpoints reais do backend.
- Nao ha fallback local de catalogo, sessao demo ou server function de exemplo no frontend.

## Integracao ATS

- A plataforma registrada em producao e a Gupy.
- `ATSAdapter` permite extensao futura, mas somente adapters com integracao real devem ser registrados.
- Adapters que apenas simulavam suporte a outras plataformas foram removidos para nao anunciar integracao inexistente.

## Verificacao

- `npm run build` no frontend.
- `mvn -DskipTests package` no backend.
- `mvn test` ainda depende de corrigir a configuracao Flyway dos testes: o profile atual enxerga duas migrations `V13` ao incluir a pasta `db/migration/postgresql` junto com as migrations gerais.
