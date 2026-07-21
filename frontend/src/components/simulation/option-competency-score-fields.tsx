import { useEffect, useRef, useState } from "react";

type SaveStatus = "idle" | "saving" | "saved" | "error";

type OptionCompetencyScoreFieldsProps = {
  initialLevels: Record<string, number>;
  onSave: (levels: Record<string, number>) => Promise<unknown>;
  onValidationError?: (message: string | null) => void;
};

function sameLevels(left: Record<string, number>, right: Record<string, number>) {
  const leftEntries = Object.entries(left);
  const rightEntries = Object.entries(right);

  return (
    leftEntries.length === rightEntries.length &&
    leftEntries.every(([name, value]) => right[name] === value)
  );
}

export function OptionCompetencyScoreFields({
  initialLevels,
  onSave,
  onValidationError,
}: OptionCompetencyScoreFieldsProps) {
  const [levels, setLevels] = useState<Record<string, number>>(() => ({ ...initialLevels }));
  const [saveStatus, setSaveStatus] = useState<SaveStatus>("idle");
  const levelsRef = useRef<Record<string, number>>({ ...initialLevels });
  const lastSavedRef = useRef<Record<string, number>>({ ...initialLevels });
  const lastQueuedRef = useRef<Record<string, number> | null>(null);
  const dirtyRef = useRef(false);
  const mountedRef = useRef(true);
  const saveTimerRef = useRef<number | null>(null);
  const saveChainRef = useRef<Promise<void>>(Promise.resolve());
  const onSaveRef = useRef(onSave);
  const onValidationErrorRef = useRef(onValidationError);

  useEffect(() => {
    onSaveRef.current = onSave;
  }, [onSave]);

  useEffect(() => {
    onValidationErrorRef.current = onValidationError;
  }, [onValidationError]);

  useEffect(() => {
    if (dirtyRef.current) return;

    const nextLevels = { ...initialLevels };
    levelsRef.current = nextLevels;
    lastSavedRef.current = nextLevels;
    lastQueuedRef.current = null;
    setLevels(nextLevels);
  }, [initialLevels]);

  function queueSave(snapshot: Record<string, number>) {
    if (sameLevels(snapshot, lastSavedRef.current)) {
      dirtyRef.current = false;
      if (mountedRef.current) setSaveStatus("saved");
      return;
    }
    if (lastQueuedRef.current && sameLevels(snapshot, lastQueuedRef.current)) return;

    const queuedSnapshot = { ...snapshot };
    lastQueuedRef.current = queuedSnapshot;
    if (mountedRef.current) setSaveStatus("saving");

    saveChainRef.current = saveChainRef.current
      .catch(() => undefined)
      .then(async () => {
        await onSaveRef.current(queuedSnapshot);
        lastSavedRef.current = queuedSnapshot;
        if (sameLevels(levelsRef.current, queuedSnapshot)) {
          dirtyRef.current = false;
          if (mountedRef.current) setSaveStatus("saved");
        }
      })
      .catch(() => {
        lastQueuedRef.current = null;
        if (mountedRef.current) setSaveStatus("error");
      });
  }

  function flushPendingSave() {
    if (saveTimerRef.current !== null) {
      window.clearTimeout(saveTimerRef.current);
      saveTimerRef.current = null;
    }
    if (dirtyRef.current) queueSave(levelsRef.current);
  }

  function scheduleSave(nextLevels: Record<string, number>) {
    if (saveTimerRef.current !== null) window.clearTimeout(saveTimerRef.current);
    saveTimerRef.current = window.setTimeout(() => {
      saveTimerRef.current = null;
      queueSave(nextLevels);
    }, 350);
  }

  useEffect(
    () => () => {
      mountedRef.current = false;
      if (saveTimerRef.current !== null) window.clearTimeout(saveTimerRef.current);
      if (dirtyRef.current) queueSave(levelsRef.current);
    },
    [],
  );

  return (
    <>
      {Object.entries(levels).map(([name, value]) => (
        <label
          key={name}
          className="inline-flex items-center gap-1 rounded border border-border px-2 py-1"
        >
          {name}
          <input
            className="w-12 rounded border border-border bg-card px-1 py-0.5"
            type="number"
            min={0}
            max={100}
            step={1}
            value={value}
            onChange={(event) => {
              const nextValue = Number(event.target.value);
              if (!Number.isInteger(nextValue) || nextValue < 0 || nextValue > 100) {
                onValidationErrorRef.current?.(
                  `Pontuação inválida para ${name}. Use um inteiro de 0 a 100.`,
                );
                return;
              }

              onValidationErrorRef.current?.(null);
              const nextLevels = { ...levelsRef.current, [name]: nextValue };
              levelsRef.current = nextLevels;
              dirtyRef.current = true;
              setLevels(nextLevels);
              setSaveStatus("saving");
              scheduleSave(nextLevels);
            }}
            onBlur={flushPendingSave}
          />
        </label>
      ))}
      <span
        className={saveStatus === "error" ? "text-danger" : "text-muted-foreground"}
        role="status"
        aria-live="polite"
      >
        {saveStatus === "saving" && "Salvando pontuações..."}
        {saveStatus === "saved" && "Pontuações salvas."}
        {saveStatus === "error" && "Falha ao salvar pontuações."}
      </span>
    </>
  );
}
