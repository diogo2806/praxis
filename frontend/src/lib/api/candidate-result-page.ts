import { getApiBaseUrl } from "@/lib/runtime-config";

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

export async function getCandidateResultPage(token: string): Promise<CandidateResultPageResponse> {
  const response = await fetch(
    `${getApiBaseUrl()}/candidate/results/${encodeURIComponent(token)}`,
    { headers: { Accept: "application/json" } },
  );

  if (!response.ok) {
    let message = "Não foi possível carregar o resultado da avaliação.";
    try {
      const error = (await response.json()) as { message?: string };
      if (error.message) message = error.message;
    } catch {
      // Mantém a mensagem segura para respostas sem JSON.
    }
    throw new Error(message);
  }

  return response.json() as Promise<CandidateResultPageResponse>;
}
