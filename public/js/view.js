import { state } from "./state.js?v=20260627-6";

const THEME_KEY = "ptagent-theme-v2";

export const $ = (selector) => document.querySelector(selector);

export function html(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

export function toast(message) {
  const node = $("#toast");
  node.textContent = message;
  node.classList.add("is-active");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => node.classList.remove("is-active"), 2600);
}

export function preferredTheme() {
  const saved = localStorage.getItem(THEME_KEY);
  if (saved === "light" || saved === "dark") {
    return saved;
  }
  return "light";
}

export function applyTheme(theme) {
  state.theme = theme;
  document.documentElement.dataset.theme = theme;
  localStorage.setItem(THEME_KEY, theme);
  const icon = $("#themeIcon");
  if (icon) {
    icon.textContent = theme === "dark" ? "☀" : "☾";
  }
}

export function preferredSidebar() {
  if (isMobileLayout()) {
    return true;
  }
  return localStorage.getItem("ptagent-sidebar-collapsed") === "1";
}

export function isMobileLayout() {
  return window.matchMedia("(max-width: 980px)").matches;
}

export function applySidebar(collapsed, options = {}) {
  state.sidebarCollapsed = collapsed;
  document.body.classList.toggle("sidebar-collapsed", collapsed);
  if (options.persist !== false && !isMobileLayout()) {
    localStorage.setItem("ptagent-sidebar-collapsed", collapsed ? "1" : "0");
  }
  const toggle = $("#sidebarToggle");
  if (toggle) {
    const label = collapsed ? "展开导航" : "收起导航";
    toggle.title = label;
    toggle.setAttribute("aria-label", label);
    toggle.querySelector("span").textContent = collapsed ? "›" : "‹";
  }
  const mobileToggle = $("#mobileSidebarToggle");
  if (mobileToggle) {
    const label = collapsed ? "打开导航" : "关闭导航";
    mobileToggle.title = label;
    mobileToggle.setAttribute("aria-label", label);
  }
}

export function onMediaQueryChange(query, handler) {
  if (query.addEventListener) {
    query.addEventListener("change", handler);
  } else if (query.addListener) {
    query.addListener(handler);
  }
}

export function fillSelect(select, values, placeholder = "全部") {
  select.innerHTML = `<option value="">${html(placeholder)}</option>` +
    values.map((item) => `<option value="${html(item)}">${html(item)}</option>`).join("");
}

export function fillMultiSelect(select, values) {
  select.innerHTML = values.map((item) => `<option value="${html(item)}">${html(item)}</option>`).join("");
}

export function selectedValues(select) {
  return Array.from(select.selectedOptions).map((option) => option.value);
}

export function formatDateTime(value) {
  return value ? value.replace("T", " ").slice(0, 16) : "";
}

export function formObject(form) {
  return Object.fromEntries(new FormData(form).entries());
}

export async function withFormPending(form, task) {
  return withPending(form, form.querySelector("button[type='submit']"), task);
}

export async function withButtonPending(button, task) {
  return withPending(button.closest("form"), button, task);
}

async function withPending(scope, button, task) {
  const controls = scope ? Array.from(scope.querySelectorAll("button,input,select,textarea")) : [button];
  try {
    controls.forEach((control) => {
      control.disabled = true;
      control.setAttribute("aria-busy", "true");
    });
    if (button) {
      button.dataset.originalText = button.dataset.originalText || button.innerHTML;
      button.innerHTML = `<span aria-hidden="true">…</span> 处理中`;
    }
    return await task();
  } finally {
    controls.forEach((control) => {
      control.disabled = false;
      control.removeAttribute("aria-busy");
    });
    if (button?.dataset.originalText) {
      button.innerHTML = button.dataset.originalText;
    }
  }
}
