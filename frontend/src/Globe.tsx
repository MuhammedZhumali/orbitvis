import { useEffect, useRef } from "react";
import * as Cesium from "cesium";
import type { CartesianPoint } from "./api";

const REALTIME_ENTITY_ID = "realtime-satellite";
const TRAIL_ENTITY_ID = "realtime-trail";
const ORBIT_ENTITY_ID = "orbit-polyline";

interface GlobeProps {
  orbitPoints: CartesianPoint[] | null;
  realtimePosition?: CartesianPoint | null;
  trailPoints?: CartesianPoint[];
  followCamera?: boolean;
  inView?: boolean;
  onViewerReady?: (viewer: Cesium.Viewer) => void;
}

export default function Globe({
  orbitPoints,
  realtimePosition = null,
  trailPoints = [],
  followCamera = false,
  inView = false,
  onViewerReady,
}: GlobeProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<Cesium.Viewer | null>(null);
  const realtimeEntityRef = useRef<Cesium.Entity | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const viewer = new Cesium.Viewer(containerRef.current, {
      baseLayerPicker: false,
      geocoder: false,
      homeButton: true,
      sceneModePicker: true,
      timeline: false,
      navigationHelpButton: false,
      fullscreenButton: true,
      useDefaultRenderLoop: true,
      requestRenderMode: false,
    });

    viewer.scene.globe.depthTestAgainstTerrain = true;
    viewerRef.current = viewer;
    onViewerReady?.(viewer);

    return () => {
      viewer.destroy();
      viewerRef.current = null;
      realtimeEntityRef.current = null;
    };
  }, [onViewerReady]);

  // Static orbit polyline (when not in live mode), without auto-zoom
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer) return;

    const existing = viewer.entities.getById(ORBIT_ENTITY_ID);
    if (existing) viewer.entities.remove(existing);

    if (orbitPoints && orbitPoints.length >= 2) {
      const positions = orbitPoints.map((p) => new Cesium.Cartesian3(p.x, p.y, p.z));
      viewer.entities.add({
        id: ORBIT_ENTITY_ID,
        polyline: {
          positions,
          width: 2,
          material: Cesium.Color.CYAN.withAlpha(0.9),
          arcType: Cesium.ArcType.NONE,
        },
      });
    }
  }, [orbitPoints]);

  // Realtime entity (point) + trail polyline
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer) return;

    const existingEntity = viewer.entities.getById(REALTIME_ENTITY_ID);
    if (existingEntity) viewer.entities.remove(existingEntity);
    const existingTrail = viewer.entities.getById(TRAIL_ENTITY_ID);
    if (existingTrail) viewer.entities.remove(existingTrail);

    if (realtimePosition) {
      const pos = new Cesium.Cartesian3(realtimePosition.x, realtimePosition.y, realtimePosition.z);
      const entity = viewer.entities.add({
        id: REALTIME_ENTITY_ID,
        position: pos,
        point: {
          pixelSize: 12,
          color: inView ? Cesium.Color.LIME : Cesium.Color.ORANGE,
          outlineColor: Cesium.Color.WHITE,
          outlineWidth: 2,
          heightReference: Cesium.HeightReference.NONE,
        },
      });
      realtimeEntityRef.current = entity;

      if (trailPoints.length >= 2) {
        const trailPositions = trailPoints.map((p) => new Cesium.Cartesian3(p.x, p.y, p.z));
        viewer.entities.add({
          id: TRAIL_ENTITY_ID,
          polyline: {
            positions: trailPositions,
            width: 3,
            material: Cesium.Color.YELLOW.withAlpha(0.7),
            arcType: Cesium.ArcType.NONE,
          },
        });
      }
    } else {
      realtimeEntityRef.current = null;
    }
  }, [realtimePosition, trailPoints, inView]);

  // Follow camera: camera follows satellite from offset along radial (above the satellite)
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer || !followCamera || !realtimePosition) return;

    const target = new Cesium.Cartesian3(realtimePosition.x, realtimePosition.y, realtimePosition.z);
    const distance = 600_000; // 600 km above satellite
    const radial = Cesium.Cartesian3.normalize(target, new Cesium.Cartesian3());
    const cameraPos = Cesium.Cartesian3.add(
      target,
      Cesium.Cartesian3.multiplyByScalar(radial, distance, new Cesium.Cartesian3()),
      new Cesium.Cartesian3()
    );
    const direction = Cesium.Cartesian3.normalize(
      Cesium.Cartesian3.subtract(target, cameraPos, new Cesium.Cartesian3()),
      new Cesium.Cartesian3()
    );

    viewer.camera.setView({
      destination: cameraPos,
      orientation: {
        direction,
        up: Cesium.Ellipsoid.WGS84.geodeticSurfaceNormal(cameraPos, new Cesium.Cartesian3()),
      },
    });
  }, [realtimePosition, followCamera]);

  return <div ref={containerRef} className="cesium-container" style={{ width: "100%", height: "100%" }} />;
}
