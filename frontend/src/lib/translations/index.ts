import { ptBr } from "./pt-br";
import { en } from "./en";
import { esMx } from "./es-mx";

export type Language = "pt-BR" | "en" | "es-MX";

export const translations: Record<Language, typeof ptBr> = {
  "pt-BR": ptBr,
  en,
  "es-MX": esMx,
};

export const languages: Array<{ code: Language; label: string }> = [
  { code: "pt-BR", label: "Português (Brasil)" },
  { code: "en", label: "English" },
  { code: "es-MX", label: "Español (México)" },
];

export const getTranslations = (language: Language) => translations[language];
