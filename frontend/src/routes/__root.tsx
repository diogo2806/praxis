import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  HeadContent,
  Link,
  Outlet,
  Scripts,
  createRootRouteWithContext,
  useRouter,
  useRouterState,
} from "@tanstack/react-router";
import { createServerFn } from "@tanstack/react-start";
import { useEffect, type ReactNode } from "react";

import { GlobalTablePagination } from "../components/global-table-pagination";
import { ScreenManual } from "../components/screen-manual";
import { reportAppError } from "../lib/app-error-reporting";
import { LanguageProvider, useLanguage } from "../lib/language-context";
import { resolveRuntimeConfigFromEnv } from "../lib/runtime-config.server";
import { clearAuthenticatedSession } from "../lib/session";
import accessibilityOverridesCss from "../styles/accessibility-overrides.css?url";
import appCss from "../styles/app.css?url";
import flowCanvasWorkspaceCss from "../styles/flow-canvas-workspace.css?url";
import landingAccessibilityCss from "../styles/landing-accessibility.css?url";
import tablesCss from "../styles/tables.css?url";

const getRuntimeConfig = createServerFn({ method: "GET" }).handler(() =>
  resolveRuntimeConfigFromEnv(),
);

function NotFoundComponent() {
  const { t } = useLanguage();

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-7xl font-bold text-foreground">404</h1>
        <h2 className="mt-4 text-xl font-semibold text-foreground">{t.common.notFound}</h2>
        <p className="mt-2 text-sm text-muted-foreground">{t.common.pageNotFoundDesc}</p>
        <div className="mt-6">
          <Link
            to="/dashboard"
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            {t.common.backToPanel}
          </Link>
        </div>
      </div>
    </div>
  );
}

function ErrorComponent({ error, reset }: { error: Error; reset: () => void }) {
  const router = useRouter();
  const { t } = useLanguage();

  useEffect(() => {
    console.error(error);
    reportAppError(error, { boundary: "tanstack_root_error_component" });
  }, [error]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">
          {t.common.pageDidNotLoad}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">{t.common.somethingWentWrong}</p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button
            type="button"
            onClick={() => {
              router.invalidate();
              reset();
            }}
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            {t.common.tryAgain}
          </button>
          <a
            href="/dashboard"
            className="inline-flex items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-accent"
          >
            {t.common.backToPanel}
          </a>
        </div>
      </div>
    </div>
  );
}

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  loader: async () => ({ runtimeConfig: await getRuntimeConfig() }),
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { httpEquiv: "Cache-Control", content: "no-cache, no-store, must-revalidate" },
      { httpEquiv: "Pragma", content: "no-cache" },
      { httpEquiv: "Expires", content: "0" },
      { title: "Práxis — Avaliações por cenários estruturadas e rastreáveis" },
      {
        name: "description",
        content:
          "Crie avaliações por cenários, configure critérios e pesos, compartilhe por link e acompanhe respostas, indicadores e registros do percurso.",
      },
      { property: "og:title", content: "Práxis - Avaliações situacionais" },
      {
        property: "og:description",
        content:
          "Crie cenários interativos, compartilhe avaliações por link e acompanhe indicadores definidos previamente pela sua equipe.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
      { name: "twitter:site", content: "@iForce" },
    ],
    links: [
      { rel: "icon", type: "image/svg+xml", href: "/favicon.svg" },
      { rel: "stylesheet", href: appCss },
      { rel: "stylesheet", href: flowCanvasWorkspaceCss },
      { rel: "stylesheet", href: tablesCss },
      { rel: "stylesheet", href: landingAccessibilityCss },
      { rel: "stylesheet", href: accessibilityOverridesCss },
    ],
  }),
  shellComponent: RootShell,
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

function RootShell({ children }: { children: ReactNode }) {
  const { runtimeConfig } = Route.useLoaderData();

  return (
    <LanguageProvider>
      <html lang="pt-BR">
        <head>
          <HeadContent />
          <script
            dangerouslySetInnerHTML={{
              __html: `window.__PRAXIS_CONFIG__=${JSON.stringify(runtimeConfig)};`,
            }}
          />
        </head>
        <body>
          {children}
          <VLibrasWidget />
          <Scripts />
        </body>
      </html>
    </LanguageProvider>
  );
}

