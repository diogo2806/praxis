import { useEffect, useRef, useState } from "react";

export type DraftPersistenceStatus = "idle" | "saving" | "saved" | "error";

export interface PersistedDraft<T> {
  schemaVersion: 1;
  savedAt: string;
  data: T;
}

interface PersistentDraftOptions<T> {
  key: string;
  value: T;
  enabled?: boolean;
  debounceMs?: number;
}

export function readPersistentDraft<T>(key: string): PersistedDraft<T> | null {
  if (typeof window === "undefined" || !key) return null;
  try {
    const raw = window.localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<PersistedDraft<T>>;
    if (parsed.schemaVersion !== 1 || !parsed.savedAt || parsed.data === undefined) return null;
    return parsed as PersistedDraft<T>;
  } catch {
    return null;
  }
}

export function clearPersistentDraft(key: string) {
  if (typeof window === "undefined" || !key) return;
  try {
    window.localStorage.removeItem(key);
  } catch {
    // O fluxo continua funcional mesmo quando o navegador bloqueia armazenamento local.
  }
}

export function usePersistentDraft<T>({
  key,
  value,
  enabled = true,
  debounceMs = 700,
}: PersistentDraftOptions<T>) {
  const [status, setStatus] = useState<DraftPersistenceStatus>("idle");
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const latestValueRef = useRef(value);
  const initializedKeyRef = useRef<string | null>(null);

  useEffect(() => {
    latestValueRef.current = value;
  }, [value]);

  useEffect(() => {
    initializedKeyRef.current = key;
    const stored = enabled ? readPersistentDraft<T>(key) : null;
    setSavedAt(stored?.savedAt ?? null);
    setStatus(stored ? "saved" : "idle");
  }, [enabled, key]);

  useEffect(() => {
    if (!enabled || !key || initializedKeyRef.current !== key) return;
    setStatus("saving");
    const timer = window.setTimeout(() => {
      try {
        const timestamp = new Date().toISOString();
        const envelope: PersistedDraft<T> = {
          schemaVersion: 1,
          savedAt: timestamp,
          data: latestValueRef.current,
        };
        window.localStorage.setItem(key, JSON.stringify(envelope));
        setSavedAt(timestamp);
        setStatus("saved");
      } catch {
        setStatus("error");
      }
    }, debounceMs);
    return () => window.clearTimeout(timer);
  }, [debounceMs, enabled, key, value]);

  useEffect(() => {
    if (!enabled || !key) return;
    const flush = () => {
      try {
        const timestamp = new Date().toISOString();
        window.localStorage.setItem(
          key,
          JSON.stringify({ schemaVersion: 1, savedAt: timestamp, data: latestValueRef.current }),
        );
      } catch {
        // beforeunload não deve bloquear a saída quando o armazenamento falhar.
      }
    };
    window.addEventListener("pagehide", flush);
    return () => window.removeEventListener("pagehide", flush);
  }, [enabled, key]);

  return {
    status,
    savedAt,
    clear: () => {
      clearPersistentDraft(key);
      setSavedAt(null);
      setStatus("idle");
    },
  };
}
