import { useQuery } from "@tanstack/react-query";
import { getEmpresaConfig, type EmpresaConfig } from "@/lib/api/praxis";
import { empresaConfigFallback } from "@/lib/empresa-config-fallback";

/**
 * Carrega os catalogos configuraveis da empresa do backend.
 * Se a API falhar (incluindo 404 ou autenticação), usa configurações padrão embutidas.
 */
export function useEmpresaConfig() {
  const query = useQuery({
    queryKey: ["empresa-config"],
    queryFn: getEmpresaConfig,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  return {
    config: query.data ?? empresaConfigFallback,
    isLoading: query.isLoading,
    isError: query.isError,
    error: query.error,
  };
}

/**
 * Limite de tempo (em segundos) marcado como padrao para novos etapas. Retorna null
 * quando o padrao configurado e "sem limite".
 */
export function defaultAnswerTimeLimitSeconds(config: EmpresaConfig): number | null {
  const selected =
    config.answerTimeLimits.find((option) => option.selectedByDefault) ??
    config.answerTimeLimits[0];
  if (!selected || selected.value === "0") {
    return null;
  }
  const parsed = Number(selected.value);
  return Number.isNaN(parsed) ? null : parsed;
}
