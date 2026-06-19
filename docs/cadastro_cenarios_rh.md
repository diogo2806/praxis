# 🎛️ Práxis — Motor de Avaliação Situacional (Integração Gupy)

> **Produto:** **Práxis** (da iForce). O nome vem do grego *práxis* = ação aplicada, em oposição à teoria — exatamente o que o sistema mede: não o que o candidato sabe ou diz, mas como ele **age** num cenário real do cargo.
> **O que é:** Um sistema que cria, valida, calibra, aplica, explica e audita simulações situacionais para seleção — não apenas um editor de diálogos.
> **Quem usa:** Renata (RH), com governança de gestor, compliance e auditoria.
> **Onde:** Painel integrado à Gupy.
>
> **Frase de 5 segundos:** "O Práxis mostra como o candidato age quando está no cenário real do trabalho — antes da entrevista."
>
> **Domínio:** `praxis.iforce.com.br` (subdomínio do domínio próprio da iForce — sem custo de registro novo, e a marca-mãe aparece na URL). Registro de marca no INPI (classes 9 e 42) pode ser feito depois, quando o produto ganhar tração.

---

## ⚠️ Reposicionamento — leia antes de tudo

O produto **não** mede "atendimento real" nem gera "transcrição real do chat". Ele mede **julgamento situacional** (o candidato escolhe entre alternativas pré-escritas). Isso é um SJT — *Situational Judgment Test* — e é legítimo, desde que vendido com honestidade.

**Não usa IA.** O sistema é 100% determinístico: a nota sai de rubrica + peso + cálculo. Sem LLM, sem custo de token, sem "IA julgando candidato". Isso é mais barato, mais auditável e mais defensável juridicamente.

**Promessa central (defensável):**
> "Avaliação situacional estruturada para recrutamento, sem IA julgando o candidato, com score por rubrica, trilha de pontuação auditável e integração à Gupy."

**Posicionamento vs. Gupy (somos camada complementar, não concorrente):**
> "A Gupy organiza o funil. Nossa simulação adiciona evidência comportamental antes da entrevista."

---

## 🧭 Modos do produto — o que entra no MVP

**O MVP avalia apenas múltipla escolha.** Qualquer campo textual é informativo e **não altera o score automaticamente** — porque, sem IA, avaliar texto livre exigiria operação humana.

| Modo | O candidato... | Status |
|---|---|---|
| **SJT (múltipla escolha)** | escolhe entre opções pré-escritas | ✅ MVP (P0) |
| SJT + justificativa curta | escolhe e justifica em texto | 🔮 Futuro — exige revisão humana |
| Resposta aberta | escreve livremente | 🔮 Futuro — só com operação humana ou IA |

> Regra: **o MVP não avalia texto livre.** O candidato escolhe alternativas; o score vem da opção escolhida, não de interpretação de texto.

---

## Visão Geral do Fluxo (redesenhado)

```
[0. Blueprint da Avaliação]  ← NOVO, antes de tudo
        ↓
[1. Escolher objetivo + modelo]
        ↓
[2. Personagem fictício] → [3. Editor de diálogo + rubricas]
        ↓
[3.5 Validador de Qualidade — por regras, sem IA]
        ↓
[4. Piloto e calibração]  ← NOVO
        ↓
[5. Pontuação normalizada por caminho]
        ↓
[6. Revisão do mapa] → [7. Governança de publicação]
        ↓
[8. Gupy Preflight Check + vínculo] → [9. Monitoramento pós-publicação]
```

---

## Passo 0 — Blueprint da Avaliação (NOVO)

Antes de escrever qualquer diálogo, Renata define **por que** essa avaliação é relevante. Sem isso, ela cria uma história interessante que talvez não meça nada útil.

```
┌─────────────────────────────────────────────────────┐
│  📋 Blueprint da Avaliação                          │
│                                                     │
│  Cargo-alvo: ____________________________           │
│  Senioridade:  ○ Júnior  ● Pleno  ○ Sênior          │
│                                                     │
│  Situação crítica REAL do cargo:                    │
│  ┌─────────────────────────────────────────────┐   │
│  │ Cliente quer estorno fora da política, com   │   │
│  │ risco de churn de conta grande.              │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  Competências avaliadas (da taxonomia):             │
│  [Empatia] [Resolução de Conflitos] [Aderência]    │
│                                                     │
│  Comportamentos observáveis de ALTA performance:    │
│  ┌─────────────────────────────────────────────┐   │
│  │ Acolhe, coleta dados mínimos, explica limite │   │
│  │ de alçada e oferece alternativa válida.      │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  Erros CRÍTICOS (disparam revisão humana obrigatória):         │
│  ┌─────────────────────────────────────────────┐   │
│  │ Prometer estorno sem validar política.       │   │
│  └─────────────────────────────────────────────┘   │
│  Efeito padrão: gera alerta crítico, bloqueia a     │
│  recomendação automática e exige validação do RH.   │
│                                                     │
│  Política da empresa envolvida: ________________    │
│  Nível de autonomia esperado: __________________    │
│                                                     │
│  Diferença por senioridade:                         │
│  Júnior faria: ____  Pleno faria: ____  Sênior: ___│
│                                                     │
│  Uso do resultado:                                  │
│  ● Triagem  ○ Ranking  ○ Apoio à entrevista        │
│  🔒 Eliminação — só p/ simulação validada, com      │
│     aprovação gestor/compliance e canal de revisão  │
│                                                     │
│             [ Cancelar ]  [ Criar simulação → ]    │
└─────────────────────────────────────────────────────┘
```

> O blueprint vira referência fixa para o Validador de Qualidade (Passo 3.5) checar se a simulação realmente mede o que prometeu.

---

## Passo 1 — Escolher por objetivo, não só por área

Em vez de só "Atendimento / Vendas / Liderança", Renata escolhe **o que quer avaliar** — aproximando o produto do problema real.

```
┌──────────────────────────────────────────────────────────────────┐
│  🎯 O que você quer avaliar?                                     │
│                                                                  │
│  [ Lidar com conflito ]        [ Priorizar sob pressão ]        │
│  [ Negociar sem dar desconto ] [ Comunicar uma negativa ]       │
│  [ Recuperar cliente em risco ][ Escalar problema corretamente ]│
│                                                                  │
│  ─────────────────────────────────────────────────────────────  │
│  Começar de:  [ 📋 Modelo pronto ]   [ ➕ Do zero ]            │
└──────────────────────────────────────────────────────────────────┘
```

### Dificuldade é calculada, não escolhida

"Leve / Moderado / Intenso" é vago demais — um cliente furioso pode ser fácil se a resposta certa for óbvia. A dificuldade final é **calculada** a partir de 5 dimensões:

```
Intensidade emocional         ▓▓▓▓▓░░░░░  alta
Ambiguidade da informação     ▓▓▓░░░░░░░  baixa
Risco de negócio              ▓▓▓▓▓▓▓░░░  alto
Conflito empatia × política   ▓▓▓▓▓▓░░░░  médio-alto
Autonomia exigida             ▓▓▓▓░░░░░░  média
                              ─────────────────────
Dificuldade calculada: INTENSO (porque há conflito real, não só
cliente gritando)
```

---

## Passo 2 — Personagem Fictício

```
┌─────────────────────────────────────────────────────┐
│  👤 Personagem do Cliente Fictício                  │
│                                                     │
│  Nome do cliente *      ┌────────────────┐          │
│                         │ Carlos M.      │          │
│                         └────────────────┘          │
│  Perfil emocional inicial *                        │
│  ○ Tranquilo   ○ Frustrado   ● Furioso             │
│                                                     │
│  ⚠️ Revisão de viés: a linguagem do personagem      │
│     não deve enviesar por classe, região, gênero    │
│     ou idade. [ Abrir checklist de linguagem ]      │
│                                                     │
│  Contexto (só visível para RH e gestor):            │
│  ┌─────────────────────────────────────────────┐   │
│  │ Produto com defeito, presente do filho.      │   │
│  └─────────────────────────────────────────────┘   │
│             [ ← Voltar ]  [ Montar diálogo → ]     │
└─────────────────────────────────────────────────────┘
```

> O checklist de linguagem é uma lista de verificação manual (sem IA): evita regionalismo desnecessário? estereótipo de classe? marcador de gênero sem necessidade? idade, sotaque, origem ou crença? usa linguagem compatível com o cargo? O RH confirma cada item — nada é analisado por classificador automático.

---

## Passo 3 — Editor de Diálogo (com alternativas plausíveis)

A regra de ouro: **todas as alternativas devem ser plausíveis**. Se uma opção é "obviamente a gentil", o teste mede bom senso, não competência.

### ❌ Alternativas fracas (óbvias)
```
A. Empática   B. Robótica   C. Defensiva
→ Qualquer candidato treinado escolhe A. Não mede nada.
```

### ✅ Alternativas fortes (todas plausíveis)
```
A. Muito empática, mas promete estorno SEM validar política.
B. Segue o processo corretamente, mas soa fria.
C. Acolhe, coleta dados mínimos e explica o próximo passo.
D. Resolve rápido, mas ignora o registro no sistema.
```

Aqui a resposta certa **não é "ser gentil"** — é equilibrar empatia, processo, autonomia e risco. A opção C é a melhor; a A contém o erro crítico do blueprint (que dispara revisão humana).

```
┌──────────────────────────────────────────────────────────────────┐
│  🗂️ Editor — Turno 1                                            │
│  MENSAGEM DO CLIENTE                                            │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ "Chegou QUEBRADO! Quero meu dinheiro de volta AGORA!"      │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ⏱️ Tempo deste turno: [ 30s ▾ ]                                │
│  ⚠️ Justifique o tempo: por que 30s? Esta competência exige     │
│     resposta rápida? [ campo obrigatório ]                      │
│                                                                  │
│  OPÇÕES (todas plausíveis — sem opção obviamente certa)         │
│  A ▸ Empática, mas promete estorno sem validar política        │
│  B ▸ Processo correto, porém frio                              │
│  C ▸ Acolhe + dados mínimos + próximo passo                    │
│  D ▸ Resolve rápido, ignora registro                          │
│  [ ➕ opção ]  (2 a 4 por turno · máx. 160 caract.)            │
│                                                                  │
│             [ ← Voltar ]  [ Ramificação → ]                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Passo 3 (cont.) — Pontuação por RUBRICA, não por achismo

A pontuação manual livre (+10, -5...) transforma o score em opinião pessoal. Em vez disso, cada nível de pontuação vem de uma **rubrica comportamental** padronizada.

### Exemplo de rubrica — competência "Empatia"

| Nível | Comportamento observável | Pontos |
|---|---|---|
| 0 | Ignora a emoção do cliente ou culpa terceiros | 0 |
| 1 | Reconhece parcialmente, mas não acolhe | 3 |
| 2 | Acolhe a emoção e assume postura de solução | 7 |
| 3 | Acolhe, prioriza, explica próximos passos e reduz tensão | 10 |

Renata classifica cada opção pelo **nível da rubrica** — o ponto vem do nível, não de um número que ela inventa. Isso dá consistência e defensabilidade (alinhado às diretrizes SIOP/APA de validação de instrumentos de seleção e à ISO 10667 de qualidade em avaliação de pessoas).

---

## Passo 3.5 — Validador de Qualidade (NOVO — o coração do produto)

Antes de publicar, o **Validador de Qualidade** dá um diagnóstico. Ele usa **regras determinísticas, não IA** — verifica estrutura, pontuação, cobertura de competências, versionamento, desfechos, pesos e riscos operacionais. Isso transforma o produto de "editor" em "sistema especialista" — e é difícil de copiar.

```
┌──────────────────────────────────────────────────────────────────┐
│  🔬 Qualidade da Simulação: 82/100                              │
├──────────────────────────────────────────────────────────────────┤
│  ✅ Todos os caminhos têm desfecho                              │
│  ✅ Cada competência tem ≥3 evidências                         │
│  ✅ Pontuação normalizada por caminho                          │
│  ✅ Nenhum comportamento "fantasma" sem evidência observável   │
│                                                                  │
│  ⚠️ Opção C óbvia? (confirmar no checklist manual / piloto)    │
│  ⚠️ "Foco em Processos" aparece pouco no caminho 2B            │
│  ⚠️ Tempo de 20s parece agressivo p/ leitura em mobile        │
│  ⚠️ Dificuldade calculada "intensa", mas os fatores              │
│     atuais sustentam melhor "moderada"                          │
│                                                                  │
│  ❌ Caminho 3C permite score máx. 28% maior que o 2A          │
│  ❌ Risco de promessa indevida: não é "transcrição real"      │
│                                                                  │
│  Recomendação: publicar como PILOTO, não como eliminatória.    │
│                                                                  │
│  Com BLOCKER:  [ Corrigir agora ]  [ Salvar rascunho ]         │
│  Só com WARNING: [ Corrigir ]  [ Publicar com alerta registrado ]│
└──────────────────────────────────────────────────────────────────┘
```

> Se houver blocker, **não há botão de publicar** — só corrigir ou salvar rascunho. Publicar com warning fica registrado no log: "Usuário X publicou com os warnings Y/Z em [data/hora]". É isso que torna o log de auditoria útil de verdade.

### Warning × Blocker — nem todo alerta impede publicar

```
🚫 BLOQUEIA publicação:
   caminho sem desfecho · competência sem evidência mínima
   score máximo desigual por caminho · ausência de rubrica
   ausência de versão · sem política de retenção
   integração Gupy não testada · erro crítico sem regra de revisão

