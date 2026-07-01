import { Star } from "lucide-react";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function StarRating({
  value,
  onChange,
  readOnly = false,
}: {
  value: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
}) {
  return (
    <div className="flex flex-wrap gap-1">
      {[1, 2, 3, 4, 5].map((item) => {
        const active = item <= value;
        if (readOnly) {
          return (
            <Star
              key={item}
              className={cn("h-4 w-4", active ? "fill-warning text-warning" : "text-muted-foreground")}
            />
          );
        }
        return (
          <Button
            key={item}
            type="button"
            variant={active ? "default" : "outline"}
            size="icon"
            aria-label={`${item} estrela(s)`}
            onClick={() => onChange?.(item)}
          >
            <Star className="h-4 w-4" />
          </Button>
        );
      })}
    </div>
  );
}
