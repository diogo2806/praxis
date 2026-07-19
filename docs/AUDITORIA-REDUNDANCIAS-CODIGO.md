# Auditoria de redundâncias de código

> Revisão técnica realizada sobre backend e frontend do Práxis em 18/07/2026.
> Este documento diferencia duplicação removida, semelhança intencional de contratos e dívida que exige migração gradual.

## Resumo

| Área | Situação encontrada | Ação nesta entrega |
| --- | --- | --- |
| SHA-256 | Implementações repetidas em autenticação, tokens, reset, idempotência e IDs públicos | Centralizado em `shared/security/Sha256` |
| Tokens aleatórios | `SecureRandom` + Base64 URL-safe repetidos | Centralizado em `shared/security/SecureTokens` |
| Cliente HTTP frontend | Autenticação, erro, 204 e parsing repetidos em vários módulos | Centralizado em `frontend/src/lib/api/http.ts` |
| Operações/notificações | Dois módulos representavam as mesmas notificações e repetiam HTTP | `operations.ts` virou fachada; `notifications.ts` é a fonte |
| Resultado Gupy | Payload montado duas vezes para domínio e entidade JPA | Normalizado por uma visão interna única |
| Links de candidato | Cliente específico não anexava sessão como os demais módulos protegidos | Corrigido pelo cliente HTTP central |

## Redundâncias removidas no backend

### Hash SHA-256

Antes, cada serviço escolhia separadamente charset, formato hexadecimal ou Base64 URL-safe e tratamento de indisponibilidade do algoritmo.

Foram migrados:

- `IntegrationAuthService`;
- `IntegrationTokenAdminService`;
- `PublicApiTokenService`;
- `TokenLookupHasher`;
- `IdempotencyKeyHasher`;
- `PasswordResetService`;
- `PublicCandidateFlowSecurity`.

As representações externas e persistidas foram preservadas. Testes com vetores fixos impedem mudança acidental do formato.

### Tokens criptográficos

Foram migrados para `SecureTokens`:

- tokens de integração ATS;
- token da API pública;
- reset de senha;
- convites da equipe.

Prefixos e quantidades de bytes continuam definidos por cada contrato.

### Resultado Gupy

`GupyTestResultMapper` possuía dois blocos praticamente iguais, um para `CandidateAttempt` e outro para `CandidateAttemptEntity`. As duas entradas agora são convertidas em uma visão interna e passam por uma única montagem do contrato externo.

## Redundâncias removidas no frontend

`apiRequest` passou a concentrar:

- composição da URL base;
- inclusão opcional do Bearer token;
- `Accept` e `Content-Type`;
- leitura padronizada de `mensagem`, `message`, `error` e `detail`;
- tratamento de `204 No Content`;
- resposta JSON ou texto;
- criação de `PraxisApiError`.

Módulos migrados:

- autenticação;
- dashboard;
- homologação Gupy;
- links de candidato;
- notificações;
- operações;
- parceiros;
- administração de empresas;
- cobrança do cliente;
- monitoramento;
- arquivamento de avaliação;
- duplicação de avaliação;
- resultado público da pessoa candidata;
- direitos do titular.

Rotas públicas declaram explicitamente `authenticated: false`.

## Semelhanças que não devem ser fundidas diretamente

### Gupy e Recrutei

Controllers e mappers têm estrutura parecida, mas os contratos externos divergem em:

- nomes e tipos de campos;
- paginação;
- autenticação e contexto da empresa;
- estados publicados;
- links de resultado;
- regras para tentativa abandonada ou expirada.

A ação correta não é criar um controller genérico. A evolução recomendada é introduzir comandos internos independentes de provedor e manter adaptadores públicos separados.

### Políticas de URL

`GupyCallbackUrlPolicy` e `GupyOutboundUrlValidator` parecem semelhantes, mas protegem fluxos diferentes:

- callback de navegação com allowlist de domínio;
- chamada servidor-servidor com defesa contra SSRF e resolução DNS.

Fundir as duas políticas poderia enfraquecer segurança. Apenas utilitários de baixo nível podem ser compartilhados.

## Dívida estrutural remanescente

### `praxis-legacy.ts`

O arquivo ainda concentra contratos e chamadas de muitas áreas. `praxis-contract.ts` adapta nomes históricos corrompidos para não expô-los aos consumidores novos.

A remoção segura exige migração por domínio:

1. mover tipos e funções para módulos pequenos;
2. atualizar consumidores;
3. manter reexports temporários;
4. remover o adaptador somente quando não houver imports legados.

Não deve ser reescrito de uma vez porque isso amplia o risco sobre toda a aplicação.

### Serviços grandes de integração e parceiros

`IntegrationManagementService` e `PartnerService` ainda contêm trechos locais de geração, preview ou hash de token. Devem migrar para os utilitários centrais em uma etapa dedicada, acompanhada pelos testes de gestão de integração e catálogo de parceiros.

### Convites administrativos e segredo de webhook

`AdminEmpresaService` e `GenericWebhookDeliveryService` também geram valores aleatórios. Antes de unificar, é necessário preservar os tamanhos, o armazenamento BCrypt/HMAC e o ciclo de rotação específico de cada credencial.

## Regra de manutenção

- Não criar novos `MessageDigest.getInstance("SHA-256")` fora de `Sha256`.
- Não criar novos `SecureRandom` para tokens URL-safe fora de `SecureTokens`, salvo algoritmo criptográfico com requisito diferente documentado.
- Novos clientes HTTP do frontend devem usar `apiRequest`.
- Contratos de provedores externos permanecem separados dos comandos internos de negócio.
