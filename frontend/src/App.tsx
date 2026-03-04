import { useState, useEffect } from "react";
import Globe from "./Globe";
import { getLocations, getSatellites, propagateOrbit, type LocationDto, type SatelliteDto, type CartesianPoint } from "./api";

export default function App() {
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [satellites, setSatellites] = useState<SatelliteDto[]>([]);
  const [selectedLocationId, setSelectedLocationId] = useState<string>("");
  const [selectedSatelliteId, setSelectedSatelliteId] = useState<string>("");
  const [orbitPoints, setOrbitPoints] = useState<CartesianPoint[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getLocations()
      .then(setLocations)
      .catch((e) => setError(e.message));
    getSatellites()
      .then(setSatellites)
      .catch((e) => setError(e.message));
  }, []);

  const selectedSatellite = satellites.find((s) => s.satelliteId === selectedSatelliteId);

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

        <button
          onClick={handlePropagate}
          disabled={loading || !selectedSatelliteId}
          style={{
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

        {error && (
          <div style={{ padding: "0.5rem", background: "rgba(248,81,73,0.15)", borderRadius: 6, fontSize: "0.85rem", color: "#f85149" }}>
            {error}
          </div>
        )}

        {orbitPoints && (
          <div style={{ fontSize: "0.8rem", color: "var(--muted)" }}>
            Orbit: {orbitPoints.length} points
          </div>
        )}
      </aside>

      <main style={{ flex: 1, position: "relative", minWidth: 0 }}>
        <Globe orbitPoints={orbitPoints} />
      </main>
    </div>
  );
}
