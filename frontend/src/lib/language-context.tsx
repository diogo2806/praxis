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

const isKnownLanguage = (value: string | null): value is Language =>
  value === "pt-BR" || value === "en" || value === "es-MX";

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
