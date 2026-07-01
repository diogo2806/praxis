import type {
  ListingCategory,
  ListingStatus,
  MarketplaceOrderStatus,
  MarketplacePayoutSummary,
  ProfessionalVerificationStatus,
} from "@/lib/api/praxis";

export const marketplaceCategories: Array<{ value: ListingCategory; label: string }> = [
  { value: "SELECAO", label: "Seleção" },
  { value: "TREINAMENTO", label: "Treinamento" },
  { value: "COMPLIANCE", label: "Compliance" },
  { value: "EDUCACAO", label: "Educação" },
  { value: "LIDERANCA", label: "Liderança" },
  { value: "ATENDIMENTO", label: "Atendimento" },
];

export const listingStatusLabels: Record<ListingStatus, string> = {
  DRAFT: "Rascunho",
  PENDING_REVIEW: "Em revisão",
  APPROVED: "Publicado",
  REJECTED: "Reprovado",
  SUSPENDED: "Suspenso",
};

export const professionalStatusLabels: Record<ProfessionalVerificationStatus, string> = {
  PENDING_VERIFICATION: "Em verificação",
  VERIFIED: "Verificado",
  REJECTED: "Reprovado",
  SUSPENDED: "Suspenso",
};

export const orderStatusLabels: Record<MarketplaceOrderStatus, string> = {
  PENDING_PAYMENT: "Pagamento pendente",
  PAID: "Pago",
  REFUNDED: "Reembolsado",
  DISPUTED: "Em disputa",
  CANCELLED: "Cancelado",
};

export const payoutStatusLabels: Record<MarketplacePayoutSummary["status"], string> = {
  ESCROW: "Em escrow",
  RELEASED: "Liberado",
  FAILED: "Falhou",
  REVERSED: "Revertido",
};

export function categoryLabel(category: ListingCategory) {
  return marketplaceCategories.find((item) => item.value === category)?.label ?? category;
}

export function formatMarketplacePrice(priceCents: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(priceCents / 100);
}

export function formatMarketplaceDate(value: string | null) {
  if (!value) return "Pendente";
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export function splitList(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}
