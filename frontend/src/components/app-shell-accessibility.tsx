import { useEffect, useState } from "react";
import { Accessibility, Eye, Focus, RotateCcw, Type } from "lucide-react";

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { useLanguage } from "@/lib/language-context";
import { cn } from "@/lib/utils";

export type CognitivePreferences = {
  simpleNavigation: boolean;
  largeText: boolean;
  reducedMotion: boolean;
  focusMode: boolean;
};

const STORAGE_KEY = "praxis-cognitive-preferences";
const DEFAULTS: CognitivePreferences = {
  simpleNavigation: true,
  largeText: false,
  reducedMotion: true,
  focusMode: false,
};

const copyByLanguage = {
  "pt-BR": {
    title: "Acessibilidade",
    description: "Ajuste a interface para reduzir distrações e facilitar a leitura.",
    simple: "Menu simples",
    simpleDescription: "Destaca somente o fluxo principal do processo de avaliação.",
    large: "Texto maior",
    largeDescription: "Aumenta textos, campos e áreas de clique.",
    motion: "Menos movimento",
    motionDescription: "Reduz animações e transições da interface.",
    focus: "Modo foco",
    focusDescription: "Oculta o menu lateral e limita a largura do conteúdo.",
    reset: "Restaurar ajustes",
    enabled: "Ativado",
    disabled: "Desativado",
  },
  en: {
    title: "Accessibility",
    description: "Adjust the interface to reduce distractions and improve readability.",
    simple: "Simple menu",
    simpleDescription: "Highlights only the main assessment process.",
    large: "Larger text",
    largeDescription: "Increases text, fields and click targets.",
    motion: "Less motion",
    motionDescription: "Reduces interface animations and transitions.",
    focus: "Focus mode",
    focusDescription: "Hides the sidebar and limits content width.",
    reset: "Reset adjustments",
    enabled: "Enabled",
    disabled: "Disabled",
  },
  "es-MX": {
    title: "Accesibilidad",
    description: "Ajusta la interfaz para reducir distracciones y facilitar la lectura.",
    simple: "Menú simple",
    simpleDescription: "Destaca solo el flujo principal del proceso de evaluación.",
    large: "Texto más grande",
    largeDescription: "Aumenta textos, campos y áreas de interacción.",
    motion: "Menos movimiento",
    motionDescription: "Reduce animaciones y transiciones.",
    focus: "Modo enfoque",
    focusDescription: "Oculta el menú lateral y limita el ancho del contenido.",
    reset: "Restaurar ajustes",
    enabled: "Activado",
    disabled: "Desactivado",
  },
} as const;

export function useCognitivePreferences() {
  const [preferences, setPreferences] = useState<CognitivePreferences>(DEFAULTS);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(STORAGE_KEY);
      if (stored) setPreferences({ ...DEFAULTS, ...(JSON.parse(stored) as Partial<CognitivePreferences>) });
    } catch {
      setPreferences(DEFAULTS);
    } finally {
      setHydrated(true);
    }
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.dataset.simpleNavigation = String(preferences.simpleNavigation);
    root.dataset.largeText = String(preferences.largeText);
    root.dataset.reducedMotion = String(preferences.reducedMotion);
    root.dataset.focusMode = String(preferences.focusMode);
    if (hydrated) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences));
  }, [hydrated, preferences]);

  return {
    preferences,
    toggle: (key: keyof CognitivePreferences) =>
      setPreferences((current) => ({ ...current, [key]: !current[key] })),
    reset: () => setPreferences(DEFAULTS),
  };
}

export function AccessibilityPanel({
  preferences,
  toggle,
  reset,
}: {
  preferences: CognitivePreferences;
  toggle: (key: keyof CognitivePreferences) => void;
  reset: () => void;
}) {
  const { language } = useLanguage();
  const copy = copyByLanguage[language];
  const options = [
    { key: "simpleNavigation" as const, title: copy.simple, description: copy.simpleDescription, icon: Eye },
    { key: "largeText" as const, title: copy.large, description: copy.largeDescription, icon: Type },
    { key: "reducedMotion" as const, title: copy.motion, description: copy.motionDescription, icon: Accessibility },
    { key: "focusMode" as const, title: copy.focus, description: copy.focusDescription, icon: Focus },
  ];

  return (
    <Sheet>
      <SheetTrigger asChild>
        <button
          type="button"
          className="inline-flex min-h-10 items-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent"
          aria-label={copy.title}
        >
          <Accessibility className="h-4 w-4" />
          <span className="hidden sm:inline">{copy.title}</span>
        </button>
      </SheetTrigger>
      <SheetContent side="right" className="w-[24rem] max-w-[92vw] overflow-y-auto bg-background p-0 text-foreground">
        <SheetHeader className="border-b border-border p-5 text-left">
          <SheetTitle className="flex items-center gap-2 text-xl">
            <Accessibility className="h-5 w-5" />
            {copy.title}
          </SheetTitle>
          <SheetDescription className="text-sm leading-6">{copy.description}</SheetDescription>
        </SheetHeader>
        <div className="space-y-3 p-5">
          {options.map((option) => {
            const enabled = preferences[option.key];
            return (
              <button
                key={option.key}
                type="button"
                aria-pressed={enabled}
                onClick={() => toggle(option.key)}
                className={cn(
                  "flex w-full items-start gap-3 rounded-lg border p-4 text-left transition",
                  enabled ? "border-primary/50 bg-primary/10" : "border-border bg-card hover:bg-accent",
                )}
              >
                <option.icon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
                <span className="min-w-0 flex-1">
                  <span className="block font-semibold">{option.title}</span>
                  <span className="mt-1 block text-sm leading-5 text-muted-foreground">{option.description}</span>
                  <span className="mt-2 block text-xs font-medium text-primary">
                    {enabled ? copy.enabled : copy.disabled}
                  </span>
                </span>
              </button>
            );
          })}
          <button
            type="button"
            onClick={reset}
            className="mt-2 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-md border border-border bg-card px-4 py-2 text-sm font-medium hover:bg-accent"
          >
            <RotateCcw className="h-4 w-4" />
            {copy.reset}
          </button>
        </div>
      </SheetContent>
    </Sheet>
  );
}
