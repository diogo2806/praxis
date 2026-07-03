# Política de Mitigação de Viés e Monitoramento de Impacto (Praxis)

> **Status: MINUTA para validação jurídica e de produto.** Operacionaliza os
> guardrails já existentes contra discriminação em avaliações situacionais.
> **Não é aconselhamento jurídico.** Endereça o risco alto #5.

## 1. Por que existe

Mesmo sem IA no scoring, uma avaliação situacional (SJT) com alternativas e pesos
mal calibrados pode produzir **impacto discriminatório** (disparate impact) sobre
grupos protegidos (gênero, raça, idade, origem, deficiência). Há precedentes
relevantes (Workday, iTutorGroup, Target; no Brasil, condenação por questionários
invasivos em recrutamento) e fiscalização crescente (ANPD; EU AI Act — recrutamento
como alto risco). O diferencial do Praxis (determinístico e auditável) só é
defensável se acompanhado de uma política de não-discriminação aplicada.

## 2. Princípios

1. **Relação com o cargo (job-relatedness).** Todo cenário e competência precisa
   representar uma tarefa real do cargo, documentada no blueprint.
2. **Sem estereótipo.** Alternativas e personagens não podem usar marcadores de
   classe, gênero, idade, região, sotaque, religião, origem ou deficiência.
3. **Evidência, não rótulo.** Erros críticos são descritos como risco
   comportamental do contexto, nunca como característica pessoal.
4. **Decisão humana.** O score apoia; não elimina automaticamente sem governança.
5. **Comparabilidade.** Candidatos só são comparados dentro da mesma versão.

## 3. Guardrails já no produto (referência)

- Guardrails de cadastro e antipadrões de personagem/alternativa em
  `docs/cadastro_cenarios_rh.md` (§ "Guardrails para RH", "Personagem", "Diálogo").
- Validador estrutural bloqueia publicação com blockers.
- Flag `humanReviewRequired` e canal de revisão do titular
  (`/candidate/attempts/{id}/review-request`).
- Trilha append-only para rastrear decisão e evidência.

## 4. Checklist antidiscriminação (antes de publicar)

- [ ] Cada competência tem relação documentada com o cargo.
- [ ] Nenhuma alternativa premia/penaliza traço de grupo protegido.
- [ ] Personagens não carregam estereótipo.
- [ ] Tempo limite tem justificativa operacional e há acomodação acessível.
- [ ] Mídias têm descrição acessível.
- [ ] Uso eliminatório só quando a simulação foi validada internamente.
- [ ] Revisão humana garantida em impacto relevante.

## 5. Monitoramento de impacto (procedimento)

Enquanto o monitoramento agregado automatizado não é entregue (roadmap), adotar
procedimento periódico manual, sem coletar dado sensível além do necessário:

1. Amostrar resultados por versão de simulação.
2. Onde houver dado demográfico **fornecido voluntariamente e com base legal**,
   comparar taxas de aprovação entre grupos (regra prática dos 4/5 como alerta).
3. Investigar competências/alternativas associadas a diferenças relevantes.
4. Corrigir o cenário (clonar nova versão) e registrar a decisão.
5. Nunca usar dado sensível para o scoring.

## 6. Contestação e correção

O candidato pode pedir revisão humana (art. 20). Pedidos e decisões ficam na
trilha e no relatório de evidência, servindo de base para auditoria e defesa.

## 7. Pendências (roadmap)

- Monitoramento de fairness agregado automatizado.
- Estudos de validade preditiva local.
- Banco de famílias de cenários anti-vazamento.

> Última revisão da minuta: preencher na validação.
