# Requisitos técnicos pendentes — praxis

Status: atualizado em 2026-07-18 após auditoria da branch `main`.

Commit auditado: `9396ce58f452b0f3a1b5bb0b7a6faf63d28a9da9`.

Este arquivo contém somente pendências técnicas implementáveis e comprovadas no sistema. Não inclui CI/CD, testes, QA, métricas observacionais, publicação ou marketing.

Não há pendências técnicas implementáveis comprovadas na HEAD auditada.

A auditoria revalidou os fluxos de criação, publicação e execução de avaliações, persistência de tentativas, resultados, integrações ATS, outbox, autenticação, autorização e isolamento por empresa. As correções históricas registradas em `docs/implementados/requisitos-implementados.md` continuam alcançadas pelos fluxos reais e não foram reabertas.

O módulo de parceiros mantém clientes, especialistas, tokens e catálogos vinculados à empresa proprietária. Tokens de cliente são armazenados somente por hash, identificam o cliente externo no contexto autenticado e são revogados quando o cliente é desativado. O catálogo liberado é validado nos fluxos externos antes da criação de tentativas.

O perfil de especialista passa pela autorização do Spring Security e por filtro restritivo adicional. O acesso permanece limitado à própria conta, mídia, leitura da configuração necessária e autoria/revisão de avaliações, sem publicação, clientes, integrações, resultados, cobrança, monitoramento, calibração ou talent match.

O salvamento automático do editor preserva rascunhos locais sem tratá-los como fonte persistida do sistema. A confirmação de salvamento remoto depende da resposta do backend, falhas mantêm o rascunho e versões publicadas continuam protegidas contra edição direta.

O monitoramento consulta estados persistidos de tentativas, integrações, notificações e entregas. A atualização periódica é somente leitura, não promove integrações, não confirma entregas e não executa reprocessamento automático.

O registro de atividades HTTP é executado após a cadeia da requisição, usa o padrão de rota resolvido, não inclui corpo, query string, cookies ou credenciais e devolve um identificador de requisição sanitizado. Esse recurso é observacional e não foi convertido em requisito de métricas ou coleta de evidências.

Os manuais contextuais adicionados às telas são conteúdo de apoio e navegação. Eles não substituem validações, permissões ou regras implementadas no backend e não introduzem uma fonte concorrente para o estado operacional.