"use client";

import { useState } from "react";
import { useRouterState } from "@tanstack/react-router";
import { BookOpen, ChevronRight, HelpCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { useLanguage } from "@/lib/language-context";
import { getHelpContent } from "@/lib/contextual-help-content";
import { cn } from "@/lib/utils";

const labels = {
  "pt-BR": {
    help: "Ajuda",
    howToUse: "Como usar esta tela",
    stepByStep: "Passo a passo",
    noHelp: "Ajuda não disponível para esta tela.",
  },
  en: {
    help: "Help",
    howToUse: "How to use this screen",
    stepByStep: "Step by step",
    noHelp: "Help is not available for this screen.",
  },
  "es-MX": {
    help: "Ayuda",
    howToUse: "Cómo usar esta pantalla",
    stepByStep: "Paso a paso",
    noHelp: "La ayuda no está disponible para esta pantalla.",
  },
} as const;

export function ContextualHelpButton() {
  const [open, setOpen] = useState(false);
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const { language } = useLanguage();
  const content = getHelpContent(pathname, language);
  const l = labels[language] ?? labels["pt-BR"];

  if (!content) {
    return null;
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <button
          type="button"
          className="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-2.5 py-1 text-xs text-muted-foreground hover:bg-accent transition"
          aria-label={l.help}
        >
          <HelpCircle className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">{l.help}</span>
        </button>
      </DialogTrigger>
      <DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-2 text-primary">
            <BookOpen className="h-5 w-5" />
            <span className="text-xs font-medium uppercase tracking-wide">
              {l.howToUse}
            </span>
          </div>
          <DialogTitle className="mt-2 text-xl">{content.title}</DialogTitle>
          <DialogDescription className="mt-1 text-sm leading-relaxed">
            {content.description}
          </DialogDescription>
        </DialogHeader>

        <div className="mt-4">
          <h3 className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {l.stepByStep}
          </h3>
          <ol className="space-y-3">
            {content.steps.map((step, index) => (
              <li key={index} className="flex gap-3">
                <span
                  className={cn(
                    "flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                    "bg-primary/10 text-primary",
                  )}
                >
                  {index + 1}
                </span>
                <div className="min-w-0 flex-1 pt-0.5">
                  <div className="flex items-center gap-1.5">
                    <ChevronRight className="h-3 w-3 text-muted-foreground" />
                    <span className="text-sm font-medium text-foreground">
                      {step.title}
                    </span>
                  </div>
                  <p className="mt-0.5 text-sm leading-relaxed text-muted-foreground">
                    {step.description}
                  </p>
                </div>
              </li>
            ))}
          </ol>
        </div>
      </DialogContent>
    </Dialog>
  );
}
