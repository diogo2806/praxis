import { useEffect } from "react";
import { useLanguage } from "@/lib/language-context";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];
const PAGINATION_HIDDEN_ATTRIBUTE = "data-table-pagination-hidden";
const COLLECTION_PAGINATION_HIDDEN_ATTRIBUTE = "data-collection-pagination-hidden";

type PaginationCopy = {
  previous: string;
  previousAria: string;
  next: string;
  nextAria: string;
  navigationAria: string;
  collectionNavigationAria: string;
  rowsPerPage: string;
  itemsPerPage: string;
  empty: string;
  range: (start: number, end: number, total: number) => string;
  page: (current: number, total: number) => string;
};

const COPY: Record<string, PaginationCopy> = {
  "pt-BR": {
    previous: "Anterior",
    previousAria: "Ir para a página anterior",
    next: "Próxima",
    nextAria: "Ir para a próxima página",
    navigationAria: "Paginação da tabela",
    collectionNavigationAria: "Paginação da lista",
    rowsPerPage: "Linhas por página",
    itemsPerPage: "Itens por página",
    empty: "0 registros",
    range: (start, end, total) => `${start}–${end} de ${total}`,
    page: (current, total) => `Página ${current} de ${total}`,
  },
  en: {
    previous: "Previous",
    previousAria: "Go to the previous page",
    next: "Next",
    nextAria: "Go to the next page",
    navigationAria: "Table pagination",
    collectionNavigationAria: "List pagination",
    rowsPerPage: "Rows per page",
    itemsPerPage: "Items per page",
    empty: "0 records",
    range: (start, end, total) => `${start}–${end} of ${total}`,
    page: (current, total) => `Page ${current} of ${total}`,
  },
  "es-MX": {
    previous: "Anterior",
    previousAria: "Ir a la página anterior",
    next: "Siguiente",
    nextAria: "Ir a la página siguiente",
    navigationAria: "Paginación de la tabla",
    collectionNavigationAria: "Paginación de la lista",
    rowsPerPage: "Filas por página",
    itemsPerPage: "Elementos por página",
    empty: "0 registros",
    range: (start, end, total) => `${start}–${end} de ${total}`,
    page: (current, total) => `Página ${current} de ${total}`,
  },
};

type PaginationState = {
  page: number;
  pageSize: number;
  itemCount: number;
  controls: HTMLDivElement;
  summary: HTMLSpanElement;
  pageLabel: HTMLSpanElement;
  previousButton: HTMLButtonElement;
  nextButton: HTMLButtonElement;
  pageSizeSelect: HTMLSelectElement;
};

