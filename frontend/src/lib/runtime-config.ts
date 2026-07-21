// Runtime (client) configuration.
//
// Vite inlines `import.meta.env.VITE_*` at build time. Values injected by the
// SSR server take precedence so public configuration can change without a new bundle.

export type PraxisRuntimeConfig = {
  apiBaseUrl: string;
  partnerModuleEnabled: boolean;
};

declare global {
  interface Window {
    __PRAXIS_CONFIG__?: {
      apiBaseUrl?: string;
      partnerModuleEnabled?: boolean;
    };
  }
}

const buildTimeApiBaseUrl = (import.meta.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? "").replace(
  /\/$/,
  "",
);
const buildTimePartnerModuleEnabled =
  String(import.meta.env.VITE_PARTNER_MODULE_ENABLED ?? "false").toLowerCase() === "true";

export function getRuntimeConfig(): PraxisRuntimeConfig {
  const injected = typeof window !== "undefined" ? window.__PRAXIS_CONFIG__ : undefined;

  return {
    apiBaseUrl: (injected?.apiBaseUrl ?? buildTimeApiBaseUrl).replace(/\/$/, ""),
    partnerModuleEnabled:
      injected?.partnerModuleEnabled ?? buildTimePartnerModuleEnabled,
  };
}

export function getApiBaseUrl(): string {
  return getRuntimeConfig().apiBaseUrl;
}
