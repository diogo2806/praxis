# Replanejamento de acessibilidade cognitiva do Práxis

## Objetivo

Reorganizar o Práxis para que pessoas idosas, pessoas com TDAH, pessoas autistas e usuários com pouca familiaridade digital consigam concluir tarefas sem precisar compreender a arquitetura técnica do produto.

A interface deve favorecer previsibilidade, baixa carga de memória, linguagem direta, redução de estímulos e uma ação principal por vez.

## Princípios obrigatórios

1. **Uma tarefa principal por tela**
   Cada página deve responder claramente: onde estou, o que posso fazer aqui e qual é o próximo passo.

2. **No máximo cinco destinos visíveis no menu principal**
   Recursos menos frequentes permanecem em “Mais opções”, sem serem removidos.

3. **Divulgação progressiva**
   Configurações avançadas, detalhes técnicos, métricas secundárias e ações destrutivas só aparecem quando solicitadas.

4. **Linguagem operacional**
   Preferir “Participantes” a “Aplicações”, “Processos de avaliação” a “Jornadas” quando o contexto não estiver explicado e “Tentar enviar novamente” a “Retry”.

5. **Padrão visual previsível**
   Título, explicação, ação principal, conteúdo e ajuda devem aparecer sempre na mesma ordem.

6. **Sem dependência exclusiva de cor, ícone ou tooltip**
   Todo estado precisa de texto visível. Informações essenciais não podem existir apenas ao passar o mouse.

7. **Áreas de interação amplas**
   Botões, links, campos e controles devem ter pelo menos 44 × 44 px em modos de leitura ampliada.

8. **Controle de estímulos**
   O usuário escolhe texto maior, menos movimento, menu simples e modo foco. As preferências devem permanecer salvas.

9. **Preservação de contexto**
   Voltar uma etapa não pode apagar dados. Formulários longos devem indicar progresso e salvar rascunhos.

10. **Erros recuperáveis**
    A mensagem deve explicar o que ocorreu, o que ficou preservado e qual ação resolve o problema.

## Nova arquitetura de navegação

### Tarefas principais

- Início
- Avaliações
- Participantes
- Resultados

### Mais opções

- Competências
- Processos de avaliação
- Comparação de talentos
- Centro operacional
- Conformidade
- Notificações
- Empresa
- Equipe
- Integrações
- Plano
- Minha conta

## Padrão comum para todas as telas

Cada tela autenticada deve conter:

1. Breadcrumb curto.
2. Bloco “Objetivo desta tela”, com uma frase.
3. Um botão principal claramente destacado.
4. Conteúdo essencial primeiro.
5. Seção “Mais detalhes” recolhida.
6. Ajuda contextual em texto, não apenas tooltip.
7. Estado vazio com uma única ação recomendada.
8. Confirmações claras para ações irreversíveis.
9. Foco de teclado visível.
10. Compatibilidade com texto ampliado e redução de movimento.

## Replanejamento por tela

### 1. Início / Dashboard

**Objetivo:** mostrar o que exige atenção hoje.

Ordem proposta:

1. Pendências urgentes.
2. Participantes em andamento.
3. Resultados recentes.
4. Ação principal “Criar avaliação”.
5. Resumo secundário recolhido: jornadas, integrações e plano.

Remover da primeira visão a competição entre faturamento, integrações, métricas e atalhos equivalentes.

### 2. Comece aqui

**Objetivo:** ensinar o fluxo sem exigir conhecimento técnico.

Fluxo proposto:

1. Criar avaliação.
2. Montar situações e respostas.
3. Revisar e publicar.
4. Convidar participantes.
5. Analisar resultados.

Evitar “pontuação determinística”, “DLQ”, “retry” e “webhook” na introdução. Esses termos ficam na área técnica.

### 3. Avaliações

**Objetivo:** localizar uma avaliação e escolher a próxima ação.

Visão simples:

- Nome.
- Estado operacional.
- Próxima ação.

Estados recomendados:

- Em construção.
- Precisa de ajustes.
- Pronta para publicar.
- Publicada.
- Arquivada.

Competências, porcentagem de conclusão, tentativas e versão publicada ficam nos detalhes do registro.

### 4. Criar e editar avaliação

**Objetivo:** concluir uma avaliação em etapas previsíveis.

Divisão proposta:

1. Informações básicas.
2. Competências observadas.
3. Situações apresentadas.
4. Respostas e critérios.
5. Revisão.
6. Publicação.

Requisitos:

- uma etapa por vez;
- progresso visível;
- rascunho automático;
- botão “Continuar” sempre no mesmo local;
- botão “Voltar” sem apagar dados;
- erros apresentados junto ao campo e em um resumo no topo.

### 5. Competências

**Objetivo:** consultar ou cadastrar competências reutilizáveis.

Separar:

- lista de competências;
- cadastro/edição em painel próprio;
- detalhes avançados recolhidos.

Evitar formulário permanente ao lado da listagem quando o usuário está apenas consultando.

### 6. Participantes / Enviar link

**Objetivo:** convidar e acompanhar pessoas.

Criar duas abas claras:

- Participantes.
- Novo convite.

Novo convite em três passos:

1. Escolher avaliação ou processo.
2. Informar participante.
3. Revisar e enviar.

Depois do envio, apresentar apenas:

- confirmação;
- botão copiar;
- botão enviar por e-mail;
- botão concluir.

Histórico, reenvio e status ficam na aba Participantes.

