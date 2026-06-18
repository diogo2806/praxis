import { Globe } from "lucide-react";
import { useLanguage } from "@/lib/language-context";
import { languages, type Language } from "@/lib/translations";
import { cn } from "@/lib/utils";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type LanguageSelectorProps = {
  className?: string;
};

export function LanguageSelector({ className }: LanguageSelectorProps) {
  const { language, setLanguage } = useLanguage();

  return (
    <Select value={language} onValueChange={(value) => setLanguage(value as Language)}>
      <SelectTrigger
        aria-label="Selecionar idioma"
        className={cn(
          "w-11 shrink-0 gap-2 border-border bg-card text-xs text-muted-foreground hover:bg-accent sm:w-44",
          className,
        )}
      >
        <Globe className="h-3.5 w-3.5" />
        <SelectValue className="hidden sm:inline" />
      </SelectTrigger>
      <SelectContent>
        {languages.map((lang) => (
          <SelectItem key={lang.code} value={lang.code}>
            {lang.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
