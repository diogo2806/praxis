export type ViewMode = "commercial" | "technical";
export type GupyConnectionState = "connected" | "connecting" | "disconnected" | "error";

export const gupyConnectionLabels: Record<GupyConnectionState, string> = {
  connected: "Gupy conectada",
  connecting: "Conectando à Gupy",
  disconnected: "Gupy desconectada",
  error: "Erro na integração Gupy",
};

export function getViewMode(): ViewMode {
  if (typeof window === "undefined") return "commercial";
  return new URLSearchParams(window.location.search).get("mode") === "technical"
    ? "technical"
    : "commercial";
}

export function getGupyConnectionState(pathname = ""): GupyConnectionState {
  if (typeof window !== "undefined") {
    const query = new URLSearchParams(window.location.search);
    const forced = query.get("gupy");
    if (
      forced === "connected" ||
      forced === "connecting" ||
      forced === "disconnected" ||
      forced === "error"
    ) {
      return forced;
    }
  }
  return pathname === "/nova/gupy" || pathname === "/nova/publicacao" ? "connecting" : "connected";
}

export function useViewMode() {
  const [mode, setMode] = useState<ViewMode>("commercial");
  useEffect(() => {
    setMode(getViewMode());
  }, []);
  return mode;
}

export function useGupyConnectionState(pathname = "") {
  const [state, setState] = useState<GupyConnectionState>(
    pathname === "/nova/gupy" || pathname === "/nova/publicacao" ? "connecting" : "connected",
  );
  useEffect(() => {
    setState(getGupyConnectionState(pathname));
  }, [pathname]);
  return state;
}
import { useEffect, useState } from "react";
