# Plano de Resposta a Incidentes de Segurança e Privacidade (Praxis)

> **Status: MINUTA para validação jurídica e de segurança.** Runbook de resposta
> a incidentes com dados pessoais (LGPD, arts. 46 a 48). **Não é aconselhamento
> jurídico.** Endereça o risco médio #11.

## 1. Objetivo

Detectar, conter, avaliar, comunicar e aprender com incidentes que envolvam dados
pessoais tratados pela plataforma, cumprindo o dever de segurança (art. 46) e de
comunicação (art. 48) da LGPD.

## 2. Papéis

| Papel | Responsabilidade |
| --- | --- |
| Encarregado (DPO) | Ponto focal; decide sobre comunicação a ANPD e titulares (no papel de controlador). |
| Líder de resposta | Coordena a contenção técnica e o registro. |
| Engenharia/DevOps | Contenção, evidência técnica (logs, trilha de auditoria), correção. |
| Jurídico | Avalia obrigações legais e contratuais. |
| Operador (iForce) × Controlador | O operador comunica a controladora sem demora; a controladora decide a comunicação externa (ver DPA). |

## 3. Classificação de severidade

| Nível | Exemplo | Alvo de resposta inicial |
| --- | --- | --- |
| Crítico | Vazamento de dados pessoais, acesso indevido multi-tenant, exposição de segredo | Imediato |
| Alto | Falha de controle de acesso sem exfiltração confirmada | Horas |
| Médio | Vulnerabilidade explorável sem impacto confirmado | 1–2 dias |
| Baixo | Evento suspeito sem risco a dado pessoal | Fluxo normal |

## 4. Fluxo de resposta

1. **Detecção e registro.** Abrir incidente com horário, autor do reporte e
   sintomas. Fontes: alertas, notificações internas/DLQ, trilha de auditoria,
   relato de cliente/titular.
2. **Contenção.** Revogar credenciais/tokens afetados, isolar componente,
   bloquear acesso. Segredos comprometidos são rotacionados
   (`PRAXIS_JWT_SECRET`, `MP_*`, tokens de integração).
3. **Erradicação e correção.** Corrigir a causa raiz; validar com testes.
4. **Avaliação de risco.** Determinar dados e titulares afetados, probabilidade
   de dano e se há risco relevante (art. 48).
5. **Comunicação.**
   - Operador → Controladora: sem demora indevida, com os elementos do art. 48.
   - Controladora → ANPD e titulares: quando houver risco/dano relevante, em
     prazo razoável.
6. **Recuperação.** Restaurar operação e monitorar recorrência.
7. **Pós-incidente.** Post-mortem sem culpados, lições e ações preventivas.

## 5. Conteúdo mínimo da comunicação (art. 48, §1º)

- Descrição da natureza dos dados afetados.
- Titulares envolvidos (número/categoria).
- Medidas técnicas e de segurança adotadas.
- Riscos relacionados ao incidente.
- Medidas tomadas para reverter/mitigar.

## 6. Evidências e retenção

- Preservar logs e a trilha append-only (`audit_events`) relevantes.
- Registrar linha do tempo e decisões para prestação de contas.

## 7. Prevenção (controles já existentes)

- Segredos por variável de ambiente; tokens de integração em hash; token MP
  cifrado; webhook MP com validação de assinatura.
- Isolamento multi-tenant e fail-fast que impede subir produção sem segurança
  (`SecurityStartupGuard`).
- Anonimização por retenção e trilha de auditoria.

## 8. Pendências para o Jurídico/Segurança

1. Definir SLAs internos e canais de acionamento (on-call).
2. Modelo de comunicação a titulares e à ANPD.
3. Exercício de simulação (tabletop) periódico.

> Última revisão da minuta: preencher na validação.