⚠️ PERMITE publicar com alerta:
   opção possivelmente óbvia · tempo agressivo
   baixa diversidade de caminhos · sem validade preditiva ainda
   biblioteca sem variações equivalentes
```

> **Como detectar "opção óbvia" sem IA:** no P0, é um *checklist manual* — o RH confirma que todas as opções são plausíveis. No P0.5, vira *sinal estatístico do piloto* — se 90% dos colaboradores-referência escolhem a mesma opção em tempo muito baixo, ela provavelmente está óbvia. Análise textual automática fica para o futuro, só se um dia houver um classificador.

> **Como nasce o "82/100" (sem IA):** o score de qualidade vem de pesos explícitos e determinísticos, não de um julgamento. Cada item soma pontos:
>
> ```
> Estrutura do grafo (sem caminho morto):  20 pts
> Cobertura de competências:               20 pts
> Equilíbrio de score por caminho:         20 pts
> Rubricas completas:                      15 pts
> Governança / versionamento:              10 pts
> Fluxo do candidato:                      10 pts
> Integração / preflight:                   5 pts
> ───────────────────────────────────────────────
> Total:                                  100 pts
> ```
>
> Sem a fórmula, o número é decoração. Com a fórmula, vira produto auditável.

---

## Passo 4 — Piloto e Calibração (NOVO)

Antes de ir para vaga real:

```
□ Testar com 3 colaboradores referência (alta performance no cargo)
□ Testar com 3 pessoas de fora da área
□ Comparar distribuição de respostas
□ Ajustar alternativas que ficaram óbvias
□ Calibrar tempo por turno com usuários reais
□ Calibrar pesos das competências
□ Aprovar versão 1.0
```

> Se os 3 colaboradores-referência **não** tiram nota alta, a simulação está medindo a coisa errada — não a competência.

---

## Passo 5 — Score Normalizado por Caminho (correção matemática crítica)

Como caminhos têm tamanhos diferentes (uns encerram em 3 turnos, outros seguem por 7), **somar pontos brutos é injusto**: quem resolveu cedo teve menos chances de pontuar; quem caiu num caminho longo teve mais oportunidades de acumular ou recuperar pontos.

**A solução — normalizar por caminho:**

```text
score_competência = pontos_obtidos / pontos_possíveis_naquele_caminho
```

E depois ponderar por competência:

```text
score_final =
    Empatia_normalizada      × 40%
  + Resolução_normalizada    × 35%
  + Processo_normalizado     × 25%
```

Assim, dois candidatos têm o **mesmo teto possível**, independente do caminho em que caíram. A nota reflete competência, não sorte de ramificação.

> Regra extra: **erro crítico** (definido no blueprint) **não reprova automaticamente por padrão**. Ele dispara *revisão humana obrigatória* e bloqueia a recomendação automática até o RH/gestor validar.
>
> Erro crítico só pode ser eliminatório quando: (1) foi definido no blueprint; (2) foi aprovado por gestor/compliance; (3) aparece no aviso ao candidato; (4) há canal de revisão; e (5) a simulação já está calibrada/validada.

---

## Passo 6 — Revisão do Mapa

```
┌──────────────────────────────────────────────────────────────────┐
│  🗺️ Mapa — "O Dia do Caos" v1.0                                │
│        [T1] → A,B,C,D                                            │
│         ├─ C → [T2a] → ... → [FIM]                              │
│         ├─ A → [T2b: erro crítico → revisão humana ⚠️]        │
│         └─ B,D → [T2c] → ...                                    │
│                                                                  │
│  Tempo total (somado dos turnos): ~4 min                       │
│  Score máx. por caminho: A=100 B=100 C=100 D=100 ✅ equilibrado│
│  ⚠️ 1 caminho sem desfecho — complete antes de publicar         │
│       [ ← Editar ]  [ Salvar rascunho ]  [ Enviar p/ revisão ] │
└──────────────────────────────────────────────────────────────────┘
```

---

## Passo 7 — Governança de Publicação (NOVO)

O RH não publica direto em vaga crítica. Há **estados** e **papéis** — isso é o que vende para empresa grande.

### Estados da simulação
```
Rascunho → Em revisão (gestor) → Em revisão (compliance/jurídico)
        → Aprovada → Publicada → Arquivada
        ↘ Reprovada por baixa qualidade
```

### Papéis
```
Criador · Revisor técnico · Aprovador RH · Aprovador gestor
Admin integração Gupy · Auditor
```

### Versionamento imutável (não só bloqueado)
```
• Toda candidatura responde a uma VERSÃO IMUTÁVEL.
• Editar SEMPRE cria nova versão (nunca altera a vigente).
• Correção de typo  → versão minor.
• Mudança de pontuação → versão major.
• Resultados só são comparáveis dentro da mesma versão (ou entre
  versões calibradas).
```

> Isso responde à contestação futura: "esse candidato foi avaliado com a mesma régua que os outros?".

---

## Passo 8 — Gupy Preflight Check + Vínculo

"Zero configuração técnica" é promessa frágil. O correto é: **"Integração no-code para o RH, após configuração inicial validada."** Antes de vincular, roda-se um checklist:

```
🔌 Gupy Preflight Check
□ Plano Gupy permite acesso à API? (token de API exige Premium/Enterprise)
□ Token válido?
□ Permissões corretas?
□ Vaga encontrada?
□ Etapas sincronizadas?
□ Evento de mudança de etapa validado no ambiente Gupy do cliente
□ Retentativas configuradas?
□ Idempotência ativa? (evita evento duplicado)
□ Campo de resultado mapeado?
```

> Após o candidato finalizar, o resultado é registrado na candidatura **conforme a capacidade disponível na integração Gupy do cliente**: tag, comentário, nota, etapa ou integração externa configurada. Tratar falha de rede com envio assíncrono (retry) é obrigatório.
>
> ⚠️ Nomes exatos de eventos e endpoints só devem ser cravados após validação no ambiente real do parceiro/cliente — não os fixe no contrato antes disso, para não criar promessa frágil.

---

## Passo 9 — Monitoramento Pós-Publicação (NOVO)

Depois que candidatos respondem, o sistema acompanha:

```
Taxa de conclusão            Drop-off por dispositivo
Tempo médio por turno        Distribuição de score
Alternativas mais escolhidas Caminhos mais frequentes
Correlação score × entrevista
Sinal de vazamento: queda brusca de tempo + alta anormal de escolhas de alto score
Diferença entre grupos agregados (quando juridicamente permitido)
```

---

## 👤 Fluxo do Candidato (estava faltando)

Uma experiência ruim aqui destrói a marca empregadora. Especificação mínima:

```
Convite:        e-mail/WhatsApp com tempo estimado e nº de etapas
Treino:         simulação de aquecimento ANTES da avaliação real
Pausa:          permitida? (definir por cargo)
Queda de net:   retoma do último turno salvo, sem perder progresso
Acessibilidade: tempo estendido p/ PCD; alto contraste; leitor de tela
Mobile ruim:    funciona em conexão lenta e aparelho fraco
Feedback:       resumo leve do perfil ao final (sem nota exata)
Revisão:        canal para contestar/pedir revisão (LGPD art. 20)
Transparência:  explicação clara do que é avaliado e do uso dos dados
```

> ⚠️ O timer pode gerar viés: mede também ansiedade, leitura rápida, qualidade do aparelho e da conexão. Use tempo só onde a competência realmente exige velocidade (atendimento sim; liderança talvez não) e ofereça acomodação para acessibilidade.

---

## 📊 Relatório do Gestor — com EVIDÊNCIAS

O gestor não vê só uma nota. Vê **por que** dela, ligada a comportamentos observáveis:

```
Candidato: Thiago    Score: 78/100 (recomenda entrevista)
Nível de evidência da simulação: MÉDIO (testada em piloto interno)

Empatia ............ 85%  ▸ "Acolheu no T1, reduziu tensão no T3"
Resolução .......... 80%  ▸ "Coletou dados mínimos antes de agir"
Aderência política . 70%  ▸ "Quase prometeu estorno indevido (T2)"

⚠️ Nenhum erro crítico (não exigiu revisão humana obrigatória).
Sugestão de pergunta p/ entrevista: explorar como ele lida com
limite de alçada sob pressão.
```

> No MVP, em vez de "intervalo de confiança" (que exige modelo estatístico, amostra e metodologia), o relatório mostra o **nível de evidência**: Baixo (simulação nova, sem calibração) · Médio (testada em piloto) · Alto (calibrada com histórico). O intervalo de confiança real entra depois, quando houver dados suficientes.
>
> O score **recomenda**, nunca reprova sozinho. Erro crítico dispara revisão humana, não eliminação automática.

---

## ⚖️ LGPD e Explicabilidade (NOVO)

Como o sistema pode influenciar decisões sobre perfil profissional, ele deve ser desenhado com salvaguardas compatíveis com a LGPD — que trata da revisão de decisões tomadas **unicamente** com base em tratamento automatizado que afetem interesses do titular. Como aqui o score **recomenda e não reprova sozinho**, as salvaguardas valem especialmente se algum cliente configurar uso automatizado do score. Requisitos:

```
Base legal definida por vaga       Registro de consentimento/ciência
Política de retenção por vaga      Explicação dos critérios avaliados
Canal de revisão/contestação       Log de quem viu cada resultado
Exportação dos dados do candidato  Exclusão/anonimização quando aplicável
Relatório de impacto (clientes Enterprise)
```

Princípios da ANPD aplicáveis: qualidade dos dados, transparência, não discriminação, responsabilização e prestação de contas.

### Fairness Auditing (auditoria de viés)

Sem IA, o fairness no MVP é por **regra, auditoria e estatística simples** — não por modelo. Antes de o score influenciar decisões relevantes, o sistema **monitora, documenta e mitiga riscos de viés** (não promete imparcialidade absoluta — nenhum sistema sério faz isso):

```
Auditar a distribuição de score por simulação
Registrar a rubrica e os pesos usados
Impedir score sem explicação (trilha de pontuação obrigatória)
Permitir revisão humana
Evitar coleta de dado sensível por padrão
Gerar alerta quando uma opção, competência ou rubrica produzir
  reprovação desproporcional em análise agregada (quando juridicamente
  permitido)
```

> ⚠️ Auditoria de viés **não autoriza coletar dado sensível** (raça, orientação, crença) do candidato só para auditar — isso fere a minimização da LGPD. A análise usa apenas metadados que a empresa já possui via Gupy/ATS, de forma agregada e anonimizada.

### Responsabilidade civil — quem responde pelo dano?

Se uma simulação enviesada reprovar candidatos de forma discriminatória, há dois polos possíveis de responsabilização, e o contrato precisa deixar claro:

```
Empresa contratante (cliente Gupy): falha de supervisão — publicou sem
  revisão de qualidade/compliance, ignorou alertas do validador.
Fornecedor do motor (nós): fluxo analítico viciado na origem — rubrica
  mal calibrada, ausência de fairness auditing, caixa-preta.
```

> Por isso o **log de auditoria imutável** (quem criou, quem aprovou, qual versão, quais alertas foram ignorados) não é burocracia — é a prova que separa as responsabilidades.

---

## 🔍 Anti-Caixa-Preta — Explicabilidade Obrigatória (NOVO)

Um score sem explicação é "operacionalmente inútil e juridicamente perigoso". Em decisão que afeta carreira, entregar só um número leva a dois extremos ruins: o gestor aceita sem questionar (complacência) ou rejeita a ferramenta (não confia). Toda nota precisa carregar:

```
Trilha de pontuação: quais turnos, opções, rubricas e pesos geraram
  cada ponto
O peso de cada competência no resultado final
O nível de evidência da simulação: Baixo, Médio ou Alto
A rubrica exata que classificou cada resposta
```

Como o cálculo é determinístico, a trilha é literal e exata. Exemplo:

```
candidato escolheu opção C no turno 1
  opção C → rubrica Empatia nível 3 = 10 pts
  opção C → rubrica Aderência nível 2 = 7 pts
caminho máximo possível em Empatia = 30 pts
score Empatia = 25 / 30 = 83,3%
```

> Regra de design: se o gestor não consegue **auditar de forma independente** por que o candidato tirou aquela nota, a tela está errada.

---

## 🎯 Validade Preditiva — o score precisa prever algo (NOVO)

Esse é o teste que separa avaliação séria de pseudociência corporativa. Não basta o score parecer bonito; ele precisa **prever um resultado real**.

```
A nota alta correlaciona com... boa entrevista? boa performance no
  cargo? retenção? NPS do cliente atendido?
```

Cuidado com a falsa segurança: validar a simulação só com os dados de quem já a usou (in-sample) infla a confiança. O correto é checar fora da amostra:

```
Calibrar com colaboradores-referência (Passo 4) ANTES de usar em vaga
Acompanhar, depois da contratação, se score previu desempenho real
Recalibrar rubricas e pesos quando a correlação cair
Marcar como "piloto" toda simulação ainda sem evidência preditiva —
  nunca eliminatória antes de provar que prevê algo
