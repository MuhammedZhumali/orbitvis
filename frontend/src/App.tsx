import { useState, useEffect, useRef } from "react";
import Globe from "./Globe";
import {
  getLocations,
  getSatellites,
  propagateOrbit,
  queryPasses,
  openRealtimeStream,
  type LocationDto,
  type SatelliteDto,
  type CartesianPoint,
  type PassPrediction,
  type RealtimeStateDto,
} from "./api";

export default function App() {
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [satellites, setSatellites] = useState<SatelliteDto[]>([]);
  const [selectedLocationId, setSelectedLocationId] = useState<string>("");
  const [selectedSatelliteId, setSelectedSatelliteId] = useState<string>("");
  const [orbitPoints, setOrbitPoints] = useState<CartesianPoint[] | null>(null);
  const [passes, setPasses] = useState<PassPrediction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [live, setLive] = useState(false);
  const [lastRealtime, setLastRealtime] = useState<RealtimeStateDto | null>(null);
  const [followCamera, setFollowCamera] = useState(false);
  const [trailSeconds, setTrailSeconds] = useState(60);
  const [trailPoints, setTrailPoints] = useState<CartesianPoint[]>([]);
  const liveCloseRef = useRef<(() => void) | null>(null);
  const trailSecondsRef = useRef(trailSeconds);
  trailSecondsRef.current = trailSeconds;

  const selectedSatellite = satellites.find((s) => s.satelliteId === selectedSatelliteId);
  const selectedLocation = locations.find((l) => String(l.id) === selectedLocationId);

  useEffect(() => {
    getLocations()
      .then(setLocations)
      .catch((e) => setError(e.message));
    getSatellites()
      .then(setSatellites)
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!live || !selectedSatelliteId) return;
    const close = openRealtimeStream({
      satelliteId: selectedSatelliteId,
      siteLat: selectedLocation?.latitude,
      siteLon: selectedLocation?.longitude,
      siteAlt: selectedLocation?.altitude ?? 0,
      rateHz: 1,
      onState(s: RealtimeStateDto) {
        setLastRealtime(s);
        setTrailPoints((prev) => {
          const pt: CartesianPoint = {
            time: new Date(s.t).getTime() / 1000,
            x: s.ecefX,
            y: s.ecefY,
            z: s.ecefZ,
          };
          const next = [...prev, pt];
          const cutoff = (new Date(s.t).getTime() / 1000) - trailSecondsRef.current;
          return next.filter((p) => p.time >= cutoff);
        });
      },
      onError(e) {
        setError(e instanceof Error ? e.message : "Realtime stream error");
        setLive(false);
      },
    });
    liveCloseRef.current = close;
    return () => {
      close();
      liveCloseRef.current = null;
    };
  }, [live, selectedSatelliteId, selectedLocation?.latitude, selectedLocation?.longitude, selectedLocation?.altitude]);

  const handleToggleLive = () => {
    if (live) {
      liveCloseRef.current?.();
      setLive(false);
      setLastRealtime(null);
      setTrailPoints([]);
    } else {
      if (!selectedSatelliteId) {
        setError("Choose a satellite for Live");
        return;
      }
      setError(null);
      setLive(true);
    }
  };

  const handlePropagate = async () => {
    if (!selectedSatellite?.line1 || !selectedSatellite?.line2) {
      setError("Choose a satellite with TLE data");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const points = await propagateOrbit({
        line1: selectedSatellite.line1,
        line2: selectedSatellite.line2,
        stepSeconds: 60,
      });
      setOrbitPoints(points);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Propagation failed");
      setOrbitPoints(null);
    } finally {
      setLoading(false);
    }
  };

  const handleLoadPasses = async () => {
    if (!selectedSatellite || !selectedLocation) {
      setError("Choose both location and satellite");
      return;
    }
    const lat = selectedLocation.latitude ?? 0;
    const lon = selectedLocation.longitude;
    const alt = selectedLocation.altitude ?? 0;
    const now = new Date();
    const startTime = now.toISOString();
    const endTime = new Date(now.getTime() + 24 * 3600 * 1000).toISOString();

    setError(null);
    setLoading(true);
    try {
      const data = await queryPasses({
        satelliteId: selectedSatellite.satelliteId,
        site: { lat, lon, altMeters: alt },
        startTime,
        endTime,
        minElevationDeg: 10,
      });
      setPasses(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Pass query failed");
      setPasses([]);
    } finally {
      setLoading(false);
    }
  };

  const handleShowPass = async (pass: PassPrediction) => {
    if (!selectedSatellite) return;
    try {
      const startEpoch = Math.floor(new Date(pass.aos).getTime() / 1000);
      const endEpoch = Math.floor(new Date(pass.los).getTime() / 1000);
      const pts = await propagateOrbit({
        line1: selectedSatellite.line1,
        line2: selectedSatellite.line2,
        startEpoch,
        endEpoch,
        stepSeconds: 15,
      });
      setOrbitPoints(pts);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Propagation failed");
    }
  };

  return (
    <div style={{ display: "flex", width: "100%", height: "100%" }}>
      <aside
        style={{
          width: 320,
          minWidth: 320,
          background: "var(--surface)",
          borderRight: "1px solid var(--border)",
          padding: "1.25rem",
          display: "flex",
          flexDirection: "column",
          gap: "1rem",
          overflowY: "auto",
        }}
      >
        <h1 style={{ margin: 0, fontSize: "1.35rem", fontWeight: 700, letterSpacing: "-0.02em" }}>
          OrbitVis
        </h1>
        <p style={{ margin: 0, color: "var(--muted)", fontSize: "0.9rem" }}>
          Pick a location and satellite, then propagate orbit.
        </p>

        <label style={{ display: "flex", flexDirection: "column", gap: "0.35rem", fontSize: "0.85rem" }}>
          Location
          <select
            value={selectedLocationId}
            onChange={(e) => setSelectedLocationId(e.target.value)}
            style={{
              padding: "0.5rem 0.6rem",
              background: "var(--bg)",
              border: "1px solid var(--border)",
              borderRadius: 6,
              color: "var(--text)",
            }}
          >
            <option value="">— Select —</option>
            {locations.map((loc) => (
              <option key={loc.id} value={String(loc.id)}>
                {loc.name}
              </option>
            ))}
          </select>
        </label>

        <label style={{ display: "flex", flexDirection: "column", gap: "0.35rem", fontSize: "0.85rem" }}>
          Satellite
          <select
            value={selectedSatelliteId}
            onChange={(e) => setSelectedSatelliteId(e.target.value)}
            style={{
              padding: "0.5rem 0.6rem",
              background: "var(--bg)",
              border: "1px solid var(--border)",
              borderRadius: 6,
              color: "var(--text)",
            }}
          >
            <option value="">— Select —</option>
            {satellites.map((s) => (
              <option key={s.satelliteId} value={s.satelliteId}>
                {s.satelliteId}
              </option>
            ))}
          </select>
        </label>

        <div style={{ display: "flex", gap: "0.5rem" }}>
          <button
            onClick={handlePropagate}
            disabled={loading || !selectedSatelliteId || live}
            style={{
              flex: 1,
              padding: "0.6rem 1rem",
              background: loading ? "var(--muted)" : "var(--accent)",
              border: "none",
              borderRadius: 6,
              color: "var(--bg)",
              fontWeight: 600,
              fontSize: "0.9rem",
            }}
          >
            {loading ? "Loading…" : "Show orbit"}
          </button>
          <button
            onClick={handleLoadPasses}
            disabled={loading || !selectedSatelliteId || !selectedLocationId || live}
            style={{
              padding: "0.6rem 1rem",
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: 6,
              color: "var(--text)",
              fontSize: "0.85rem",
            }}
          >
            Next passes
          </button>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: "0.5rem", flexWrap: "wrap" }}>
          <button
            onClick={handleToggleLive}
            disabled={!selectedSatelliteId}
            style={{
              padding: "0.6rem 1rem",
              background: live ? "#22c55e" : "var(--surface)",
              border: live ? "none" : "1px solid var(--border)",
              borderRadius: 6,
              color: live ? "#fff" : "var(--text)",
              fontWeight: 600,
              fontSize: "0.9rem",
            }}
          >
            {live ? "Live ON" : "Live"}
          </button>
          {live && (
            <span
              style={{
                padding: "0.25rem 0.5rem",
                borderRadius: 4,
                fontSize: "0.75rem",
                fontWeight: 700,
                background: lastRealtime?.inView ? "rgba(34, 197, 94, 0.3)" : "var(--surface)",
                color: lastRealtime?.inView ? "#4ade80" : "var(--muted)",
                border: lastRealtime?.inView ? "1px solid #22c55e" : "1px solid var(--border)",
              }}
            >
              {lastRealtime?.inView ? "IN VIEW" : "—"}
            </span>
          )}
        </div>

        {live && (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem", fontSize: "0.85rem" }}>
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={followCamera}
                onChange={(e) => setFollowCamera(e.target.checked)}
              />
              Follow camera
            </label>
            <label style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              Trail (sec):
              <input
                type="number"
                min={10}
                max={300}
                value={trailSeconds}
                onChange={(e) => setTrailSeconds(Math.max(10, Math.min(300, Number(e.target.value) || 60)))}
                style={{
                  width: 56,
                  padding: "0.25rem 0.4rem",
                  background: "var(--bg)",
                  border: "1px solid var(--border)",
                  borderRadius: 4,
                  color: "var(--text)",
                }}
              />
            </label>
          </div>
        )}

        {error && (
          <div style={{ padding: "0.5rem", background: "rgba(248,81,73,0.15)", borderRadius: 6, fontSize: "0.85rem", color: "#f85149" }}>
            {error}
          </div>
        )}

        {passes.length > 0 && (
          <div style={{ marginTop: "0.5rem", fontSize: "0.8rem" }}>
            <div style={{ marginBottom: "0.3rem", fontWeight: 600 }}>Next passes</div>
            <div style={{ display: "flex", flexDirection: "column", gap: "0.35rem", maxHeight: 220, overflowY: "auto" }}>
              {passes.map((p, i) => (
                <div
                  key={`${p.aos}-${i}`}
                  style={{
                    padding: "0.4rem 0.5rem",
                    borderRadius: 6,
                    border: "1px solid var(--border)",
                    background: "rgba(15,23,42,0.7)",
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.15rem",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem" }}>
                    <span>AOS: {new Date(p.aos).toLocaleTimeString()}</span>
                    <span>LOS: {new Date(p.los).toLocaleTimeString()}</span>
                  </div>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", color: "var(--muted)" }}>
                    <span>Max el: {p.maxElevationDeg.toFixed(1)}°</span>
                    <span>Dur: {Math.round(p.durationSec)} s</span>
                  </div>
                  <button
                    onClick={() => handleShowPass(p)}
                    style={{
                      marginTop: "0.25rem",
                      alignSelf: "flex-start",
                      padding: "0.25rem 0.6rem",
                      fontSize: "0.75rem",
                      borderRadius: 999,
                      border: "none",
                      background: "var(--accent)",
                      color: "var(--bg)",
                    }}
                  >
                    Show this pass
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}
      </aside>

      <main style={{ flex: 1, position: "relative", minWidth: 0 }}>
        <Globe
          orbitPoints={live ? null : orbitPoints}
          realtimePosition={lastRealtime ? { time: new Date(lastRealtime.t).getTime() / 1000, x: lastRealtime.ecefX, y: lastRealtime.ecefY, z: lastRealtime.ecefZ } : null}
          trailPoints={trailPoints}
          followCamera={followCamera}
          inView={lastRealtime?.inView ?? false}
        />
      </main>
    </div>
  );
}
