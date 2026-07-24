import { apiRequest } from "@/lib/api/http";

const SUPPORTED_LOCALES = ["pt-BR", "en", "es-MX"] as const;

export function getCandidateContentLocale() {
  if (typeof window === "undefined") return "pt-BR";
  const url = new URL(window.location.href);
  const requested = url.searchParams.get("locale") ?? url.searchParams.get("lang");
  const stored = window.localStorage.getItem("praxis-language");
  const browser = navigator.language;
  return normalizeSupportedLocale(requested ?? stored ?? browser);
}

export function candidateLocaleQuery() {
  return `locale=${encodeURIComponent(getCandidateContentLocale())}`;
}

export function listCandidateLocales(token: string) {
  return apiRequest<string[]>(`/candidate/attempts/${encodeURIComponent(token)}/locale/available`);
}

export function selectCandidateLocale(
  token: string,
  locale: string,
  source: "INVITATION" | "ATS" | "CANDIDATE" = "CANDIDATE",
) {
  return apiRequest<{
    requestedLocale: string;
    selectedLocale: string;
    source: string;
    fallbackApplied: boolean;
    availableLocales: string[];
  }>(`/candidate/attempts/${encodeURIComponent(token)}/locale`, {
    method: "POST",
    body: JSON.stringify({ locale: normalizeSupportedLocale(locale), source }),
  });
}

function normalizeSupportedLocale(value: string) {
  const normalized = value.replace("_", "-").toLowerCase();
  if (normalized.startsWith("en")) return "en";
  if (normalized.startsWith("es")) return "es-MX";
  return SUPPORTED_LOCALES[0];
}