```

> Sem isso, a ferramenta vira só um gerador de relatórios para justificar decisões que o gestor já tinha tomado de qualquer jeito.

---

## 🏰 Defensabilidade — por que isso não é copiável (NOVO)

Na era dos construtores rápidos, **a tela é copiável**: um editor de diálogo ramificado pode ser clonado por um dev em poucas horas. O diferencial não está em gerar texto com IA, mas em **estruturar uma avaliação defensável**: rubrica, score comparável, versionamento, auditoria, integração e evidência histórica. O fosso real está em quatro camadas que não se copiam:

### Os 4 níveis de integração (quanto mais fundo, mais difícil substituir)

| Nível | O que faz | Fosso |
|---|---|---|
| 1 — Armazenamento | guarda as simulações criadas | Fraco — dados migram fácil para um rival |
| 2 — Execução padronizada | automatiza criação e disparo | Moderado — um concorrente replica |
| 3 — Automação decisória | valida qualidade, normaliza score, sugere melhorias por regra | Forte — vira dependência analítica do RH |
| 4 — Memória institucional | acumula evidência de quais cenários se correlacionam com boa contratação naquela empresa | Muito forte — anos de calibração não se copiam |

### O flywheel de dados (o moat que realmente importa)

```
Mais empresas usam → mais resultados de contratação coletados
        → mais evidência de quais simulações se correlacionam com
          bons hires
        → a calibração das rubricas e pesos fica melhor
        → mais empresas adotam → (repete)
```

Cada correção que a Renata e o gestor fazem (human-in-the-loop) alimenta esse ciclo com estatística simples — correlação score × aprovação na entrevista, taxa de conclusão, comparação entre score e decisão do gestor. **Não precisa de machine learning nem modelo caro.** Um concorrente novo começa do zero, sem histórico, e não alcança a precisão acumulada.

> **Teste do fosso:** se um dev clonar nossa tela num fim de semana, os clientes grandes continuam? Sim — porque o valor não está na interface, está no banco de dados proprietário de "quais cenários preveem boas contratações" e na memória institucional de cada cliente.

---

## 🧬 Anti-Vazamento — Famílias de Cenários (NOVO)

Em vaga com muitos candidatos, o conteúdo vaza (print, WhatsApp, Glassdoor). O produto não tem "um cenário" — tem **famílias de cenários equivalentes**:

```
Banco de variações equivalentes (mesma competência e dificuldade)
Ordem das opções randomizada por candidato
Variações pré-cadastradas equivalentes (banco manual de versões irmãs)
Limite de exposição por modelo
Versões "irmãs" intercambiáveis
Detector de vazamento: queda brusca no tempo + aumento anormal de escolhas de alto score + concentração incomum nos mesmos caminhos
```

---

## 🏷️ Competências Customizadas → mapeadas a uma taxonomia

Deixar cada empresa criar "ownership", "sangue no olho", "cultura XPTO" vende bem, mas destrói comparabilidade. Solução: permitir o nome customizado, mas **mapear para a taxonomia interna**.

```
Competência customizada: "Ownership"
  └─ mapeada para: Proatividade + Tomada de Decisão + Responsabilização
```

O cliente sente personalização; o sistema preserva benchmark.

---

## 💰 Dashboard de ROI (para quem assina o cheque)

Renata usa, mas quem compra é Head de TA / RH / diretoria — e quer ROI:

```
Entrevistas evitadas            Horas economizadas por vaga
Tempo médio até shortlist       Custo por candidato avaliado
Taxa de conclusão da simulação  Qualidade dos finalistas
Satisfação do gestor            Candidatos não avançados antes da entrevista
Correlação score × entrevista   → métrica exploratória até haver amostra
Correlação score × performance pós-contratação → P1/P2
```

> Sem isso é "ferramenta legal". Com isso, vira linha de orçamento. As correlações começam como métricas **exploratórias** — só viram prova preditiva quando houver amostra suficiente.

---

## 🗣️ Argumentos de Venda (ajustados)

```
"A Gupy organiza o funil. Nossa simulação adiciona evidência
 comportamental antes da entrevista."

"Antes de gastar a agenda do gestor, veja como o candidato
 decide em situações reais do cargo."

"Menos entrevista baseada em currículo. Mais decisão baseada
 em evidência."
```

---

## ✅ Prioridades de Implementação

### P0 real — MVP vendável com segurança
```
Modo SJT apenas                    Versionamento imutável
Blueprint simples                  Fluxo básico do candidato
Rubricas comportamentais           Relatório do gestor com evidências
Score normalizado por caminho      Gupy Preflight básico
Log de auditoria                   Política LGPD mínima
Validador MÍNIMO de publicação (só os blockers)
```

O **Validador mínimo** do P0 verifica apenas as travas obrigatórias antes de publicar:
```
caminho sem desfecho · rubrica ausente · score máximo desigual por caminho
peso ≠ 100% · versão ausente · política de retenção ausente
integração Gupy não testada
```

### P0.5 — depois dos primeiros pilotos
```
Validador COMPLETO (score 0–100)   Controle básico de vazamento
Governança (aprovação gestor/compliance)
Monitoramento pós-publicação       Biblioteca por cargo/senioridade
```

O **Validador completo** do P0.5 acrescenta: qualidade da simulação (0–100), cobertura de competências, sinal de opção óbvia por piloto, tempo agressivo, diversidade de caminhos e alertas estatísticos pós-uso.

### P1 / P2 — para virar produto forte e depois incomparável
```
Fairness auditing avançado         Benchmark interno e de mercado
Validade preditiva                 Modo resposta aberta
Famílias equivalentes de cenários  Modelos certificados
Flywheel de dados (Nível 4 — memória institucional)
```

> Não tente nascer enterprise. O MVP é só o P0 real — modo múltipla escolha, bem feito, com governança mínima e integração confiável. **Tudo determinístico, sem IA.**

### Arquitetura — o "motor" é regra, não IA

```
1. Banco de modelos            7. Normalização por caminho
2. Editor de simulações        8. Validador por regras
3. Blueprint da avaliação      9. Versionamento imutável
4. Rubricas comportamentais   10. Relatório explicável (trilha)
5. Grafo de turnos/ramificações 11. Auditoria e LGPD
6. Motor de score determinístico 12. Integração Gupy
```

> O cálculo é barato, auditável e explicável: opção escolhida → nível de rubrica → pontos → normalização por caminho → score. Nenhuma etapa precisa de modelo de linguagem.

---

## 🔄 Status de Maturidade da Simulação (regra de produto)

A validade preditiva vira um status operacional que define o que cada simulação **pode** fazer:

```
Rascunho               → não publicável
Piloto                 → pode ranquear, mas NÃO eliminar
Calibrada              → pode recomendar entrevista
Validada internamente  → pode pesar mais na decisão
Expirada               → precisa recalibrar antes de reusar
Arquivada              → fora de uso
```

> Uma simulação só "sobe de nível" com evidência: piloto interno → calibração com colaboradores-referência → correlação com resultado real de contratação.

### O status controla o campo "Eliminação" (regra de interface)

A governança não pode ser só texto — vira trava na tela:

```
Status = Piloto                → campo "Eliminação" BLOQUEADO
Status = Calibrada             → "Eliminação" bloqueado; libera só
                                  recomendação de entrevista
Status = Validada internamente → "Eliminação" pode ser habilitado SOMENTE
                                  com aprovação gestor/compliance +
                                  canal de revisão ativo
```

---

## 🚫 O Que o Sistema NÃO Promete

Deixar isso explícito aumenta a confiança do comprador (e protege juridicamente):

```
Não usa IA para julgar o candidato — a nota é determinística (rubrica).
Não avalia texto livre no MVP — só alternativas pré-escritas.
Não é teste de QI, avaliação psicológica nem diagnóstico clínico.
  É uma avaliação situacional de julgamento profissional, baseada em
  cenários do cargo e rubricas comportamentais.
Não mede atendimento real no modo múltipla escolha.
Não substitui a entrevista humana.
Não recomenda eliminação automática por padrão.
Não garante ausência absoluta de viés.
Não trata o score como verdade objetiva.
Não compara candidatos avaliados em versões não calibradas.
Não publica simulação crítica sem blueprint, rubrica e versionamento.
```

---

## 🗃️ Modelo de Dados Mínimo

Para o time técnico interpretar o fluxo de forma única, estas são as entidades centrais:

```
Simulation              SimulationVersion       AuditLog
AssessmentBlueprint     CandidateAttempt        GupyIntegrationConfig
Competency              CandidateResponse        ReviewRequest
Rubric                  ScoreBreakdown
ScenarioNode            ScoringRule
CandidateOption         Branch
```

> Regra-chave: `CandidateAttempt` sempre aponta para uma `SimulationVersion` imutável. Editar gera nova versão — nunca altera a vigente.

---

## 🔌 Gupy — Casos de Borda da Integração

O Preflight valida a conexão; estes são os cenários que **não podem** derrubar a operação nem explodir o suporte:

```
Evento duplicado (idempotência)
Candidato removido da vaga
Candidato mudou de etapa antes de responder
Webhook recebido fora de ordem
Simulação expirada
Candidato abriu e não terminou
Resultado gerado, mas Gupy fora do ar (retry assíncrono)
Reenvio manual pelo admin
Desvincular simulação de uma vaga
```

---

## 🎯 O Salto

**Hoje:** "RH cria uma simulação ramificada e publica na Gupy."

**Ápice:** "RH escolhe uma competência crítica do cargo, o sistema valida a simulação, garante score comparável, aplica com governança, devolve evidências ao gestor e acumula evidência histórica de quais cenários se correlacionam com melhor contratação."

> A feature copiável é o editor de diálogo. O diferencial é a camada de **validade, calibração, justiça, integração e evidência** — e, acima de tudo, o **fosso de dados**: o banco proprietário de quais cenários preveem boas contratações, que só cresce com o uso e não se clona num fim de semana.

---

## 🧭 Tese de Foco

> **O MVP não precisa provar todo o fosso.**
> O MVP precisa provar que RH e gestor aceitam uma avaliação situacional estruturada antes da entrevista, com score comparável, relatório útil e integração operacional confiável.

E a visão de longo prazo:

> **Primeiro vendemos evidência comportamental antes da entrevista.**
> Depois acumulamos a evidência histórica de quais cenários e rubricas se correlacionam com boas contratações.

> Próximo passo do documento: parar de adicionar ideias. Agora é cortar, organizar e executar o P0 real.

---
---

# 📐 REQUISITO TÉCNICO — Especificação de Telas (P0)

> Esta seção detalha cada tela do produto com dados máximos para a engenharia: propósito, quem usa, campos, entidades/atributos, estados, ações, validações e casos de borda. Convenção de entidades segue o **Modelo de Dados Mínimo** acima.
>
> **Stack de referência do protótipo:** React + TanStack Router (file-based routing), Tailwind, shadcn/ui. Paleta azul-marinho sóbria (`--foreground` matiz ~264). Tudo determinístico, sem IA.

## 🚫 Regra Inegociável — IA no desenvolvimento ≠ IA no produto

Para evitar qualquer ambiguidade na hora de implementar:

- ✅ **PODE usar IA para CONSTRUIR o sistema** — Lovable, Claude, Copilot ou qualquer assistente escrevendo o código. Isso é ferramenta de desenvolvimento e **não aparece no produto final**.
- 🚫 **O PRODUTO EM RUNTIME NÃO TEM NENHUMA IA** — nenhuma chamada a LLM, nenhuma API de modelo de IA, nenhum consumo de tokens de IA, nenhum classificador de texto. Nada de IA processando dados de candidato, avaliando respostas, gerando texto ou pontuando.

```
Nenhuma das telas/regras abaixo deve gerar uma chamada a serviço de IA.
Onde o documento diz "sem IA", significa: NÃO INTEGRAR IA — usar regra,
cálculo, rubrica e estatística simples.

As palavras "token" que aparecem no Gupy Preflight referem-se ao TOKEN
DE API DA GUPY (autenticação), NÃO a tokens de IA.

