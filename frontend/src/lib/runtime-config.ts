// Runtime (client) configuration.
//
// Why this exists: Vite inlines `import.meta.env.VITE_*` at BUILD time, so a
// value set only as a container/runtime env var (e.g. in the EasyPanel panel)
// never reaches the browser bundle and the app falls back to the build-time
// default (historically http://localhost:8080).
//
// To support changing public config at runtime without rebuilding, the SSR
// server reads the env vars per request and injects them as
// `window.__PRAXIS_CONFIG__` (see src/routes/__root.tsx). The client reads
// that first, falling back to the build-time value, then to a same-origin
// default. The SSR server proxies API paths to the real backend.

export type PraxisRuntimeConfig = {
  apiBaseUrl: string;
};

declare global {
  interface Window {
    __PRAXIS_CONFIG__?: { apiBaseUrl?: string };
  }
}

const buildTimeApiBaseUrl = (import.meta.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? "").replace(
  /\/$/,
  "",
);

export function getRuntimeConfig(): PraxisRuntimeConfig {
  const injected = typeof window !== "undefined" ? window.__PRAXIS_CONFIG__ : undefined;

  return {
    apiBaseUrl: (injected?.apiBaseUrl ?? buildTimeApiBaseUrl).replace(/\/$/, ""),
  };
}

export function getApiBaseUrl(): string {
  return getRuntimeConfig().apiBaseUrl;
}
