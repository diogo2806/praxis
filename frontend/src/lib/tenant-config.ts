import { useQuery } from "@tanstack/react-query";
import { getTenantConfig, type TenantConfig } from "@/lib/api/praxis";

/**
 * Padroes de fallback usados enquanto a configuracao do tenant carrega ou quando a
 * API esta indisponivel. Espelham os defaults do backend (TenantConfigDefaults), que
 * por sua vez reproduzem as listas que antes eram fixas no codigo das telas.
 */
export const DEFAULT_TENANT_CONFIG: TenantConfig = {
  competencies: [
    "Empatia",
    "Resolução de Conflitos",
    "Aderência à Política",
    "Comunicação",
    "Negociação",
    "Tomada de Decisão",
    "Liderança",
    "Proatividade",
  ].map((value) => ({ value, label: value, locked: false, selectedByDefault: false })),
  seniorityLevels: [
    { value: "Júnior", label: "Júnior", locked: false, selectedByDefault: false },
    { value: "Pleno", label: "Pleno", locked: false, selectedByDefault: true },
    { value: "Sênior", label: "Sênior", locked: false, selectedByDefault: false },
  ],
  languageChecklist: [
    "Evita regionalismo desnecessario",
    "Nao usa estereotipo de classe",
    "Sem marcador de genero sem necessidade",
    "Sem referencia a idade, sotaque, origem ou crenca",
    "Linguagem compativel com o cargo avaliado",
  ].map((value) => ({ value, label: value, locked: false, selectedByDefault: false })),
  resultUses: [
    { value: "Triagem", label: "Triagem", locked: false, selectedByDefault: true },
    { value: "Ranking", label: "Ranking", locked: false, selectedByDefault: false },
    {
      value: "Apoio à entrevista",
      label: "Apoio à entrevista",
      locked: false,
      selectedByDefault: false,
    },
    { value: "Decisão final", label: "Decisão final", locked: true, selectedByDefault: false },
  ],
  answerTimeLimits: [
    { value: "0", label: "Sem limite", locked: false, selectedByDefault: false },
    { value: "30", label: "30 s", locked: false, selectedByDefault: false },
    { value: "45", label: "45 s", locked: false, selectedByDefault: true },
    { value: "60", label: "60 s", locked: false, selectedByDefault: false },
  ],
};

/**
 * Carrega os catalogos configuraveis do tenant. Sempre devolve uma config utilizavel:
 * os padroes de fallback enquanto a query resolve ou em caso de erro de rede.
 */
export function useTenantConfig() {
  const query = useQuery({
    queryKey: ["tenant-config"],
    queryFn: getTenantConfig,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  return {
    config: query.data ?? DEFAULT_TENANT_CONFIG,
    isLoading: query.isLoading,
    isFallback: !query.data,
  };
}

/**
 * Limite de tempo (em segundos) marcado como padrao para novos turnos. Retorna null
 * quando o padrao configurado e "sem limite".
 */
export function defaultAnswerTimeLimitSeconds(config: TenantConfig): number | null {
  const selected =
    config.answerTimeLimits.find((option) => option.selectedByDefault) ??
    config.answerTimeLimits[0];
  if (!selected || selected.value === "0") {
    return null;
  }
  const parsed = Number(selected.value);
  return Number.isNaN(parsed) ? null : parsed;
}
