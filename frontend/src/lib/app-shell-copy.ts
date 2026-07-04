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
    notificationsLabel: "Notificações",
    notificationsDescription: "Alertas internos e DLQ",
    dlqTitle: "Entregas em DLQ precisam de atenção",
    dlqBody: "Há resultados aguardando reprocessamento. Revise em Notificações antes de considerar a integração saudável.",
    dlqCta: "Abrir notificações",
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
    dlqTitle: "Entregas en DLQ requieren atención",
    dlqBody: "Hay resultados esperando reprocesamiento. Revísalos en Notificaciones antes de considerar saludable la integración.",
    dlqCta: "Abrir notificaciones",
  },
};
