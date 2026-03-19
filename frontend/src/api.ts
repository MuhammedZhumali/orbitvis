const API_BASE = "/api";

export interface LocationDto {
  id: number;
  name: string;
  longitude: number;
  latitude?: number;
  altitude: number;
  polygon?: { latDeg: number; lonDeg: number }[];
}

export interface SatelliteDto {
  satelliteId: string;
  line1: string;
  line2: string;
  maxNadirOffAngle: number;
  minSunAngle: number;
  maxRollAngle: number;
}

export interface CartesianPoint {
  time: number;
  x: number;
  y: number;
  z: number;
}

export interface PassPrediction {
  aos: string; // ISO instant
  tca: string;
  los: string;
  maxElevationDeg: number;
  durationSec: number;
  azimuthAtAosDeg: number;
  azimuthAtLosDeg: number;
}

export interface PassQueryResponse {
  passes: PassPrediction[];
}

/** Realtime SSE state (ECEF position + optional site-relative view). */
export interface RealtimeStateDto {
  t: string; // ISO instant
  latDeg: number;
  lonDeg: number;
  altMeters: number;
  ecefX: number;
  ecefY: number;
  ecefZ: number;
  azimuthDeg: number;
  elevtionDeg: number;
  rangeKm: number;
  inView: boolean;
}

export async function getLocations(): Promise<LocationDto[]> {
  const r = await fetch(`${API_BASE}/location/getAll`);
  if (!r.ok) throw new Error("Failed to load locations");
  return r.json();
}

export async function getSatellites(): Promise<SatelliteDto[]> {
  const r = await fetch(`${API_BASE}/satellite/getAll`);
  if (!r.ok) throw new Error("Failed to load satellites");
  return r.json();
}

const ORBIT_BODY = (request: {
  line1: string;
  line2: string;
  startEpoch?: number;
  endEpoch?: number;
  stepSeconds?: number;
}) =>
  JSON.stringify({
    line1: request.line1,
    line2: request.line2,
    startEpoch: request.startEpoch ?? -1,
    endEpoch: request.endEpoch ?? -1,
    stepSeconds: request.stepSeconds ?? 60,
  });

export async function propagateOrbit(request: {
  line1: string;
  line2: string;
  startEpoch?: number;
  endEpoch?: number;
  stepSeconds?: number;
}): Promise<CartesianPoint[]> {
  const r = await fetch(`${API_BASE}/orbit/propagate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: ORBIT_BODY(request),
  });
  if (!r.ok) throw new Error("Orbit propagation failed");
  return r.json();
}

/** Parse buffered SSE text into complete events; returns leftover partial text. */
function consumeSseEvents(
  buffer: string,
  onEvent: (eventName: string, data: string) => void
): string {
  const sep = /\r?\n\r?\n/;
  let rest = buffer;
  for (;;) {
    const m = sep.exec(rest);
    if (!m || m.index === undefined) break;
    const block = rest.slice(0, m.index);
    rest = rest.slice(m.index + m[0].length);
    let eventName = "message";
    const dataLines: string[] = [];
    for (const line of block.split(/\r?\n/)) {
      if (line.startsWith("event:")) eventName = line.slice(6).trim();
      else if (line.startsWith("data:")) dataLines.push(line.slice(5).trimStart());
    }
    const data = dataLines.join("\n");
    onEvent(eventName, data);
  }
  return rest;
}

/**
 * Stream orbit points via POST + SSE (event `point` = JSON CartesianPoint, then `done`).
 */
export async function propagateOrbitStream(
  request: {
    line1: string;
    line2: string;
    startEpoch?: number;
    endEpoch?: number;
    stepSeconds?: number;
  },
  opts: {
    onPoint: (p: CartesianPoint) => void;
    signal?: AbortSignal;
    onError?: (e: unknown) => void;
  }
): Promise<void> {
  const r = await fetch(`${API_BASE}/orbit/propagate/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    body: ORBIT_BODY(request),
    signal: opts.signal,
  });
  if (!r.ok) {
    const err = new Error(`Orbit stream failed: ${r.status}`);
    opts.onError?.(err);
    throw err;
  }
  const reader = r.body?.getReader();
  if (!reader) {
    const err = new Error("No response body");
    opts.onError?.(err);
    throw err;
  }
  const decoder = new TextDecoder();
  let buf = "";
  try {
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += decoder.decode(value, { stream: true });
      buf = consumeSseEvents(buf, (eventName, data) => {
        if (eventName === "point") {
          try {
            opts.onPoint(JSON.parse(data) as CartesianPoint);
          } catch (e) {
            opts.onError?.(e);
          }
        }
      });
    }
  } catch (e) {
    if (opts.signal?.aborted) return;
    opts.onError?.(e);
    throw e;
  } finally {
    reader.releaseLock();
  }
}

export async function queryPasses(params: {
  satelliteId: string;
  site: { lat: number; lon: number; altMeters: number };
  startTime: string;
  endTime: string;
  minElevationDeg: number;
}): Promise<PassPrediction[]> {
  const r = await fetch(`${API_BASE}/passes/query`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(params),
  });
  if (!r.ok) throw new Error("Pass query failed");
  const data: PassQueryResponse = await r.json();
  return data.passes ?? [];
}

export function openRealtimeStream(params: {
  satelliteId: string;
  siteLat?: number;
  siteLon?: number;
  siteAlt?: number;
  rateHz?: number;
  onState: (s: RealtimeStateDto) => void;
  onError?: (e: unknown) => void;
}): () => void {
  const url = new URL("/api/realtime/stream", window.location.origin);
  url.searchParams.set("satelliteId", params.satelliteId);
  if (params.siteLat != null && params.siteLon != null) {
    url.searchParams.set("siteLat", String(params.siteLat));
    url.searchParams.set("siteLon", String(params.siteLon));
    url.searchParams.set("siteAlt", String(params.siteAlt ?? 0));
  }
  url.searchParams.set("rateHz", String(params.rateHz ?? 1));

  const es = new EventSource(url.toString());

  es.addEventListener("state", (ev: MessageEvent) => {
    params.onState(JSON.parse(ev.data));
  });

  es.onerror = (e) => {
    params.onError?.(e);
    es.close();
  };

  return () => es.close();
}