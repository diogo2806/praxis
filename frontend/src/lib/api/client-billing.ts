import { getApiBaseUrl } from "@/lib/runtime-config";
import { getSession } from "@/lib/session";
import { PraxisApiError } from "@/lib/api/praxis";

export type CommercialPlanType = "AVULSO" | "PROFISSIONAL" | "ENTERPRISE";
export type EmpresaStatus = "ATIVO" | "EM_TESTE" | "PENDENTE_PAGAMENTO" | "INADIMPLENTE" | "SEM_CREDITO" | "SUSPENSO" | "CANCELADO";
export type SubscriptionStatus = "PENDING" | "AUTHORIZED" | "DELINQUENT" | "PAUSED" | "CANCELLED";
export type FinancialStatus = "REGULAR" | "PENDENTE_PAGAMENTO" | "INADIMPLENTE" | "SEM_CREDITO" | "CANCELADO";
export type PlanChangeRequestType = "CHANGE_PLAN" | "CANCEL_CONTRACT";
export type AutoRechargeStatus = "IDLE" | "PENDING";

export interface SubscriptionPlan { id: number; code: string; name: string; planType: CommercialPlanType; priceCents: number; currency: string; creditAmount: number | null; }
export interface BillingEvent { id: number; eventType: string; mpResourceType: string | null; mpResourceId: string | null; mpStatus: string | null; amountCents: number | null; currency: string | null; createdAt: string; }
export interface CreditMovement { id: number; delta: number; reason: string; balanceAfter: number; note: string | null; createdAt: string; }
export interface CheckoutResult { kind: string; mpResourceId: string | null; initPoint: string | null; externalReference: string; }
export interface ClientBillingUsage { completedLast7Days: number; completedLast30Days: number; completedAllTime: number; }
export interface ClientBillingSubscription { status: SubscriptionStatus; initPoint: string | null; currentPeriodEnd: string | null; lastPaymentAt: string | null; graceUntil: string | null; }
export interface ClientBillingResponse { empresaId: string; plan: CommercialPlanType | null; empresaStatus: EmpresaStatus; financialStatus: FinancialStatus; creditBalance: number; usage: ClientBillingUsage; subscription: ClientBillingSubscription | null; availableActions: string[]; events: BillingEvent[]; creditMovements: CreditMovement[]; }
export interface PlanChangeRequest { id: number; requestType: PlanChangeRequestType; currentPlan: CommercialPlanType; requestedPlan: CommercialPlanType | null; status: string; note: string | null; createdAt: string; updatedAt: string; }
export interface PlanManagementResponse { currentPlan: CommercialPlanType; enterpriseRequests: PlanChangeRequest[]; }
export interface AutoRechargeConfigResponse {
  enabled: boolean;
  thresholdCredits: number;
  planId: number | null;
  cardConfigured: boolean;
  status: AutoRechargeStatus;
  lastTriggeredAt: string | null;
  lastOutcome: string | null;
}
export interface AutoRechargeConfigRequest {
  enabled: boolean;
  thresholdCredits: number;
  planId: number | null;
  mpCustomerId?: string | null;
  mpCardId?: string | null;
}

async function billingRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const session = getSession();
  const headers: Record<string, string> = { "Content-Type": "application/json" };
  if (session.token) headers.Authorization = `Bearer ${session.token}`;
  const response = await fetch(`${getApiBaseUrl()}${path}`, { ...init, headers: { ...headers, ...init?.headers } });
  if (!response.ok) {
    let message = `Falha na API (${response.status})`;
    try {
      const body = (await response.json()) as { mensagem?: string; message?: string; error?: string };
      message = body.mensagem ?? body.message ?? body.error ?? message;
    } catch { /* mantém a mensagem HTTP */ }
    throw new PraxisApiError(message, response.status);
  }
  if (response.status === 204) return undefined as T;
  if ((response.headers.get("content-type") ?? "").includes("application/json")) return response.json() as Promise<T>;
  const text = await response.text();
  return (text.length > 0 ? text : undefined) as T;
}

export const getClientBilling = () => billingRequest<ClientBillingResponse>("/api/v1/billing");
export const listClientBillingPlans = () => billingRequest<SubscriptionPlan[]>("/api/v1/billing/plans");
export const getAutoRechargeConfig = () => billingRequest<AutoRechargeConfigResponse>("/api/v1/billing/auto-recharge");
export const configureAutoRecharge = (request: AutoRechargeConfigRequest) => billingRequest<AutoRechargeConfigResponse>("/api/v1/billing/auto-recharge", { method: "PUT", body: JSON.stringify(request) });

/**
 * A tela de cobrança já consulta /billing. Reutilizar esse resumo evita quebrar o carregamento
 * quando o frontend é publicado antes do backend que expõe as rotas de gestão de plano.
 */
export async function getClientPlanManagement(): Promise<PlanManagementResponse> {
  const billing = await getClientBilling();
  if (!billing.plan) {
    throw new PraxisApiError("Plano da empresa ainda não está definido.", 409);
  }
  return { currentPlan: billing.plan, enterpriseRequests: [] };
}

export const createClientCreditCheckout = (planId: number) => billingRequest<CheckoutResult>(`/api/v1/billing/credits/checkout?planId=${planId}`, { method: "POST" });
export const createClientSubscriptionCheckout = (planId: number) => billingRequest<CheckoutResult>(`/api/v1/billing/subscription/checkout?planId=${planId}`, { method: "POST" });
export const changeClientPlan = (planId: number) => billingRequest<CheckoutResult>(`/api/v1/billing/plan/change?planId=${planId}`, { method: "POST" });
export const syncClientSubscription = () => billingRequest<ClientBillingResponse>("/api/v1/billing/subscription/sync", { method: "POST" });
export const cancelClientSubscription = () => billingRequest<ClientBillingResponse>("/api/v1/billing/subscription/cancel", { method: "POST" });

export function createEnterprisePlanRequest(type: PlanChangeRequestType, requestedPlan?: CommercialPlanType, note?: string) {
  const params = new URLSearchParams({ type });
  if (requestedPlan) params.set("requestedPlan", requestedPlan);
  if (note?.trim()) params.set("note", note.trim());
  return billingRequest<PlanChangeRequest>(`/api/v1/billing/enterprise-request?${params.toString()}`, { method: "POST" });
}
