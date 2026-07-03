# Exposição Regulatória Internacional (Praxis)

> **Status: MINUTA para decisão de negócio e validação jurídica.** Mapeia o risco
> de operar fora do Brasil. **Não é aconselhamento jurídico.** Endereça o risco
> médio #9.

## 1. Por que isto importa

O produto já tem traduções `en` e `es-mx` (`frontend/src/lib/translations/`), o
que sinaliza intenção de vender fora do Brasil. Recrutamento é tratado como **alto
risco** em várias jurisdições, o que adiciona obrigações além da LGPD.

## 2. Mapa de exposição

| Jurisdição | Marco | Exige (resumo) |
| --- | --- | --- |
| União Europeia | **EU AI Act** | Recrutamento = alto risco: gestão de risco, documentação técnica, supervisão humana, testes de viés, transparência. |
| UE / EEE | **GDPR** | Base legal, direitos do titular, DPO/representante, transferência internacional, RIPD. |
| EUA — NYC | **Local Law 144** | Auditoria de viés independente anual e aviso a candidatos para ferramentas automatizadas de decisão de emprego. |
| EUA — Colorado | **SB 24-205** | Avaliação de risco e avisos de transparência para IA de alto risco. |
| EUA — Califórnia | Regras do Civil Rights Council | Proíbe sistemas automatizados que discriminem características protegidas. |
| México | LFPDPPP | Regime próprio de proteção de dados (relevante para `es-mx`). |

## 3. Recomendação

**Curto prazo — geo-restrição consciente.** Enquanto não houver compliance por
jurisdição, restringir a operação ao Brasil (LGPD), tratando `en`/`es-mx` como
material institucional/idioma de interface, **não** como habilitação de venda no
exterior. Deixar isso explícito em contrato e onboarding.

**Médio prazo — compliance por jurisdição, se houver expansão.** Antes de operar
em cada mercado: mapear o marco aplicável, produzir a documentação exigida
(ex.: auditoria de viés para NYC; documentação técnica e supervisão humana para o
EU AI Act; representante/DPO e RIPD para GDPR) e ajustar contratos e avisos.

## 4. O que o produto já oferece de suporte

- Scoring determinístico e auditável (reduz o esforço de "explicabilidade").
- Trilha append-only e relatório de evidência (apoia auditoria de viés).
- Revisão humana e canal de direitos do titular.
- Política de mitigação de viés (`POLITICA-MITIGACAO-VIES.md`).

## 5. Pendências

1. Decisão de negócio: restringir ao Brasil vs. expandir.
2. Se expandir: escolher jurisdições-alvo e montar o compliance de cada uma.
3. Revisar traduções para não sugerir habilitação onde não há compliance.

> Última revisão da minuta: preencher na validação.
