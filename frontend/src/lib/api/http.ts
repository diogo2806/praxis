import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError } from "./praxis-legacy";

export type ApiRequestOptions = {
  authenticated?: boolean;
  fallbackMessage?: string | ((status: number) => string);
};

type ApiErrorBody = {
  mensagem?: string;
  message?: string;
  error?: string;
  detail?: string;
};

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  options: ApiRequestOptions = {},
): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Accept")) {
    headers.set("Accept", "application/json");
  }
  if (hasJsonBody(init.body) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  if (options.authenticated !== false) {
    const session = getSession();
    if (session.token && !headers.has("Authorization")) {
      headers.set("Authorization", `Bearer ${session.token}`);
    }
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    const fallback = resolveFallback(options.fallbackMessage, response.status);
    throw new PraxisApiError(await readApiErrorMessage(response, fallback), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json() as Promise<T>;
  }

  const text = await response.text();
  return (text.length > 0 ? text : undefined) as T;
}

export async function readApiErrorMessage(
  response: Response,
  fallback = `Falha na API (${response.status})`,
): Promise<string> {
  try {
    const body = (await response.json()) as ApiErrorBody;
    return body.mensagem ?? body.message ?? body.error ?? body.detail ?? fallback;
  } catch {
    return fallback;
  }
}

function resolveFallback(
  fallback: ApiRequestOptions["fallbackMessage"],
  status: number,
): string {
  if (typeof fallback === "function") {
    return fallback(status);
  }
  return fallback ?? `Falha na API (${status})`;
}

function hasJsonBody(body: BodyInit | null | undefined): boolean {
  if (body == null) {
    return false;
  }
  return typeof FormData === "undefined" || !(body instanceof FormData);
}
