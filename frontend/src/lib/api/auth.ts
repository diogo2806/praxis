import { apiRequest } from "@/lib/api/http";

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

export function login(body: LoginRequest) {
  return apiRequest<LoginResponse>(
    "/api/v1/auth/login",
    {
      method: "POST",
      body: JSON.stringify(body),
    },
    {
      authenticated: false,
      fallbackMessage: (status) => `Falha na autenticação (${status})`,
    },
  );
}