Qualquer recurso que pareça exigir IA (avaliar texto livre, detectar
"opção óbvia" por leitura, gerar cenários/paráfrases) está FORA DO
ESCOPO ou resolvido por checklist manual / estatística pós-piloto.
```

Em caso de dúvida do desenvolvedor: **se a implementação precisaria chamar um modelo de IA para funcionar, a implementação está errada** — existe uma alternativa determinística no documento.



## ⚠️ Princípio de Arquitetura — Fronteira com a Gupy (LER PRIMEIRO)

**Regra de ouro:** *tudo que é consumo de resultado acontece DENTRO da Gupy. Fora dela só existe o que a Gupy não consegue fazer.* O objetivo da integração é o RH e o gestor **não precisarem sair da Gupy** no dia a dia.

A Gupy não oferece um construtor de simulações ramificadas nem renderiza o chat do teste — por isso essas duas coisas precisam ser tela própria. Todo o resto (ver nota, decidir avançar) acontece no painel nativo da Gupy.

### Classificação de cada superfície

| Superfície | Onde vive | Por quê |
|---|---|---|
| **Construtor (wizard de criação)** | 🟦 Tela própria | A Gupy não tem editor de simulação ramificada — é o motivo de existirmos |
| **Simulação que o candidato responde** | 🟦 Tela própria (redirect) | Candidato clica "Iniciar teste" na Gupy → redirect curto → volta pra Gupy. Padrão de todo teste parceiro |
| **Resultado do candidato (nota + competências)** | 🟩 Dentro da Gupy | Volta como tag/comentário/nota na candidatura. Gestor NÃO sai da Gupy |
| **Decisão do gestor (avançar/aprovar)** | 🟩 Dentro da Gupy | Acontece no fluxo nativo da vaga |
| **Configuração da integração** | 🟦 Tela própria (raro) | Setup inicial, feito 1x pelo admin |

### Decisão de produto sobre o gestor (Lucas)

> **Só a nota + competências voltam para a Gupy. NÃO há painel externo para o gestor.** Lucas vê tudo que precisa dentro da Gupy. Não existe tela `/resultado` obrigatória que o force a sair da plataforma.

Isso significa que a **Tela 12** (relatório do gestor) deixa de ser uma superfície de uso do gestor e vira: (a) o *payload* estruturado que retorna à Gupy, e (b) no máximo um link opcional de detalhe/auditoria — nunca o caminho principal.

---

## Mapa de Rotas

🟦 = tela própria (fora da Gupy) · 🟩 = consumido dentro da Gupy

```
🟦 /                     Painel/Construtor (uso da Renata — criação)
🟦 /nova/blueprint       Passo 0  — Blueprint da Avaliação
🟦 /nova/objetivo        Passo 1  — Objetivo + modelo
🟦 /nova/personagem      Passo 2  — Personagem fictício
🟦 /nova/dialogo         Passo 3  — Editor de Diálogo & Rubricas
🟦 /nova/validador       Passo 3.5 — Validador de Qualidade
🟦 /nova/piloto          Passo 4  — Piloto & Calibração
🟦 /nova/mapa            Passo 5  — Mapa & Score
🟦 /nova/governanca      Passo 7  — Governança de Publicação
🟦 /nova/gupy            Passo 8  — Gupy Preflight & Vínculo (setup)
🟦 /monitoramento        Passo 9  — Monitoramento (uso da Renata)
🟦 /candidato/:token     Fluxo do candidato (redirect a partir da Gupy)
🟩 RESULTADO             Nota + competências → retornam PARA DENTRO da Gupy
🟦 /auditoria            Log de auditoria (admin/auditor — back-office)
```

> A rota `/resultado/:attemptId` **deixa de ser tela de gestor**. Vira endpoint que monta o payload de retorno à Gupy + (opcional) página de auditoria interna, nunca o fluxo principal de decisão.



## Papéis e Permissões (RBAC)

| Papel | Cria/edita | Aprova | Publica | Vê resultado | Configura Gupy | Audita |
|---|---|---|---|---|---|---|
| Criador (RH) | ✅ | ❌ | só rascunho/piloto | na Gupy + back-office | ❌ | ❌ |
| Revisor técnico | comenta | ❌ | ❌ | na Gupy | ❌ | ❌ |
| Aprovador RH | ✅ | ✅ | ✅ | na Gupy + back-office | ❌ | ❌ |
| Aprovador gestor | ❌ | ✅ (na Gupy) | ❌ | **só na Gupy** | ❌ | ❌ |
| Admin integração | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ |
| Auditor | ❌ | ❌ | ❌ | detalhe read-only | ❌ | ✅ |

> "Vê resultado **na Gupy**" = a nota/competências aparecem na candidatura dentro da própria Gupy. O gestor **nunca precisa de uma tela externa**. "back-office" = a Renata acessa o sistema próprio só para manter as simulações, não para decidir candidatos.

---

## TELA 0 — Painel (Dashboard) · `/`

**Quem usa:** Renata (Criador/Aprovador RH).
**Propósito:** visão geral das simulações, seu status, qualidade e maturidade; ponto de partida para criar nova.

**Cabeçalho (hero):**
- Saudação personalizada ("Boa tarde, Renata.")
- Subtítulo com a tese: "Avaliação situacional estruturada para recrutamento, sem IA julgando o candidato. Score por rubrica, trilha auditável, integrada à Gupy."
- Botões: `Ver monitoramento` (secundário), `Nova simulação` (primário → `/nova/blueprint`)

**Cards de totais (KPIs):**
```
Publicadas | Em piloto | Rascunhos | Tentativas (soma de attempts)
```

**Tabela/grid de simulações** — cada linha = uma `Simulation`:
| Campo (entidade) | Tipo | Exemplo |
|---|---|---|
| `name` | string | "O Dia do Caos" |
| `role` | string | "Analista de Atendimento" |
| `seniority` | enum {Júnior, Pleno, Sênior} | "Pleno" |
| `status` | enum {rascunho, em-revisao, aprovada, publicada, piloto, arquivada} | "publicada" |
| `maturity` | enum {Rascunho, Piloto, Calibrada, Validada internamente, Expirada, Arquivada} | "Calibrada" |
| `version` | string semver-like | "v1.2" |
| `quality` | int 0–100 | 88 |
| `competencies` | string[] | ["Empatia", "Resolução de Conflitos"] |
| `attempts` | int | 142 |
| `completion` | float 0–1 | 0.91 |
| `updated` | relative time | "há 2 dias" |

**Badges de status (tom de cor):** rascunho=muted · em-revisão=warning · aprovada=info · publicada=success · piloto=info · arquivada=muted.

**Ações por linha:** Abrir · Duplicar (gera "Cópia de…") · Arquivar · Ver resultados.

**Estados de tela:** vazio (sem simulações → CTA grande "Criar primeira simulação"); carregando (skeleton de linhas); erro (banner).

**Casos de borda:** simulação `Expirada` aparece com selo de alerta "recalibrar"; simulação com candidatos em andamento exibe cadeado de edição.

---

## TELA 1 — Blueprint da Avaliação · `/nova/blueprint` (Passo 0)

**Quem usa:** Criador (RH).
**Propósito:** definir POR QUE a avaliação é relevante antes de criar qualquer diálogo. Alimenta o Validador.
**Entidade:** `AssessmentBlueprint` (1:1 com `Simulation`).

**Campos:**
| Campo | Tipo | Obrigatório | Regras |
|---|---|---|---|
| `targetRole` | string | ✅ | cargo-alvo |
| `seniority` | enum {Júnior, Pleno, Sênior} | ✅ | radio |
| `criticalSituation` | textarea | ✅ | situação real do cargo |
| `competencies` | ref[] → `Competency` | ✅ | mín. 1, vêm da taxonomia |
| `observableBehaviors` | textarea | ✅ | comportamentos de alta performance |
| `criticalErrors` | textarea/list | opcional | erros que disparam **revisão humana** (NÃO zeram) |
| `companyPolicy` | string | opcional | política envolvida |
| `expectedAutonomy` | enum {baixa, média, alta} | opcional | |
| `behaviorBySeniority` | {junior, pleno, senior: string} | opcional | diferenciação |
| `resultUse` | enum {Triagem, Ranking, Apoio à entrevista, Eliminação🔒} | ✅ | **Eliminação travada** — só p/ simulação Validada + aprovação + canal de revisão |

**Regra crítica de UI:** o radio "Eliminação" nasce **desabilitado** (cadeado). Só habilita se `maturity = Validada internamente` E aprovação gestor/compliance E canal de revisão ativo.

**Ações:** `Cancelar` · `Criar simulação →` (cria `Simulation` em status `rascunho` + `SimulationVersion` v0.1).

**Validações:** bloqueia avançar se faltar campo obrigatório; competências devem existir na taxonomia.

---

## TELA 2 — Objetivo + Modelo · `/nova/objetivo` (Passo 1)

**Quem usa:** Criador (RH).
**Propósito:** escolher o que avaliar (por objetivo, não só por área) e o ponto de partida.

**Seção A — Objetivo de avaliação (chips selecionáveis):**
```
Lidar com conflito · Priorizar sob pressão · Negociar sem dar desconto
Comunicar uma negativa · Recuperar cliente em risco · Escalar problema
```

**Seção B — Começar de:**
- `📋 Modelo pronto` → abre biblioteca de modelos pré-definidos (cada um carrega blueprint + diálogo editáveis)
- `➕ Do zero`

**Seção C — Dificuldade (CALCULADA, read-only):** exibe as 5 dimensões e o resultado calculado:
```
Intensidade emocional · Ambiguidade da informação · Risco de negócio
Conflito empatia × política · Autonomia exigida
→ Dificuldade calculada: [INTENSO]  (nunca escolhida manualmente)
```

**Entidades:** atualiza `Simulation.objective`, `Simulation.difficultyVector{}`, `Simulation.difficultyCalculated`.

**Ação:** `← Voltar` · `Próximo →` (`/nova/personagem`).

---

## TELA 3 — Personagem Fictício · `/nova/personagem` (Passo 2)

**Quem usa:** Criador (RH).
**Propósito:** definir o personagem que o candidato vai atender.

**Campos:**
| Campo | Tipo | Obrigatório |
|---|---|---|
| `character.name` | string | ✅ |
| `character.initialEmotion` | enum {Tranquilo, Frustrado, Furioso} | ✅ |
| `character.problemContext` | textarea (visível só RH/gestor) | ✅ |
| `character.channel` | enum {Chat, E-mail, WhatsApp} | ✅ |

**Componente especial — Checklist de linguagem (manual, sem IA):**
Botão `Abrir checklist de linguagem` → modal com itens de confirmação:
```
□ Evita regionalismo desnecessário?
□ Evita estereótipo de classe?
□ Evita marcador de gênero sem necessidade?
□ Evita idade, sotaque, origem ou crença?
□ Usa linguagem compatível com o cargo?
```
O RH marca cada item. Resultado salvo em `Simulation.languageChecklist{}`. Nenhuma análise automática.

**Ação:** `← Voltar` · `Montar diálogo →`.

---

## TELA 4 — Editor de Diálogo & Rubricas · `/nova/dialogo` (Passo 3)

**Quem usa:** Criador (RH). **A tela central.**
**Entidades:** `ScenarioNode` (turno), `CandidateOption`, `Branch`, `Rubric`, `ScoringRule`.

**Estrutura de um turno (`ScenarioNode`):**
| Campo | Tipo | Regra |
|---|---|---|
| `id` | uuid | |
| `turnIndex` | int | 1..N (N ≤ 10) |
| `clientMessage` | string | máx. 300 caract.; contador visível |
| `timeLimitSec` | enum {10,20,30,60,sem limite} | exige justificativa textual |
| `timeJustification` | string | obrigatório se houver limite |
| `options` | `CandidateOption`[] | 2 a 4 por turno |

**Estrutura de uma opção (`CandidateOption`):**
| Campo | Tipo | Regra |
|---|---|---|
| `label` | char {A,B,C,D} | |
| `text` | string | máx. 160 caract. (mobile) |
| `rubricLevels` | map<competency, level 0–3> | nível por rubrica, não número solto |
| `isBest` | bool | marca a melhor (validação) |
| `isCritical` | bool | erro crítico → revisão humana |
| `nextNode` | ref `ScenarioNode` \| FIM | ramificação |
| `resultingTone` | enum {mais calmo, neutro, mais furioso, furioso extremo} | |

**Regra de ouro (exibida na tela):** *todas as alternativas devem ser plausíveis — sem opção obviamente certa.*

**Exemplo real (do documento):**
```
A · "Muito empática, mas promete estorno SEM validar política"  → crítico
B · "Segue o processo corretamente, mas soa fria"
C · "Acolhe, coleta dados mínimos e explica o próximo passo"     → melhor
D · "Resolve rápido, mas ignora o registro no sistema"
```

**Painel lateral — Rubrica:** mostra a tabela de níveis 0–3 por competência (ex.: Empatia nível 0=ignora emoção … nível 3=acolhe+prioriza+reduz tensão). Nota: "O ponto vem do nível, não de número inventado. Alinhado a SIOP/APA e ISO 10667."

**Ações:** `+ opção` (até 4) · `+ turno` (até 10) · `← Voltar` · `Definir ramificação` · `Configurar pontuação` · `Validar →`.

**Validações inline:** opção sem rubrica; turno sem `isBest`; texto acima do limite; tempo sem justificativa.

---

## TELA 5 — Validador de Qualidade · `/nova/validador` (Passo 3.5)

**Quem usa:** Criador/Aprovador.
**Propósito:** diagnóstico determinístico (sem IA) antes de publicar.

**Score de qualidade (fórmula fixa 0–100):**
```
Estrutura do grafo (sem caminho morto):  20
Cobertura de competências:               20
Equilíbrio de score por caminho:         20
Rubricas completas:                      15
Governança / versionamento:              10
Fluxo do candidato:                      10
Integração / preflight:                   5
─────────────────────────────────────────────
Total:                                  100
```
Cada item exibe barra (verde ≥80%, amarelo ≥60%, vermelho <60%).

**Diagnóstico (lista ✓ / ! / ✕):**
- ✓ OK · ! Warning · ✕ Blocker

**Regras de Blocker × Warning:**
```
🚫 BLOQUEIA (sem botão de publicar):
   caminho sem desfecho · rubrica ausente · score máx. desigual por caminho
   peso ≠ 100% · versão ausente · política de retenção ausente
   integração Gupy não testada · erro crítico sem regra de revisão

