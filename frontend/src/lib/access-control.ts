export const ADMIN_ROLE = "ADMIN";
export const EMPRESA_ROLE = "EMPRESA";
export const PARTNER_SPECIALIST_ROLE = "PARTNER_SPECIALIST";

const PARTNER_SPECIALIST_ALLOWED_PATHS = new Set([
  "/avaliacoes",
  "/competencias",
  "/configuracoes/conta",
  "/manual",
  "/nova/avaliacao",
  "/nova/personagem",
  "/nova/validador",
  "/nova/objetivo",
  "/nova/dialogo",
  "/nova/mapa",
  "/nova/piloto",
  "/nova/rapido",
  "/nova/avaliacoes",
  "/nova/blueprint",
  "/nova/competencias",
  "/simulations/new",
]);

const PUBLIC_PATH_PREFIXES = [
  "/login",
  "/recuperar-senha",
  "/reset-password",
  "/convite",
  "/candidato",
];

let activeRoles: string[] = [];
let observer: MutationObserver | null = null;
let clickHandlerInstalled = false;
let routeCheckScheduled = false;

export function hasRole(roles: string[], role: string): boolean {
  return roles.includes(role);
}

export function isRestrictedPartnerSpecialist(roles: string[]): boolean {
  return hasRole(roles, PARTNER_SPECIALIST_ROLE)
    && !hasRole(roles, EMPRESA_ROLE)
    && !hasRole(roles, ADMIN_ROLE);
}

export function resolveDefaultAuthenticatedRoute(roles: string[]): "/admin" | "/avaliacoes" {
  return hasRole(roles, ADMIN_ROLE) ? "/admin" : "/avaliacoes";
}

export function canAccessFrontendPath(pathname: string, roles: string[]): boolean {
  if (!isRestrictedPartnerSpecialist(roles)) {
    return true;
  }

  if (pathname === "/" || PUBLIC_PATH_PREFIXES.some((prefix) => matchesPath(pathname, prefix))) {
    return true;
  }

  return [...PARTNER_SPECIALIST_ALLOWED_PATHS].some((allowedPath) => matchesPath(pathname, allowedPath));
}

export function applyBrowserAccessPolicy(roles: string[]): void {
  if (typeof window === "undefined" || typeof document === "undefined") {
    return;
  }

  activeRoles = [...roles];
  document.documentElement.dataset.praxisAccessProfile = isRestrictedPartnerSpecialist(activeRoles)
    ? "partner-specialist"
    : "default";

  enforceCurrentRoute();
  filterRenderedLinks();
  installDocumentClickGuard();
  installDomObserver();
}

function installDocumentClickGuard(): void {
  if (clickHandlerInstalled) {
    return;
  }

  document.addEventListener(
    "click",
    (event) => {
      if (!isRestrictedPartnerSpecialist(activeRoles)) {
        return;
      }

      const target = event.target;
      if (!(target instanceof Element)) {
        return;
      }

      const anchor = target.closest<HTMLAnchorElement>("a[href]");
      if (!anchor) {
        return;
      }

      const pathname = resolveSameOriginPath(anchor);
      if (pathname && !canAccessFrontendPath(pathname, activeRoles)) {
        event.preventDefault();
        event.stopPropagation();
        window.location.assign(resolveDefaultAuthenticatedRoute(activeRoles));
      }
    },
    true,
  );
  clickHandlerInstalled = true;
}

function installDomObserver(): void {
  if (observer) {
    return;
  }

  observer = new MutationObserver(() => {
    filterRenderedLinks();
    scheduleRouteCheck();
  });
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener("popstate", enforceCurrentRoute);
}

function scheduleRouteCheck(): void {
  if (routeCheckScheduled) {
    return;
  }

  routeCheckScheduled = true;
  window.requestAnimationFrame(() => {
    routeCheckScheduled = false;
    enforceCurrentRoute();
  });
}

function enforceCurrentRoute(): void {
  if (!isRestrictedPartnerSpecialist(activeRoles)) {
    return;
  }

  if (!canAccessFrontendPath(window.location.pathname, activeRoles)) {
    window.location.replace(resolveDefaultAuthenticatedRoute(activeRoles));
  }
}

function filterRenderedLinks(): void {
  const restricted = isRestrictedPartnerSpecialist(activeRoles);

  document.querySelectorAll<HTMLAnchorElement>('a[data-praxis-original-href]').forEach((anchor) => {
    if (!restricted) {
      anchor.setAttribute("href", anchor.dataset.praxisOriginalHref ?? "/dashboard");
      anchor.removeAttribute("data-praxis-original-href");
    }
  });

  document.querySelectorAll<HTMLAnchorElement>("a[href]").forEach((anchor) => {
    const pathname = resolveSameOriginPath(anchor);
    const allowed = !restricted || !pathname || canAccessFrontendPath(pathname, activeRoles);

    if (allowed) {
      if (anchor.dataset.praxisAccessHidden === "true") {
        anchor.hidden = false;
        anchor.removeAttribute("aria-hidden");
        anchor.removeAttribute("tabindex");
        anchor.removeAttribute("data-praxis-access-hidden");
      }
      return;
    }

    if (pathname === "/dashboard" && anchor.querySelector(".font-display")) {
      if (!anchor.dataset.praxisOriginalHref) {
        anchor.dataset.praxisOriginalHref = anchor.getAttribute("href") ?? "/dashboard";
      }
      anchor.setAttribute("href", resolveDefaultAuthenticatedRoute(activeRoles));
      return;
    }

    if (anchor.dataset.praxisAccessHidden !== "true") {
      anchor.hidden = true;
      anchor.setAttribute("aria-hidden", "true");
      anchor.setAttribute("tabindex", "-1");
      anchor.dataset.praxisAccessHidden = "true";
    }
  });

  document.querySelectorAll<HTMLElement>("nav .space-y-5 > div").forEach((group) => {
    const visibleLinks = Array.from(group.querySelectorAll<HTMLAnchorElement>("a[href]")).some(
      (anchor) => !anchor.hidden,
    );
    group.hidden = restricted && !visibleLinks;
  });
}

function resolveSameOriginPath(anchor: HTMLAnchorElement): string | null {
  const href = anchor.getAttribute("href");
  if (!href || href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) {
    return null;
  }

  try {
    const url = new URL(href, window.location.origin);
    return url.origin === window.location.origin ? url.pathname : null;
  } catch {
    return null;
  }
}

function matchesPath(pathname: string, rootPath: string): boolean {
  return pathname === rootPath || pathname.startsWith(`${rootPath}/`);
}
