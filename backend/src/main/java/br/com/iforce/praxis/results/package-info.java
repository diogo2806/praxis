/**
 * Central de Resultados — onde a empresa acompanha e decide sobre os candidatos já avaliados.
 *
 * <p>Depois que um candidato faz uma avaliação, o que ele respondeu, quanto pontuou e em
 * quais competências se destacou passa a viver aqui. Este é o pacote da "tela de resultados"
 * do processo seletivo: ele reúne todas as avaliações, deixa o recrutador filtrar e comparar
 * candidatos, abrir o detalhe de cada um e registrar a decisão final.</p>
 *
 * <p>O fluxo, do ponto de vista de quem recruta:</p>
 * <ol>
 *   <li><b>Listar e filtrar</b> — ver todos os candidatos avaliados, filtrando por nome,
 *       avaliação, situação, origem ou período, com um resumo no topo (quantos concluíram,
 *       quantos estão em andamento, quantos expiraram e a nota média).</li>
 *   <li><b>Abrir o detalhe</b> — analisar um candidato a fundo: notas, desempenho por
 *       competência e o passo a passo do que ele respondeu na avaliação.</li>
 *   <li><b>Registrar a decisão</b> — dizer o que fazer com o candidato (avançar, reprovar,
 *       contratar ou deixar em espera), de forma auditável.</li>
 * </ol>
 *
 * <p>Um princípio guia todo o pacote: a plataforma organiza, resume e sugere, mas a decisão
 * de contratação continua sendo humana. Aqui apenas se <em>lê e apresenta</em> avaliações que
 * já aconteceram e se guarda a decisão da pessoa; não é onde a prova é aplicada nem onde a
 * nota é calculada.</p>
 */
package br.com.iforce.praxis.results;
