import { useQuery } from "@tanstack/react-query";
import { getEmpresaConfig, type EmpresaConfig } from "@/lib/api/praxis";

/**
 * Carrega os catalogos configuraveis do empresa diretamente do backend.
 * Sem fallback local: se a API falhar, a tela deve exibir erro e bloquear a acao.
 */
export function useEmpresaConfig() {
  const query = useQuery({
    queryKey: ["empresa-config"],
    queryFn: getEmpresaConfig,
    retry: false,
    staleTime: 5 * 60 * 1000,
  });

  return {
    config: query.data ?? null,
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
