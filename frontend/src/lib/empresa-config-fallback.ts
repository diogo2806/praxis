import type { EmpresaConfig } from "@/lib/api/praxis";

/**
 * Configurações padrão embutidas para quando a API não responde.
 * Usadas como fallback quando o endpoint /api/v1/empresa-config falha.
 */
export const empresaConfigFallback: EmpresaConfig = {
  competencies: [
    { value: "communication", label: "Comunicação", locked: true, selectedByDefault: true },
    { value: "leadership", label: "Liderança", locked: true, selectedByDefault: false },
    { value: "teamwork", label: "Trabalho em Equipe", locked: true, selectedByDefault: false },
    { value: "problem-solving", label: "Resolução de Problemas", locked: true, selectedByDefault: false },
    { value: "critical-thinking", label: "Pensamento Crítico", locked: true, selectedByDefault: false },
  ],
  answerTimeLimits: [
    { value: "30", label: "30 segundos", locked: true, selectedByDefault: false },
    { value: "60", label: "1 minuto", locked: true, selectedByDefault: true },
    { value: "120", label: "2 minutos", locked: true, selectedByDefault: false },
    { value: "300", label: "5 minutos", locked: true, selectedByDefault: false },
    { value: "0", label: "Sem limite", locked: true, selectedByDefault: false },
  ],
};