### 7. Processos de avaliação / Jornadas

**Objetivo:** organizar várias avaliações para um mesmo processo.

Dividir a tela atual em rotas distintas:

- Lista de processos.
- Composição do processo.
- Participantes.
- Acompanhamento.

A lista não deve exibir simultaneamente formulário de criação, composição, publicação, convite e tentativas.

### 8. Resultados

**Objetivo:** encontrar uma pessoa e compreender seu resultado.

Visão simples:

- participante;
- avaliação/processo;
- conclusão;
- resultado;
- ação “Ver análise”.

Filtros avançados ficam recolhidos. A tela de detalhe deve separar:

1. Resumo.
2. Evidências.
3. Critérios.
4. Histórico.
5. Decisão humana.

### 9. Comparação de talentos

**Objetivo:** comparar sem sugerir decisão automática.

Limitar comparação inicial a duas ou três pessoas. Explicar cada indicador em texto. Não usar ranking isolado, cores competitivas ou linguagem de aprovação automática.

### 10. Centro operacional

**Objetivo:** mostrar falhas que precisam de ação.

Traduzir estados técnicos:

- “Aguardando novo envio”.
- “Falha no envio”.
- “Enviado com sucesso”.

Termos como DLQ, webhook e retry podem aparecer em “Detalhes técnicos”.

### 11. Conformidade

**Objetivo:** verificar riscos e evidências.

Organizar por prioridade:

1. Problemas que bloqueiam uso.
2. Alertas que precisam de revisão.
3. Itens em conformidade.
4. Evidências técnicas recolhidas.

### 12. Integrações

**Objetivo:** conectar um sistema externo.

Apresentar catálogo primeiro. Ao escolher um provedor, usar etapas:

1. O que será compartilhado.
2. Credenciais.
3. Teste da conexão.
4. Ativação.

Explicar permissões antes de solicitar dados sensíveis.

### 13. Equipe

**Objetivo:** convidar e administrar acessos.

Separar “Pessoas com acesso” de “Novo convite”. Explicar permissões com exemplos práticos, evitando nomes de perfil sem descrição.

### 14. Empresa, conta e plano

**Objetivo:** consultar e alterar configurações administrativas.

Manter páginas independentes, com formulários curtos. Campos raramente alterados ficam em “Configurações avançadas”. Alterações financeiras ou de acesso exigem resumo antes da confirmação.

### 15. Notificações

**Objetivo:** identificar o que exige ação.

Agrupar em:

- Precisa da sua atenção.
- Informações.
- Resolvidas.

Cada notificação deve ter uma ação direta e explicar a consequência de não agir.

### 16. Experiência da pessoa candidata

**Objetivo:** concluir a avaliação com segurança e previsibilidade.

Requisitos:

- informar quantidade aproximada de etapas sem criar pressão de tempo;
- apresentar uma situação por vez;
- permitir pausa quando a regra da avaliação aceitar;
- não usar animações desnecessárias;
- manter instruções acessíveis durante toda a etapa;
- confirmar respostas antes do envio final;
- mostrar claramente o que foi concluído;
- evitar mudanças inesperadas de contexto.

## Modos de acessibilidade cognitiva

### Menu simples

Ativado por padrão para novos usuários. Mostra somente tarefas principais e mantém recursos adicionais em uma seção expansível.

### Texto maior

Aumenta tipografia, campos e áreas de interação sem quebrar o layout.

### Menos movimento

Reduz animações e transições, respeitando também `prefers-reduced-motion` do sistema operacional.

### Modo foco

Oculta o menu lateral, reduz controles periféricos e limita a largura do conteúdo para facilitar leitura sequencial.

## Critérios de aceite

- É possível chegar às quatro tarefas principais com uma única escolha no menu.
- Nenhuma funcionalidade existente deixa de estar acessível.
- Preferências cognitivas permanecem após recarregar a página.
- Toda tela autenticada informa seu objetivo.
- O modo foco pode ser ativado e desativado sem perder dados.
- O texto maior não provoca sobreposição ou corte de conteúdo em 200% de zoom.
- A interface respeita `prefers-reduced-motion`.
- O foco de teclado é visível em links, botões, campos e elementos expansíveis.
- Tabelas densas exibem uma visão essencial no menu simples e permitem acesso aos detalhes.
- Mensagens de erro orientam uma ação de recuperação.

## Implementação inicial desta entrega

- Menu principal reduzido para quatro tarefas.
- Recursos secundários preservados em “Mais opções”.
- Painel persistente de acessibilidade cognitiva.
- Menu simples ativado por padrão.
- Texto maior opcional.
- Redução de movimento ativada por padrão.
- Modo foco opcional.
- Link para pular diretamente ao conteúdo.
- Objetivo contextual em todas as páginas que usam o AppShell.
- Redução de colunas secundárias nas tabelas de avaliações e participantes quando o menu simples está ativo.
- Foco de teclado reforçado.

## Próximas entregas

1. Separar a tela de processos de avaliação em lista, composição, participantes e acompanhamento.
2. Separar Participantes de Novo convite.
3. Transformar criação e edição de avaliação em fluxo por etapas com salvamento automático.
4. Reduzir o dashboard a pendências, andamento e resultados.
5. Substituir termos técnicos visíveis por linguagem operacional, mantendo detalhes técnicos opcionais.
6. Realizar testes moderados com pessoas idosas, pessoas com TDAH e pessoas autistas, registrando tempo, erros, abandono e necessidade de ajuda.
