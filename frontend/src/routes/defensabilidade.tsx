import { createFileRoute } from "@tanstack/react-router";

import CompliancePage from "./compliance";

export const Route = createFileRoute("/defensabilidade")({
  component: CompliancePage,
});
