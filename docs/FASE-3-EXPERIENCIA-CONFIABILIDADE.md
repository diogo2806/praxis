# Fase 3 — Experiência e confiabilidade

## Objetivo

Reduzir perda de trabalho durante a edição, tornar a operação utilizável em celular, reforçar os recursos de acessibilidade e concentrar a saúde das integrações na Central Operacional.

## Escopo entregue

### 1. Mobile

A base já possuía menu lateral em formato de drawer para telas pequenas e uma experiência mobile-first para participantes. A fase 3 complementa essa estrutura com:

- cartões de tentativas no monitoramento para telas menores que `768px`;
- filtros e ações com altura mínima adequada para toque;
- respeito às safe areas do dispositivo;
- quebra segura de URLs, códigos e textos longos;
- ajustes específicos no cenário da pessoa participante;
- controles com área mínima de toque em dispositivos de ponteiro impreciso.

A tabela completa continua disponível a partir de tablets e desktops.

### 2. Salvamento automático

Foi criada uma infraestrutura reutilizável de rascunhos locais em:

```text
frontend/src/lib/use-persistent-draft.ts
```

O formulário de personagem da avaliação usa duas camadas de proteção:

1. **Rascunho local:** salvo no navegador após uma pausa curta na digitação e novamente no evento `pagehide`.
2. **Salvamento no Práxis:** quando a primeira etapa já existe e os campos obrigatórios estão válidos, as alterações são persistidas automaticamente no backend após o debounce.

O indicador visual diferencia:

- salvando localmente;
- salvando no Práxis;
- salvo;
- falha no backend com rascunho local preservado;
- bloqueio do armazenamento local pelo navegador.

O rascunho é isolado por avaliação e versão. Quando o fluxo é salvo e avança para a revisão, a cópia local é removida.

### 3. Acessibilidade

Os recursos já existentes foram preservados:

- link para pular ao conteúdo principal;
- foco visível;
- texto maior;
- menos movimento;
- modo foco;
- navegação simplificada;
- alto contraste, texto maior e fonte de apoio na experiência do participante;
- VLibras.

A fase 3 acrescenta:

- foco compatível com as cores próprias da experiência do participante;
- suporte a `prefers-contrast: more`;
- eliminação da animação do timer quando o sistema pede menos movimento;
- áreas de toque de pelo menos 44px em dispositivos móveis;
- atributos `aria-required`, `aria-invalid`, `aria-describedby`, `role=status` e regiões vivas no editor alterado;
- progresso das tentativas identificado como `progressbar`.

### 4. Monitoramento de integrações

A rota abaixo passou a reunir tentativas, provedores e entregas:

```text
/monitoramento
```

A tela consulta e atualiza automaticamente:

- tentativas paginadas;
- avaliações publicadas;
- notificações;
- entregas de resultado;
- integrações Gupy, Recrutei e API própria.

Para cada integração são exibidos:

- estado atual;
- última atividade autenticada;
- data da configuração;
- mensagem de erro, quando houver;
- acesso direto à configuração e ao diagnóstico.

O resumo destaca:

- tentativas ativas;
- tentativas sem sinal;
- integrações conectadas;
- integrações pendentes ou com erro;
- entregas aguardando nova tentativa;
- entregas em DLQ.

A atualização automática ocorre a cada 30 segundos e pode ser pausada pelo usuário. O botão **Atualizar agora** refaz todas as consultas em conjunto.

## Critérios de confiabilidade

- O rascunho local não substitui o backend; funciona como proteção contra perda durante falhas, recarga ou fechamento da aba.
- Dados de versões publicadas continuam protegidos contra edição direta.
- O monitoramento não promove uma integração para conectada. Ele apenas apresenta o estado persistido e a evidência de atividade registrada pelo backend.
- Falhas de uma consulta são exibidas sem ocultar os demais dados disponíveis.
- A atualização automática não altera dados nem reprocessa entregas por conta própria.

## Arquivos principais

```text
frontend/src/lib/use-persistent-draft.ts
frontend/src/routes/nova.personagem.tsx
frontend/src/routes/monitoramento.tsx
frontend/src/styles/accessibility-overrides.css
```