⚠️ PERMITE publicar com alerta registrado:
   opção possivelmente óbvia · tempo agressivo · baixa diversidade de caminhos
   sem validade preditiva ainda · biblioteca sem variações equivalentes
```

**Botões condicionais (REGRA — corrigir no protótipo):**
```
Se há BLOCKER:    [ Corrigir agora ]  [ Salvar rascunho ]   ← SEM avançar
Só com WARNING:   [ Corrigir ]  [ Publicar com alerta registrado ]
```
Publicar com warning grava em `AuditLog`: "Usuário X publicou com warnings Y/Z em [data/hora]".

**Recomendação fixa para simulação nova:** "Publicar como PILOTO, não eliminatória."

---

## TELA 6 — Piloto & Calibração · `/nova/piloto` (Passo 4)

**Quem usa:** Criador/Aprovador.
**Propósito:** rodar com referência antes de vaga real.

**Checklist de piloto:**
```
□ Testar com 3 colaboradores referência (alta performance)
□ Testar com 3 pessoas de fora da área
□ Comparar distribuição de respostas
□ Ajustar alternativas que ficaram óbvias
□ Calibrar tempo por turno com usuários reais
□ Calibrar pesos das competências
□ Aprovar versão 1.0
```

**Painel de distribuição (após coleta):** histograma de escolhas por turno; alerta automático se ≥90% escolhem a mesma opção em tempo baixo → flag "opção óbvia".

**Regra:** referência deve tirar nota alta; se não tirar, a simulação mede a coisa errada.

**Ação:** `Salvar calibração` · avança maturidade `Piloto → Calibrada` (com aprovação).

---

## TELA 7 — Mapa & Score · `/nova/mapa` (Passo 5)

**Quem usa:** Criador/Aprovador.
**Propósito:** visão em árvore do fluxo + verificação de equilíbrio do score.

**Visual:** árvore de `ScenarioNode` → `CandidateOption` → próximos nós/FIM. Nós com erro crítico marcados `⚠️ revisão humana`.

**Métricas exibidas:**
```
Turnos · Opções totais · Caminhos completos (n/total)
Score máximo por caminho (deve ser igual entre caminhos)
Tempo total = SOMA dos tempos por turno (calculado, sem campo manual)
```

**Fórmula de score (exibida para transparência):**
```
score_competência = pontos_obtidos / pontos_possíveis_naquele_caminho
score_final = Σ (competência_normalizada × peso_competência)
Σ pesos = 100%
```

**Alerta:** caminhos sem desfecho (blocker) sinalizados antes de permitir publicar.

---

## TELA 8 — Governança de Publicação · `/nova/governanca` (Passo 7)

**Quem usa:** Aprovador RH / gestor / compliance.
**Entidades:** `Simulation.status`, `ReviewRequest`, `AuditLog`.

**Pipeline de estados (visual de fluxo):**
```
Rascunho → Em revisão (gestor) → Em revisão (compliance) → Aprovada
        → Publicada → Arquivada     ↘ Reprovada por baixa qualidade
```

**Trava de "Eliminação" ligada à maturidade (regra de interface):**
```
Piloto                → campo "Eliminação" BLOQUEADO
Calibrada             → bloqueado; libera só recomendação de entrevista
Validada internamente → habilita Eliminação SOMENTE com aprovação
                        gestor/compliance + canal de revisão ativo
```

**Versionamento imutável:**
```
CandidateAttempt sempre aponta para uma SimulationVersion imutável
Editar → cria nova versão (typo = minor; mudança de pontuação = major)
Resultados só comparáveis dentro da mesma versão (ou versões calibradas)
```

**Ações:** `Solicitar revisão` (cria `ReviewRequest`) · `Aprovar` · `Reprovar` · `Publicar` (respeita blocker do validador).

---

## TELA 9 — Gupy: Ativação & Conferência · `/nova/gupy` (Passo 8)

**Quem usa:** Admin integração + Aprovador RH.
**Entidade:** `GupyIntegrationConfig`.

> ⚠️ **Como a integração realmente funciona** (doc oficial): nós expomos 3 endpoints REST (`GET /test`, `POST /test/candidate`, `GET /test/result/{resultId}`) e **a Gupy nos chama**. A ativação não é self-service: é preciso **enviar à Gupy a URL da nossa API + um token por empresa** e a Gupy habilita o parceiro. Não há ambiente de sandbox — valida-se numa vaga real não-listada, com o cliente.

**Checklist de ativação/conferência:**
```
□ Os 3 endpoints implementados e no ar (GET /test, POST /test/candidate,
  GET /test/result/{resultId})
□ Token Bearer por empresa configurado
□ GET /test devolve nossas simulações PUBLICADAS como Test[]
□ POST /test/candidate cria o attempt e devolve test_url + test_result_id
□ callback_url tratada (GET ao finalizar → redireciona candidato à Gupy)
□ result_webhook_url tratada (POST assíncrono do TestResult — com retry/DLQ)
□ TestResult no formato aceito (score inteiro 0–100, tier major/minor)
□ Validação feita COM o cliente em vaga não-listada (não há sandbox)
```

**Vínculo à vaga (feito pelo cliente DENTRO da Gupy):**
- O cliente cria a etapa de teste na vaga e seleciona uma das nossas simulações (vindas do `GET /test`).
- A escolha da etapa onde o teste aparece é configuração da Gupy, não nossa.

**Registro de resultado:** retorna via `TestResult` — `results[]` (nota por competência) + `company_result_string` (Markdown só p/ empresa). Tudo aparece **dentro da Gupy**.

**Casos de borda (obrigatórios):**
```
Chamada duplicada de /test/candidate → idempotência por (company_id,
  document_id, test_id): devolve o mesmo test_result_id/test_url
Candidato abriu e não terminou → status "paused"/"notStarted" no TestResult
callback falhou (rede/aba fechada) → garantir entrega via POST no result_webhook_url
result_webhook_url fora do ar → DLQ + retry exponencial + reenvio manual (admin)
previous_result = "fail" → recandidatura; tratar conforme política da vaga
candidate_type internal/external → pode mudar regra de fluxo
```

---

## TELA 10 — Monitoramento Pós-Publicação · `/monitoramento` (Passo 9)

**Quem usa:** Renata (RH) — back-office de quem **mantém** as simulações. **Não é tela do gestor.**
**Propósito:** acompanhar a saúde das simulações em produção (qualidade do instrumento), não a decisão de candidatos — essa fica na Gupy.

> Distinção importante: aqui o RH cuida do **teste** (está funcionando? vazou? o tempo está bom?). A decisão sobre **candidatos** acontece dentro da Gupy. Esta tela não duplica a candidatura.

**Métricas:**
```
Taxa de conclusão · Tempo médio por turno · Alternativas mais escolhidas
Drop-off por dispositivo · Distribuição de score · Caminhos mais frequentes
Revisões humanas por erro crítico · Comparação score × decisão do gestor
Correlação score × entrevista → métrica EXPLORATÓRIA até haver amostra
Sinal de vazamento: queda brusca de tempo + alta anormal de escolhas de
  alto score + concentração incomum nos mesmos caminhos
```

**Alertas:** vazamento suspeito; simulação expirando; drop-off alto em mobile.

---

## TELA 11 — Fluxo do Candidato · `/candidato/:token` (app público)

**Quem usa:** Candidato (Thiago). **Mobile First.**
**Entidades:** `CandidateAttempt`, `CandidateResponse`, `CandidateSession`.

> **Entrada e saída são via Gupy (redirect real):** o candidato clica "Iniciar teste" **na Gupy** → a Gupy chama nosso `POST /test/candidate` → devolvemos `test_url` → o candidato é redirecionado para `test_url` (esta tela). Ao finalizar, fazemos `GET` no `callback_url` e **redirecionamos o candidato de volta à Gupy**, que mostra o resultado dele lá. O `test_url` é o `/candidato/:token`.

**Sequência de telas:**

**11.1 — Abertura (já dentro do nosso app, pós-redirect):**
- Nome da simulação, tempo estimado, nº de turnos
- Aviso de transparência: o que é avaliado e uso dos dados (LGPD)
- Botão `Começar` · link de acessibilidade (tempo estendido p/ PCD)

**11.2 — Treino (aquecimento):** 1 turno de exemplo que não conta para o score.

**11.3 — Turno de simulação:**
- Mensagem do cliente fictício (estilo chat/WhatsApp)
- Cronômetro do turno (se houver `timeLimitSec`)
- 2–4 botões de opção (`CandidateOption.text`)
- Ao escolher → grava `CandidateResponse{optionId, timeSpentMs, timestamp}` → próximo nó conforme `Branch`

**11.4 — Encerramento + redirect de volta à Gupy:**
- Feedback leve baseado nos itens `tier: major` ("alta Empatia, boa Resolução") — sem nota exata
- Dispara `GET callback_url` (registra conclusão) **e** garante o resultado via `POST result_webhook_url` (TestResult)
- **Redireciona o candidato de volta à Gupy**, onde ele vê a página de resultado dele (itens `major`)
- Se o redirect falhar, o `result_webhook_url` assíncrono garante que a nota chega mesmo assim

**Comportamentos obrigatórios:**
```
Mobile First · funciona em conexão lenta/aparelho fraco
Queda de internet → retoma do último turno salvo (sessão persistida)
Timer esgota → registra "sem resposta" e avança (não trava)
Pausa: configurável por cargo
Acessibilidade: tempo estendido, alto contraste, leitor de tela
Canal de revisão/contestação (LGPD art. 20)
```

**Campos textuais:** se existirem, são **informativos** e NÃO alteram o score (MVP sem IA).

---

## TELA 12 — Resultado: Payload de Retorno à Gupy (NÃO é painel de gestor)

> **Revisão de escopo:** o gestor (Lucas) **não usa uma tela externa**. Tudo que ele precisa **volta para dentro da Gupy**. Esta seção descreve (a) o que retorna à Gupy e (b) uma página de detalhe/auditoria **opcional**, nunca o fluxo principal de decisão.

**Quem consome:** o gestor, **dentro da Gupy** (via tag/comentário/nota na candidatura). A página `/resultado/:attemptId` existe só como link opcional de detalhe ou para auditoria interna.
**Entidades:** `ScoreBreakdown`, `CandidateAttempt`, `SimulationVersion`.

### (a) O que retorna PARA DENTRO da Gupy (essencial)

Conforme a capacidade da integração do cliente (tag, comentário, nota ou etapa):
```
Score final: 78/100 — recomenda entrevista
Competências: Empatia 85% · Resolução 80% · Aderência 70%
Nível de evidência: MÉDIO
Flag: nenhum erro crítico  (ou: ⚠️ erro crítico → revisão humana)
Versão avaliada: v1.2
```
> O gestor lê isso na candidatura, decide avançar **sem sair da Gupy**.

### (b) Página de detalhe — OPCIONAL (link, não fluxo principal)

Só abre se o gestor clicar num link "ver detalhe/trilha". Útil para auditoria e contestação (LGPD). Contém:

**Cabeçalho:** Candidato [UUID] · Score 78/100 · Nível de evidência MÉDIO · Versão v1.2 (imutável)

**Breakdown com evidência:**
```
Empatia ............ 85%  ▸ "Acolheu no T1, reduziu tensão no T3"
Resolução .......... 80%  ▸ "Coletou dados mínimos antes de agir"
Aderência política . 70%  ▸ "Quase prometeu estorno indevido (T2)"
```

**Trilha de pontuação (determinística, auditável):**
```
T1 → opção C → Empatia nível 3 (10pts) + Aderência nível 2 (7pts)
caminho máx. Empatia = 30 → score Empatia = 25/30 = 83,3%
```

**Avisos:** erro crítico (se houver) · "o score recomenda, nunca reprova sozinho".

> ⚠️ **Não** existem botões "Aprovar para entrevista" nesta página — essa ação é da Gupy. A página externa é read-only (detalhe/auditoria).

---

## TELA 13 — Log de Auditoria · `/auditoria`

**Quem usa:** Auditor (read-only), Admin.
**Entidade:** `AuditLog`.

**Colunas:**
```
Timestamp · Ator (usuário/papel) · Ação · Entidade · Versão
Detalhe (ex.: "publicou com warnings Y/Z") · IP/origem
```

**Eventos registrados (mínimo):**
```
Criação/edição de simulação      Mudança de versão (minor/major)
Publicação (com/sem warnings)    Aprovação/reprovação
Erro crítico → revisão humana    Quem viu cada resultado
Mudança de status de maturidade  Config de integração Gupy
Reenvio manual de resultado      Pedido de revisão do candidato
```

**Imutável:** registros não podem ser editados nem apagados.

---

## Critérios de Aceite Globais (P0)

```
✓ Nenhuma tela usa IA/LLM EM RUNTIME — todo cálculo é determinístico
  (IA pode ter sido usada para escrever o código; o produto rodando não a chama)
