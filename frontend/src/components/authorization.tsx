import type { ReactNode } from "react";

import {
  canAccessFrontendPath,
  canPerformFrontendAction,
  resolveDefaultAuthenticatedRoute,
  type FrontendAction,
} from "@/lib/access-control";
import { useSession } from "@/lib/session";

export function RouteAccessBoundary({
  pathname,
  children,
}: {
  pathname: string;
  children: ReactNode;
}) {
  const session = useSession();

  if (canAccessFrontendPath(pathname, session.roles)) {
    return <>{children}</>;
  }

  const destination = resolveDefaultAuthenticatedRoute(session.roles);
  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center px-6 py-12 text-center">
      <p className="text-sm font-semibold uppercase tracking-wide text-primary">Acesso negado</p>
      <h1 className="mt-3 text-3xl font-semibold text-foreground">Seu perfil não possui acesso a esta área.</h1>
      <p className="mt-4 text-sm leading-6 text-muted-foreground">
        A permissão é verificada antes da tela ser renderizada. Use a navegação disponível para o seu perfil.
      </p>
      <a
        href={destination}
        className="mx-auto mt-6 inline-flex min-h-11 items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-medium text-primary-foreground"
      >
        Voltar para uma área permitida
      </a>
    </main>
  );
}

export function AuthorizedAction({
  action,
  children,
  fallback = null,
}: {
  action: FrontendAction;
  children: ReactNode;
  fallback?: ReactNode;
}) {
  const session = useSession();
  return canPerformFrontendAction(action, session.roles) ? <>{children}</> : <>{fallback}</>;
}

export function useAuthorization() {
  const session = useSession();
  return {
    canAccessPath: (pathname: string) => canAccessFrontendPath(pathname, session.roles),
    canPerform: (action: FrontendAction) => canPerformFrontendAction(action, session.roles),
  };
}
