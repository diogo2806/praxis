# Vertical de Saúde — Enquadramento ANVISA e checklist de habilitação

> **Status: MINUTA para validação jurídica.** Complementa
> [`../PROPOSTA-CONSENTIMENTO-SAUDE.md`](../PROPOSTA-CONSENTIMENTO-SAUDE.md) com o
> enquadramento regulatório (ANVISA/SaMD) e o checklist para habilitar a vertical.
> **Não é aconselhamento jurídico.** Endereça o risco alto #6.

## 1. Dupla natureza do risco

A vertical de Saúde combina dois regimes:

1. **Proteção de dados (LGPD).** Trata **dado pessoal sensível de saúde**
   (art. 5º, II; art. 11), exigindo base legal específica e consentimento
   destacado quando aplicável, e cuidado redobrado com menores/vulneráveis
   (art. 14). Já tratado na minuta de consentimento.
2. **Regulação sanitária (ANVISA).** Se o software for percebido como
   diagnóstico, prescrição ou conduta clínica, pode ser classificado como
   **dispositivo médico (SaMD — Software as a Medical Device)** e ficar sujeito à
   regularização na ANVISA (RDC 751/2022 e correlatas).

## 2. Posicionamento que afasta o enquadramento SaMD

O Praxis deve ser mantido, com rigor, como **material de apoio educativo**:

- **Sim:** exercício de tomada de decisão, treino, letramento, autoconhecimento.
- **Não:** diagnóstico, triagem clínica, indicação de tratamento, monitoramento
  de paciente, cálculo de risco clínico ou qualquer saída que oriente conduta.

Regras de conteúdo (obrigatórias quando a vertical estiver ativa):

1. Sem promessa de resultado clínico, cura ou melhora de saúde.
2. Sem linguagem de diagnóstico/prescrição nas alternativas ou no resultado.
3. Aviso explícito de que **não substitui avaliação profissional**.
4. Resultado educativo, sem recomendação de conduta clínica.
5. Decisão sempre humana; sem automação de decisão clínica.

> Estas regras são a fronteira que sustenta o posicionamento educativo. Mudanças
> de copy/produto que se aproximem de "diagnóstico" exigem reanálise ANVISA.

## 3. Controles técnicos já existentes

- Flag por empresa `empresas.health_vertical` (default `false`) — vertical
  **desligada por padrão**.
- Trava de publicação: exige aceite do termo `HEALTH_USE` pelo recrutador.
- Consentimento do participante antes de iniciar
  (`POST /candidate/attempts/{id}/health-consent`, evento `healthConsentRecorded`).

## 4. Checklist para habilitar a vertical (por empresa)

- [ ] Validação jurídica formal e **datada** das minutas A, B e C.
- [ ] Base legal definida para o contexto do cliente (art. 11, I ou II).
- [ ] Fluxo de menor/vulnerável definido (art. 14), se aplicável.
- [ ] Parecer de enquadramento ANVISA confirmando o caráter educativo (não SaMD).
- [ ] Prazo de retenção específico para dado de saúde definido.
- [ ] Canal do Encarregado (DPO) configurado e visível ao participante.
- [ ] Conteúdo dos cenários revisado contra as regras da §2.
- [ ] `empresas.health_vertical = TRUE` aplicado somente após os itens acima.

## 5. Pendências (do documento de consentimento)

Ver §7 e §8 de `../PROPOSTA-CONSENTIMENTO-SAUDE.md`: base legal por contexto,
enquadramento operador × controlador no DPA, versionamento do texto da Minuta A e
fluxo dedicado de responsável legal.

> Última revisão da minuta: preencher na validação jurídica.
