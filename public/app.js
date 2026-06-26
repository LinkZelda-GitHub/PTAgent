import { login, loadBootstrap } from "./js/data.js";
import { attachEvents } from "./js/events.js";
import { render } from "./js/render.js";
import { applySidebar, applyTheme, preferredSidebar, preferredTheme, toast } from "./js/view.js";

async function init() {
  applyTheme(preferredTheme());
  applySidebar(preferredSidebar(), { persist: false });
  attachEvents();
  document.querySelector("#recordForm").lessonDate.value = new Date().toISOString().slice(0, 10);
  await loadBootstrap();
  await login("teacher", "teacher123");
  render();
}

init().catch((error) => {
  console.error(error);
  toast(error.message);
});
