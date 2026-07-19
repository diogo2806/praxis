import { apiRequest } from "@/lib/api/http";
import type { DashboardResponse } from "./praxis-contract";
import { PraxisApiError } from "./praxis-legacy";

export class DashboardCompatibilityError extends PraxisApiError {
  constructor() {
    super(
      "O backend conectado não disponibiliza o endpoint de dashboard desta versão do frontend.",
      404,
    );
    this.name = "DashboardCompatibilityError";
  }
}

export async function getDashboard(): Promise<DashboardResponse> {
  try {
    return await apiRequest<DashboardResponse>("/api/v1/dashboard");
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      throw new DashboardCompatibilityError();
    }
    throw error;
  }
}
