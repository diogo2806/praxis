import { ptBr } from "./pt-br";
import { en } from "./en";
import { esMx } from "./es-mx";
import { candidateAccessTranslations } from "./candidate-access";

export type Language = "pt-BR" | "en" | "es-MX";

const withSharedTranslations = <T extends typeof ptBr>(language: Language, translation: T) => ({
  ...translation,
  candidateAccess: candidateAccessTranslations[language],
});

export const translations = {
  "pt-BR": withSharedTranslations("pt-BR", ptBr),
  en: withSharedTranslations("en", en),
  "es-MX": withSharedTranslations("es-MX", esMx),
};

export const languages: Array<{ code: Language; label: string }> = [
  { code: "pt-BR", label: "Português (Brasil)" },
  { code: "en", label: "English" },
  { code: "es-MX", label: "Español (México)" },
];

export const getTranslations = (language: Language) => translations[language];
