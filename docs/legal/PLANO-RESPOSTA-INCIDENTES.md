# Plano de Resposta a Incidentes de Segurança e Privacidade — Praxis

> **Status operacional:** controles técnicos implementados. Os responsáveis,
> contatos, modelos de comunicação e cláusulas contratuais devem ser validados
> pelo Jurídico e pelo Encarregado antes da operação com dados reais.

## 1. Objetivo

Detectar, conter, avaliar, comunicar e aprender com incidentes envolvendo dados
pessoais tratados pelo Praxis, mantendo evidências suficientes para prestação de
contas.

## 2. Papéis

| Papel | Responsabilidade |
| --- | --- |
| Encarregado/DPO do controlador | Avaliar comunicação à ANPD e aos titulares. |
| Líder de resposta | Coordenar contenção, linha do tempo e decisões. |
| Engenharia/DevOps | Preservar evidências, corrigir a causa e restaurar a operação. |
| Jurídico | Avaliar obrigações legais, regulatórias e contratuais. |
| iForce/Praxis como operadora | Comunicar a controladora sem demora indevida e cooperar com a investigação. |

## 3. Classificação

| Nível | Exemplo | Resposta inicial interna |
| --- | --- | --- |
| Crítico | Vazamento, acesso cruzado entre empresas ou segredo exposto | Imediata |
| Alto | Falha de autorização com risco real | Em horas |
| Médio | Vulnerabilidade explorável sem impacto confirmado | Até 1 dia útil |
| Baixo | Evento suspeito sem risco confirmado | Fluxo normal |

## 4. Registro obrigatório no produto

A empresa registra e acompanha o incidente pelas rotas autenticadas:

- `GET /api/v1/privacy/incidents`;
- `POST /api/v1/privacy/incidents`;
- `PATCH /api/v1/privacy/incidents/{incidentId}`.

O registro contém severidade, descoberta, confirmação, dados afetados, estimativa
de titulares, avaliação de risco, responsável e horários das comunicações. O
prazo de retenção técnico do registro é de pelo menos cinco anos.

## 5. Fluxo

1. **Detectar e registrar.** Abrir o incidente imediatamente, sem aguardar a
   conclusão da investigação.
2. **Conter.** Revogar credenciais, bloquear acessos, isolar componentes e
   preservar logs.
3. **Erradicar.** Corrigir a causa raiz e validar a correção.
4. **Avaliar risco ou dano relevante.** Considerar natureza e volume dos dados,
   possibilidade de reidentificação, consequências aos titulares e medidas de
   proteção existentes.
5. **Comunicar a controladora.** A operadora comunica sem demora indevida, pelo
   canal e SLA definidos no DPA.
6. **Comunicar ANPD e titulares.** Quando aplicável, a controladora realiza a
   comunicação no prazo regulatório vigente. A referência operacional adotada é
   de até três dias úteis contados do conhecimento do incidente relevante,
   salvo regra superveniente ou determinação específica.
7. **Recuperar e monitorar.** Restaurar a operação de forma controlada.
8. **Encerrar.** Registrar causa, impacto, comunicações, medidas e ações
   preventivas.

## 6. Conteúdo mínimo da comunicação

- natureza e categorias dos dados;
- quantidade ou categorias de titulares;
- medidas de segurança utilizadas;
- riscos relacionados ao incidente;
- medidas de contenção e mitigação;
- data de conhecimento e linha do tempo;
- contato do encarregado ou canal responsável.

## 7. Evidências

- preservar logs, trilhas e configurações relevantes;
- registrar quem tomou cada decisão e em qual momento;
- evitar incluir segredos ou dados pessoais desnecessários no relato;
- manter os registros de incidentes por pelo menos cinco anos;
- realizar exercício de simulação ao menos anual.

## 8. Pendências externas obrigatórias

1. Definir nomes, telefone, e-mail e escala dos responsáveis.
2. Definir no DPA o prazo da operadora para notificar a controladora.
3. Validar modelos de comunicação com Jurídico e DPO.
4. Confirmar periodicamente o prazo regulatório aplicável.
5. Executar e registrar o exercício anual.

Última revisão técnica: 20/07/2026.
