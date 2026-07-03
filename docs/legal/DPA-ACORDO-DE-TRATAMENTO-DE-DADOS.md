# DPA — Acordo de Tratamento de Dados (Controlador × Operador)

> **Status: MINUTA para validação jurídica.** Modelo de anexo contratual (Data
> Processing Agreement) entre a **empresa cliente (controladora)** e a **iForce /
> Praxis (operadora)**, nos termos dos arts. 37 a 39 da LGPD. **Não é
> aconselhamento jurídico**; revisar com o Jurídico antes de assinar. Cobre o
> risco crítico #3.

## 1. Definições e papéis

- **Controladora:** a empresa cliente, que decide sobre as finalidades e os meios
  do tratamento dos dados dos candidatos.
- **Operadora:** a iForce, que realiza o tratamento **em nome e conforme
  instruções** da controladora, por meio da plataforma Praxis.
- **Titulares:** candidatos/participantes das avaliações.

## 2. Objeto e instruções

A operadora tratará dados pessoais **exclusivamente** para prestar os serviços de
avaliação situacional contratados e conforme as instruções documentadas da
controladora. A operadora não usará os dados para finalidades próprias.

| Item | Detalhe |
| --- | --- |
| Categorias de titulares | Candidatos e participantes de avaliações. |
| Categorias de dados | Identificação (nome, e-mail, documento), respostas, score, decisão do recrutador, metadados de auditoria. |
| Dados sensíveis | Somente na vertical de Saúde, se habilitada — ver `PROPOSTA-CONSENTIMENTO-SAUDE.md`. |
| Finalidade | Triagem/entrevista com evidência comportamental; apoio à decisão humana. |
| Duração | Enquanto vigente o contrato + prazo de retenção (`PRAXIS_PRIVACY_RETENTION_DAYS`). |

## 3. Obrigações da operadora

1. Tratar dados apenas conforme instruções da controladora e a LGPD.
2. Garantir confidencialidade de quem acessa os dados.
3. Adotar medidas técnicas e organizacionais de segurança (art. 46): isolamento
   multi-tenant, segredos em variáveis de ambiente, tokens em hash/criptografia,
   trilha de auditoria append-only.
4. Auxiliar a controladora no atendimento aos direitos do titular (art. 18) e à
   revisão de decisão automatizada (art. 20) — ver `DIREITOS-DO-TITULAR-LGPD.md`.
5. Notificar a controladora sobre incidentes de segurança sem demora indevida.
6. Eliminar ou anonimizar os dados ao fim do tratamento, salvo obrigação legal.

## 4. Suboperadores

A controladora autoriza os suboperadores abaixo; a operadora responde por eles e
comunica alterações com antecedência razoável.

| Suboperador | Função | Local do tratamento |
| --- | --- | --- |
| Provedor de hospedagem/infra | Execução da aplicação e banco de dados | preencher |
| Mercado Pago | Pagamentos (billing e marketplace) | Brasil |
| Gupy / Recrutei | ATS integrados que criam tentativas e recebem resultados | preencher |
| Armazenamento de objetos (`OBJECT_STORAGE_*`) | Mídia de avaliações, quando configurado | preencher |

> **Transferência internacional:** se algum suboperador tratar dados fora do
> Brasil, aplicar as salvaguardas do art. 33 da LGPD e registrar aqui.

## 5. Direitos do titular e cooperação

A operadora fornece à controladora os meios para atender titulares: canal
in-product (`/candidate/attempts/{id}/data-request` e `/review-request`),
anonimização automática e trilha de auditoria. A resposta ao titular é
responsabilidade da controladora.

## 6. Segurança, incidentes e auditoria

- A operadora mantém registro das operações (domínio `audit`).
- Em incidente com risco relevante, a operadora informa a controladora com os
  elementos do art. 48; a **comunicação à ANPD e aos titulares** é decisão e
  responsabilidade da controladora.
- A controladora pode auditar a conformidade mediante aviso prévio razoável.

## 7. Pendências para o Jurídico

1. Preencher suboperadores, locais de tratamento e transferências internacionais.
2. Definir prazos de notificação de incidente e SLA de cooperação.
3. Integrar este DPA ao contrato principal e à política de privacidade.

> Última revisão da minuta: preencher na validação jurídica.
