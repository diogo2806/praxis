import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const PORTABILITY_MANUALS: ScreenManualDefinition[] = [
  {
    id: "portabilidade-avaliacoes",
    title: "Portabilidade de avaliações",
    purpose:
      "Exportar conteúdo autoral versionado para backup lógico ou intercâmbio controlado e importar pacotes íntegros como novas avaliações independentes em rascunho.",
    flow: [
      "Abra uma versão na Central de Avaliações e exporte o pacote JSON quando precisar preservar ou transportar o conteúdo.",
      "Na importação, selecione o arquivo e execute a validação prévia; nenhuma informação é gravada nessa etapa.",
      "Revise erros por caminho, avisos de compatibilidade, competências e remapeamento planejado.",
      "Confirme as competências, informe o nome da nova avaliação e autorize explicitamente a importação.",
      "Abra a avaliação criada em rascunho, revise o conteúdo e passe pelos validadores e aprovações normais antes de publicar.",
    ],
    fields: [
      {
        name: "Arquivo JSON do Práxis",
        description:
          "Envelope no formato praxis-assessment-package/1.0 com manifesto, origem, conteúdo, mídias declaradas e hash SHA-256.",
      },
      {
        name: "Hash calculado",
        description:
          "Identificador de integridade recalculado sobre o manifesto canônico. Divergência bloqueia a importação.",
      },
      {
        name: "Erros e avisos",
        description:
          "Diagnósticos por caminho do manifesto. Erros bloqueiam; avisos exigem revisão, como licença de mídia não declarada.",
      },
      {
        name: "Remapeamento planejado",
        description:
          "Correspondência entre identificadores da origem e a nova avaliação, usada para evitar colisões e manter rastreabilidade.",
      },
      {
        name: "Nome da nova avaliação",
        description:
          "Nome independente da origem. A versão importada sempre nasce como versão 1 em rascunho.",
      },
      {
        name: "Confirmação de competências",
        description:
          "Aceite explícito para criar ou tratar como equivalentes as competências declaradas no pacote.",
      },
    ],
    permissions: [
      "Usuário autenticado com acesso à empresa e à avaliação de origem para exportar.",
      "Permissão de criação de avaliações na empresa atual para importar.",
      "O isolamento por empresa impede exportar versões de outra organização.",
    ],
    states: [
      "Sem contexto de exportação",
      "Pacote selecionado",
      "Validação em andamento",
      "Pacote rejeitado",
      "Pacote íntegro e importável",
      "Confirmação pendente",
      "Avaliação importada em rascunho",
    ],
    blocks: [
      "Formato de pacote incompatível.",
      "Hash divergente, indicando alteração ou corrupção.",
      "Grafo inválido, destino inexistente, pesos incorretos ou competência desconhecida.",
      "Referência de mídia insegura, executável, não declarada ou acima do limite.",
      "Confirmação de importação ou de competências ausente.",
      "Avaliação ou versão de origem inexistente ou fora da empresa atual.",
    ],
    examples: [
      "Exportar a versão publicada de uma avaliação para backup lógico antes de iniciar uma revisão ampla.",
      "Validar um pacote recebido de homologação, corrigir um destino inexistente apontado em $.manifest.version.nodes e importar novamente.",
      "Importar um modelo como nova avaliação em rascunho sem transportar candidatos, respostas, resultados, tokens ou credenciais.",
    ],
    shortcuts: [
      "Use a Central de Avaliações para abrir a portabilidade já com simulationId e versionNumber preenchidos.",
      "Execute sempre Validar sem gravar antes de habilitar a confirmação.",
      "Depois da importação, use Validador estrutural, Gabarito por especialistas e Governança antes da publicação.",
      "Consulte docs/PORTABILIDADE-AVALIACOES.md para o formato completo e a estratégia de evolução.",
    ],
    matches: (pathname) => pathname === "/nova/portabilidade",
  },
  {
    id: "catalogo-modelos-avaliacao",
    title: "Biblioteca de modelos de avaliação",
    purpose:
      "Pesquisar, comparar, favoritar, revisar e reutilizar modelos governados, gerando avaliações independentes sem alterar o modelo nem misturar resultados entre empresas.",
    flow: [
      "Use os filtros de negócio, competência, idioma e complexidade para localizar modelos internos, compartilhados ou oficiais disponíveis.",
      "Abra a prévia para revisar estrutura, duração, competências, requisitos de acessibilidade, evidências metodológicas e limitações de uso.",
      "Compare até três modelos antes de escolher a base mais adequada ao cargo e ao contexto.",
      "Ao cadastrar um modelo, selecione uma versão de origem, preencha a classificação e envie o rascunho para revisão independente.",
      "Depois da aprovação, use o modelo para criar uma nova avaliação. A cópia nasce como versão 1 em rascunho e permanece independente de mudanças futuras no catálogo.",
    ],
    fields: [
      {
        name: "Busca e filtros",
        description:
          "Pesquisa por título, resumo, cargo, área, setor, competência, senioridade, idioma, complexidade e favoritos.",
      },
      {
        name: "Escopo",
        description:
          "INTERNAL restringe o modelo à empresa; SHARED disponibiliza após aprovação administrativa; OFFICIAL identifica modelo mantido pelo Práxis.",
      },
      {
        name: "Versão de origem",
        description:
          "Avaliação e versão imutável usadas para gerar cópias. A publicação do modelo não modifica a origem.",
      },
      {
        name: "Evidências metodológicas",
        description:
          "Fundamentos, revisão técnica e evidências que justificam o uso do modelo.",
      },
      {
        name: "Limitações de uso",
        description:
          "Contextos, públicos ou decisões para os quais o modelo não deve ser utilizado sem adaptação e nova validação.",
      },
      {
        name: "Prévia",
        description:
          "Quantidade de cenários, finais e alternativas, duração, cobertura de competências, nó inicial e recursos de acessibilidade.",
      },
      {
        name: "Parecer da revisão",
        description:
          "Registro usado pelo revisor para aprovar ou rejeitar o modelo sem permitir autoaprovação pelo autor.",
      },
      {
        name: "Nome da nova avaliação",
        description:
          "Nome da cópia independente criada na empresa atual como versão 1 em rascunho.",
      },
    ],
    permissions: [
      "Usuários da empresa podem cadastrar modelos internos a partir de versões às quais têm acesso.",
      "Somente ADMIN pode propor ou aprovar modelos compartilhados e oficiais.",
      "O autor não pode aprovar o próprio modelo.",
      "Somente a empresa proprietária altera o cadastro; outras empresas apenas consultam e reutilizam modelos compartilhados ou oficiais aprovados.",
    ],
    states: [
      "DRAFT: cadastro ainda editável e não reutilizável.",
      "IN_REVIEW: aguardando parecer independente.",
      "APPROVED: disponível para reutilização conforme o escopo.",
      "REJECTED: devolvido ao proprietário para ajuste e novo envio.",
      "ARCHIVED: retirado do catálogo sem alterar cópias já criadas.",
    ],
    blocks: [
      "Versão de origem inexistente ou fora da empresa proprietária.",
      "Competência informada que não pertence à versão de origem.",
      "Tentativa de publicar modelo cuja origem ainda não está publicada.",
      "Tentativa de autoaprovação pelo autor.",
      "Modelo compartilhado ou oficial sem perfil ADMIN.",
      "Modelo não aprovado ao tentar criar uma avaliação.",
      "Modelo interno de outra empresa ou modelo não visível para o usuário atual.",
    ],
    examples: [
      "Filtrar por cargo Desenvolvedor Java, senioridade Sênior e competência Tomada de decisão para comparar modelos antes de criar a avaliação.",
      "Publicar internamente uma avaliação validada, receber parecer de outro revisor e disponibilizá-la somente para a própria empresa.",
      "Criar uma cópia de um modelo oficial; depois atualizar o catálogo sem alterar a versão já copiada pelo cliente.",
    ],
    shortcuts: [
      "Marque modelos recorrentes como favoritos para usar o filtro Somente favoritos.",
      "Selecione até três cartões em Comparar para revisar diferenças lado a lado.",
      "Abra a tela com simulationId e versionNumber para cadastrar diretamente aquela versão como modelo.",
      "Depois de criar a cópia, use o Validador estrutural, o Gabarito por especialistas e a Governança antes de publicar.",
      "Consulte docs/CATALOGO-MODELOS-AVALIACAO.md para o processo completo.",
    ],
    matches: (pathname) => pathname === "/avaliacoes/modelos",
  },
];

export function resolvePortabilityManual(pathname: string) {
  return PORTABILITY_MANUALS.find((manual) => manual.matches(pathname));
}
