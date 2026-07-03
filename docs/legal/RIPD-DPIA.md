# RIPD — Relatório de Impacto à Proteção de Dados Pessoais (Praxis)

> **Status: MINUTA para validação jurídica.** Este documento é um modelo de
> RIPD/DPIA para o tratamento de dados no fluxo de avaliação de candidatos.
> **Não constitui aconselhamento jurídico** e deve ser revisado e assinado pelo
> Encarregado (DPO) e pelo Jurídico antes de valer como registro oficial.
> Cobre o risco crítico #1 (decisão automatizada — LGPD art. 20).

## 1. Por que este relatório existe

O Praxis calcula, de forma automatizada, um **score situacional** que apoia a
triagem de candidatos. Ainda que o cálculo seja **determinístico e sem IA**, é
um tratamento automatizado que pode **afetar interesses do titular** (art. 20 da
LGPD) e, no recrutamento, é considerado tratamento de **alto risco** por
autoridades (ANPD, Nota Técnica 12/2025; EU AI Act — recrutamento como alto
risco). Por isso, um RIPD é a peça de conformidade adequada.

## 2. Papéis

| Papel | Quem | Observação |
| --- | --- | --- |
| Controlador | Empresa cliente que aplica a avaliação | Define finalidade, base legal e retenção. |
| Operador | iForce / Praxis | Trata dados por conta e ordem do controlador (ver DPA). |
| Titular | Candidato / participante | Exerce os direitos do art. 18 e a revisão do art. 20. |

## 3. Descrição do tratamento

- **Dados tratados:** nome, e-mail, documento (`documentId`), respostas às
  situações, score por competência, decisão do recrutador e trilha de eventos.
- **Finalidade:** apoiar triagem e entrevista com evidência comportamental
  estruturada; **não** substituir a decisão humana.
- **Fluxo:** criação da tentativa (Gupy/Recrutei ou link interno) → resposta do
  candidato → cálculo determinístico → resultado ao controlador (consulta ou
  webhook via outbox).
- **Retenção:** anonimização automática após o período configurado
  (`PRAXIS_PRIVACY_RETENTION_DAYS`, padrão 180 dias) — ver §6.

## 4. Necessidade e proporcionalidade

- Os dados coletados são os mínimos para identificar o candidato e produzir a
  evidência (princípio da necessidade, art. 6º, III).
- O score é **explicável**: deriva de alternativas, competências e pesos
  cadastrados, sem caixa-preta. O `EvidenceReport` consolida a rastreabilidade.

## 5. Riscos e salvaguardas (art. 20)

| Risco | Salvaguarda no produto | Onde |
| --- | --- | --- |
| Decisão automatizada eliminatória sem revisão | Flag `humanReviewRequired`; guardrails de uso; resultado marcado para análise quando há resposta crítica | `CandidateAttemptEntity`, `cadastro_cenarios_rh.md` |
| Titular sem canal de revisão | Endpoint público de revisão humana | `POST /candidate/attempts/{id}/review-request` (evento `reviewRequested`) |
| Falta de rastreabilidade | Trilha append-only por tentativa | domínio `audit` |
| Viés/discriminação | Guardrails anti-estereótipo; comparação só dentro da mesma versão | `cadastro_cenarios_rh.md` |
| Transparência ao titular | Aviso no fluxo do candidato + `/api/v1/privacy/compliance` | `PrivacyController` |

## 6. Medidas de segurança e retenção

- Isolamento multi-tenant por empresa (não usar `PRAXIS_SECURITY_ENABLED=false`
  em produção).
- Segredos por variável de ambiente; tokens de integração em hash SHA-256.
- Anonimização automática por `PrivacyRetentionService`/`PrivacyRetentionScheduler`,
  registrada como evento `attemptAnonymized`.

## 7. Conclusão e pendências para o Jurídico/DPO

1. Confirmar base legal por contexto de cada cliente e refletir no DPA.
2. Definir se o uso eliminatório exige **trava técnica** de revisão humana (hoje
   é flag + guardrail, não bloqueio).
3. Datar e assinar este RIPD e revisá-lo a cada mudança material de tratamento.
4. Definir monitoramento agregado de impacto/viés (roadmap).

> Última revisão da minuta: preencher na validação jurídica.
