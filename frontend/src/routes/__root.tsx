import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet,
  Link,
  createRootRouteWithContext,
  useRouter,
  HeadContent,
  Scripts,
} from "@tanstack/react-router";
import { createServerFn } from "@tanstack/react-start";
import { useEffect, type ReactNode } from "react";

import appCss from "../styles/app.css?url";
import { reportAppError } from "../lib/app-error-reporting";
import { resolveRuntimeConfigFromEnv } from "../lib/runtime-config.server";
import { LanguageProvider, useLanguage } from "../lib/language-context";

// Runs only on the server. Reads the public runtime config from env per request
// so it can be injected into the page and picked up by the browser bundle.
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
        <p className="mt-2 text-sm text-muted-foreground">
          {t.common.pageNotFoundDesc}
        </p>
        <div className="mt-6">
          <Link
            to="/app"
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
  console.error(error);
  const router = useRouter();
  const { t } = useLanguage();
  useEffect(() => {
    reportAppError(error, { boundary: "tanstack_root_error_component" });
  }, [error]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold tracking-tight text-foreground">
          {t.common.pageDidNotLoad}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {t.common.somethingWentWrong}
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-2">
          <button
            onClick={() => {
              router.invalidate();
              reset();
            }}
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            {t.common.tryAgain}
          </button>
          <a
            href="/app"
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
      { title: "Práxis — Avaliação de candidatos de forma justa e clara" },
      {
        name: "description",
        content:
          "Práxis é uma plataforma para avaliar candidatos com situações reais do trabalho. Resultados justos e transparentes, sem inteligência artificial decidindo. Veja como funciona.",
      },
      { property: "og:title", content: "Práxis — Avaliação de candidatos" },
      {
        property: "og:description",
        content:
          "Avalie candidatos com situações reais do trabalho. Resultados justos e transparentes, sem IA decidindo sozinha.",
      },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
      { name: "twitter:site", content: "@iForce" },
    ],
    links: [
      {
        rel: "stylesheet",
        href: appCss,
      },
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
    <html lang="pt-BR">
      <head>
        <HeadContent />
        {/* Must run before the app bundle so getRuntimeConfig() sees it. */}
        <script
          dangerouslySetInnerHTML={{
            __html: `window.__PRAXIS_CONFIG__=${JSON.stringify(runtimeConfig)};
            (function() {
              const lang = localStorage.getItem('praxis-language') || 'pt-BR';
              document.documentElement.lang = lang;
            })();`,
          }}
        />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}

function RootComponent() {
  const { queryClient } = Route.useRouteContext();

  useEffect(() => {
    if (!("serviceWorker" in navigator)) return;
    navigator.serviceWorker.register("/praxis-sw.js").catch(() => undefined);
  }, []);

  return (
    <LanguageProvider>
      <QueryClientProvider client={queryClient}>
        <Outlet />
      </QueryClientProvider>
    </LanguageProvider>
  );
}
