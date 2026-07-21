import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const PUBLICATION_MANUALS: ScreenManualDefinition[] = [
  {
    id: "piloto-indicadores",
    title: "Piloto e indicadores",
    purpose:
      "Acompanhar os indicadores reais e a calibração de uma avaliação e versão específicas antes ou depois da publicação, sem manter uma segunda lista global de avaliações.",
    flow: [
      "Abra uma avaliação e versão em Avaliações, Validador ou Governança.",
      "Acesse Piloto e indicadores preservando o identificador e a versão no endereço.",
      "Revise tentativas criadas, em andamento, concluídas, abandonadas e expiradas.",
      "Analise as taxas de conclusão e desistência.",
      "Consulte a calibração calculada a partir das tentativas concluídas.",
      "Retorne ao Validador ou abra o Mapa usando os atalhos contextuais.",
    ],
    fields: [
      {
        name: "Avaliação e versão",
        description: "Contexto obrigatório recebido do fluxo de autoria; não pode ser escolhido em uma lista global nesta tela.",
      },
      {
        name: "Criadas",
        description: "Quantidade total de tentativas geradas para a versão selecionada.",
      },
      {
        name: "Em andamento e concluídas",
        description: "Tentativas atualmente abertas e tentativas finalizadas com resultado.",
      },
      {
        name: "Abandonadas e expiradas",
        description: "Tentativas interrompidas pela pessoa participante ou encerradas por validade.",
      },
      {
        name: "Conclusão e desistência",
        description: "Percentuais calculados pelo backend para a versão contextual.",
      },
      {
        name: "Calibração",
        description: "Análise agregada das tentativas concluídas para apoiar revisão de critérios e pesos.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com acesso à avaliação.",
      "A avaliação e a versão devem pertencer à empresa autenticada.",
    ],
    states: [
      "Contexto ausente com retorno para Avaliações",
      "Carregando monitoramento",
      "Indicadores disponíveis",
      "Calculando calibração",
      "Sem tentativas concluídas suficientes",
      "Erro de carregamento",
    ],
    blocks: [
      "Identificador da avaliação ou número da versão ausente.",
      "Avaliação ou versão inexistente ou sem acesso.",
      "Falha ao carregar monitoramento ou calibração.",
      "Ausência de tentativas concluídas para produzir calibração representativa.",
    ],
    examples: [
      "Abrir o piloto da avaliação Atendimento ao cliente, versão 3, a partir do Validador.",
      "Comparar a taxa de conclusão com o volume de tentativas abandonadas antes da publicação definitiva.",
    ],
    shortcuts: [
      "Use Voltar: Validador para revisar bloqueios da mesma versão.",
      "Use Ver mapa e pontuação para consultar o fluxo sem perder o contexto.",
      "Quando faltar contexto, use Ir para Avaliações e abra a versão desejada.",
    ],
    matches: (pathname) => pathname === "/nova/piloto",
  },
  {
    id: "governanca-publicacao",
    title: "Governança e publicação",
    purpose:
      "Controlar aceite de termos, publicação e auditoria de uma avaliação e versão, com confirmação acessível antes de colocar a versão no ar.",
    flow: [
      "Abra a avaliação e versão que será publicada.",
      "Revise o estado atual e o registro de auditoria.",
      "Leia e aceite o termo de responsabilidade.",
      "Quando aplicável, aceite também o termo de uso na vertical de saúde.",
      "Clique em Publicar e revise o impacto no diálogo de confirmação.",
      "Confirme a publicação e acompanhe o novo estado e o evento de auditoria.",
    ],
    fields: [
      {
        name: "Estado atual",
        description: "Situação inferida pelos eventos de auditoria: rascunho ou no ar.",
      },
      {
        name: "Termo de responsabilidade",
        description: "Aceite obrigatório associado ao usuário autenticado antes da publicação.",
      },
      {
        name: "Termo de uso em saúde",
        description: "Aceite adicional apresentado quando o backend identifica operação na vertical de saúde.",
      },
      {
        name: "Publicar",
        description: "Inicia a confirmação que protege a versão contra alterações depois de colocada no ar.",
      },
      {
        name: "Registro de auditoria",
        description: "Eventos reais da versão, com mensagem, identificador e data.",
      },
    ],
    permissions: [
      "Usuário autenticado da empresa com permissão para publicar avaliações.",
      "A avaliação e a versão devem pertencer à empresa autenticada.",
      "Os termos exigidos devem estar aceitos pelo usuário responsável.",
    ],
    states: [
      "Sem avaliação ou versão selecionada",
      "Carregando governança",
      "Rascunho",
      "Confirmação de publicação aberta",
      "Publicando",
      "No ar",
      "Transição recusada",
    ],
    blocks: [
      "Avaliação ou versão não informada.",
      "Termo de responsabilidade ainda não aceito.",
      "Termo da vertical de saúde pendente quando aplicável.",
      "Bloqueios críticos identificados pelo backend.",
      "Versão inexistente, sem acesso ou incompatível com a transição.",
      "Falha de conexão durante o aceite ou a publicação.",
    ],
    examples: [
      "Aceitar o termo de responsabilidade e publicar a versão 2 de uma avaliação validada.",
      "Receber bloqueio da vertical de saúde, aceitar o termo adicional e repetir a publicação.",
    ],
    shortcuts: [
      "Use Voltar: Revisão para corrigir bloqueios antes de publicar.",
      "Use Gupy: verificação para executar o preflight da mesma versão.",
      "Pressione Escape ou use Cancelar para fechar a confirmação quando a publicação ainda não começou.",
    ],
    matches: (pathname) => pathname === "/nova/governanca",
  },
];

export function resolvePublicationManual(pathname: string) {
  return PUBLICATION_MANUALS.find((manual) => manual.matches(pathname));
}
