import { Globe } from "lucide-react";
import { useLanguage } from "@/lib/language-context";
import { languages, type Language } from "@/lib/translations";
import { cn } from "@/lib/utils";
import { Select, SelectContent, SelectItem, SelectTrigger } from "@/components/ui/select";

type LanguageSelectorProps = {
  className?: string;
};

export function LanguageSelector({ className }: LanguageSelectorProps) {
  const { language, setLanguage, t } = useLanguage();

  return (
    <Select value={language} onValueChange={(value) => setLanguage(value as Language)}>
      <SelectTrigger
        aria-label={t.language.select}
        className={cn(
          "w-auto min-w-0 shrink-0 gap-1.5 border-border bg-card px-2.5 text-xs text-foreground hover:bg-accent [&>svg:last-child]:hidden",
          className,
        )}
      >
        <Globe className="h-3.5 w-3.5" />
        <span>{languages.find((lang) => lang.code === language)?.label ?? language}</span>
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
