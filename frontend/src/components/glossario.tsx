import type { ReactNode } from "react";

import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

/**
 * Glossário central de termos técnicos do produto.
 *
 * As definições usam linguagem simples e do cotidiano para que pessoas sem
 * formação técnica (por exemplo, estudantes em programas educacionais)
 * consigam entender a interface. Cada termo destacado com <Termo> mostra a
 * explicação ao passar o mouse ou ao focar pelo teclado.
 */
export const glossario = {
  "pontuacao-criterios":
    "Pontuação por critérios definidos: cada ponto segue uma regra clara, não por opinião.",
  "criterios-pontuacao":
    "Critérios de pontuação: regras simples que dizem quantos pontos cada resposta vale.",
  "decisao-contexto":
    "Escolha baseada na situação: avaliar a pessoa pela decisão que toma diante de um cenário real, e não por uma prova teórica.",
  "trilha-auditavel":
    "Histórico completo de alterações: registro de todos os passos da avaliação, que pode ser conferido depois por outra pessoa.",
  "score-auditavel":
    "Cada ponto da nota pode ser conferido: dá para ver de qual regra e de qual resposta ele veio.",
  blueprint: "Plano da avaliação: define o cargo, a situação e o que será medido.",
  validador:
    "Etapa que confere se a avaliação realmente mede o que prometeu antes de entrar no ar.",
  calibracao:
    "Análise dos dados reais do piloto para encontrar ajustes necessários. Não é uma etapa obrigatória para entender a tela.",
  "vazamento-prova":
    "Quando as respostas certas vazam e a avaliação perde o valor; aqui isso é monitorado.",
  maturidade:
    "Prontidão da avaliação: indica se ela ainda está em rascunho, em revisão ou pronta para uso.",
  backend: "A parte do sistema que roda nos servidores, nos bastidores, fora da tela.",
  "caixa-preta":
    "Sistema que decide sem explicar como chegou ao resultado. Aqui evitamos isso: tudo é explicado.",
  "erro-critico": "Uma falha grave na resposta que obriga uma pessoa a revisar o resultado.",
  "pontuacao-normalizada":
    "Notas ajustadas para a mesma base, como provas escolares colocadas na mesma escala.",
  "julgamento-situacional":
    "Capacidade de tomar boas decisões diante de situações do dia a dia do trabalho.",
  "evidencia-comportamental":
    "Provas baseadas em como a pessoa age, e não apenas no que ela diz saber.",
  construto: "O conceito que se quer medir, por exemplo liderança ou atenção a detalhes.",
  "pontuacao-deterministica":
    "A nota sai sempre das mesmas regras e cálculos: respostas iguais geram sempre a mesma nota.",
  versionamento: "Guardar cada alteração como uma nova versão, sem apagar as anteriores.",
  auditlog:
    "Registro de auditoria: lista, em ordem, tudo o que aconteceu, e não pode ser alterada.",
  wizard: "Guia passo a passo que conduz você por cada etapa.",
  blocker: "Bloqueio: um problema que impede de avançar até ser resolvido.",
  "sjt": "Teste de Julgamento Situacional (SJT) — avaliação que mostra como alguém decide diante de situações reais do trabalho.",
  "determinisitco": "Nota calculada por regras fixas, sempre igual: as mesmas respostas geram sempre a mesma pontuação.",
  tenant: "Sua empresa ou cliente — espaço isolado onde você cria e gerencia avaliações.",
  taxonomia: "Catálogo de competências da sua empresa — lista de habilidades que você mede.",
  workspace: "Área de trabalho: seu espaço pessoal para criar e gerenciar simulações.",
  score: "Nota ou pontuação — resultado numérico da avaliação.",
  defensabilidade: "Confiabilidade e segurança técnica: por que o resultado dessa avaliação se sustenta (inclusive se contestado na justiça).",
  override: "Ajuste manual ou exceção — quando alguém altera um resultado fora das regras normais.",
  "explicabilidade": "Transparência do resultado: capacidade de explicar de forma clara por que o candidato recebeu aquela nota.",
} as const;

export type TermoId = keyof typeof glossario;

/**
 * Destaca um termo técnico com uma definição em linguagem simples, exibida
 * num tooltip acessível (mouse e teclado).
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