✓ Zero chamadas a serviço de IA, zero consumo de token de IA no produto
✓ CandidateAttempt sempre referencia SimulationVersion imutável
✓ Validador bloqueia publicação com qualquer blocker
✓ Campo "Eliminação" só habilita em simulação Validada + aprovação + revisão
✓ Erro crítico dispara revisão humana, nunca eliminação automática
✓ Score normalizado por caminho (tetos iguais entre caminhos)
✓ Fluxo do candidato persiste sessão (sobrevive a queda de internet)
✓ Integração Gupy com idempotência, DLQ e retry
✓ Toda ação relevante gera AuditLog imutável
✓ Mobile First no fluxo do candidato
✓ Nível de evidência (Baixo/Médio/Alto) no lugar de intervalo de confiança
✓ Gestor NÃO usa tela externa — nota + competências voltam para dentro da Gupy
✓ Telas próprias só existem para o que a Gupy não faz: construtor e chat do candidato
```

## Fora de Escopo do P0 (recap)

```
Resposta aberta / justificativa avaliada automaticamente
Análise semântica de "opção óbvia" por IA
Geração automática de cenários ou paráfrases
Fairness auditing avançado · Validade preditiva comprovada
Benchmark de mercado · Modelos certificados · Simulações adaptativas
Flywheel de dados (Nível 4)
```

---
---

# 🔧 CONTRATO TÉCNICO EXECUTÁVEL (P0) — Backend Spring Boot

> Esta seção fecha as lacunas que faltavam para a implementação: contratos de dados (JSON), máquina de estados, fórmulas exatas do score, regras de grafo, integração Gupy, tempo, versionamento, monitoramento e fluxo de erro. **Nada de IA em runtime** — tudo determinístico.
> Convenções: IDs = UUID v4; timestamps = ISO-8601 UTC; dinheiro/score = inteiros ou decimais explícitos; nomes de campo em `camelCase` no JSON.

## 1. Contrato de Dados (entidades e payloads)

### 1.1 Entidades persistidas (modelo relacional)

```
Tenant            (id, name, gupyApiPlan, createdAt)
User              (id, tenantId, name, role[CRIADOR|REVISOR|APROVADOR_RH|
                   APROVADOR_GESTOR|ADMIN_INTEGRACAO|AUDITOR])
Competency        (id, tenantId, name, taxonomyKey, customLabel?)
Rubric            (id, competencyId, levels[ {level:0..3, descriptor, points} ])
Simulation        (id, tenantId, name, role, seniority, objective,
                   difficultyVector{}, difficultyCalculated, maturity, createdBy)
SimulationVersion (id, simulationId, semver, immutable=true, status,
                   blueprintId, graphRootNodeId, weights{}, createdAt, publishedAt?)
AssessmentBlueprint(id, simulationVersionId, targetRole, seniority,
                   criticalSituation, competencyIds[], observableBehaviors,
                   criticalErrors[], companyPolicy?, expectedAutonomy?,
                   behaviorBySeniority{}, resultUse[TRIAGEM|RANKING|APOIO|ELIMINACAO])
ScenarioNode      (id, simulationVersionId, turnIndex, clientMessage,
                   timeLimitSec?, timeJustification?, isTerminal)
CandidateOption   (id, scenarioNodeId, label[A|B|C|D], text,
                   isBest, isCritical, nextNodeId?, resultingTone)
ScoringRule       (id, candidateOptionId, competencyId, rubricLevel) -- nível, não pontos soltos
CandidateAttempt  (id, simulationVersionId, candidateRef, gupyApplicationId,
                   status, startedAt, finishedAt?, idempotencyKey)
CandidateResponse (id, attemptId, scenarioNodeId, chosenOptionId?,
                   timeSpentMs, answeredAt, timedOut=false)
ScoreBreakdown    (id, attemptId, finalScore, evidenceLevel[BAIXO|MEDIO|ALTO],
                   perCompetency[], hadCriticalError, computedAt)
GupyIntegrationConfig(id, tenantId, jobId, triggerStep, resultTarget,
                   contractVersion, tokenRef)
ReviewRequest     (id, attemptId|simulationVersionId, reason, status, createdBy)
AuditLog          (id, tenantId, actorId, action, entity, entityId, version,
                   detail, ip, createdAt) -- imutável (append-only)
DeadLetter        (id, payloadJson, targetJobId, attempts, lastError, nextRetryAt)
```

### 1.2 Payload do candidato (sessão → frontend do teste)

Gerado ao iniciar o attempt. **Não contém gabarito, pesos, isBest nem isCritical** (segurança — o candidato não pode inferir a resposta certa).

```json
{
  "attemptId": "uuid",
  "simulationVersionId": "uuid",
  "candidateRef": "uuid-anon",
  "totalTurnsEstimated": 6,
  "node": {
    "nodeId": "uuid",
    "turnIndex": 1,
    "clientMessage": "Chegou QUEBRADO! Quero meu dinheiro AGORA!",
    "timeLimitSec": 30,
    "options": [
      { "optionId": "uuid", "label": "A", "text": "Peço desculpas..." },
      { "optionId": "uuid", "label": "B", "text": "Preciso do seu CPF..." },
      { "optionId": "uuid", "label": "C", "text": "Acolho e coleto dados..." },
      { "optionId": "uuid", "label": "D", "text": "Resolvo rápido..." }
    ]
  }
}
```

### 1.3 Submissão de resposta (frontend → backend)

```json
{
  "attemptId": "uuid",
  "nodeId": "uuid",
  "chosenOptionId": "uuid",
  "timeSpentMs": 8400,
  "clientTimestamp": "2026-06-15T14:03:21Z"
}
```
Resposta do backend = o próximo nó (mesmo formato de 1.2) ou `{ "finished": true }`.

### 1.4 ScoreBreakdown (resultado calculado)

```json
{
  "attemptId": "uuid",
  "simulationVersionId": "uuid",
  "finalScore": 78,
  "evidenceLevel": "MEDIO",
  "hadCriticalError": false,
  "perCompetency": [
    { "competency": "Empatia", "raw": 25, "max": 30, "normalized": 0.833, "weight": 0.40 },
    { "competency": "Resolucao", "raw": 18, "max": 22, "normalized": 0.818, "weight": 0.35 },
    { "competency": "Aderencia", "raw": 9,  "max": 14, "normalized": 0.643, "weight": 0.25 }
  ],
  "path": ["nodeId1","nodeId2","nodeId5"],
  "computedAt": "2026-06-15T14:09:55Z"
}
```

### 1.5 Payload de resultado — INTERNO (nosso) vs. Gupy

Este é o nosso payload **interno** (ScoreBreakdown enriquecido). O formato que **realmente vai para a Gupy** é o `TestResult` — ver §5.3, que é o contrato oficial. Mantemos este interno para o relatório/auditoria e o convertemos para `TestResult` no envio.

```json
{
  "attemptId": "uuid",
  "gupyDocumentId": 4398157034,
  "gupyJobId": 100,
  "gupyCompanyId": 1,
  "testResultId": "uuid",
  "result": {
    "score": 78,
    "recommendation": "RECOMENDA_ENTREVISTA",
    "evidenceLevel": "MEDIO",
    "competencies": [
      { "name": "Empatia", "percent": 83 },
      { "name": "Resolucao", "percent": 82 },
      { "name": "Aderencia", "percent": 64 }
    ],
    "hadCriticalError": false,
    "simulationVersion": "v1.2",
    "detailUrl": "https://praxis.iforce.com.br/resultado/uuid"
  }
}
```
`recommendation` ∈ {RECOMENDA_ENTREVISTA, NEUTRO, REVISAO_HUMANA}. Nunca "REPROVADO" automático. **Conversão para a Gupy:** `score`→item `major`; cada competência→`TestResultItem`; `recommendation`/trilha→`company_result_string`. Ver §5.3.

## 2. Regras Exatas do Motor de Score (fechamento matemático)

```
Para cada competência c presente no CAMINHO percorrido pelo candidato:
  raw(c)  = Σ pontos das opções escolhidas que mapeiam para c
            (pontos = Rubric.levels[nível].points definido na ScoringRule)
  max(c)  = Σ MAIOR pontuação possível em c, nó a nó, AO LONGO DO MESMO CAMINHO
            (para cada nó do caminho, a opção de maior ponto naquela competência)
  norm(c) = raw(c) / max(c)        // 0..1 ; se max(c)=0 → competência ignorada (não entra)

finalScore = round( Σ ( norm(c) × weight(c) ) × 100 )
  onde Σ weight(c) = 1.00 considerando apenas as competências com max(c) > 0
  (renormalizar pesos se alguma competência não apareceu no caminho)
```

**Regras de borda do score:**
```
- Empate entre candidatos: permitido. Desempate NÃO é automático — fica para
  o gestor (na Gupy). O sistema não ranqueia "vencedor".
- Competência sem ocorrência no caminho (max=0): excluída e pesos renormalizados.
- Erro crítico (isCritical) escolhido: NÃO zera o score. Seta hadCriticalError=true
  e recommendation=REVISAO_HUMANA, independentemente do número.
- Score é sempre relativo ao caminho: dois caminhos têm o mesmo teto (100).
- Arredondamento: half-up, 0 casas no finalScore; 3 casas em normalized.
```

**Justificativa anti-injustiça:** como `max(c)` é calculado sobre o mesmo caminho que o candidato percorreu, caminhos curtos e longos têm o mesmo teto possível (100%). Ninguém é penalizado por cair num ramo mais curto.

## 3. Máquina de Estados (state machine fechada)

### 3.1 Estados da SimulationVersion

```
DRAFT → IN_REVIEW → APPROVED → PUBLISHED → ARCHIVED
   ↘ REJECTED (de IN_REVIEW)
PUBLISHED → EXPIRED (quando contexto do cargo muda / validade vence)
```

### 3.2 Maturidade (eixo paralelo, independente do status)

```
RASCUNHO → PILOTO → CALIBRADA → VALIDADA_INTERNAMENTE → EXPIRADA → ARQUIVADA
```

### 3.3 Estados do CandidateAttempt

```
CREATED → RUNNING → COMPLETED → SCORED → SENT_TO_GUPY
   ↘ ABANDONED (sessão expirou sem concluir)
   ↘ EXPIRED (link expirou antes de iniciar)
SCORED → PENDING_HUMAN_REVIEW (se hadCriticalError) → REVIEWED
SENT_TO_GUPY → DELIVERY_FAILED → (DLQ) → SENT_TO_GUPY (após retry)
```

## 4. Regras de Transição (quem pode o quê)

| De → Para | Quem | Pré-condição |
|---|---|---|
| DRAFT → IN_REVIEW | Criador | validador sem blocker |
| IN_REVIEW → APPROVED | Aprovador RH/gestor | revisão concluída |
| IN_REVIEW → REJECTED | Aprovador | — |
| APPROVED → PUBLISHED | Aprovador RH | Preflight Gupy OK |
| PUBLISHED → EXPIRED | Aprovador RH / job | validade ou decisão |
| qualquer → ARCHIVED | Aprovador RH | sem attempts RUNNING |
| PILOTO → CALIBRADA | Aprovador | volume mínimo + aprovação |
| CALIBRADA → VALIDADA | Aprovador gestor + compliance | correlação com resultado real |

**Travas:**
```
- Editar SimulationVersion PUBLISHED ou com attempts RUNNING → PROIBIDO.
  Edição cria nova versão (ver §8).
- "Eliminação" no blueprint só habilita se maturity=VALIDADA_INTERNAMENTE
  + aprovação gestor/compliance + canal de revisão ativo.
- Publicar com blocker no validador → PROIBIDO (sem botão).
- Publicar com warning → permitido + AuditLog obrigatório.
```

## 5. Contrato de Integração Gupy (detalhe operacional)

> ⚠️ **ATUALIZADO conforme a documentação oficial da Gupy** ("Integração com testes de provedores externos"). A integração **não** usa um webhook `application.moved` genérico — o parceiro (nós) **implementa 3 endpoints REST que a Gupy chama**. Esquece a ideia de "nós chamamos a Gupy" no fluxo de teste: é a Gupy que chama a nossa API.

### 5.1 Os 3 endpoints que NÓS implementamos (a Gupy consome)

Todos autenticados com `Authorization: Bearer <apiKey>` (um token por empresa cliente).

```
GET  /test
   Lista as simulações que oferecemos àquela empresa.
   Query: searchString? · offset? · limit (máx 400)
   Retorna TestItems { limit, offset, total_tests, payload: [Test] }
   Test = { id, name, category?, description?, level[advanced|intermediate|basic] }
   → mapear: cada Simulation PUBLICADA = um Test. id = simulationId.

POST /test/candidate
   A Gupy registra o candidato e nos manda os dados.
   Body (BodyCandidateRegistration):
     name, email, document_id, test_id, company_id, job_id,
     callback_url (p/ redirecionar o candidato de volta à Gupy),
     candidate_type[internal|external], previous_result[fail|null],
     result_webhook_url (POST assíncrono do resultado)
   Retorna 201 CandidateRegistrationResponse {
     test_result_id,   ← nosso ID do resultado (a Gupy guarda p/ buscar depois)
     test_url          ← link da simulação que o candidato vai abrir
   }
   → aqui criamos o CandidateAttempt e devolvemos a URL do chat.

