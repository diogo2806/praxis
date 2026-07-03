# Política de Privacidade — Praxis

> **Status: MINUTA para validação jurídica.** Modelo base da política de
> privacidade da plataforma. Ajustar os campos entre colchetes e validar com o
> Jurídico/DPO antes de publicar. **Não é aconselhamento jurídico.** Cobre parte
> do risco crítico #4.

## 1. Quem trata seus dados

- **Plataforma:** Praxis, operada pela **iForce** (**[razão social / CNPJ]**).
- **Papel da iForce:** conforme o caso, **controladora** dos dados de usuários da
  plataforma (recrutadores, contas de empresa) e **operadora** dos dados de
  **candidatos**, que são tratados por conta da empresa contratante (controladora).
- **Encarregado (DPO):** **[nome / e-mail — `PRAXIS_PRIVACY_DPO_CONTACT`]**.

## 2. Dados que tratamos

| Categoria | Exemplos | Origem |
| --- | --- | --- |
| Conta/usuário | Nome, e-mail, empresa, credenciais | Cadastro |
| Candidato | Nome, e-mail, documento, respostas, score, decisão | ATS (Gupy/Recrutei) ou link interno |
| Dados sensíveis (saúde) | Somente na vertical de Saúde, se habilitada | Consentimento específico |
| Pagamento | Dados de cobrança/assinatura | Mercado Pago |
| Uso e segurança | Logs, trilha de auditoria, cookies essenciais | Automática |

## 3. Finalidades e bases legais

| Finalidade | Base legal (LGPD) |
| --- | --- |
| Prestar o serviço de avaliação e apoiar a triagem | Execução de contrato / legítimo interesse do controlador (art. 7º) |
| Cobrança e gestão da assinatura | Execução de contrato (art. 7º, V) |
| Dados sensíveis de saúde (vertical) | Consentimento específico e destacado (art. 11, I) |
| Segurança, prevenção a fraude e auditoria | Legítimo interesse / cumprimento de obrigação legal |

A base legal aplicável a **candidatos** é definida pela empresa contratante
(controladora).

## 4. Compartilhamento

Compartilhamos dados apenas com suboperadores necessários à operação (hospedagem,
Mercado Pago, ATS integrados e armazenamento de mídia) e quando exigido por lei.
Ver o mapa de suboperadores no `DPA-ACORDO-DE-TRATAMENTO-DE-DADOS.md`.

## 5. Retenção e eliminação

Os dados de candidatos são anonimizados automaticamente após **[período —
`PRAXIS_PRIVACY_RETENTION_DAYS`, padrão 180 dias]** do encerramento do processo,
mantendo-se o mínimo necessário à rastreabilidade e ao exercício regular de
direitos. Cada anonimização é registrada na trilha de auditoria.

## 6. Seus direitos (art. 18) e revisão (art. 20)

Você pode confirmar o tratamento, acessar, corrigir, pedir anonimização/eliminação,
portabilidade, informação sobre compartilhamentos e revogar consentimento; e pode
**pedir a revisão humana** de uma decisão automatizada. Como exercer:

- Durante uma avaliação: pelos canais do próprio processo (a decisão é sempre de
  uma pessoa, não de um sistema automático).
- A qualquer momento: **[canal — `PRAXIS_PRIVACY_SERVICE_EMAIL` /
  `PRAXIS_PRIVACY_SERVICE_URL`]** ou o Encarregado (DPO).

Detalhes operacionais em `DIREITOS-DO-TITULAR-LGPD.md`.

## 7. Segurança

Adotamos medidas técnicas e organizacionais: isolamento por empresa, segredos em
variáveis de ambiente, tokens em hash/criptografia e trilha de auditoria
append-only. Nenhum sistema é 100% imune; em incidente relevante, seguimos os
procedimentos de comunicação previstos na LGPD.

## 8. Cookies

Ver `POLITICA-DE-COOKIES.md`.

## 9. Alterações e contato

Podemos atualizar esta política; mudanças relevantes serão comunicadas. Contato:
**[e-mail]** · Encarregado (DPO): **[contato]**.

> Última revisão da minuta: preencher na validação jurídica.
