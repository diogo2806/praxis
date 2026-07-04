import type { Language } from "@/lib/translations";

export type AppShellCopy = {
  notificationsLabel: string;
  notificationsDescription: string;
  dlqTitle: string;
  dlqBody: string;
  dlqCta: string;
};

export const appShellCopy: Record<Language, AppShellCopy> = {
  "pt-BR": {
    notificationsLabel: "Notificacoes",
    notificationsDescription: "Alertas internos e DLQ",
    dlqTitle: "Entregas em DLQ precisam de atencao",
    dlqBody: "Ha resultados aguardando reprocessamento. Revise em Notificacoes antes de considerar a integracao saudavel.",
    dlqCta: "Abrir notificacoes",
  },
  en: {
    notificationsLabel: "Notifications",
    notificationsDescription: "Internal alerts and DLQ",
    dlqTitle: "DLQ deliveries need attention",
    dlqBody: "Some result deliveries are waiting for reprocessing. Review them in Notifications before treating the integration as healthy.",
    dlqCta: "Open notifications",
  },
  "es-MX": {
    notificationsLabel: "Notificaciones",
    notificationsDescription: "Alertas internos y DLQ",
    dlqTitle: "Entregas en DLQ requieren atencion",
    dlqBody: "Hay resultados esperando reprocesamiento. Revisalos en Notificaciones antes de considerar saludable la integracion.",
    dlqCta: "Abrir notificaciones",
  },
};
