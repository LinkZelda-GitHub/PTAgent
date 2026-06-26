import { api, getAuthToken, queryString, setAuthToken } from "./api.js?v=20260627-4";
import { renderDemoAccounts } from "./render.js?v=20260627-4";
import { defaultTab, filterIds, state } from "./state.js?v=20260627-4";
import { $, fillMultiSelect, fillSelect } from "./view.js?v=20260627-4";

const FILTERS_KEY = "ptagent-demand-filters";

export async function loadBootstrap() {
  state.bootstrap = await api("/bootstrap");
  fillSelect($("#filterSubject"), state.bootstrap.subjects, "全部科目");
  fillSelect($("#filterGrade"), state.bootstrap.grades, "全部年级");
  fillSelect($("#filterRegion"), state.bootstrap.regions, "全部区域");
  fillSelect($("#filterTag"), state.bootstrap.tags, "全部标签");
  fillSelect($("#demandSubject"), state.bootstrap.subjects, "选择科目");
  fillSelect($("#demandGrade"), state.bootstrap.grades, "选择年级");
  fillSelect($("#demandRegion"), state.bootstrap.regions, "选择区域");
  fillMultiSelect($("#demandTags"), state.bootstrap.tags);
  restoreFilters();
  renderDemoAccounts();
}

export async function login(username, password) {
  const data = await api("/auth/login", { method: "POST", body: { username, password } });
  setAuthToken(data.token);
  state.user = data.user;
  state.profile = data.profile || null;
  state.currentTab = defaultTab();
  await refreshAll();
}

export async function restoreSession() {
  if (!getAuthToken()) return false;
  try {
    const data = await api("/auth/me");
    state.user = data.user;
    state.profile = data.profile || null;
    state.currentTab = defaultTab();
    await refreshAll();
    return true;
  } catch (error) {
    clearSession();
    if (!["AUTH_REQUIRED", "AUTH_SESSION_INVALID", "AUTH_DISABLED"].includes(error.code)) {
      throw error;
    }
    return false;
  }
}

export async function logout() {
  try {
    if (getAuthToken()) {
      await api("/auth/logout", { method: "POST", body: {} });
    }
  } finally {
    clearSession();
  }
}

export function clearSession() {
  setAuthToken("");
  state.user = null;
  state.profile = null;
  state.currentTab = "plaza";
  state.demands = [];
  state.allDemands = [];
  state.applications = [];
  state.teachers = [];
  state.resumes = [];
  state.orders = [];
  state.records = [];
  state.feedbacks = [];
  state.auditLogs = [];
  state.notifications = [];
}

export async function refreshAll() {
  state.loading.refresh = true;
  try {
    state.bootstrap = await api("/bootstrap");
    await Promise.all([
      loadDemands(),
      api("/demands?status=ALL").then((data) => state.allDemands = data),
      api("/applications?status=ALL").then((data) => state.applications = data),
      api("/teachers").then((data) => state.teachers = data),
      api("/resumes").then((data) => state.resumes = data),
      api(`/orders${state.user?.role === "TEACHER" ? `?teacherId=${state.user.id}` : ""}`).then((data) => state.orders = data),
      api("/records").then((data) => state.records = data),
      api("/feedbacks").then((data) => state.feedbacks = data),
      ["ADMIN", "SUPER_ADMIN"].includes(state.user?.role)
        ? api(`/audit-logs?actorId=${state.user.id}`).then((data) => state.auditLogs = data)
        : Promise.resolve(state.auditLogs = []),
      api(`/notifications?userId=${state.user?.id || 0}`).then((data) => state.notifications = data)
    ]);
  } finally {
    state.loading.refresh = false;
  }
}

export async function loadDemands() {
  const query = queryString({
    status: "OPEN",
    teacherId: state.user?.role === "TEACHER" ? state.user.id : "",
    ...readFilters()
  });
  state.demands = await api(`/demands?${query}`);
}

export function readFilters() {
  return {
    subject: $("#filterSubject")?.value || "",
    grade: $("#filterGrade")?.value || "",
    region: $("#filterRegion")?.value || "",
    tag: $("#filterTag")?.value || "",
    sort: $("#filterSort")?.value || "latest"
  };
}

export function persistFilters() {
  localStorage.setItem(FILTERS_KEY, JSON.stringify(readFilters()));
}

function restoreFilters() {
  const saved = JSON.parse(localStorage.getItem(FILTERS_KEY) || "{}");
  const map = {
    filterSubject: saved.subject,
    filterGrade: saved.grade,
    filterRegion: saved.region,
    filterTag: saved.tag,
    filterSort: saved.sort
  };
  filterIds.forEach((id) => {
    if (map[id] !== undefined && $(`#${id}`)) {
      $(`#${id}`).value = map[id];
    }
  });
}
