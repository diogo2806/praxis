import type { CSSProperties, ReactNode } from "react";

import { cn } from "@/lib/utils";

type ResponsiveTableProps = {
  children: ReactNode;
  className?: string;
  minWidth?: CSSProperties["minWidth"];
};

export function ResponsiveTable({ children, className, minWidth = "760px" }: ResponsiveTableProps) {
  return (
    <div className={cn("overflow-x-auto rounded-lg border border-border", className)}>
      <div style={{ minWidth }}>{children}</div>
    </div>
  );
}
