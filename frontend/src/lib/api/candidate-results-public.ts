import { apiRequest } from "@/lib/api/http";
import { candidateLocaleQuery } from "@/lib/api/candidate-locale";

export interface CandidateResultItem {
  titulo: string;
  pontuacao: number;
  resultado: string;
}

export interface CandidateResultPage {
  avaliacaoNome: string;
  status: string;
  finalizado: boolean;
  redirectUrl?: string | null;
  concluidoEm?: string | null;
  resultados: CandidateResultItem[];
  pontuacaoBruta: number;
  pontuacaoMaximaCaminho: number;
  pontuacaoNormalizada: number;
  versaoAlgoritmoPontuacao: string;
}

export function getCandidateResult(token: string) {
  return apiRequest<CandidateResultPage>(
    `/candidate/results/${encodeURIComponent(token)}?${candidateLocaleQuery()}`,
  );
}
