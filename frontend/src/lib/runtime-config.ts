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

type PublicRuntimeConfigResponse = {
  securityEnabled?: boolean;
  partnerModuleEnabled?: boolean;
  defaultEmpresaId?: string | null;
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
const RUNTIME_CONFIG_TIMEOUT_MS = 1_500;

let runtimeConfigRefreshPromise: Promise<PraxisRuntimeConfig> | null = null;

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

export function refreshRuntimeConfigFromBackend(): Promise<PraxisRuntimeConfig> {
  if (typeof window === "undefined") {
    return Promise.resolve(getRuntimeConfig());
  }
  if (!runtimeConfigRefreshPromise) {
    runtimeConfigRefreshPromise = loadRuntimeConfigFromBackend();
  }
  return runtimeConfigRefreshPromise;
}

async function loadRuntimeConfigFromBackend(): Promise<PraxisRuntimeConfig> {
  const current = getRuntimeConfig();
  const [publicConfig, probedSecurityEnabled] = await Promise.all([
    fetchPublicRuntimeConfig(current.apiBaseUrl),
    probeSecurityEnabled(current.apiBaseUrl),
  ]);

  window.__PRAXIS_CONFIG__ = {
    ...window.__PRAXIS_CONFIG__,
    apiBaseUrl: current.apiBaseUrl,
    securityEnabled:
      probedSecurityEnabled ?? publicConfig?.securityEnabled ?? current.securityEnabled,
    partnerModuleEnabled:
      publicConfig?.partnerModuleEnabled ?? current.partnerModuleEnabled,
    defaultEmpresaId:
      readNonBlank(publicConfig?.defaultEmpresaId) ?? current.defaultEmpresaId,
  };

  return getRuntimeConfig();
}

async function fetchPublicRuntimeConfig(
  apiBaseUrl: string,
): Promise<PublicRuntimeConfigResponse | null> {
  const response = await fetchRuntimeEndpoint(`${apiBaseUrl}/api/v1/runtime-config`);
  if (!response?.ok) return null;

  try {
    const body: unknown = await response.json();
    return isPublicRuntimeConfigResponse(body) ? body : null;
  } catch {
    return null;
  }
}

async function probeSecurityEnabled(apiBaseUrl: string): Promise<boolean | null> {
  const response = await fetchRuntimeEndpoint(`${apiBaseUrl}/api/v1/dashboard`);
  if (!response) return null;
  if (response.status === 401 || response.status === 403) return true;
  if (response.ok) return false;
  return null;
}

async function fetchRuntimeEndpoint(url: string): Promise<Response | null> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), RUNTIME_CONFIG_TIMEOUT_MS);

  try {
    return await window.fetch(url, {
      cache: "no-store",
      credentials: "omit",
      headers: { Accept: "application/json" },
      redirect: "manual",
      signal: controller.signal,
    });
  } catch {
    return null;
  } finally {
    window.clearTimeout(timeout);
  }
}

function readNonBlank(value: string | null | undefined): string | null {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function isPublicRuntimeConfigResponse(value: unknown): value is PublicRuntimeConfigResponse {
  if (typeof value !== "object" || value === null || Array.isArray(value)) return false;
  const config = value as Record<string, unknown>;
  return (
    (config.securityEnabled === undefined || typeof config.securityEnabled === "boolean") &&
    (config.partnerModuleEnabled === undefined ||
      typeof config.partnerModuleEnabled === "boolean") &&
    (config.defaultEmpresaId === undefined ||
      config.defaultEmpresaId === null ||
      typeof config.defaultEmpresaId === "string")
  );
}
