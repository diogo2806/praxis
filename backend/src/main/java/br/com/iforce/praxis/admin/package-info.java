/**
 * Painel administrativo — como a equipe da plataforma cuida dos clientes (empresas).
 *
 * <p>Este é o pacote do "back-office". Enquanto os demais pacotes cuidam de montar avaliações,
 * aplicar a prova e apurar resultados <em>dentro</em> de cada cliente, aqui se responde a uma
 * pergunta de bastidor: <em>quem administra os clientes da plataforma e mantém tudo em ordem?</em>
 * O ator deste pacote é o operador ADMIN — uma pessoa da equipe da plataforma, e não um usuário
 * de um cliente. Por isso ele age de fora: em toda operação, o cliente alvo é informado
 * explicitamente, sem depender de um "cliente logado".</p>
 *
 * <p>O que o operador faz por aqui, na visão do processo:</p>
 * <ol>
 *   <li><b>Cadastrar e manter clientes</b> — abrir a conta de uma empresa nova (já criando o
 *       primeiro usuário responsável, normalmente com convite por link), editar o cadastro e
 *       ajustar plano e condição comercial.</li>
 *   <li><b>Controlar a situação do cliente</b> — colocar em teste, ativar, <b>suspender</b>
 *       (pausar o acesso), <b>reativar</b> (religar) e <b>cancelar</b> (encerrar a relação).
 *       Suspender e cancelar preservam o histórico: nada é apagado.</li>
 *   <li><b>Gerenciar acessos do cliente</b> — convidar novas pessoas para usar o sistema em nome
 *       da empresa, reenviar convites, bloquear e desbloquear usuários. Quem é convidado entra
 *       sempre como usuário da empresa, nunca como operador da plataforma.</li>
 *   <li><b>Liberar consumo</b> — conceder créditos de cortesia para um cliente voltar a rodar
 *       avaliações, sem alterar o plano comercial.</li>
 *   <li><b>Acompanhar e prestar contas</b> — ver o dashboard com o resumo da operação, medir o
 *       uso (avaliações concluídas) de cada cliente e consultar a trilha de auditoria.</li>
 * </ol>
 *
 * <p>Dois princípios governam tudo aqui:</p>
 * <ul>
 *   <li><b>Toda ação tem dono, motivo e registro.</b> Cada operação sensível exige um motivo e
 *       gera um evento na trilha de auditoria <em>append-only</em> (que só acrescenta e nunca
 *       apaga), guardando quem foi o operador e qual o cliente alvo. Até <em>consultar</em> o uso
 *       de um cliente vira um registro — afinal, é olhar dado de negócio de terceiros.</li>
 *   <li><b>Nada se perde.</b> Suspensão, cancelamento e bloqueio mudam o acesso, mas conservam
 *       avaliações, usuários e histórico. Assim, uma decisão sempre pode ser revista com o
 *       contexto completo à mão.</li>
 * </ul>
 *
 * <p>Situações de um cliente (empresa): <b>em teste</b> (avaliação inicial), <b>ativo</b>
 * (operação normal), <b>suspenso</b> (acesso pausado) e <b>cancelado</b> (relação encerrada). O
 * "uso" que aparece nas listas, na ficha e no dashboard significa sempre a mesma coisa: avaliações
 * efetivamente <em>concluídas</em> por candidatos no período — quem começou e abandonou não conta.</p>
 */
package br.com.iforce.praxis.admin;
