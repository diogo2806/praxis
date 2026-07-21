import process from "node:process";

// Server-only resolution of public runtime config. Read env inside the function
// because request-scoped runtimes resolve bindings per request.
export function resolveRuntimeConfigFromEnv(): {
  apiBaseUrl?: string;
  partnerModuleEnabled: boolean;
} {
  const apiBaseUrl =
    process.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? process.env.PRAXIS_BROWSER_API_BASE_URL;
  const partnerModuleEnabled =
    String(
      process.env.PRAXIS_PARTNER_ENABLED ??
        process.env.VITE_PARTNER_MODULE_ENABLED ??
        "false",
    ).toLowerCase() === "true";

  const config: { apiBaseUrl?: string; partnerModuleEnabled: boolean } = {
    partnerModuleEnabled,
  };
  if (apiBaseUrl) config.apiBaseUrl = apiBaseUrl.replace(/\/$/, "");
  return config;
}
