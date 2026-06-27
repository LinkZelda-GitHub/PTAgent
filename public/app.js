import { loadBootstrap, restoreSession } from "./js/data.js?v=20260627-6";
import { attachEvents } from "./js/events.js?v=20260627-6";
import { render } from "./js/render.js?v=20260627-6";
import { applySidebar, applyTheme, preferredSidebar, preferredTheme, toast } from "./js/view.js?v=20260627-6";

async function init() {
  applyTheme(preferredTheme());
  applySidebar(preferredSidebar(), { persist: false });
  attachEvents();
  document.querySelector("#recordForm").lessonDate.value = new Date().toISOString().slice(0, 10);
  await loadBootstrap();
  await restoreSession();
  render();
}

init().catch((error) => {
  console.error(error);
  document.body.classList.remove("auth-pending");
  render();
  toast(error.message);
});
