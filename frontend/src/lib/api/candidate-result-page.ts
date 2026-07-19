import { apiRequest } from "@/lib/api/http";

export type CandidateResultPageStatus =
  | "nao_iniciada"
  | "em_andamento"
  | "concluida"
  | "abandonada"
  | "expirada";

export interface CandidateResultItemResponse {
  titulo: string;
  pontuacao: number;
  resultado: string;
}

export interface CandidateResultPageResponse {
  avaliacaoNome: string;
  status: CandidateResultPageStatus;
  finalizado: boolean;
  redirectUrl?: string | null;
  concluidoEm?: string | null;
  resultados: CandidateResultItemResponse[];
}

export function getCandidateResultPage(
  token: string,
): Promise<CandidateResultPageResponse> {
  return apiRequest<CandidateResultPageResponse>(
    `/candidate/results/${encodeURIComponent(token)}`,
    {},
    {
      authenticated: false,
      fallbackMessage: "Não foi possível carregar o resultado da avaliação.",
    },
  );
}
