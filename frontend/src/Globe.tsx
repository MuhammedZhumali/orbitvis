import { useEffect, useRef } from "react";
import * as Cesium from "cesium";
import type { CartesianPoint } from "./api";

interface GlobeProps {
  orbitPoints: CartesianPoint[] | null;
  onViewerReady?: (viewer: Cesium.Viewer) => void;
}

export default function Globe({ orbitPoints, onViewerReady }: GlobeProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<Cesium.Viewer | null>(null);

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
    };
  }, [onViewerReady]);

  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer || !orbitPoints || orbitPoints.length < 2) return;

    const positions: Cesium.Cartesian3[] = [];
    for (const p of orbitPoints) {
      positions.push(new Cesium.Cartesian3(p.x, p.y, p.z));
    }

    viewer.entities.removeAll();
    viewer.entities.add({
      polyline: {
        positions,
        width: 2,
        material: Cesium.Color.CYAN.withAlpha(0.9),
        arcType: Cesium.ArcType.NONE,
      },
    });

    viewer.zoomTo(viewer.entities);
  }, [orbitPoints]);

  return <div ref={containerRef} className="cesium-container" style={{ width: "100%", height: "100%" }} />;
}
