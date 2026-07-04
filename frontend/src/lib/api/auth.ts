import { getApiBaseUrl } from "@/lib/runtime-config";
import { PraxisApiError } from "@/lib/api/praxis";

export interface LoginRequest {
  empresaId: string;
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  empresaId: string;
  name: string;
  roles: string[];
}

export async function login(body: LoginRequest) {
  const response = await fetch(`${getApiBaseUrl()}/api/v1/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new PraxisApiError(await readErrorMessage(response), response.status);
  }

  return response.json() as Promise<LoginResponse>;
}

async function readErrorMessage(response: Response) {
  const fallback = `Falha na autenticação (${response.status})`;

  try {
    const body = (await response.json()) as {
      mensagem?: string;
      message?: string;
      error?: string;
    };
    return body.mensagem ?? body.message ?? body.error ?? fallback;
  } catch {
    return fallback;
  }
}
