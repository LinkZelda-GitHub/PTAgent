import { html } from "./view.js?v=20260628-4";

const CONFIG_KEY = "ptagent-amap-config";
let amapPromise = null;
let amapMap = null;
let renderVersion = 0;

export function readMapConfig() {
  try {
    const config = JSON.parse(localStorage.getItem(CONFIG_KEY) || "{}");
    return {
      key: String(config.key || "").trim(),
      securityJsCode: String(config.securityJsCode || "").trim()
    };
  } catch {
    return { key: "", securityJsCode: "" };
  }
}

export function saveMapConfig(config) {
  localStorage.setItem(CONFIG_KEY, JSON.stringify({
    key: String(config.key || "").trim(),
    securityJsCode: String(config.securityJsCode || "").trim()
  }));
  amapPromise = null;
  amapMap = null;
}

export function clearMapConfig() {
  localStorage.removeItem(CONFIG_KEY);
  amapPromise = null;
  amapMap = null;
}

export function fillMapConfigForm() {
  const config = readMapConfig();
  const keyInput = document.querySelector("#amapKey");
  const securityInput = document.querySelector("#amapSecurityCode");
  if (keyInput) {
    keyInput.value = config.key;
  }
  if (securityInput) {
    securityInput.value = config.securityJsCode;
  }
}

export function renderDemandMap(board, demands) {
  const version = ++renderVersion;
  const config = readMapConfig();
  if (!demands.length) {
    board.innerHTML = "";
    return;
  }
  if (!config.key) {
    renderLocalMap(board, demands);
    return;
  }

  board.innerHTML = `<div class="map-loading">正在加载高德地图</div>`;
  loadAmap(config)
    .then((AMap) => {
      if (version !== renderVersion) {
        return;
      }
      renderAmap(AMap, board, demands);
    })
    .catch((error) => {
      console.warn(error);
      if (version !== renderVersion) {
        return;
      }
      renderLocalMap(board, demands, "高德地图加载失败，已切换本地地图");
    });
}

function loadAmap(config) {
  if (amapPromise) {
    return amapPromise;
  }
  window._AMapSecurityConfig = config.securityJsCode
    ? { securityJsCode: config.securityJsCode }
    : undefined;
  amapPromise = ensureLoader().then(() => window.AMapLoader.load({
    key: config.key,
    version: "2.0",
    plugins: ["AMap.Scale", "AMap.ToolBar"]
  }));
  return amapPromise;
}

function ensureLoader() {
  if (window.AMapLoader) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const existing = document.querySelector("script[data-amap-loader]");
    if (existing) {
      existing.addEventListener("load", resolve, { once: true });
      existing.addEventListener("error", reject, { once: true });
      return;
    }
    const script = document.createElement("script");
    script.src = "https://webapi.amap.com/loader.js";
    script.async = true;
    script.dataset.amapLoader = "1";
    script.onload = resolve;
    script.onerror = () => reject(new Error("高德地图加载器不可用"));
    document.head.appendChild(script);
  });
}

function renderAmap(AMap, board, demands) {
  board.innerHTML = "";
  const points = demands
    .filter((demand) => Number.isFinite(Number(demand.longitude)) && Number.isFinite(Number(demand.latitude)))
    .map((demand) => ({
      demand,
      position: [Number(demand.longitude), Number(demand.latitude)]
    }));
  if (!points.length) {
    renderLocalMap(board, demands, "暂无有效坐标，已切换本地地图");
    return;
  }

  if (amapMap) {
    amapMap.destroy();
  }
  amapMap = new AMap.Map(board, {
    zoom: 11,
    center: points[0].position,
    resizeEnable: true,
    viewMode: "2D"
  });
  amapMap.addControl(new AMap.Scale());
  amapMap.addControl(new AMap.ToolBar({ position: "RB" }));

  const infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -28) });
  const markers = points.map(({ demand, position }) => {
    const marker = new AMap.Marker({
      position,
      title: `${demand.grade}${demand.subject}`,
      label: {
        content: demand.subject,
        direction: "top"
      }
    });
    marker.on("click", () => {
      infoWindow.setContent(`
        <div class="map-info">
          <strong>${html(demand.grade)}${html(demand.subject)}</strong>
          <span>${html(demand.region)} · ${html(demand.salaryRange || "面议")}</span>
          <span>${html(demand.address || "")}</span>
        </div>
      `);
      infoWindow.open(amapMap, position);
    });
    return marker;
  });
  amapMap.add(markers);
  amapMap.setFitView(markers, false, [40, 40, 40, 40]);
}

function renderLocalMap(board, demands, note = "") {
  const longitudes = demands.map((demand) => demand.longitude);
  const latitudes = demands.map((demand) => demand.latitude);
  const minLon = Math.min(...longitudes);
  const maxLon = Math.max(...longitudes);
  const minLat = Math.min(...latitudes);
  const maxLat = Math.max(...latitudes);
  const markerHtml = demands.map((demand, index) => {
    const x = 12 + scale(demand.longitude, minLon, maxLon) * 76;
    const y = 88 - scale(demand.latitude, minLat, maxLat) * 76;
    return `<button class="marker ${index < 2 ? "is-hot" : ""}" style="left:${x}%;top:${y}%"
      title="${html(demand.grade)}${html(demand.subject)} ${html(demand.region)}">
      <span>${html(demand.subject.slice(0, 1))}</span>
    </button>`;
  }).join("");
  board.innerHTML = `${note ? `<div class="map-note">${html(note)}</div>` : ""}${markerHtml}`;
}

function scale(value, min, max) {
  if (max === min) return 0.5;
  return (value - min) / (max - min);
}
