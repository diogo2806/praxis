import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError } from "@/lib/api/praxis";

export type CommercialPlanType = "AVULSO" | "PROFISSIONAL" | "ENTERPRISE";
export type EmpresaStatus =
  | "ATIVO"
  | "EM_TESTE"
  | "PENDENTE_PAGAMENTO"
  | "INADIMPLENTE"
  | "SEM_CREDITO"
  | "SUSPENSO"
  | "CANCELADO";
export type SubscriptionStatus = "PENDING" | "AUTHORIZED" | "DELINQUENT" | "PAUSED" | "CANCELLED";
export type FinancialStatus = "REGULAR" | "PENDENTE_PAGAMENTO" | "INADIMPLENTE" | "SEM_CREDITO" | "CANCELADO";
export type BillingAction =
  | "BUY_CREDITS"
  | "MANAGE_AUTO_RECHARGE"
  | "VIEW_HISTORY"
  | "VIEW_SUBSCRIPTION"
  | "SYNC_SUBSCRIPTION"
  | "UPDATE_PAYMENT"
  | "CONTACT_SUPPORT";

export interface SubscriptionPlan {
  id: number;
  code: string;
  name: string;
  planType: CommercialPlanType;
  priceCents: number;
  currency: string;
  creditAmount: number | null;
}

export interface BillingEvent {
  id: number;
  eventType: string;
  mpResourceType: string | null;
  mpResourceId: string | null;
  mpStatus: string | null;
  amountCents: number | null;
  currency: string | null;
  createdAt: string;
}

export interface CheckoutResult {
  kind: string;
  mpResourceId: string | null;
  initPoint: string | null;
  externalReference: string;
}

export interface ClientBillingUsage {
  completedLast7Days: number;
  completedLast30Days: number;
  completedAllTime: number;
}

export interface ClientBillingSubscription {
  status: SubscriptionStatus;
  initPoint: string | null;
  currentPeriodEnd: string | null;
  lastPaymentAt: string | null;
  graceUntil: string | null;
}

export interface ClientBillingResponse {
  empresaId: string;
  plan: CommercialPlanType | null;
  empresaStatus: EmpresaStatus;
  financialStatus: FinancialStatus;
  creditBalance: number;
  usage: ClientBillingUsage;
  subscription: ClientBillingSubscription | null;
  availableActions: BillingAction[];
  events: BillingEvent[];
}

async function billingRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  if (session.token) {
    headers.Authorization = `Bearer ${session.token}`;
  }

  const response = await fetch(`${getApiBaseUrl()}${path}`, {
    ...init,
    headers: {
      ...headers,
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let message = `Falha na API (${response.status})`;
    try {
      const body = (await response.json()) as {
        mensagem?: string;
        message?: string;
        error?: string;
      };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch {
      // Mantem a mensagem HTTP padrao quando a resposta nao vem em JSON.
    }
    throw new PraxisApiError(message, response.status);
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

export function getClientBilling() {
  return billingRequest<ClientBillingResponse>("/api/v1/billing");
}

export function listClientBillingPlans() {
  return billingRequest<SubscriptionPlan[]>("/api/v1/billing/plans");
}

export function createClientCreditCheckout(planId: number) {
  return billingRequest<CheckoutResult>(`/api/v1/billing/credits/checkout?planId=${planId}`, {
    method: "POST",
  });
}

export function createClientSubscriptionCheckout(planId: number) {
  return billingRequest<CheckoutResult>(`/api/v1/billing/subscription/checkout?planId=${planId}`, {
    method: "POST",
  });
}

export function syncClientSubscription() {
  return billingRequest<ClientBillingResponse>("/api/v1/billing/subscription/sync", {
    method: "POST",
  });
}
