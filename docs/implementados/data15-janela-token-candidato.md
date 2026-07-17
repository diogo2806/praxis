# DATA15 — janela persistida do token do candidato

Status: concluído em 2026-07-17.

## Problema corrigido

Após o vencimento da janela original, o `JwtService` substituía o instante apenas em memória por `Instant.now()`. Como a tentativa continuava persistindo somente `created_at`, cada consulta ou reenvio posterior gerava outro `iat` e outro JWT.

## Implementação

- A migration `V1009__persist_candidate_token_window.sql` adiciona `candidate_token_issued_at`, preenche os registros existentes com `created_at` e aplica `NOT NULL`.
- `CandidateAttemptEntity` mapeia o novo campo e o inicializa antes da primeira persistência.
- `CandidateAttemptMapper` inicializa o instante na criação e não sobrescreve renovações já persistidas.
- `CandidateTokenWindowService` bloqueia a tentativa com `PESSIMISTIC_WRITE`, reutiliza a janela válida e persiste uma única renovação quando expirada.
- `JwtService` apenas assina o JWT com o instante canônico resolvido e falha explicitamente quando o serviço de persistência não está disponível.
- Consultas paginadas executam a eventual renovação em transação própria antes de montar a resposta, sem escrita implícita na transação `readOnly`.
- Criação, reaproveitamento e reenvio continuam usando a mesma tentativa e não consomem crédito adicional.

## Critério comprovado

Para uma tentativa com janela expirada, a primeira geração persiste um novo instante canônico. Gerações concorrentes ou subsequentes usam esse mesmo valor até o próximo vencimento, produzindo o mesmo JWT para empresa, tentativa, TTL e janela.

## Testes

- `CandidateTokenWindowServiceTest`: reutilização, renovação persistida, tentativa inexistente e ausência do estado canônico.
- `JwtServiceCandidateResultTokenTest`: estabilidade do token para a mesma janela e falha explícita sem a fonte canônica.
- `CandidateLinkQueryServiceTest`: resolução transacional da janela antes da assinatura do link paginado.
