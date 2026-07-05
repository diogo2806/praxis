"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import type { Language } from "./translations";
import { getTranslations } from "./translations";

interface LanguageContextType {
  language: Language;
  setLanguage: (language: Language) => void;
  t: ReturnType<typeof getTranslations>;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

const LANGUAGE_STORAGE_KEY = "praxis-language";

const localizedDocumentMetadata: Record<
  Language,
  { title: string; description: string; socialTitle: string; socialDescription: string }
> = {
  "pt-BR": {
    title: "Práxis — Avaliações por cenários estruturadas e rastreáveis",
    description:
      "Crie avaliações por cenários, configure critérios e pesos, compartilhe por link e acompanhe respostas, indicadores e registros do percurso.",
    socialTitle: "Práxis - Avaliações situacionais",
    socialDescription:
      "Crie cenários interativos, compartilhe avaliações por link e acompanhe indicadores definidos previamente pela sua equipe.",
  },
  en: {
    title: "Praxis — Structured and traceable scenario-based assessments",
    description:
      "Create scenario-based assessments, configure criteria and weights, share them by link, and follow responses, indicators, and activity records.",
    socialTitle: "Praxis - Situational assessments",
    socialDescription:
      "Create interactive scenarios, share assessments by link, and follow indicators defined in advance by your team.",
  },
  "es-MX": {
    title: "Praxis — Evaluaciones por escenarios estructuradas y rastreables",
    description:
      "Crea evaluaciones por escenarios, configura criterios y ponderaciones, compártelas por enlace y sigue respuestas, indicadores y registros del recorrido.",
    socialTitle: "Praxis - Evaluaciones situacionales",
    socialDescription:
      "Crea escenarios interactivos, comparte evaluaciones por enlace y sigue indicadores definidos previamente por tu equipo.",
  },
};

const isKnownLanguage = (value: string | null): value is Language =>
  value === "pt-BR" || value === "en" || value === "es-MX";

function updateDocumentMetadata(language: Language) {
  const metadata = localizedDocumentMetadata[language];
  document.title = metadata.title;

  const setMetaContent = (selector: string, content: string) => {
    document.querySelector<HTMLMetaElement>(selector)?.setAttribute("content", content);
  };

  setMetaContent('meta[name="description"]', metadata.description);
  setMetaContent('meta[property="og:title"]', metadata.socialTitle);
  setMetaContent('meta[property="og:description"]', metadata.socialDescription);
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  // O primeiro render do cliente precisa bater com o HTML do servidor (pt-BR);
  // a preferência salva é aplicada depois da hidratação, no efeito abaixo.
  const [language, setLanguageState] = useState<Language>("pt-BR");

  useEffect(() => {
    try {
      const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
      if (isKnownLanguage(stored)) {
        setLanguageState(stored);
        document.documentElement.lang = stored;
      }
    } catch {
      // localStorage pode estar indisponível
    }
  }, []);

  useEffect(() => {
    updateDocumentMetadata(language);
  }, [language]);

  const setLanguage = (newLanguage: Language) => {
    setLanguageState(newLanguage);
    try {
      localStorage.setItem(LANGUAGE_STORAGE_KEY, newLanguage);
    } catch {
      // localStorage pode estar indisponível
    }
    document.documentElement.lang = newLanguage;
  };

  const t = getTranslations(language);

  return (
    <LanguageContext.Provider value={{ language, setLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error("useLanguage must be used within LanguageProvider");
  }
  return context;
}
