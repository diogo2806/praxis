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

const getDefaultLanguage = (): Language => {
  if (typeof window === "undefined") return "pt-BR";

  const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY) as Language | null;
  if (stored) return stored;

  const browserLang = navigator.language.toLowerCase();
  if (browserLang.startsWith("en")) return "en";
  if (browserLang.startsWith("es")) return "es-MX";

  return "pt-BR";
};

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<Language>("pt-BR");
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setLanguageState(getDefaultLanguage());
    setMounted(true);
  }, []);

  const setLanguage = (newLanguage: Language) => {
    setLanguageState(newLanguage);
    localStorage.setItem(LANGUAGE_STORAGE_KEY, newLanguage);
    document.documentElement.lang = newLanguage;
  };

  const t = getTranslations(language);

  if (!mounted) {
    return children;
  }

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
