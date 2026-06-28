import { api } from "./api.js?v=20260628-5";
import { clearSession, loadBootstrap, loadDemands, login, logout, persistFilters, refreshAll, registerTeacher, requestPhoneCode } from "./data.js?v=20260628-5";
import { clearMapConfig, fillMapConfigForm, saveMapConfig } from "./map.js?v=20260628-5";
import { render, renderPlaza } from "./render.js?v=20260628-5";
import { filterIds, state, tabs } from "./state.js?v=20260628-5";
import {
  $,
  applySidebar,
  applyTheme,
  formObject,
  isMobileLayout,
  onMediaQueryChange,
  preferredSidebar,
  selectedValues,
  toast,
  withButtonPending,
  withFormPending
} from "./view.js?v=20260628-5";

async function handleClick(event) {
  const button = event.target.closest("button[data-action]");
  if (!button || button.disabled) return;
  const action = button.dataset.action;
  try {
    if (action === "auth-mode") {
      state.authMode = button.dataset.mode || "login";
      state.registrationResult = null;
      render();
    }
    if (action === "login-method") {
      state.loginMethod = button.dataset.method || "WECHAT";
      $("#loginForm").reset();
      render();
    }
    if (action === "registration-login-method") {
      state.registrationLoginMethod = button.dataset.method || "PHONE";
      $("#registerForm").verificationCode.value = "";
      render();
    }
    if (action === "request-phone-code") {
      const form = button.closest("form");
      const phoneNumber = button.dataset.scope === "register" ? form.phoneNumber.value : form.loginId.value;
      let result;
      await withButtonPending(button, async () => {
        result = await requestPhoneCode(phoneNumber);
      });
      form.verificationCode.value = result.demoCode || "";
      toast(`验证码已发送至 ${result.phoneNumber}`);
    }
    if (action === "demo-login") {
      const loginMethod = button.dataset.method;
      const loginId = button.dataset.loginId;
      await withButtonPending(button, async () => {
        let verificationCode = "";
        if (loginMethod === "PHONE") {
          verificationCode = (await requestPhoneCode(loginId)).demoCode;
        }
        await login(loginMethod, loginId, verificationCode);
        render();
      });
      toast(`已登录：${state.user.roleLabel}`);
    }
    if (action === "logout") {
      await withButtonPending(button, logout);
      render();
      toast("已安全退出");
    }
    if (action === "nav") {
      const tab = tabs.find((item) => item.id === button.dataset.tab);
      if (!tab || (state.user && !tab.roles.includes(state.user.role))) {
        toast("当前角色暂无该页面权限");
        return;
      }
      state.currentTab = button.dataset.tab;
      if (isMobileLayout()) {
        applySidebar(true, { persist: false });
      }
      render();
    }
    if (action === "open-apply") {
      state.selectedDemandId = Number(button.dataset.id);
      const demand = state.demands.find((item) => item.id === state.selectedDemandId);
      $("#applyTitle").textContent = `申请 ${demand.grade}${demand.subject}`;
      $("#applyDialog").showModal();
    }
    if (action === "close-demand") {
      await withButtonPending(button, async () => {
        await api(`/demands/${button.dataset.id}/close`, { method: "POST", body: {} });
        await refreshAll();
      });
      toast("需求已关闭");
      render();
    }
    if (action === "review-application") {
      await withButtonPending(button, async () => {
        await api(`/applications/${button.dataset.id}/review`, {
          method: "POST",
          body: { status: button.dataset.status }
        });
        await refreshAll();
      });
      toast(button.dataset.status === "APPROVED" ? "已通过并生成课程订单" : "已拒绝申请");
      render();
    }
    if (action === "toggle-teacher") {
      await withButtonPending(button, async () => {
        await api(`/teachers/${button.dataset.id}/enabled`, {
          method: "POST",
          body: { enabled: button.dataset.enabled === "true" }
        });
        await refreshAll();
      });
      toast("教师状态已更新");
      render();
    }
    if (action === "resume-status") {
      await withButtonPending(button, async () => {
        await api(`/resumes/${button.dataset.id}/status`, {
          method: "POST",
          body: { status: Number(button.dataset.status) }
        });
        await refreshAll();
      });
      toast("简历状态已更新");
      render();
    }
    if (action === "save-map-config") {
      saveMapConfig({
        key: $("#amapKey").value,
        securityJsCode: $("#amapSecurityCode").value
      });
      toast("地图配置已保存");
      renderPlaza();
    }
    if (action === "clear-map-config") {
      clearMapConfig();
      fillMapConfigForm();
      toast("已切换为本地地图");
      renderPlaza();
    }
  } catch (error) {
    toast(error.message);
  }
}