GET  /test/result/{resultId}
   A Gupy busca o resultado quando o candidato termina.
   Retorna 200 TestResult (ver 5.3).
```

### 5.2 Callback + webhook de resultado (obrigatórios)

```
callback_url:        ao FINALIZAR o teste, NÓS fazemos um GET nessa URL para
                     registrar a conclusão e redirecionar o candidato de volta à Gupy.
result_webhook_url:  para garantir entrega mesmo se o callback falhar (queda de rede,
                     candidato fechou a aba), NÓS fazemos um POST assíncrono com o
                     mesmo payload TestResult. URL pública, SEM autenticação.
```
> É exatamente aqui que entra nossa política de **retry exponencial + DLQ**: se o callback falhar, o POST assíncrono no `result_webhook_url` garante que a nota chega.

### 5.3 TestResult — formato EXATO que a Gupy aceita

A Gupy só suporta **2 formatos: numérico único e numérico múltiplo**. Nosso score por competência usa o **múltiplo**.

```json
{
  "title": "O Dia do Caos — Atendimento",
  "testCode": "praxis_atendimento_v1_2",
  "status": "done",                          // notStarted | paused | done
  "providerName": "Práxis (iForce)",
  "providerLink": "https://praxis.iforce.com.br",
  "company_result_string": "**Score 78/100.** Trilha: T1→C (Empatia N3)...",  // Markdown SÓ p/ empresa
  "result_page_url": "https://praxis.iforce.com.br/resultado/uuid",        // visão recrutadora
  "result_candidate_page_url": "https://praxis.iforce.com.br/fb/uuid",     // visão candidato
  "results": [
    { "tier": "major", "score": 78, "type_result": "percentage", "title": "Score geral",
      "description": "Julgamento situacional no cenário do cargo" },
    { "tier": "major", "score": 85, "type_result": "percentage", "title": "Empatia" },
    { "tier": "minor", "score": 82, "type_result": "percentage", "title": "Resolução de Conflitos" },
    { "tier": "minor", "score": 64, "type_result": "percentage", "title": "Aderência à política" }
  ]
}
```

**Mapeamento do nosso modelo → TestResult:**
```
finalScore                → results[0] com tier "major", title "Score geral"
cada competência          → um TestResultItem (score = percent inteiro 0–100)
trilha de pontuação +     → company_result_string (Markdown, só a empresa vê)
  evidências + erro crítico
score: SEMPRE inteiro 0–100 (percentage). Se internamente for 0–10, multiplicar por 10.
```

### 5.4 Controle de visão candidato × empresa (via `tier`)

```
tier "major" → aparece para CANDIDATO **e** empresa
tier "minor" → aparece SÓ para a empresa
```
> Uso no Práxis: marcamos como `major` só o que vira o **feedback leve do candidato** (ex.: "Score geral", "Empatia"). Competências sensíveis ou a aderência à política ficam `minor` — só a empresa vê. Isso entrega a tela de feedback do candidato **sem expor o gabarito**, dentro da própria Gupy.

### 5.5 Operacional (mantido)

```
Idempotency:         1 CandidateAttempt por (company_id, document_id, test_id).
                     test_result_id é o nosso identificador estável do resultado.
Retry/DLQ:           exponencial 1s/4s/16s/64s/256s no POST do result_webhook_url;
                     após 5 falhas → DeadLetter + alerta admin + reenvio manual.
previous_result:     se vier "fail", é recandidatura — tratar conforme política da vaga.
candidate_type:      internal/external pode mudar regras (ex.: recrutamento interno).
SEM ambiente de teste: a Gupy NÃO tem sandbox. Validação é feita com o cliente, numa
                     vaga não-listada real. (Reforça o Preflight no ambiente do cliente.)
Ativação:            enviar à Gupy a URL da nossa API + token (1 token por empresa).
```

> ⚠️ Substitui qualquer menção anterior a `application.moved` / `PATCH api/v1/jobs`. O contrato real é o de "Plataforma de Validações ou Exames externos" descrito acima.

## 6. Cálculo do Tempo do Teste

```
Tempo por turno:   ScenarioNode.timeLimitSec (10/20/30/60 ou null=sem limite).
Tempo total:       CALCULADO = Σ timeLimitSec dos nós do caminho mais longo.
                   NÃO existe campo de tempo total manual.
Sem resposta:      timer esgota → CandidateResponse {timedOut:true, chosenOptionId:null}
                   → conta como nível 0 nas competências daquele nó → avança ao
                   nextNode padrão (definido como fallback do nó) ou encerra.
Timeout global:    opcional por tenant; default = soma dos turnos + 20% de folga.
Pausa de sessão:   configurável por cargo (Simulation.allowPause). Se true, o cronômetro
                   do turno congela; se false, continua correndo.
Persistência:      cada resposta é salva imediatamente; queda de internet retoma do
                   último nó salvo (attempt RUNNING + último CandidateResponse).
```

## 7. Regras de Ramificação (grafo)

```
Profundidade máx.: 10 turnos (ScenarioNode.turnIndex ≤ 10).
Opções por nó:     2 a 4.
Anti-loop:         nextNodeId deve apontar para nó com turnIndex MAIOR que o atual.
                   Aresta para índice ≤ atual = BLOCKER (cria ciclo).
Branch sem destino: nextNodeId null E isTerminal=false → BLOCKER (caminho morto).
Nó terminal:       isTerminal=true encerra a simulação naquele caminho.
Validação de grafo (pré-publicação, determinística):
  - todo caminho da raiz alcança um nó terminal
  - nenhum ciclo (DAG: ordenação topológica deve existir)
  - todo nó (exceto terminais) tem ≥2 opções
  - toda opção tem ScoringRule para ≥1 competência
  - score máximo igual entre todos os caminhos (tolerância 0)
```

## 8. "Melhor Opção" (isBest) — regra formal

```
isBest:        marcação informativa do criador; PODE haver mais de uma por nó.
Impacto:       NÃO altera o cálculo do score (score vem só da rubrica/pontos).
               Serve para: (a) validação ("existe ao menos 1 best por nó?"),
               (b) relatório de evidência, (c) detector de "opção óbvia" no piloto.
Opção óbvia:   se no piloto ≥90% escolhem a MESMA opção em tempo < limiar →
               warning "opção possivelmente óbvia" (estatístico, sem IA).
```

## 9. Versionamento (detalhe operacional)

```
Imutabilidade:  CandidateAttempt referencia SEMPRE uma SimulationVersion fixa.
Nova versão:    qualquer edição em versão PUBLISHED gera nova SimulationVersion.
                - correção de texto/typo  → bump MINOR (v1.2 → v1.3)
                - mudança de pontuação/peso/grafo → bump MAJOR (v1.x → v2.0)
Candidatos em andamento: continuam e concluem na versão que iniciaram (imutável).
                NÃO são migrados para a nova versão.
Coexistência:   uma vaga pode ter apenas 1 versão ATIVA por vez para novos attempts;
                versões antigas seguem existindo só para attempts já iniciados.
Comparabilidade: resultados só comparáveis dentro da MESMA versão (ou versões
                marcadas como calibradas-equivalentes).
```

## 10. Monitoramento — métricas com fórmula fechada

```
Taxa de conclusão   = attempts COMPLETED / attempts CREATED  (por versão)
Drop-off            = 1 − (COMPLETED / RUNNING_iniciados); por dispositivo = mesma
                      fórmula filtrada por userAgent class (mobile/desktop)
Tempo médio/turno   = média de CandidateResponse.timeSpentMs por scenarioNode
Distribuição score  = histograma de ScoreBreakdown.finalScore (bins de 10)
Caminho frequente   = contagem de sequências distintas de CandidateResponse.path
Eventos que geram log/métrica:
  attempt.created · node.viewed · response.submitted · response.timedout ·
  attempt.completed · attempt.scored · result.sent · result.failed
Sinal de vazamento  = (tempo médio caindo > X% semana-a-semana) E
                      (escolhas de alto score subindo anormalmente) E
                      (concentração nos mesmos caminhos acima de limiar)
```

## 11. Fluxo de Erro (casos fechados)

```
Falha de persistência (resposta não salva): retorna 503 ao front; front re-tenta
  a submissão (idempotente por attemptId+nodeId); resposta duplicada = no-op.
Sessão expirada (attempt ABANDONED): front mostra "sessão expirada", oferece
  retomar se dentro da janela; fora da janela → contato com RH.
Versão inválida/arquivada no meio do attempt: attempt já iniciado continua na sua
  versão imutável; novos attempts barrados.
Falha de integração Gupy: ver §5 (retry + DLQ).
Erro de contrato (payload 4xx): não retenta; DLQ p/ inspeção; alerta admin.
Token Gupy expirado/sem plano: Preflight falha ANTES de publicar; bloqueia vínculo.
```

## 12. Regras de Publicação (operacional)

```
Impede publicar:  qualquer BLOCKER do validador (§ Validador) + Preflight Gupy falho
                  + ausência de aprovação exigida + grafo inválido (§7).
Override:         NÃO há override de blocker. Warning pode ser publicado com registro.
Warning expira?:  não expira; fica anexado à versão e ao AuditLog.
Auditoria:        registra (não bloqueia), EXCETO publicar-com-blocker que é barrado
                  na transição de estado (§4), não só no log.
```

---

## ✅ Critérios de Aceite do Contrato Técnico (P0)

```
✓ Payload do candidato NÃO expõe gabarito/pesos/isBest/isCritical
✓ idempotencyKey = attemptId garante 1 envio efetivo por tentativa
✓ Score normalizado por caminho com tetos iguais (verificável em teste unitário)
✓ Erro crítico → REVISAO_HUMANA, nunca score zero nem reprovação automática
✓ Grafo validado como DAG (sem ciclo) e sem caminho morto antes de publicar
✓ Edição de versão publicada gera nova versão; attempts em andamento não migram
✓ Retry exponencial + DLQ + reenvio manual para integração Gupy
✓ Nenhuma chamada a serviço de IA em runtime (todo cálculo determinístico)
✓ Toda transição de estado registra AuditLog imutável
✓ Telas de compliance e transparência, incluindo LGPD, devem usar layout de coluna única; não usar grids, sidebars ou cards lado a lado para conteúdo principal.
```

---
---

# 🎨 ESPECIFICAÇÃO VISUAL / UX (P0) — Como o usuário VÊ o sistema

> Os contratos acima definem a *lógica*. Esta seção define a *percepção*: estados visuais, validador na tela, editor como fluxo vivo, onboarding vazio e cronômetro. São decisões de design — descritas aqui de forma acionável, mas o lugar de refiná-las é no protótipo (Lovable), iterando na tela real.

## 1. Estado Visual Unificado da Simulação

Cada estado tem um tratamento visual fixo e consistente em todas as telas (lista, editor, detalhe). Componente: **badge + faixa de cor + ação primária contextual**.

| Estado | Badge (cor) | O que o usuário vê | Ação primária |
|---|---|---|---|
| DRAFT / Rascunho | cinza neutro | "Em construção — não publicável" | Continuar editando |
| IN_REVIEW | âmbar | "Aguardando aprovação de [papel]" | Ver/aprovar |
| APPROVED | azul-info | "Aprovada — pronta para publicar" | Publicar |
| PUBLISHED + PILOTO | azul-marinho + selo "PILOTO" | "No ar (piloto) — ranqueia, não elimina" | Ver monitoramento |
| PUBLISHED + CALIBRADA | verde | "No ar — calibrada" | Ver monitoramento |
| PUBLISHED + VALIDADA | verde + selo "✓ validada" | "No ar — validada internamente" | Ver monitoramento |
| EXPIRED | vermelho suave | "Expirada — recalibrar para reutilizar" | Recalibrar (duplica) |
| ARCHIVED | cinza apagado | "Arquivada" | Restaurar |

**Regra de UI:** o selo de **maturidade** (Piloto/Calibrada/Validada) aparece sempre ao lado do status técnico, porque é ele que governa o que a simulação *pode fazer* (ex.: "Piloto" deixa o cadeado de Eliminação visível e travado).

## 2. Validador na Tela — como o bloqueio aparece

Layout em **duas zonas**:

```
┌─ Score de qualidade (topo, grande) ──────────────┐
│  82/100  ▓▓▓▓▓▓▓▓░░   [barra por critério]       │
└──────────────────────────────────────────────────┘
┌─ Checklist lateral (diagnóstico) ────────────────┐
│  ✓ Todos os caminhos têm desfecho                │
│  ✓ Pontuação normalizada                         │
│  ⚠️ Opção C óbvia (confirmar no piloto)           │
│  🚫 Caminho 3C: score 28% maior  ← BLOCKER       │
└──────────────────────────────────────────────────┘
```

**Comportamento visual do bloqueio:**
```
Se há BLOCKER:
  - faixa vermelha fixa no topo: "Publicação bloqueada — 1 item crítico"
  - item blocker destacado em vermelho, com link "ir ao problema"
  - botão "Publicar" NÃO existe (só "Corrigir agora" / "Salvar rascunho")
