import { apiRequest } from "@/lib/api/http";

export type InterviewQuestionSource = "RULE" | "EVIDENCE" | "INTERVIEWER";

export interface ResultEvidence {
  reference: string;
  stepTitle: string;
  observedAnswer: string;
  configuredInterpretation: string | null;
  critical: boolean;
  competencyScores: Record<string, number>;
}

export interface ResultCompetencyInsight {
  name: string;
  score: number;
  level: string;
  interpretation: string;
  evidenceCount: number;
  evidenceReferences: string[];
}

export interface ResultInterviewQuestion {
  id: string;
  competency: string | null;
  question: string;
  sourceType: InterviewQuestionSource;
  evidenceReference: string | null;
}

export interface ResultExecutiveReportResponse {
  attemptId: string;
  simulationTitle: string;
  simulationVersionNumber: number | null;
  generatedAt: string;
  summary: {
    competencies: ResultCompetencyInsight[];
    criticalEvidence: ResultEvidence[];
    deepDiveCompetencies: string[];
    limitations: string[];
  };
  interviewGuide: {
    questions: ResultInterviewQuestion[];
    interviewerNotes: string | null;
    persisted: boolean;
    savedBy: string | null;
    savedAt: string | null;
  };
  auditTrail: Array<{
    eventType: string;
    message: string;
    createdAt: string;
  }>;
}

export interface SaveInterviewGuideRequest {
  questions: ResultInterviewQuestion[];
  interviewerNotes?: string | null;
}

export function getResultExecutiveReport(attemptId: string) {
  return apiRequest<ResultExecutiveReportResponse>(
    `/api/v1/results/${encodeURIComponent(attemptId)}/executive-report`,
    undefined,
    { fallbackMessage: "Não foi possível carregar o relatório executivo." },
  );
}

export function saveResultInterviewGuide(attemptId: string, body: SaveInterviewGuideRequest) {
  return apiRequest<void>(
    `/api/v1/results/${encodeURIComponent(attemptId)}/interview-guide`,
    {
      method: "POST",
      body: JSON.stringify(body),
    },
    { fallbackMessage: "Não foi possível registrar o roteiro de entrevista." },
  );
}
