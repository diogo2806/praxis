import { useEffect } from "react";

const DEFAULT_PAGE_SIZE = 10;
const PAGE_SIZE_OPTIONS = [10, 25, 50];
const PAGINATION_HIDDEN_ATTRIBUTE = "data-table-pagination-hidden";

type TablePaginationState = {
  page: number;
  pageSize: number;
  rows: HTMLTableRowElement[];
  controls: HTMLDivElement;
  summary: HTMLSpanElement;
  pageLabel: HTMLSpanElement;
  previousButton: HTMLButtonElement;
  nextButton: HTMLButtonElement;
  pageSizeSelect: HTMLSelectElement;
};

export function GlobalTablePagination() {
  useEffect(() => {
    if (typeof document === "undefined") return;

    const states = new Map<HTMLTableElement, TablePaginationState>();
    const style = document.createElement("style");
    let scheduledFrame: number | null = null;

    style.dataset.tablePaginationStyles = "true";
    style.textContent = `tr[${PAGINATION_HIDDEN_ATTRIBUTE}="true"] { display: none !important; }`;
    document.head.appendChild(style);

    function getRows(table: HTMLTableElement) {
      return Array.from(table.tBodies).flatMap((body) =>
        Array.from(body.rows).filter((row) => !row.hasAttribute("data-pagination-ignore")),
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

    function createControls(table: HTMLTableElement) {
      const controls = document.createElement("div");
      const summary = document.createElement("span");
      const actions = document.createElement("div");
      const pageSizeLabel = document.createElement("label");
      const pageSizeText = document.createElement("span");
      const pageSizeSelect = document.createElement("select");
      const previousButton = createButton("Anterior", "Ir para a página anterior");
      const pageLabel = document.createElement("span");
      const nextButton = createButton("Próxima", "Ir para a próxima página");

      controls.dataset.tablePagination = "true";
      controls.setAttribute("role", "navigation");
      controls.setAttribute("aria-label", "Paginação da tabela");
      controls.className =
        "mt-3 flex w-full flex-wrap items-center justify-between gap-3 border-t border-border pt-3 text-sm text-muted-foreground";

      summary.setAttribute("aria-live", "polite");
      summary.className = "tabular-nums";

      actions.className = "flex flex-wrap items-center gap-2";

      pageSizeLabel.className = "flex items-center gap-2";
      pageSizeText.textContent = "Linhas por página";
      pageSizeText.className = "sr-only sm:not-sr-only";

      pageSizeSelect.setAttribute("aria-label", "Linhas por página");
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
      table.insertAdjacentElement("afterend", controls);

      const state: TablePaginationState = {
        page: 1,
        pageSize: DEFAULT_PAGE_SIZE,
        rows: [],
        controls,
        summary,
        pageLabel,
        previousButton,
        nextButton,
        pageSizeSelect,
      };

      previousButton.addEventListener("click", () => {
        state.page = Math.max(1, state.page - 1);
        renderTable(table, state);
      });

      nextButton.addEventListener("click", () => {
        const totalPages = Math.max(1, Math.ceil(getRows(table).length / state.pageSize));
        state.page = Math.min(totalPages, state.page + 1);
        renderTable(table, state);
      });

      pageSizeSelect.addEventListener("change", () => {
        const nextPageSize = Number(pageSizeSelect.value);
        state.pageSize = Number.isFinite(nextPageSize) ? nextPageSize : DEFAULT_PAGE_SIZE;
        state.page = 1;
        renderTable(table, state);
      });

      states.set(table, state);
      return state;
    }

    function rowsChanged(previousRows: HTMLTableRowElement[], currentRows: HTMLTableRowElement[]) {
      return (
        previousRows.length !== currentRows.length ||
        currentRows.some((row, index) => row !== previousRows[index])
      );
    }

    function setText(element: HTMLElement, value: string) {
      if (element.textContent !== value) element.textContent = value;
    }

    function renderTable(table: HTMLTableElement, state: TablePaginationState) {
      const rows = getRows(table);
      if (state.rows.length > 0 && rowsChanged(state.rows, rows)) state.page = 1;
      state.rows = rows;

      const totalRows = rows.length;
      const totalPages = Math.max(1, Math.ceil(totalRows / state.pageSize));
      state.page = Math.min(Math.max(1, state.page), totalPages);

      const startIndex = (state.page - 1) * state.pageSize;
      const endIndex = Math.min(startIndex + state.pageSize, totalRows);

      rows.forEach((row, index) => {
        if (index >= startIndex && index < endIndex) {
          row.removeAttribute(PAGINATION_HIDDEN_ATTRIBUTE);
        } else {
          row.setAttribute(PAGINATION_HIDDEN_ATTRIBUTE, "true");
        }
      });

      setText(
        state.summary,
        totalRows === 0 ? "0 registros" : `${startIndex + 1}–${endIndex} de ${totalRows}`,
      );
      setText(state.pageLabel, `Página ${state.page} de ${totalPages}`);

      state.previousButton.disabled = state.page <= 1;
      state.nextButton.disabled = state.page >= totalPages;
      state.controls.hidden = table.hidden || table.closest("[hidden]") != null;
    }

    function removeDisconnectedTables() {
      states.forEach((state, table) => {
        if (table.isConnected) return;
        state.controls.remove();
        states.delete(table);
      });
    }

    function refreshTables() {
      scheduledFrame = null;
      removeDisconnectedTables();

      document.querySelectorAll<HTMLTableElement>("table").forEach((table) => {
        if (table.closest("[data-no-pagination]")) return;

        let state = states.get(table);
        if (state && !state.controls.isConnected) {
          states.delete(table);
          state = undefined;
        }

        renderTable(table, state ?? createControls(table));
      });
    }

    function scheduleRefresh() {
      if (scheduledFrame != null) return;
      scheduledFrame = window.requestAnimationFrame(refreshTables);
    }

    const observer = new MutationObserver((mutations) => {
      const hasRelevantMutation = mutations.some(
        (mutation) => !(mutation.target instanceof Element && mutation.target.closest("[data-table-pagination]")),
      );
      if (hasRelevantMutation) scheduleRefresh();
    });

    refreshTables();
    observer.observe(document.body, { childList: true, subtree: true });

    return () => {
      observer.disconnect();
      if (scheduledFrame != null) window.cancelAnimationFrame(scheduledFrame);

      states.forEach((state, table) => {
        getRows(table).forEach((row) => row.removeAttribute(PAGINATION_HIDDEN_ATTRIBUTE));
        state.controls.remove();
      });
      states.clear();
      style.remove();
    };
  }, []);

  return null;
}