Se só há WARNING:
  - faixa âmbar: "Pode publicar — 2 alertas registrados"
  - botão "Publicar com alerta registrado" (amarelo, não verde)
  - ao clicar, modal de confirmação: "Estes alertas ficarão no log de auditoria"
Sem nada:
  - faixa verde "Pronta para publicar" + botão verde "Publicar"
```

Cada item do checklist é clicável e leva direto ao ponto do editor que o gerou (highlight do nó/opção).

## 3. Editor como "Fluxo Vivo" (não formulário)

Upgrade de UX sobre o editor estruturado. **Mesma lógica, percepção diferente.**

```
- Visão dupla: à esquerda o ÁRVORE/grafo navegável (nós como cartões
  conectados); à direita o turno em edição.
- Adicionar turno = arrastar/clicar "+" no fim de um ramo, não abrir formulário.
- Preview ao vivo: um painel "ver como o candidato vê" renderiza o chat em
  tempo real enquanto a Renata escreve (mobile frame).
- Cada opção mostra inline o resumo de rubrica que recebe (ex.: "Empatia N3 ·
  Aderência N2") sem precisar abrir outra tela.
- Estados de preenchimento visíveis: nó incompleto = contorno tracejado;
  nó pronto = contorno sólido; nó com blocker = contorno vermelho.
```

> Isto é direção de design para o protótipo — o "fluxo vivo" se prova iterando no Lovable, não no texto. O requisito aqui é: **o editor deve parecer construção de um fluxo, com preview do chat ao lado, não um formulário de cadastro.**

## 4. Primeira Experiência (estado vazio / onboarding)

Crítico para demo e adoção — hoje inexistente.

```
Conta nova, zero simulações → NÃO mostrar tabela vazia.
Mostrar tela de boas-vindas com 3 opções grandes:
  1. [▶ Criar minha primeira simulação guiada]  (wizard com dicas passo a passo)
  2. [📋 Começar de um modelo pronto]            (biblioteca pré-carregada)
  3. [👁 Ver uma simulação de exemplo]           (demo "O Dia do Caos" só leitura)

Wizard guiado (primeira vez):
  - tooltips contextuais em cada passo (o que é blueprint, o que é rubrica)
  - um template parcialmente preenchido como ponto de partida
  - barra de progresso "Passo 2 de 8"
  - possibilidade de pular para o editor avançado a qualquer momento

Meta de demo: do "conta nova" à "primeira simulação publicável" em < 5 min.
```

> Pelo menos 1 simulação-exemplo ("O Dia do Caos") deve vir pré-carregada em toda conta nova, em modo leitura, para o RH entender o produto sem criar nada.

## 5. Cronômetro e Comportamento de Tempo (UI explícita)

Lado do candidato — o tempo precisa ser visível e previsível.

```
Cronômetro do turno:
  - barra circular ou linear regressiva, visível no topo do chat
  - cor: neutra → âmbar nos últimos 30% → vermelha nos últimos 10%
  - número de segundos restantes sempre legível
  - SEM som por padrão (acessibilidade); vibração leve no mobile opcional

Quando o tempo estoura:
  - micro-aviso "Tempo esgotado neste turno" (não punitivo, sem alarme)
  - registra resposta como "sem resposta" e avança automaticamente
  - NÃO encerra a simulação inteira (só o turno)

Turno sem limite de tempo (timeLimitSec = null):
  - não mostra cronômetro; mostra só "Sem limite de tempo"

Pausa (se Simulation.allowPause = true):
  - botão "pausar" congela o cronômetro do turno
  - estado salvo; ao voltar, retoma do mesmo ponto

Acessibilidade:
  - candidato com acomodação PCD recebe tempo estendido (multiplicador
    configurável) — o cronômetro reflete o tempo já ajustado, sem expor o motivo
```

---

## ✅ Critérios de Aceite — UX/UX (P0)

```
✓ Todo estado (DRAFT…ARCHIVED) tem badge + cor + ação consistentes em todas as telas
✓ Selo de maturidade sempre visível junto ao status
✓ Validador: blocker = sem botão publicar + faixa vermelha; warning = publicar com modal
✓ Itens do validador são clicáveis e levam ao ponto do editor
✓ Editor mostra árvore navegável + preview do chat ao vivo (mobile frame)
✓ Conta nova abre em onboarding (3 opções), nunca em tabela vazia
✓ Toda conta nova tem 1 simulação-exemplo em modo leitura
✓ Cronômetro visível, com cores progressivas e sem som por padrão
✓ Estouro de tempo encerra só o turno, nunca a simulação inteira
```

---
---

# ✅ CHECKLIST DE FECHAMENTO DE FRONT (P0) — antes de codar

> Consolida o comportamento de tela que faltava deixar explícito. Onde algo já foi definido antes (estados, validador, versionamento), aqui vira regra única e consultável. **Nada de IA em runtime.**

## 1. Estados obrigatórios de TODA tela

Toda tela implementa os 5 estados (quando aplicável). Convenção visual única:

| Estado | Tratamento padrão |
|---|---|
| **loading** | skeleton do conteúdo (nunca spinner solto em tela branca) |
| **vazio** | empty state com ícone + frase + CTA (nunca tabela/lista vazia muda) |
| **erro** | banner vermelho com mensagem clara + ação ("tentar de novo") |
| **sucesso** | toast/confirmação curta, não bloqueante |
| **bloqueado** | faixa explicativa do motivo + ação para destravar (ou quem destrava) |

Matriz por tela:

```
Dashboard         → loading: skeleton de linhas · vazio: onboarding (item 8)
                    erro: banner "não foi possível carregar" · bloqueado: n/a
Blueprint         → erro de validação inline · bloqueado: campos obrigatórios
Editor diálogo    → autosave (item 5) · erro: "alteração não salva, retentando"
                    bloqueado: nó com blocker (contorno vermelho)
Validador         → loading: "analisando estrutura…" · bloqueado: faixa vermelha
Piloto            → vazio: "nenhuma resposta ainda" · loading de distribuição
Mapa & Score      → erro: "grafo inválido" com link ao problema
Governança        → bloqueado: "aguardando aprovação de [papel]"
Gupy Preflight    → loading por item do checklist · erro: item falho em vermelho
Monitoramento     → vazio: "sem dados suficientes ainda"
Candidato         → loading: "preparando…" · erro: "conexão perdida" (item 6)
                    sucesso: tela de encerramento · bloqueado: link expirado
Resultado/detalhe → loading: skeleton · erro: "resultado indisponível"
Auditoria         → vazio: "nenhum evento no período"
```

## 2. Navegação sem buracos

```
- Todo passo do wizard tem ← Voltar e → Próximo (exceto o passo 0: só Próximo).
- Próximo desabilitado (não escondido) enquanto faltar obrigatório; tooltip diz o quê.
- Nenhuma tela terminal sem saída: a de encerramento do candidato explica
  "o RH retorna em até X dias"; a de erro sempre tem ação de recuperação.
- Breadcrumb/stepper visível no wizard: "Passo 3 de 8".
- Sair do wizard no meio → salva rascunho automaticamente e pergunta confirmação.
- Deep-link: cada passo tem rota própria (já definidas); recarregar não perde estado.
```

## 3. Comportamento de edição (regra única)

```
Durante a CRIAÇÃO (status DRAFT, sem attempts):
  - pode voltar e editar qualquer passo anterior livremente
  - editar um passo anterior que invalida passos seguintes → marca os seguintes
    como "revisar" (badge âmbar) e re-roda o validador; não apaga o trabalho
  - salva como rascunho (mesma versão, ainda mutável)

Depois de PUBLICADA ou com attempts RUNNING:
  - edição é PROIBIDA na versão vigente
  - "Editar" abre diálogo: "Isto criará a versão vX+1. Candidatos em andamento
    continuam na versão atual." → confirma → nova SimulationVersion (ver §9 do contrato)
  - typo = minor; pontuação/peso/grafo = major
```

## 4. Validador — comportamento final da UI

```
Botão "Publicar" com BLOCKER: NÃO aparece (removido, não só desabilitado) —
  evita o usuário ficar tentando clicar. No lugar: "Corrigir agora" + "Salvar rascunho".
Botão com WARNING: aparece como "Publicar com alerta registrado" (amarelo) →
  exige modal de confirmação ("estes alertas vão para o log") → publica.
Sem nada: "Publicar" verde, publica direto.
Score 0–100: no TOPO da tela do validador, grande, com barra por critério
  (os 7 pesos da fórmula). É a primeira coisa que o usuário vê.
Itens do diagnóstico: clicáveis → highlight do nó/opção que gerou.
```

## 5. Editor de simulação — operações completas

```
Adicionar turno:   botão "+" no fim de um ramo OU no grafo; cria ScenarioNode
                   com turnIndex = maior+1. Limite 10 (ao atingir, botão some).
Deletar turno:     ícone lixeira no cartão do nó → confirmação. Se o nó tem filhos,
                   avisa "isto remove N turnos abaixo" e re-religa ou marca órfãos.
Editar opção:      clique inline no texto; contador de 160 caract.; rubrica ao lado.
Adicionar opção:   "+ opção" (2 a 4); ao passar de 4, botão desabilita.
Conectar ramificação: cada opção tem dropdown "vai para → [nó / FIM]"; só permite
                   nós com turnIndex maior (anti-loop, ver §7 do contrato).
Visualizar fluxo:  toggle Lista ↔ Grafo. Grafo = cartões conectados navegáveis.
Salvar:            AUTOSAVE (debounce ~2s) enquanto DRAFT; indicador "salvo às HH:MM".
                   Botão "salvar" manual também existe para tranquilidade.
Desfazer:          undo/redo das últimas N ações de edição.
```

## 6. Candidato — UX crítica de venda

```
Timer:        sempre visível quando o turno tem limite; sumido quando não tem.
              Barra regressiva, cor progressiva (neutra→âmbar→vermelha), sem som.
Escolha de opção: transição suave (não instantânea seca, não lenta) — a mensagem
              do candidato "envia" no chat e o cliente fictício "digita" a resposta
              (efeito de digitação curto) → próximo turno. Sensação de conversa real.
Fim do teste: tela de encerramento com feedback leve ("alta Empatia, boa Resolução"),
              sem nota exata, + "suas respostas foram enviadas; RH retorna em X dias".
Conexão perdida: banner "conexão perdida — reconectando…"; ao voltar, retoma do
              último turno salvo (cada resposta persiste na hora). Nada se perde.
Não terminou: se fechar o navegador, ao reabrir o link retoma de onde parou (se
              dentro da janela de validade).
Mobile first: toda essa UX é desenhada primeiro para o celular.
```

## 7. Integração Gupy — UX final (uso do Admin/RH)

```
Sucesso de publicação: toast verde "Simulação vinculada à vaga [nome]" +
                       a simulação passa a status PUBLISHED na lista.
Erro de publicação:    se o Preflight falhar, mostra QUAL item falhou (token, plano,
                       vaga, etc.) em vermelho, com orientação. Não publica.
Status de envio do resultado (back-office): cada attempt mostra selo —
                       "Enviado à Gupy ✓" / "Reenviando…" / "Falha — na fila (DLQ)".
Reenvio manual:        aparece SÓ para Admin integração, na tela de auditoria, como
                       botão "reprocessar" nos itens em DLQ. RH comum não vê isso.
Para o gestor:         nada disso aparece — ele só vê a nota dentro da Gupy (§ Fronteira).
```

## 8. Dashboard — primeiro impacto

```
Empty state (primeiro uso): NÃO tabela vazia. Tela de boas-vindas com 3 opções
   grandes (criar guiada / modelo pronto / ver exemplo) — ver §4 da Espec. Visual.
CTA principal único:        "Nova simulação" em destaque (primário). "Ver
   monitoramento" é secundário. Nada compete com o CTA principal.
Com simulações:             tabela/grid com os campos da Simulation (Tela 0),
   badges de estado consistentes (§1 da Espec. Visual), busca e filtro por status.
Criar primeira sem dúvida:  o caminho guiado parte do empty state e leva, com
   tooltips, do blueprint à primeira simulação publicável em < 5 min.
KPIs no topo:               Publicadas · Em piloto · Rascunhos · Tentativas.
```

---

## ✅ Critérios de Aceite — Fechamento de Front (P0)

```
✓ Toda tela trata loading, vazio, erro, sucesso e bloqueado (quando aplicável)
✓ Todo passo tem voltar/próximo; nenhuma tela terminal sem ação de saída
✓ Próximo desabilitado (não escondido) com tooltip do que falta
✓ Editar versão publicada cria nova versão com diálogo de confirmação
✓ Publicar some com blocker; com warning exige modal; score 0–100 no topo
✓ Editor: add/delete turno, editar/add opção, conectar ramo, lista↔grafo, autosave, undo
✓ Candidato: timer visível, transição de chat fluida, retoma após queda de conexão
✓ Gupy: sucesso/erro claros; reenvio manual só para Admin; gestor não vê fila
✓ Dashboard abre em onboarding no 1º uso, com CTA único e exemplo pré-carregado
```
