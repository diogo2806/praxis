# Direitos do titular — LGPD art. 18 (Praxis)

> **Status: MINUTA operacional para validação jurídica.** Descreve como o titular
> (candidato) exerce seus direitos e como o controlador os atende. Cobre o risco
> crítico #2. **Não é aconselhamento jurídico.**

## 1. Direitos garantidos (art. 18)

| Direito (art. 18) | O que é | `requestType` na API |
| --- | --- | --- |
| I e II | Confirmação da existência do tratamento e acesso aos dados | `confirmationAccess` |
| III | Correção de dados incompletos, inexatos ou desatualizados | `rectification` |
| IV | Anonimização, bloqueio ou eliminação de dados desnecessários/excessivos | `anonymizationDeletion` |
| V | Portabilidade a outro fornecedor | `portability` |
| VI | Eliminação dos dados tratados com base em consentimento | `deletionConsent` |
| VII | Informação sobre com quem os dados foram compartilhados | `informationSharing` |
| IX | Revogação do consentimento | `consentRevocation` |

A **revisão de decisão automatizada** (art. 20) tem canal próprio:
`POST /candidate/attempts/{attemptId}/review-request` (evento `reviewRequested`).

## 2. Canal in-product (implementado)

O candidato registra o pedido pela própria participação, sem precisar de login:

```
POST /candidate/attempts/{attemptId}/data-request
Content-Type: application/json

{
  "requestType": "anonymizationDeletion",
  "contact": "titular@example.com",   // opcional, para retorno
  "details": "Quero excluir meus dados."  // opcional
}
```

- O pedido é gravado na **trilha append-only** como evento `dataSubjectRequest`,
  com a empresa resolvida a partir da tentativa (`CandidateDataRequestService`).
- Resposta `204 No Content`. Requisição sem `requestType` retorna `400`.
- O Praxis atua como **operador**: registra e encaminha; a **decisão e o
  atendimento** são do controlador (empresa), que detém a base legal.

## 3. Canal de atendimento do controlador

Além do canal in-product, o controlador deve manter um canal externo. As
variáveis abaixo alimentam `GET /api/v1/privacy/compliance`, exibido ao titular:

| Variável | Uso |
| --- | --- |
| `PRAXIS_PRIVACY_CONTROLLER_NAME` | Nome do controlador exibido ao titular. |
| `PRAXIS_PRIVACY_SERVICE_EMAIL` | E-mail do canal de privacidade. |
| `PRAXIS_PRIVACY_SERVICE_URL` | Página/portal de privacidade. |
| `PRAXIS_PRIVACY_DPO_CONTACT` | Contato do Encarregado (DPO). |
| `PRAXIS_PRIVACY_RETENTION_DAYS` | Prazo de retenção (padrão 180 dias). |

> **Obrigatório antes de operar processos reais:** configurar ao menos
> `PRAXIS_PRIVACY_SERVICE_EMAIL` ou `PRAXIS_PRIVACY_SERVICE_URL`. Sem isso, o
> endpoint devolve aviso de "canal não configurado".

## 4. Prazos e retenção

- Requisições de acesso/confirmação: em regra, atendimento imediato em formato
  simplificado, ou até 15 dias em declaração completa (art. 19, II).
- Eliminação/anonimização: atendida pelo controlador; a plataforma também
  anonimiza automaticamente após a retenção configurada
  (`PrivacyRetentionService`), preservando o mínimo para rastreabilidade legal.
- Toda anonimização gera evento `attemptAnonymized` na trilha.

## 5. Pendências para evolução (roadmap)

- Autoatendimento de exportação/portabilidade (gerar pacote de dados do titular).
- Execução automática de eliminação sob validação do controlador.
- UI dedicada no fluxo do candidato para acionar os direitos (hoje via API).

> Última revisão da minuta: preencher na validação jurídica.
