# Marketplace — Termos, Consumidor (CDC) e Pagamentos (Praxis)

> **Status: MINUTA para validação jurídica.** Cobre o marketplace de
> profissionais com pagamentos via Mercado Pago. **Não é aconselhamento
> jurídico.** Endereça o risco alto #8.

## 1. Papéis e natureza da relação

- **Praxis/iForce:** **plataforma intermediária** que aproxima empresas
  contratantes e profissionais e processa pagamentos via Mercado Pago. Não é
  parte na prestação do serviço profissional em si.
- **Profissional:** presta o serviço anunciado; responde por sua execução,
  habilitação e tributos.
- **Empresa contratante:** contrata e paga pelo serviço.

O enquadramento exato (intermediário/marketplace) e o grau de responsabilidade
solidária devem ser confirmados pelo Jurídico à luz do CDC e da jurisprudência.

## 2. Direito do consumidor (CDC), quando aplicável

- **Informação clara** sobre serviço, preço, prazo e condições antes da compra.
- **Direito de arrependimento** (art. 49 do CDC) em contratações fora do
  estabelecimento: prazo de 7 dias, quando aplicável ao tipo de serviço.
- **Canais de atendimento e reclamação** acessíveis.
- **Transparência de taxas** cobradas pela plataforma.

## 3. Pagamentos, reembolso e disputa

- Pagamentos processados pelo **Mercado Pago**; a plataforma não armazena dados
  de cartão. Credenciais do vendedor são cifradas
  (`MarketplaceTokenCryptoService`) e o webhook valida assinatura
  (`MercadoPagoSignatureValidator`).
- **Política de reembolso/estorno:** definir prazos e hipóteses (serviço não
  entregue, em desacordo, cancelamento). Refletir nos estados
  `MARKETPLACE_ORDER_DISPUTED` e `MARKETPLACE_ORDER_REFUNDED` já existentes.
- **Chargeback:** definir tratamento e ônus entre plataforma e profissional.
- **Disputas:** fluxo de mediação registrado em trilha antes de reembolso.

## 4. KYC e idoneidade dos profissionais

- Verificar identidade e documento (CPF) e, quando exigível, **registro
  profissional** antes de habilitar anúncios.
- Estados de verificação já existem (`ProfessionalVerificationStatus`:
  `PENDING_VERIFICATION`/`VERIFIED`/`REJECTED`/`SUSPENDED`) e moderação pelo admin.
- Prevenção à lavagem de dinheiro e à fraude conforme exigências do Mercado Pago.

## 5. Conteúdo, avaliações e responsabilidade

- **Anúncios e avaliações** passam por moderação (`MARKETPLACE_LISTING_*`,
  `MARKETPLACE_PROFESSIONAL_*`).
- **Difamação:** avaliações devem permitir contraditório/denúncia; conteúdo
  ilícito é removível.
- **Dados pessoais** de profissionais seguem a política de privacidade e a
  retenção/anonimização (`PrivacyRetentionService` anonimiza profissionais
  rejeitados/suspensos após a retenção).

## 6. Tributário

- Cada parte responde por seus tributos. Definir emissão de **nota fiscal** do
  serviço (profissional) e da taxa de intermediação (plataforma), e eventuais
  obrigações acessórias.

## 7. Pendências para o Jurídico

1. Confirmar responsabilidade da plataforma como intermediária (CDC).
2. Redigir política de reembolso/estorno/chargeback e prazos.
3. Definir requisitos de KYC por categoria de serviço.
4. Definir regras fiscais e de emissão de nota.
5. Termos específicos de profissional e de contratante.

> Última revisão da minuta: preencher na validação jurídica.
