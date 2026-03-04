import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "cesium/Build/Cesium/Widgets/widgets.css";
import "./index.css";

declare const CESIUM_BASE_URL: string;
(window as unknown as { CESIUM_BASE_URL?: string }).CESIUM_BASE_URL = CESIUM_BASE_URL;

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
