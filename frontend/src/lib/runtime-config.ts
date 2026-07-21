// Runtime (client) configuration.
//
// Vite inlines `import.meta.env.VITE_*` at build time. Values injected by the
// SSR server take precedence so public configuration can change without a new bundle.

export type PraxisRuntimeConfig = {
  apiBaseUrl: string;
  securityEnabled: boolean;
  partnerModuleEnabled: boolean;
  defaultEmpresaId: string;
};

declare global {
  interface Window {
    __PRAXIS_CONFIG__?: {
      apiBaseUrl?: string;
      securityEnabled?: boolean;
      partnerModuleEnabled?: boolean;
      defaultEmpresaId?: string;
    };
  }
}

const buildTimeApiBaseUrl = (import.meta.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? "").replace(
  /\/$/,
  "",
);
const buildTimeSecurityEnabled =
  String(import.meta.env.VITE_PRAXIS_SECURITY_ENABLED ?? "true").toLowerCase() === "true";
const buildTimePartnerModuleEnabled =
  String(import.meta.env.VITE_PARTNER_MODULE_ENABLED ?? "false").toLowerCase() === "true";
const buildTimeDefaultEmpresaId =
  String(import.meta.env.VITE_PRAXIS_DEFAULT_EMPRESA_ID ?? "empresa-1").trim() || "empresa-1";

export function getRuntimeConfig(): PraxisRuntimeConfig {
  const injected = typeof window !== "undefined" ? window.__PRAXIS_CONFIG__ : undefined;

  return {
    apiBaseUrl: (injected?.apiBaseUrl ?? buildTimeApiBaseUrl).replace(/\/$/, ""),
    securityEnabled: injected?.securityEnabled ?? buildTimeSecurityEnabled,
    partnerModuleEnabled:
      injected?.partnerModuleEnabled ?? buildTimePartnerModuleEnabled,
    defaultEmpresaId: injected?.defaultEmpresaId?.trim() || buildTimeDefaultEmpresaId,
  };
}

export function getApiBaseUrl(): string {
  return getRuntimeConfig().apiBaseUrl;
}
