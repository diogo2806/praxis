import type { ScreenManualDefinition } from "@/lib/screen-manuals";

const previewJourneyManual: ScreenManualDefinition = {
  id: "previa-jornada-candidato",
  title: "Prévia da jornada do candidato",
  purpose:
    "Permitir que a equipe de autoria percorra a avaliação como candidata e confirme conteúdo, ramificações e cobertura antes da publicação, sem criar uma participação oficial.",
  flow: [
    "Abra uma avaliação e versão pela criação, pelo Editor de diálogo, pelo Mapa ou pelo Validador.",
    "Selecione Testar como candidato para iniciar o percurso na etapa raiz.",
    "Escolha alternativas e simule o tempo esgotado quando houver limite configurado.",
    "Acompanhe a cobertura de etapas, alternativas e encerramentos no painel lateral.",
    "Revise problemas de fluxo e pendências do Validador e abra diretamente o editor responsável.",
    "Reinicie a prévia para percorrer outros caminhos até cobrir os encerramentos relevantes.",
  ],
  fields: [
    {
      name: "Caminho atual",
      description: "Sequência de identificadores técnicos percorrida somente nesta sessão de prévia.",
    },
    {
      name: "Etapa e alternativas",
      description: "Conteúdo apresentado como na execução do candidato, sem registrar respostas oficiais.",
    },
    {
      name: "Cobertura",
      description: "Percentual e quantidade de etapas, alternativas e finais já visitados na sessão.",
    },
    {
      name: "Problemas de fluxo",
      description: "Destinos ausentes, etapas inexistentes, ciclos, caminhos longos e finais inacessíveis.",
    },
    {
      name: "Pendências do Validador",
      description: "Bloqueios e avisos calculados pelas regras reais de publicação, com atalho de correção.",
    },
  ],
  permissions: [
    "Usuário autenticado com acesso à avaliação e à área de autoria.",
    "Especialista parceiro pode usar a prévia nas avaliações em que possui permissão de edição.",
    "A prévia é somente leitura e pode ser usada também em versão publicada ou arquivada para conferência.",
  ],
  states: [
    "Sem avaliação ou versão informada",
    "Carregando jornada e validação",
    "Percurso em andamento",
    "Tempo esgotado simulado",
    "Final alcançado",
    "Caminho interrompido por destino inválido",
    "Limite de segurança atingido",
    "Cobertura parcial ou completa",
  ],
  blocks: [
    "Avaliação ou versão não informada.",
    "Etapa raiz inexistente.",
    "Alternativa sem destino ou apontando para etapa inexistente.",
    "Etapa não final sem alternativas.",
    "Ciclo que ultrapassa o limite de segurança de transições.",
    "Falha ao carregar a versão ou executar o Validador.",
  ],
  examples: [
    "Percorrer a alternativa 1 da etapa inicial, alcançar o final A e reiniciar para testar o final B.",
    "Simular o tempo esgotado e descobrir que o destino configurado não existe.",
    "Abrir diretamente no Mapa uma alternativa sem destino indicada pelo Validador.",
  ],
  shortcuts: [
    "Use Reiniciar prévia para testar outro caminho sem recarregar a página.",
    "Use Voltar ao Validador para revisar todos os diagnósticos da versão.",
    "A prévia nunca cria participação, não consome crédito e não gera resultado oficial.",
    "Consulte o processo completo na Central de manuais.",
  ],
  matches: (pathname) => pathname === "/nova/previa",
};

export function resolvePreviewJourneyManual(pathname: string) {
  return previewJourneyManual.matches(pathname) ? previewJourneyManual : undefined;
}
