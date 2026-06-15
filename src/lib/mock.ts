export type SimStatus =
  | "rascunho"
  | "em-revisao"
  | "aprovada"
  | "publicada"
  | "piloto"
  | "arquivada";

export type Maturity =
  | "Rascunho"
  | "Piloto"
  | "Calibrada"
  | "Validada internamente"
  | "Expirada"
  | "Arquivada";

export interface Simulation {
  id: string;
  name: string;
  role: string;
  seniority: "Júnior" | "Pleno" | "Sênior";
  status: SimStatus;
  maturity: Maturity;
  version: string;
  quality: number;
  competencies: string[];
  updated: string;
  attempts: number;
  completion: number;
}

export const simulations: Simulation[] = [
  {
    id: "sim-001",
    name: "O Dia do Caos",
    role: "Analista de Atendimento",
    seniority: "Pleno",
    status: "publicada",
    maturity: "Calibrada",
    version: "v1.2",
    quality: 88,
    competencies: ["Empatia", "Resolução de Conflitos", "Aderência à Política"],
    updated: "há 2 dias",
    attempts: 142,
    completion: 0.91,
  },
  {
    id: "sim-002",
    name: "Negociar sem dar desconto",
    role: "Executivo de Vendas",
    seniority: "Sênior",
    status: "piloto",
    maturity: "Piloto",
    version: "v0.4",
    quality: 72,
    competencies: ["Negociação", "Comunicação", "Aderência à Política"],
    updated: "há 5 horas",
    attempts: 18,
    completion: 0.83,
  },
  {
    id: "sim-003",
    name: "Escalar problema corretamente",
    role: "Tech Lead",
    seniority: "Sênior",
    status: "em-revisao",
    maturity: "Rascunho",
    version: "v0.1",
    quality: 64,
    competencies: ["Tomada de Decisão", "Comunicação"],
    updated: "ontem",
    attempts: 0,
    completion: 0,
  },
  {
    id: "sim-004",
    name: "Recuperar cliente em risco",
    role: "Customer Success",
    seniority: "Pleno",
    status: "rascunho",
    maturity: "Rascunho",
    version: "v0.1",
    quality: 41,
    competencies: ["Empatia", "Negociação"],
    updated: "há 1 hora",
    attempts: 0,
    completion: 0,
  },
  {
    id: "sim-005",
    name: "Comunicar uma negativa",
    role: "Coordenador de Operações",
    seniority: "Pleno",
    status: "aprovada",
    maturity: "Validada internamente",
    version: "v2.0",
    quality: 94,
    competencies: ["Comunicação", "Empatia", "Liderança"],
    updated: "há 3 semanas",
    attempts: 412,
    completion: 0.88,
  },
];

export const statusMeta: Record<
  SimStatus,
  { label: string; tone: "muted" | "warn" | "info" | "ok" | "danger" }
> = {
  rascunho: { label: "Rascunho", tone: "muted" },
  "em-revisao": { label: "Em revisão", tone: "warn" },
  aprovada: { label: "Aprovada", tone: "info" },
  publicada: { label: "Publicada", tone: "ok" },
  piloto: { label: "Piloto", tone: "info" },
  arquivada: { label: "Arquivada", tone: "muted" },
};

export const wizardSteps = [
  { slug: "blueprint", label: "Blueprint", n: 0 },
  { slug: "objetivo", label: "Objetivo", n: 1 },
  { slug: "personagem", label: "Personagem", n: 2 },
  { slug: "dialogo", label: "Diálogo & Rubricas", n: 3 },
  { slug: "validador", label: "Validador", n: 3.5 },
  { slug: "piloto", label: "Piloto", n: 4 },
  { slug: "mapa", label: "Mapa & Score", n: 5 },
  { slug: "governanca", label: "Governança", n: 7 },
  { slug: "gupy", label: "Gupy Preflight", n: 8 },
] as const;

export type WizardSlug = (typeof wizardSteps)[number]["slug"];