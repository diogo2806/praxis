import type { ReactNode } from "react";

import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

/**
 * Gloss├írio central de termos t├®cnicos do produto.
 *
 * As defini├º├Áes usam linguagem simples e do cotidiano para que pessoas sem
 * forma├º├úo t├®cnica (por exemplo, estudantes em programas educacionais)
 * consigam entender a interface. Cada termo destacado com <Termo> mostra a
 * explica├º├úo ao passar o mouse ou ao focar pelo teclado.
 */
export const glossario = {
  "pontuacao-criterios":
    "Pontua├º├úo por crit├®rios definidos: cada ponto segue uma regra clara, n├úo por opini├úo.",
  "criterios-pontuacao":
    "Crit├®rios de pontua├º├úo: regras simples que dizem quantos pontos cada resposta vale.",
  "decisao-contexto":
    "Escolha baseada na situa├º├úo: registrar a decis├úo da pessoa diante de um cen├írio simulado, para apoiar a an├ílise do processo.",
  "trilha-auditavel":
    "Hist├│rico completo de altera├º├Áes: registro de todos os passos do teste, que pode ser conferido depois por outra pessoa.",
  "score-auditavel":
    "Cada ponto da nota pode ser conferido: d├í para ver de qual regra e de qual resposta ele veio.",
  blueprint: "Plano do teste: define o cargo, a situa├º├úo e o que ser├í medido.",
  validador: "Etapa que confere se a estrutura do teste est├í completa e coerente antes de entrar no ar.",
  calibracao:
    "An├ílise dos dados reais do piloto para encontrar ajustes necess├írios. N├úo ├® uma etapa obrigat├│ria para entender a tela.",
  "vazamento-prova":
    "Quando respostas ou crit├®rios internos circulam fora do p├║blico autorizado, reduzindo a utilidade do teste.",
  maturidade:
    "Prontid├úo do teste: indica se ele ainda est├í em rascunho, em revis├úo ou pronto para uso.",
  backend: "A parte do sistema que roda nos servidores, nos bastidores, fora da tela.",
  "caixa-preta":
    "Sistema que decide sem explicar como chegou ao resultado. Aqui o c├ílculo usa regras, pesos e crit├®rios consult├íveis.",
  "erro-critico": "Resposta marcada como cr├¡tica que gera um sinal para an├ílise da equipe respons├ível.",
  "pontuacao-normalizada":
    "Notas ajustadas para a mesma base, como provas escolares colocadas na mesma escala.",
  "julgamento-situacional":
    "Capacidade de tomar boas decis├Áes diante de situa├º├Áes do dia a dia do trabalho.",
  "evidencia-comportamental":
    "Indicadores derivados das escolhas realizadas pelo candidato diante de cen├írios simulados. Esses indicadores devem ser analisados em conjunto com outras etapas do processo seletivo.",
  construto: "O conceito que se quer medir, por exemplo lideran├ºa ou aten├º├úo a detalhes.",
  "pontuacao-deterministica":
    "A nota sai sempre das mesmas regras e c├ílculos: respostas iguais geram sempre a mesma nota.",
  versionamento: "Guardar cada altera├º├úo como uma nova vers├úo, sem apagar as anteriores.",
  auditlog:
    "Trilha cronol├│gica de auditoria: eventos relevantes registrados em ordem temporal para consulta por usu├írios autorizados.",
  wizard: "Guia passo a passo que conduz voc├¬ por cada etapa.",
  blocker: "Bloqueio: um problema que impede de avan├ºar at├® ser resolvido.",
  sjt: "Teste de Julgamento Situacional (SJT) ÔÇö teste que mostra como algu├®m decide diante de situa├º├Áes reais do trabalho.",
  determinisitco:
    "Nota calculada por regras fixas, sempre igual: as mesmas respostas geram sempre a mesma pontua├º├úo.",
  tenant: "Sua empresa ou cliente ÔÇö espa├ºo isolado onde voc├¬ cria e gerencia testes.",
  taxonomia: "Cat├ílogo de compet├¬ncias da sua empresa ÔÇö lista de habilidades que voc├¬ mede.",
  workspace: "├ürea de trabalho: seu espa├ºo pessoal para criar e gerenciar testes.",
  score: "Nota ou pontua├º├úo ÔÇö resultado num├®rico do teste.",
  defensabilidade:
    "Capacidade de demonstrar quais regras, crit├®rios, pesos, respostas e vers├Áes participaram do c├ílculo apresentado.",
  override: "Ajuste manual ou exce├º├úo ÔÇö quando algu├®m altera um resultado fora das regras normais.",
  explicabilidade:
    "Transpar├¬ncia do resultado: capacidade de explicar de forma clara por que o candidato recebeu aquela nota.",
} as const;

export type TermoId = keyof typeof glossario;

/**
 * Destaca um termo t├®cnico com uma defini├º├úo em linguagem simples, exibida
 * num tooltip acess├¡vel (mouse e teclado).
 */
export function Termo({
  id,
  children,
  className,
}: {
  id: TermoId;
  children?: ReactNode;
  className?: string;
}) {
  const definicao = glossario[id];

  return (
    <TooltipProvider delayDuration={150}>
      <Tooltip>
        <TooltipTrigger asChild>
          <button
            type="button"
            aria-label={`O que significa: ${typeof children === "string" ? children : id}`}
            className={cn(
              "cursor-help rounded-sm align-baseline underline decoration-dotted decoration-muted-foreground/60 underline-offset-2 transition-colors hover:decoration-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring",
              className,
            )}
          >
            {children ?? id}
          </button>
        </TooltipTrigger>
        <TooltipContent className="max-w-[260px] text-left text-xs font-normal leading-snug">
          {definicao}
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}
