import "./lib/error-capture";

import process from "node:process";

import { consumeLastCapturedError } from "./lib/error-capture";
import { renderErrorPage } from "./lib/error-page";

type ServerEntry = {
  fetch: (request: Request, env: unknown, ctx: unknown) => Promise<Response> | Response;
};

let serverEntryPromise: Promise<ServerEntry> | undefined;

async function getServerEntry(): Promise<ServerEntry> {
  if (!serverEntryPromise) {
    serverEntryPromise = import("@tanstack/react-start/server-entry").then(
      (m) => (m.default ?? m) as ServerEntry,
    );
  }
  return serverEntryPromise;
}

// h3 swallows in-handler throws into a normal 500 Response with body
// {"unhandled":true,"message":"HTTPError"} — try/catch alone never fires for those.
async function normalizeCatastrophicSsrResponse(response: Response): Promise<Response> {
  if (response.status < 500) return response;
  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) return response;

  const body = await response.clone().text();
  if (!body.includes('"unhandled":true') || !body.includes('"message":"HTTPError"')) {
    return response;
  }

  console.error(consumeLastCapturedError() ?? new Error(`h3 swallowed SSR error: ${body}`));
  return new Response(renderErrorPage(), {
    status: 500,
    headers: { "content-type": "text/html; charset=utf-8" },
  });
}

const requestLoggingEnabled = process.env.PRAXIS_REQUEST_LOG !== "false";

function logRequest(request: Request, response: Response, startedAt: number): void {
  if (!requestLoggingEnabled) return;
  const { pathname } = new URL(request.url);
  const durationMs = Math.round(performance.now() - startedAt);
  const line = `${request.method} ${pathname} → ${response.status} (${durationMs}ms)`;
  if (response.status >= 500) {
    console.error(line);
  } else {
    console.log(line);
  }
}

function getBackendBaseUrl(): string {
  return (
    process.env.VITE_PRAXIS_API_BASE_URL ??
    process.env.PRAXIS_API_BASE_URL ??
    "http://localhost:8080"
  ).replace(/\/$/, "");
}

function shouldProxyToBackend(pathname: string): boolean {
  return (
    pathname.startsWith("/api/v1/") ||
    pathname === "/api/v1" ||
    pathname.startsWith("/api/admin/") ||
    pathname === "/api/admin" ||
    pathname.startsWith("/candidate/")
  );
}

function getProxyHeaders(request: Request): Headers {
  const headers = new Headers(request.headers);
  headers.delete("host");
  headers.delete("connection");
  headers.delete("content-length");
  headers.delete("accept-encoding");
  headers.delete("expect");
  headers.delete("origin");
  return headers;
}

async function proxyToBackend(request: Request): Promise<Response> {
  const sourceUrl = new URL(request.url);
  const targetUrl = new URL(`${sourceUrl.pathname}${sourceUrl.search}`, getBackendBaseUrl());
  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  const response = await fetch(targetUrl, {
    method: request.method,
    headers: getProxyHeaders(request),
    body: hasBody ? await request.arrayBuffer() : undefined,
    redirect: "manual",
  });
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  });
}

export default {
  async fetch(request: Request, env: unknown, ctx: unknown) {
    const startedAt = performance.now();
    let response: Response;
    try {
      const url = new URL(request.url);
      if (shouldProxyToBackend(url.pathname)) {
        response = await proxyToBackend(request);
      } else {
        const handler = await getServerEntry();
        const handled = await handler.fetch(request, env, ctx);
        response = await normalizeCatastrophicSsrResponse(handled);
      }
    } catch (error) {
      console.error(error);
      response = new Response(renderErrorPage(), {
        status: 500,
        headers: { "content-type": "text/html; charset=utf-8" },
      });
    }
    logRequest(request, response, startedAt);
    return response;
  },
};
