import type { CSSProperties, ReactNode } from "react";

import { cn } from "@/lib/utils";

type ResponsiveTableProps = {
  children: ReactNode;
  className?: string;
  contentClassName?: string;
  minWidth?: CSSProperties["minWidth"];
};

export function ResponsiveTable({
  children,
  className,
  contentClassName,
  minWidth = "760px",
}: ResponsiveTableProps) {
  return (
    <div
      data-slot="responsive-table"
      className={cn("overflow-x-auto rounded-lg border border-border bg-card shadow-sm", className)}
    >
      <div data-slot="responsive-table-content" className={contentClassName} style={{ minWidth }}>
        {children}
      </div>
    </div>
  );
}
