import {
  BookOpenText,
  CircleAlert,
  ExternalLink,
  FileText,
  Keyboard,
  LockKeyhole,
  Route,
  SquareMousePointer,
  Workflow,
} from "lucide-react";

import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { resolveAccessOnboardingManual } from "@/lib/screen-manual-access-onboarding";
import { resolveAnalysisOperationManual } from "@/lib/screen-manual-analysis-operations";
import { resolveCompetencyOwnershipManual } from "@/lib/screen-manual-competency-ownership";
import { useLanguage } from "@/lib/language-context";
import { resolvePartnerSpecialistManual } from "@/lib/screen-manual-specialist";
import { resolvePortabilityManual } from "@/lib/screen-manual-portability";
import { resolvePreviewJourneyManual } from "@/lib/screen-manual-preview";
import { resolvePublicationManual } from "@/lib/screen-manual-publication";
import { resolveQualityManual } from "@/lib/screen-manual-quality";
import { resolveScenarioOwnershipManual } from "@/lib/screen-manual-scenario-ownership";
import { resolveScreenManualOverride } from "@/lib/screen-manual-overrides";
import {
  resolveScreenManual,
  type ScreenManualDefinition,
  type ScreenManualField,
} from "@/lib/screen-manuals";
import { cn } from "@/lib/utils";

const manualCopy = {
  "pt-BR": {
    button: "Manual",
    description: "Orientações operacionais desta tela.",
    purpose: "Finalidade da tela",
    flow: "Fluxo operacional",
    fields: "Explicação dos campos",
    permissions: "Permissões necessárias",
    states: "Estados possíveis",
    blocks: "Motivos de bloqueio",
    examples: "Exemplos",
    shortcuts: "Atalhos",
    completeProcess: "Ver processo completo",
  },
  en: {
    button: "Manual",
    description: "Operational guidance for this page.",
    purpose: "Page purpose",
    flow: "Operational flow",
    fields: "Field explanations",
    permissions: "Required permissions",
    states: "Possible states",
    blocks: "Blocking reasons",
    examples: "Examples",
    shortcuts: "Shortcuts",
    completeProcess: "View complete process",
  },
  "es-MX": {
    button: "Manual",
    description: "Orientaciones operativas de esta pantalla.",
    purpose: "Finalidad de la pantalla",
    flow: "Flujo operacional",
    fields: "Explicación de los campos",
    permissions: "Permisos necesarios",
    states: "Estados posibles",
    blocks: "Motivos de bloqueio",
    examples: "Ejemplos",
    shortcuts: "Atajos",
    completeProcess: "Ver processo completo",
  },
} as const;

type ManualLabels = (typeof manualCopy)[keyof typeof manualCopy];

type ScreenManualProps = {
  pathname: string;
  iconOnly?: boolean;
  className?: string;
};

