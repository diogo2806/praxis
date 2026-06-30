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
    "Escolha baseada na situação: registrar a decisão da pessoa diante de um cenário simulado, para apoiar a análise do processo.",
  "trilha-auditavel":
    "Histórico completo de alterações: registro de todos os passos do teste, que pode ser conferido depois por outra pessoa.",
  "score-auditavel":
    "Cada ponto da nota pode ser conferido: dá para ver de qual regra e de qual resposta ele veio.",
  blueprint: "Plano do teste: define o cargo, a situação e o que será medido.",
  validador: "Etapa que confere se a estrutura do teste está completa e coerente antes de entrar no ar.",
  calibracao:
    "Análise dos dados reais do piloto para encontrar ajustes necessários. Não é uma etapa obrigatória para entender a tela.",
  "vazamento-prova":
    "Quando respostas ou critérios internos circulam fora do público autorizado, reduzindo a utilidade do teste.",
  maturidade:
    "Prontidão do teste: indica se ele ainda está em rascunho, em revisão ou pronto para uso.",
  backend: "A parte do sistema que roda nos servidores, nos bastidores, fora da tela.",
  "caixa-preta":
    "Sistema que decide sem explicar como chegou ao resultado. Aqui o cálculo usa regras, pesos e critérios consultáveis.",
  "erro-critico": "Resposta marcada como crítica que gera um sinal para análise da equipe responsável.",
  "pontuacao-normalizada":
    "Notas ajustadas para a mesma base, como provas escolares colocadas na mesma escala.",
  "julgamento-situacional":
    "Capacidade de tomar boas decisões diante de situações do dia a dia do trabalho.",
  "evidencia-comportamental":
    "Indicadores derivados das escolhas realizadas pela pessoa participante diante dos cenários apresentados. Devem ser analisados em conjunto com outras informações relevantes para o contexto da avaliação.",
  construto: "O conceito que se quer medir, por exemplo liderança ou atenção a detalhes.",
  "pontuacao-deterministica":
    "A nota sai sempre das mesmas regras e cálculos: respostas iguais geram sempre a mesma nota.",
  versionamento: "Guardar cada alteração como uma nova versão, sem apagar as anteriores.",
  auditlog:
    "Trilha cronológica de auditoria: eventos relevantes registrados em ordem temporal para consulta por usuários autorizados.",
  wizard: "Guia passo a passo que conduz você por cada etapa.",
  blocker: "Bloqueio: um problema que impede de avançar até ser resolvido.",
  sjt: "Teste de Julgamento Situacional (SJT) — teste que mostra como alguém decide diante de situações reais do trabalho.",
  determinisitco:
    "Nota calculada por regras fixas, sempre igual: as mesmas respostas geram sempre a mesma pontuação.",
  empresa: "Sua empresa ou cliente — espaço isolado onde você cria e gerencia testes.",
  taxonomia: "Catálogo de competências da sua empresa — lista de habilidades que você mede.",
  workspace: "Área de trabalho: seu espaço pessoal para criar e gerenciar testes.",
  score: "Nota ou pontuação — resultado numérico do teste.",
  defensabilidade:
    "Capacidade de demonstrar quais regras, critérios, pesos, respostas e versões participaram do cálculo apresentado.",
  override: "Ajuste manual ou exceção — quando alguém altera um resultado fora das regras normais.",
  explicabilidade:
    "Capacidade de demonstrar quais critérios, pesos, respostas e regras participaram da formação da pontuação apresentada.",
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
