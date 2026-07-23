export type ParticipacaoStatus =
  | "nao_iniciada"
  | "em_andamento"
  | "concluida"
  | "abandonada"
  | "expirada";

export type CandidateMediaType = "IMAGE" | "AUDIO" | "VIDEO";

export interface CandidateOptionContract {
  id: string;
  texto: string;
  descricaoAcessivel?: string | null;
  audioDescricaoUrl?: string | null;
  mediaUrl?: string | null;
  tipoMidia?: CandidateMediaType | null;
  transcricaoMidia?: string | null;
  legendaMidiaUrl?: string | null;
  versaoMidia?: string | null;
  proximaEtapaId?: string | null;
}

export interface CandidateNodeContract {
  id: string;
  numero: number;
  pessoa: string;
  descricao: string;
  descricaoAcessivel?: string | null;
  tempoLimiteSegundos: number | null;
  tempoLimiteSegundosAcomodado?: number | null;
  audioDescricaoUrl?: string | null;
  midiaUrl?: string | null;
  tipoMidia?: CandidateMediaType | null;
  transcricaoMidia?: string | null;
  legendaMidiaUrl?: string | null;
  versaoMidia?: string | null;
  proximaEtapaTempoEsgotadoId?: string | null;
  alternativas: CandidateOptionContract[];
}

export interface CandidateProgressContract {
  passoAtual: number;
  passosEstimados: number;
  percentual: number;
}

export interface CandidateAttemptContract {
  participacaoId: string;
  avaliacaoNome: string;
  status: ParticipacaoStatus;
  finalizado: boolean;
  redirectUrl?: string | null;
  acaoSugeridaFrontend?: "INICIAR" | "CONTINUAR_TESTE" | "VER_RESULTADOS";
  progresso: CandidateProgressContract;
  etapaAtual: CandidateNodeContract | null;
  verticalSaude?: boolean;
}

export interface SubmitAnswerRequestContract {
  etapaId?: string | null;
  etapaNumero?: number | null;
  respostaId?: string | null;
  respondidaEm?: string | null;
  tempoEsgotado: boolean;
}

export interface SubmitAnswerResponseContract {
  participacaoId: string;
  status: ParticipacaoStatus;
  repetida: boolean;
  finalizado: boolean;
  redirectUrl?: string | null;
  progresso: CandidateProgressContract;
  etapaAtual: CandidateNodeContract | null;
}