export function ScreenManual({ pathname, iconOnly = false, className }: ScreenManualProps) {
  const { language } = useLanguage();
  const copy = manualCopy[language];
  const manual =
    resolveAccessOnboardingManual(pathname) ??
    resolveAnalysisOperationManual(pathname) ??
    resolvePartnerSpecialistManual(pathname) ??
    resolvePreviewJourneyManual(pathname) ??
    resolvePortabilityManual(pathname) ??
    resolveScenarioOwnershipManual(pathname) ??
    resolvePublicationManual(pathname) ??
    resolveCompetencyOwnershipManual(pathname) ??
    resolveQualityManual(pathname) ??
    resolveScreenManualOverride(pathname) ??
    resolveScreenManual(pathname);

  return (
    <Sheet>
      <SheetTrigger asChild>
        <button
          type="button"
          className={cn(
            "inline-flex min-h-10 items-center justify-center gap-2 rounded-md border border-border bg-card px-3 py-2 text-sm font-medium text-foreground transition-colors hover:bg-accent focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
            iconOnly && "h-11 w-11 min-h-11 rounded-full p-0 shadow-lg",
            className,
          )}
          aria-label={`${copy.button}: ${manual.title}`}
          title={`${copy.button}: ${manual.title}`}
        >
          <BookOpenText className="h-4 w-4 shrink-0" />
          {!iconOnly && <span className="hidden sm:inline">{copy.button}</span>}
        </button>
      </SheetTrigger>

      <SheetContent side="right" className="w-[34rem] max-w-[96vw] overflow-y-auto bg-background p-0 text-foreground">
        <SheetHeader className="border-b border-border p-5 text-left">
          <SheetTitle className="flex items-center gap-2 text-xl">
            <BookOpenText className="h-5 w-5 text-primary" />
            {manual.title}
          </SheetTitle>
          <SheetDescription className="text-sm leading-6">{copy.description}</SheetDescription>
        </SheetHeader>

        <ScreenManualContent manual={manual} labels={copy} />

        <div className="sticky bottom-0 border-t border-border bg-background/95 p-5 backdrop-blur">
          <SheetClose asChild>
            <a href={`/manual#${manual.id}`} className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
              {copy.completeProcess}
              <ExternalLink className="h-4 w-4" />
            </a>
          </SheetClose>
        </div>
      </SheetContent>
    </Sheet>
  );
}

export function ScreenManualContent({ manual, labels }: { manual: ScreenManualDefinition; labels?: ManualLabels }) {
  const { language } = useLanguage();
  const copy = labels ?? manualCopy[language];

  return (
    <div className="space-y-6 p-5">
      <ManualTextSection icon={FileText} title={copy.purpose} values={[manual.purpose]} />
      <ManualNumberedSection icon={Workflow} title={copy.flow} values={manual.flow} />
      <ManualFieldsSection title={copy.fields} fields={manual.fields} />
      <ManualTextSection icon={LockKeyhole} title={copy.permissions} values={manual.permissions} />
      <ManualTextSection icon={Route} title={copy.states} values={manual.states} />
      <ManualTextSection icon={CircleAlert} title={copy.blocks} values={manual.blocks} />
      <ManualTextSection icon={SquareMousePointer} title={copy.examples} values={manual.examples} />
      <ManualTextSection icon={Keyboard} title={copy.shortcuts} values={manual.shortcuts} />
    </div>
  );
}

function ManualSectionTitle({ icon: Icon, title }: { icon: typeof FileText; title: string }) {
  return <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground"><Icon className="h-4 w-4 shrink-0 text-primary" />{title}</h3>;
}

function ManualTextSection({ icon, title, values }: { icon: typeof FileText; title: string; values: string[] }) {
  return (
    <section className="space-y-2">
      <ManualSectionTitle icon={icon} title={title} />
      <ul className="space-y-2 pl-6 text-sm leading-6 text-muted-foreground">
        {values.map((value) => <li key={value} className="list-disc pl-1">{value}</li>)}
      </ul>
    </section>
  );
}

function ManualNumberedSection({ icon, title, values }: { icon: typeof FileText; title: string; values: string[] }) {
  return (
    <section className="space-y-3">
      <ManualSectionTitle icon={icon} title={title} />
      <ol className="space-y-3">
        {values.map((value, index) => (
          <li key={value} className="flex gap-3 text-sm leading-6 text-muted-foreground">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">{index + 1}</span>
            <span>{value}</span>
          </li>
        ))}
      </ol>
    </section>
  );
}

function ManualFieldsSection({ title, fields }: { title: string; fields: ScreenManualField[] }) {
  return (
    <section className="space-y-3">
      <ManualSectionTitle icon={FileText} title={title} />
      <dl className="divide-y divide-border rounded-lg border border-border bg-card">
        {fields.map((field) => (
          <div key={field.name} className="p-3">
            <dt className="text-sm font-semibold text-foreground">{field.name}</dt>
            <dd className="mt-1 text-sm leading-5 text-muted-foreground">{field.description}</dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