export function GlobalTablePagination() {
  const { language } = useLanguage();

  useEffect(() => {
    if (typeof document === "undefined") return;

    const copy = COPY[language] ?? COPY["pt-BR"];
    const tableStates = new Map<HTMLTableElement, PaginationState>();
    const collectionStates = new Map<HTMLElement, PaginationState>();
    let scheduledFrame: number | null = null;

    const style = document.createElement("style");
    style.dataset.tablePaginationStyles = "true";
    style.textContent = `
      tr[${PAGINATION_HIDDEN_ATTRIBUTE}="true"],
      [${COLLECTION_PAGINATION_HIDDEN_ATTRIBUTE}="true"] {
        display: none !important;
      }
    `;
    document.head.appendChild(style);

    function getRows(table: HTMLTableElement) {
      return Array.from(table.tBodies).flatMap((body) =>
        Array.from(body.rows).filter((row) => !row.hasAttribute("data-pagination-ignore")),
      );
    }

    function getCollectionItems(collection: HTMLElement) {
      return Array.from(collection.children).filter(
        (item): item is HTMLElement =>
          item instanceof HTMLElement && !item.hasAttribute("data-pagination-ignore"),
      );
    }

    function getCollections() {
      return Array.from(
        document.querySelectorAll<HTMLElement>("section > div.divide-y.divide-border"),
      ).filter((collection) => {
        const items = getCollectionItems(collection);
        return items.length > 0 && items.every((item) => item instanceof HTMLButtonElement);
      });
    }

    function hasManualPagination(target: HTMLElement) {
      let scope: HTMLElement | null = target.parentElement;
      for (let level = 0; scope && level < 5; level += 1, scope = scope.parentElement) {
        if (
          scope.querySelector("[data-manual-pagination]") ||
          scope.querySelector('[role="navigation"][aria-label*="agina"]')
        ) {
          return true;
        }

        const buttonLabels = Array.from(scope.querySelectorAll("button"))
          .map((button) => button.textContent?.trim().toLowerCase() ?? "")
          .filter(Boolean);
        const hasPrevious = buttonLabels.some((label) =>
          ["anterior", "previous"].includes(label),
        );
        const hasNext = buttonLabels.some((label) =>
          ["próxima", "proxima", "next", "siguiente"].includes(label),
        );
        if (hasPrevious && hasNext) return true;

        if (scope.classList.contains("praxis-page-shell")) break;
      }
      return false;
    }

    function shouldSkip(target: HTMLElement) {
      return (
        target.hasAttribute("data-no-pagination") ||
        target.hasAttribute("data-server-pagination") ||
        target.closest("[data-no-pagination]") != null ||
        target.closest("[data-server-pagination]") != null ||
        hasManualPagination(target)
      );
    }

    function createButton(label: string, ariaLabel: string) {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = label;
      button.setAttribute("aria-label", ariaLabel);
      button.className =
        "inline-flex h-9 items-center justify-center rounded-md border border-border bg-background px-3 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50";
      return button;
    }

    function createControls(
      target: HTMLElement,
      navigationAria: string,
      pageSizeTextValue: string,
      getItemCount: () => number,
      renderTarget: () => void,
      datasetKey: "tablePagination" | "collectionPagination",
    ) {
      const controls = document.createElement("div");
      const summary = document.createElement("span");
      const actions = document.createElement("div");
      const pageSizeLabel = document.createElement("label");
      const pageSizeText = document.createElement("span");
      const pageSizeSelect = document.createElement("select");
      const previousButton = createButton(copy.previous, copy.previousAria);
      const pageLabel = document.createElement("span");
      const nextButton = createButton(copy.next, copy.nextAria);

      controls.dataset[datasetKey] = "true";
      controls.setAttribute("role", "navigation");
      controls.setAttribute("aria-label", navigationAria);
      controls.className =
        "mt-3 flex w-full flex-wrap items-center justify-between gap-3 border-t border-border pt-3 text-sm text-muted-foreground";
      summary.setAttribute("aria-live", "polite");
      summary.className = "tabular-nums";
      actions.className = "flex flex-wrap items-center gap-2";
      pageSizeLabel.className = "flex items-center gap-2";
      pageSizeText.textContent = pageSizeTextValue;
      pageSizeText.className = "sr-only sm:not-sr-only";
      pageSizeSelect.setAttribute("aria-label", pageSizeTextValue);
      pageSizeSelect.className =
        "h-9 rounded-md border border-border bg-background px-2 text-sm text-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring";

      PAGE_SIZE_OPTIONS.forEach((pageSize) => {
        const option = document.createElement("option");
        option.value = String(pageSize);
        option.textContent = String(pageSize);
        if (pageSize === DEFAULT_PAGE_SIZE) option.selected = true;
        pageSizeSelect.appendChild(option);
      });

      pageLabel.setAttribute("aria-live", "polite");
      pageLabel.className = "min-w-24 text-center tabular-nums text-foreground";
      pageSizeLabel.append(pageSizeText, pageSizeSelect);
      actions.append(pageSizeLabel, previousButton, pageLabel, nextButton);
      controls.append(summary, actions);
      target.insertAdjacentElement("afterend", controls);

      const state: PaginationState = {
        page: 1,
        pageSize: DEFAULT_PAGE_SIZE,
        itemCount: 0,
        controls,
        summary,
        pageLabel,
        previousButton,
        nextButton,
        pageSizeSelect,
      };

      previousButton.addEventListener("click", () => {
        state.page = Math.max(1, state.page - 1);
        renderTarget();
      });
      nextButton.addEventListener("click", () => {
        const totalPages = Math.max(1, Math.ceil(getItemCount() / state.pageSize));
        state.page = Math.min(totalPages, state.page + 1);
        renderTarget();
      });
      pageSizeSelect.addEventListener("change", () => {
        const nextPageSize = Number(pageSizeSelect.value);
        state.pageSize = Number.isFinite(nextPageSize) ? nextPageSize : DEFAULT_PAGE_SIZE;
        state.page = 1;
        renderTarget();
      });

      return state;
    }

    function updateControls(
      target: HTMLElement,
      state: PaginationState,
      totalItems: number,
      setVisibility: (startIndex: number, endIndex: number) => void,
    ) {
      if (state.itemCount !== totalItems) state.page = 1;
      state.itemCount = totalItems;
      const totalPages = Math.max(1, Math.ceil(totalItems / state.pageSize));
      state.page = Math.min(Math.max(1, state.page), totalPages);
      const startIndex = (state.page - 1) * state.pageSize;
      const endIndex = Math.min(startIndex + state.pageSize, totalItems);
      setVisibility(startIndex, endIndex);

      state.summary.textContent =
        totalItems === 0 ? copy.empty : copy.range(startIndex + 1, endIndex, totalItems);
      state.pageLabel.textContent = copy.page(state.page, totalPages);
      state.previousButton.disabled = state.page <= 1;
      state.nextButton.disabled = state.page >= totalPages;
      state.controls.hidden =
        totalItems <= DEFAULT_PAGE_SIZE || target.hidden || target.closest("[hidden]") != null;
    }

    function showAllRows(table: HTMLTableElement) {
      getRows(table).forEach((row) => row.removeAttribute(PAGINATION_HIDDEN_ATTRIBUTE));
    }

    function showAllCollectionItems(collection: HTMLElement) {
      getCollectionItems(collection).forEach((item) =>
        item.removeAttribute(COLLECTION_PAGINATION_HIDDEN_ATTRIBUTE),
      );
    }

    function removeTableState(table: HTMLTableElement) {
      const state = tableStates.get(table);
      if (!state) return;
      showAllRows(table);
      state.controls.remove();
      tableStates.delete(table);
    }

    function removeCollectionState(collection: HTMLElement) {
      const state = collectionStates.get(collection);
      if (!state) return;
      showAllCollectionItems(collection);
      state.controls.remove();
      collectionStates.delete(collection);
    }

    function renderTable(table: HTMLTableElement, state: PaginationState) {
      const rows = getRows(table);
      updateControls(table, state, rows.length, (startIndex, endIndex) => {
        rows.forEach((row, index) => {
          row.toggleAttribute(
            PAGINATION_HIDDEN_ATTRIBUTE,
            index < startIndex || index >= endIndex,
          );
        });
      });
    }

    function renderCollection(collection: HTMLElement, state: PaginationState) {
      const items = getCollectionItems(collection);
      updateControls(collection, state, items.length, (startIndex, endIndex) => {
        items.forEach((item, index) => {
          item.toggleAttribute(
            COLLECTION_PAGINATION_HIDDEN_ATTRIBUTE,
            index < startIndex || index >= endIndex,
          );
        });
      });
    }

    function refresh() {
      tableStates.forEach((_state, table) => {
        if (!table.isConnected) removeTableState(table);
      });
      collectionStates.forEach((_state, collection) => {
        if (!collection.isConnected) removeCollectionState(collection);
      });

      document.querySelectorAll<HTMLTableElement>("table").forEach((table) => {
        if (shouldSkip(table)) {
          removeTableState(table);
          showAllRows(table);
          return;
        }
        let state = tableStates.get(table);
        if (!state) {
          state = createControls(
            table,
            copy.navigationAria,
            copy.rowsPerPage,
            () => getRows(table).length,
            () => state && renderTable(table, state),
            "tablePagination",
          );
          tableStates.set(table, state);
        }
        renderTable(table, state);
      });

      const collections = new Set(getCollections());
      collectionStates.forEach((_state, collection) => {
        if (!collections.has(collection) || shouldSkip(collection)) {
          removeCollectionState(collection);
        }
      });
      collections.forEach((collection) => {
        if (shouldSkip(collection)) {
          removeCollectionState(collection);
          showAllCollectionItems(collection);
          return;
        }
        let state = collectionStates.get(collection);
        if (!state) {
          state = createControls(
            collection,
            copy.collectionNavigationAria,
            copy.itemsPerPage,
            () => getCollectionItems(collection).length,
            () => state && renderCollection(collection, state),
            "collectionPagination",
          );
          collectionStates.set(collection, state);
        }
        renderCollection(collection, state);
      });
    }

    function scheduleRefresh() {
      if (scheduledFrame != null) return;
      scheduledFrame = window.requestAnimationFrame(() => {
        scheduledFrame = null;
        refresh();
      });
    }

    const observer = new MutationObserver(scheduleRefresh);
    observer.observe(document.body, { childList: true, subtree: true });
    refresh();

    return () => {
      observer.disconnect();
      if (scheduledFrame != null) window.cancelAnimationFrame(scheduledFrame);
      tableStates.forEach((_state, table) => removeTableState(table));
      collectionStates.forEach((_state, collection) => removeCollectionState(collection));
      style.remove();
    };
  }, [language]);

  return null;
}
