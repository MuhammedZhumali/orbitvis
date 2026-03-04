const API_BASE = "/api";

export interface LocationDto {
  id: number;
  name: string;
  longitude: number;
  latitude?: number;
  lattitude?: number; // backend typo
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
    body: JSON.stringify({
      line1: request.line1,
      line2: request.line2,
      startEpoch: request.startEpoch ?? -1,
      endEpoch: request.endEpoch ?? -1,
      stepSeconds: request.stepSeconds ?? 60,
    }),
  });
  if (!r.ok) throw new Error("Orbit propagation failed");
  return r.json();
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
