import process from "node:process";

// Server-only resolution of public runtime config. The .server.ts suffix keeps
// this out of the client bundle. Read env INSIDE the function so it resolves
// per request (matters on request-scoped runtimes like Cloudflare Workers).
//
// Only keys that are actually set are returned, so the client can fall back to
// its build-time defaults for anything omitted. Keep backend target URLs out of
// the browser config; src/server.ts proxies same-origin API calls instead.
export function resolveRuntimeConfigFromEnv(): {
  apiBaseUrl?: string;
} {
  const apiBaseUrl =
    process.env.VITE_PRAXIS_BROWSER_API_BASE_URL ?? process.env.PRAXIS_BROWSER_API_BASE_URL;

  const config: { apiBaseUrl?: string } = {};
  if (apiBaseUrl) config.apiBaseUrl = apiBaseUrl.replace(/\/$/, "");
  return config;
}