export function attachEvents() {
  document.addEventListener("click", handleClick);
  fillMapConfigForm();

  $("#themeToggle").addEventListener("click", () => {
    applyTheme(state.theme === "dark" ? "light" : "dark");
  });

  $("#sidebarToggle").addEventListener("click", () => {
    applySidebar(!state.sidebarCollapsed);
  });

  $("#mobileSidebarToggle").addEventListener("click", () => {
    applySidebar(!state.sidebarCollapsed, { persist: false });
  });

  $("#sidebarScrim").addEventListener("click", () => {
    applySidebar(true, { persist: false });
  });

  onMediaQueryChange(window.matchMedia("(max-width: 980px)"), () => {
    applySidebar(preferredSidebar(), { persist: false });
  });

  $("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const errorBox = $("#loginError");
    errorBox.hidden = true;
    try {
      await withFormPending(form, async () => {
        await login(state.loginMethod, form.loginId.value, form.verificationCode.value);
      });
      render();
      toast(`已登录：${state.user.roleLabel}`);
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
      toast(error.message);
    }
  });

  $("#registerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const errorBox = $("#registerError");
    errorBox.hidden = true;
    const body = formObject(form);
    body.loginMethod = state.registrationLoginMethod;
    body.loginId = state.registrationLoginMethod === "PHONE" ? body.phoneNumber : body.loginId;
    body.gender = Number(body.gender);
    body.subjects = registrationList(form.subjects.value);
    body.serviceArea = registrationList(form.serviceArea.value);
    body.availableTime = registrationList(form.availableTime.value);
    body.hasTeacherCert = form.hasTeacherCert.checked;
    body.normalUniversity = form.normalUniversity.checked;
    body.competitionExperience = form.competitionExperience.checked;
    delete body.agreement;
    if (state.registrationLoginMethod !== "PHONE") {
      delete body.verificationCode;
    }
    try {
      let result;
      await withFormPending(form, async () => {
        result = await registerTeacher(body);
      });
      form.reset();
      state.registrationLoginMethod = "PHONE";
      state.registrationResult = result;
      state.authMode = "registered";
      render();
      toast("注册信息已提交");
    } catch (error) {
      errorBox.textContent = error.message;
      errorBox.hidden = false;
      toast(error.message);
    }
  });

  window.addEventListener("ptagent:auth-expired", () => {
    if (!state.user) return;
    clearSession();
    render();
    toast("登录状态已失效，请重新登录");
  });

  filterIds.forEach((id) => {
    $(`#${id}`).addEventListener("change", async () => {
      persistFilters();
      state.loading.demands = true;
      renderPlaza();
      try {
        await loadDemands();
      } catch (error) {
        toast(error.message);
      } finally {
        state.loading.demands = false;
        renderPlaza();
      }
    });
  });

  $("#demandForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    body.salaryMin = Number(body.salaryMin || 0);
    body.salaryMax = Number(body.salaryMax || 0);
    body.longitude = Number(body.longitude || 113.2644);
    body.latitude = Number(body.latitude || 23.1291);
    body.teacherGender = Number(body.teacherGender || 3);
    body.qualificationTags = selectedValues(form.qualificationTags);
    body.is985Required = form.is985Required.checked;
    body.is211Required = form.is211Required.checked;
    body.isKeyUniversityRequired = form.isKeyUniversityRequired.checked;
    try {
      await withFormPending(form, async () => {
        await api("/demands", { method: "POST", body });
        await refreshAll();
      });
      toast("需求已发布");
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#importDemandForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    try {
      let result;
      await withFormPending(form, async () => {
        result = await api("/import/demands/xlsx", { method: "POST", body });
        await loadBootstrap();
        await refreshAll();
      });
      toast(`已导入 ${result.importedCount} 条，跳过 ${result.skippedCount} 条`);
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#applyForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await withFormPending(form, async () => {
        await api(`/demands/${state.selectedDemandId}/applications`, {
          method: "POST",
          body: { selfIntro: form.selfIntro.value }
        });
        await refreshAll();
      });
      $("#applyDialog").close();
      toast("申请已提交");
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#closeApply").addEventListener("click", () => $("#applyDialog").close());
  $("#cancelApply").addEventListener("click", () => $("#applyDialog").close());

  $("#resumeForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    try {
      await withFormPending(form, async () => {
        await api("/resumes", { method: "POST", body });
        await refreshAll();
      });
      toast("简历已投递");
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#recordForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    try {
      await withFormPending(form, async () => {
        await api(`/orders/${body.orderId}/records`, { method: "POST", body });
        await refreshAll();
      });
      toast("授课记录已新增");
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#feedbackForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    body.ratingScore = Number(body.ratingScore);
    body.feedbackSource = Number(body.feedbackSource);
    try {
      await withFormPending(form, async () => {
        await api("/feedbacks", { method: "POST", body });
        await refreshAll();
      });
      toast("评价已录入");
      render();
    } catch (error) {
      toast(error.message);
    }
  });
}

function registrationList(value) {
  return String(value || "").split(/[、,，]/).map((item) => item.trim()).filter(Boolean);
}