type VLibrasApi = {
  Widget: new (baseUrl: string) => unknown;
};

type VLibrasWindow = Window & {
  VLibras?: VLibrasApi;
};

function VLibrasWidget() {
  useEffect(() => {
    const widgetWindow = window as VLibrasWindow;
    let disposed = false;
    let script: HTMLScriptElement | null = null;

    const applyPopupDimensions = () => {
      document.querySelectorAll<HTMLImageElement>("img.vp-pop-up").forEach((image) => {
        image.width = 150;
        image.height = 40;
      });
    };

    const observer = new MutationObserver(applyPopupDimensions);
    observer.observe(document.body, { childList: true, subtree: true });

    const initializeWidget = () => {
      if (disposed || !widgetWindow.VLibras) return;
      new widgetWindow.VLibras.Widget("https://vlibras.gov.br/app");
      applyPopupDimensions();
    };

    const loadWidget = () => {
      if (disposed) return;

      const existingScript = document.querySelector<HTMLScriptElement>(
        'script[data-praxis-vlibras="true"]',
      );
      if (existingScript) {
        script = existingScript;
        if (widgetWindow.VLibras) {
          initializeWidget();
        } else {
          existingScript.addEventListener("load", initializeWidget, { once: true });
        }
        return;
      }

      script = document.createElement("script");
      script.src = "https://vlibras.gov.br/app/vlibras-plugin.js";
      script.async = true;
      script.dataset.praxisVlibras = "true";
      script.addEventListener("load", initializeWidget, { once: true });
      document.body.appendChild(script);
    };

    const startAfterLoad = () => window.setTimeout(loadWidget, 0);
    let timeoutId: number | undefined;

    if (document.readyState === "complete") {
      timeoutId = startAfterLoad();
    } else {
      window.addEventListener("load", startAfterLoad, { once: true });
    }

    return () => {
      disposed = true;
      observer.disconnect();
      if (timeoutId !== undefined) window.clearTimeout(timeoutId);
      window.removeEventListener("load", startAfterLoad);
      script?.removeEventListener("load", initializeWidget);
    };
  }, []);

  return (
    <div
      dangerouslySetInnerHTML={{
        __html:
          '<div vw class="enabled"><div vw-access-button class="active"></div><div vw-plugin-wrapper><div class="vw-plugin-top-wrapper"></div></div></div>',
      }}
    />
  );
}

function RootComponent() {
  const { queryClient } = Route.useRouteContext();

  return (
    <QueryClientProvider client={queryClient}>
      <SessionExpiryRedirect />
      <GlobalTablePagination />
      <GlobalScreenManual />
      <Outlet />
    </QueryClientProvider>
  );
}

function GlobalScreenManual() {
  const pathname = useRouterState({ select: (state) => state.location.pathname });

  return (
    <div className="fixed right-4 top-20 z-40 sm:right-6">
      <ScreenManual pathname={pathname} iconOnly />
    </div>
  );
}

function SessionExpiryRedirect() {
  useEffect(() => {
    if (typeof window === "undefined") return;

    const originalFetch = window.fetch.bind(window);
    window.fetch = async (...args) => {
      const response = await originalFetch(...args);
      if (response.status === 401 && shouldHandleApiFailure(args[0])) {
        clearAuthenticatedSession();
        const pathname = window.location.pathname;
        if (!pathname.startsWith("/login") && !pathname.startsWith("/candidato")) {
          window.location.assign("/login");
        }
      }
      return response;
    };

    return () => {
      window.fetch = originalFetch;
    };
  }, []);

  return null;
}

function shouldHandleApiFailure(input: RequestInfo | URL) {
  const url = typeof input === "string" ? input : input instanceof URL ? input.toString() : input.url;
  return url.includes("/api/");
}
