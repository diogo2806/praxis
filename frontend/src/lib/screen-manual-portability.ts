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
];

export function resolvePortabilityManual(pathname: string) {
  return PORTABILITY_MANUALS.find((manual) => manual.matches(pathname));
}
