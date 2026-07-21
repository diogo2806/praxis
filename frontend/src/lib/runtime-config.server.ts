import process from "node:process";

type BackendRuntimeConfig = {
  securityEnabled?: boolean;
  partnerModuleEnabled?: boolean;
  defaultEmpresaId?: string | null;
};

export type ServerRuntimeConfig = {
  apiBaseUrl?: string;
  securityEnabled: boolean;
  partnerModuleEnabled: boolean;
  defaultEmpresaId: string;
};

const RUNTIME_CONFIG_TIMEOUT_MS = 1_500;

// Server-only resolution of public runtime config. Read env inside the function
// because request-scoped runtimes resolve bindings per request.
export async function resolveRuntimeConfigFromEnv(): Promise<ServerRuntimeConfig> {
  const apiBaseUrl =
    process.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? process.env.PRAXIS_BROWSER_API_BASE_URL;
  const backendConfig = await fetchBackendRuntimeConfig();
  const securityEnabled =
    backendConfig?.securityEnabled ??
    readBoolean(
      process.env.PRAXIS_SECURITY_ENABLED ??
        process.env.VITE_PRAXIS_SECURITY_ENABLED,
      true,
    );
  const partnerModuleEnabled =
    backendConfig?.partnerModuleEnabled ??
    readBoolean(
      process.env.PRAXIS_PARTNER_ENABLED ??
        process.env.VITE_PARTNER_MODULE_ENABLED,
      false,
    );
  const defaultEmpresaId =
    readNonBlank(backendConfig?.defaultEmpresaId) ??
    readNonBlank(process.env.PRAXIS_DEFAULT_EMPRESA_ID) ??
    readNonBlank(process.env.PRAXIS_DEFAULT_TENANT_ID) ??
    "empresa-1";

  const config: ServerRuntimeConfig = {
    securityEnabled,
    partnerModuleEnabled,
    defaultEmpresaId,
  };
  if (apiBaseUrl) config.apiBaseUrl = apiBaseUrl.replace(/\/$/, "");
  return config;
}

async function fetchBackendRuntimeConfig(): Promise<BackendRuntimeConfig | null> {
  const backendBaseUrl = (
    process.env.VITE_PRAXIS_API_BASE_URL ??
    process.env.PRAXIS_API_BASE_URL ??
    "http://localhost:8080"
  ).replace(/\/$/, "");
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), RUNTIME_CONFIG_TIMEOUT_MS);

  try {
    const response = await fetch(`${backendBaseUrl}/api/v1/runtime-config`, {
      headers: { Accept: "application/json" },
      signal: controller.signal,
    });
    if (!response.ok) return null;

    const body: unknown = await response.json();
    return isBackendRuntimeConfig(body) ? body : null;
  } catch {
    return null;
  } finally {
    clearTimeout(timeout);
  }
}

function readBoolean(value: string | undefined, fallback: boolean): boolean {
  if (value === undefined) return fallback;
  return value.trim().toLowerCase() === "true";
}

function readNonBlank(value: string | null | undefined): string | null {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function isBackendRuntimeConfig(value: unknown): value is BackendRuntimeConfig {
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
