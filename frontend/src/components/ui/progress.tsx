"use client";

import * as React from "react";
import * as ProgressPrimitive from "@radix-ui/react-progress";

import { cn } from "@/lib/utils";

const Progress = React.forwardRef<
  React.ElementRef<typeof ProgressPrimitive.Root>,
  React.ComponentPropsWithoutRef<typeof ProgressPrimitive.Root>
>(({ className, value, ...props }, ref) => {
  const progressValue = Math.min(100, Math.max(0, value || 0));

  return (
    <ProgressPrimitive.Root
      ref={ref}
      className={cn(
        "relative bg-primary/20",
        "praxis-progress-track praxis-progress-track-md",
        className,
      )}
      {...props}
    >
      <ProgressPrimitive.Indicator
        className="praxis-progress-indicator"
        style={
          {
            "--praxis-progress-value": `${progressValue}%`,
          } as React.CSSProperties
        }
      />
    </ProgressPrimitive.Root>
  );
});
Progress.displayName = ProgressPrimitive.Root.displayName;

export { Progress };
