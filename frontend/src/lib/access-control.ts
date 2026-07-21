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

const CREATE_DRAFT_PATHS = new Set([
  "/nova/avaliacao",
  "/nova/personagem",
  "/nova/dialogo",
  "/nova/validador",
]);

const CREATE_DRAFT_LABELS = new Set([
  "criar rascunho",
  "criar rascunho para editar",
]);

const PUBLISH_LABELS = new Set([
  "ir para publicação",
  "ir para publicação →",
]);

const MANAGE_COMPETENCY_LABELS = new Set([
  "adicionar e salvar",
]);

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

export function isPartnerSpecialistForbiddenAction(pathname: string, label: string): boolean {
  const normalizedPath = normalizePath(pathname);
  const normalizedLabel = normalizeLabel(label);

  if (CREATE_DRAFT_PATHS.has(normalizedPath) && CREATE_DRAFT_LABELS.has(normalizedLabel)) {
    return true;
  }

  if (normalizedPath === "/nova/validador" && PUBLISH_LABELS.has(normalizedLabel)) {
    return true;
  }

  return normalizedPath === "/nova/avaliacao" && MANAGE_COMPETENCY_LABELS.has(normalizedLabel);
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
  filterRenderedActions();
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

      const button = target.closest<HTMLButtonElement>("button");
      if (
        button
        && isPartnerSpecialistForbiddenAction(window.location.pathname, button.textContent ?? "")
      ) {
        event.preventDefault();
        event.stopImmediatePropagation();
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
    filterRenderedActions();
    scheduleRouteCheck();
  });
  observer.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ["href", "hidden"],
    childList: true,
    subtree: true,
  });
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
    filterRenderedActions();
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

function filterRenderedActions(): void {
  const restricted = isRestrictedPartnerSpecialist(activeRoles);
  const pathname = normalizePath(window.location.pathname);

  document.querySelectorAll<HTMLElement>('[data-praxis-specialist-action-hidden="true"]').forEach(
    (element) => {
      if (restricted) {
        return;
      }
      restoreRestrictedAction(element);
    },
  );

  document.querySelectorAll<HTMLElement>('[data-praxis-specialist-note="true"]').forEach((note) => {
    if (!restricted) {
      note.remove();
    }
  });

  if (!restricted) {
    return;
  }

  document.querySelectorAll<HTMLButtonElement>("button").forEach((button) => {
    const label = button.textContent ?? "";
    if (!isPartnerSpecialistForbiddenAction(pathname, label)) {
      return;
    }

    const normalizedLabel = normalizeLabel(label);
    const container = resolveRestrictedActionContainer(button, pathname, normalizedLabel);
    hideRestrictedAction(container);
    addRestrictedActionNote(container, pathname, normalizedLabel);
  });
}

function resolveRestrictedActionContainer(
  button: HTMLButtonElement,
  pathname: string,
  normalizedLabel: string,
): HTMLElement {
  if (
    pathname === "/nova/avaliacao"
    && MANAGE_COMPETENCY_LABELS.has(normalizedLabel)
  ) {
    return button.closest<HTMLElement>(".grid") ?? button;
  }
  return button;
}

function hideRestrictedAction(element: HTMLElement): void {
  if (element.dataset.praxisSpecialistActionHidden === "true") {
    return;
  }

  element.dataset.praxisOriginalHidden = String(element.hidden);
  element.hidden = true;
  element.setAttribute("aria-hidden", "true");
  element.dataset.praxisSpecialistActionHidden = "true";
}

function restoreRestrictedAction(element: HTMLElement): void {
  element.hidden = element.dataset.praxisOriginalHidden === "true";
  element.removeAttribute("aria-hidden");
  element.removeAttribute("data-praxis-original-hidden");
  element.removeAttribute("data-praxis-specialist-action-hidden");
}

function addRestrictedActionNote(
  hiddenElement: HTMLElement,
  pathname: string,
  normalizedLabel: string,
): void {
  const noteKey = restrictedActionNoteKey(pathname, normalizedLabel);
  if (document.querySelector(`[data-praxis-specialist-note-key="${noteKey}"]`)) {
    return;
  }

  const note = document.createElement("p");
  note.dataset.praxisSpecialistNote = "true";
  note.dataset.praxisSpecialistNoteKey = noteKey;
  note.className = "text-xs text-muted-foreground";
  note.textContent = restrictedActionNote(pathname, normalizedLabel);
  hiddenElement.insertAdjacentElement("afterend", note);
}

function restrictedActionNoteKey(pathname: string, normalizedLabel: string): string {
  if (pathname === "/nova/avaliacao" && MANAGE_COMPETENCY_LABELS.has(normalizedLabel)) {
    return "competency-catalog";
  }
  if (pathname === "/nova/validador" && PUBLISH_LABELS.has(normalizedLabel)) {
    return "publication";
  }
  return "draft-clone";
}

function restrictedActionNote(pathname: string, normalizedLabel: string): string {
  if (pathname === "/nova/avaliacao" && MANAGE_COMPETENCY_LABELS.has(normalizedLabel)) {
    return "Você pode selecionar competências existentes. O cadastro no catálogo é responsabilidade da empresa.";
  }
  if (pathname === "/nova/validador" && PUBLISH_LABELS.has(normalizedLabel)) {
    return "A revisão foi concluída. A publicação deve ser realizada por um usuário da empresa.";
  }
  return "Versões publicadas são somente leitura para especialistas. Solicite à empresa a criação de um novo rascunho.";
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

function normalizePath(pathname: string): string {
  if (pathname.length > 1 && pathname.endsWith("/")) {
    return pathname.slice(0, -1);
  }
  return pathname;
}

function normalizeLabel(label: string): string {
  return label.replace(/\s+/g, " ").trim().toLocaleLowerCase("pt-BR");
}

function matchesPath(pathname: string, rootPath: string): boolean {
  return pathname === rootPath || pathname.startsWith(`${rootPath}/`);
}
