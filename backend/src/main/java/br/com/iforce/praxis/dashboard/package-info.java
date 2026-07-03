/**
 * Painel inicial (dashboard) — a primeira tela que o RH vê ao entrar no sistema.
 *
 * <p>Enquanto os outros pacotes cuidam de <em>fazer</em> cada parte do processo (criar
 * avaliações, aplicar a prova, apurar resultados, cobrar), este pacote tem um papel
 * diferente: ele apenas <em>resume</em>. Reúne, em uma única visão, os números e os
 * atalhos que respondem à pergunta "como está minha operação hoje e o que devo fazer
 * a seguir?".</p>
 *
 * <p>O que o painel mostra, na visão de quem recruta:</p>
 * <ul>
 *   <li><b>Indicadores</b> — quantas avaliações estão ativas, quantas jornadas existem,
 *       quantos candidatos estão em andamento e quantas avaliações foram concluídas no
 *       último mês.</li>
 *   <li><b>Últimos resultados</b> — os candidatos avaliados mais recentemente, cada um
 *       com um atalho para acompanhar ou ver o detalhe.</li>
 *   <li><b>Jornadas</b> — as jornadas de avaliação da empresa e quantos candidatos estão
 *       em andamento em cada uma.</li>
 *   <li><b>Integrações</b> — se os canais (Gupy, Recrutei, API própria) estão conectados
 *       ou ainda precisam ser configurados.</li>
 *   <li><b>Plano e consumo</b> — o plano contratado, o saldo de créditos e o uso no
 *       período.</li>
 *   <li><b>Próximas ações</b> — sugestões priorizadas do que fazer agora (ex.: publicar
 *       uma jornada em rascunho, comprar créditos, configurar uma integração).</li>
 * </ul>
 *
 * <p>Este pacote não cria nem altera nada do processo: ele só lê dados que já existem
 * em outros lugares e os apresenta de forma consolidada e sempre restrita à empresa
 * logada, de modo que uma empresa nunca enxerga dados de outra.</p>
 */
package br.com.iforce.praxis.dashboard;
