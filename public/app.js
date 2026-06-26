const state = {
  user: null,
  profile: null,
  bootstrap: null,
  currentTab: "plaza",
  theme: "light",
  sidebarCollapsed: false,
  demands: [],
  allDemands: [],
  applications: [],
  teachers: [],
  resumes: [],
  orders: [],
  records: [],
  feedbacks: [],
  notifications: [],
  selectedDemandId: null
};

const tabs = [
  { id: "plaza", label: "需求广场", icon: "⌕", roles: ["TEACHER"] },
  { id: "admin-demands", label: "需求发布", icon: "+", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "applications", label: "匹配审核", icon: "✓", roles: ["SUPER_ADMIN"] },
  { id: "teachers", label: "教师简历", icon: "≡", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "courses", label: "课程记录", icon: "▦", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "feedbacks", label: "评价回访", icon: "★", roles: ["ADMIN", "SUPER_ADMIN"] }
];

const $ = (selector) => document.querySelector(selector);

function preferredTheme() {
  const saved = localStorage.getItem("ptagent-theme");
  if (saved === "light" || saved === "dark") {
    return saved;
  }
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function applyTheme(theme) {
  state.theme = theme;
  document.documentElement.dataset.theme = theme;
  localStorage.setItem("ptagent-theme", theme);
  const icon = $("#themeIcon");
  if (icon) {
    icon.textContent = theme === "dark" ? "☀" : "☾";
  }
}

function preferredSidebar() {
  if (isMobileLayout()) {
    return true;
  }
  return localStorage.getItem("ptagent-sidebar-collapsed") === "1";
}

function isMobileLayout() {
  return window.matchMedia("(max-width: 1050px)").matches;
}

function applySidebar(collapsed, options = {}) {
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

function onMediaQueryChange(query, handler) {
  if (query.addEventListener) {
    query.addEventListener("change", handler);
  } else if (query.addListener) {
    query.addListener(handler);
  }
}

async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.ok === false) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function html(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function toast(message) {
  const node = $("#toast");
  node.textContent = message;
  node.classList.add("is-active");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => node.classList.remove("is-active"), 2600);
}

function queryString(values) {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      query.set(key, value);
    }
  });
  return query.toString();
}

function fillSelect(select, values, placeholder = "全部") {
  select.innerHTML = `<option value="">${html(placeholder)}</option>` +
    values.map((item) => `<option value="${html(item)}">${html(item)}</option>`).join("");
}

function fillMultiSelect(select, values) {
  select.innerHTML = values.map((item) => `<option value="${html(item)}">${html(item)}</option>`).join("");
}

function selectedValues(select) {
  return Array.from(select.selectedOptions).map((option) => option.value);
}

async function loadBootstrap() {
  state.bootstrap = await api("/bootstrap");
  fillSelect($("#filterSubject"), state.bootstrap.subjects, "全部科目");
  fillSelect($("#filterGrade"), state.bootstrap.grades, "全部年级");
  fillSelect($("#filterRegion"), state.bootstrap.regions, "全部区域");
  fillSelect($("#filterTag"), state.bootstrap.tags, "全部标签");
  fillSelect($("#demandSubject"), state.bootstrap.subjects, "选择科目");
  fillSelect($("#demandGrade"), state.bootstrap.grades, "选择年级");
  fillSelect($("#demandRegion"), state.bootstrap.regions, "选择区域");
  fillMultiSelect($("#demandTags"), state.bootstrap.tags);
  renderDemoAccounts();
}

function renderDemoAccounts() {
  $("#demoAccounts").innerHTML = state.bootstrap.demoAccounts.map((account) => `
    <button type="button" class="demo-btn" data-action="demo-login"
      data-username="${html(account.username)}" data-password="${html(account.password)}">
      <span aria-hidden="true">•</span>
      ${html(account.role)}：${html(account.username)}
    </button>
  `).join("");
}

async function login(username, password) {
  const data = await api("/auth/login", { method: "POST", body: { username, password } });
  state.user = data.user;
  state.profile = data.profile || null;
  state.currentTab = defaultTab();
  await refreshAll();
  render();
  toast(`已登录：${state.user.roleLabel}`);
}

function defaultTab() {
  if (!state.user) return "plaza";
  const first = tabs.find((tab) => tab.roles.includes(state.user.role));
  return first ? first.id : "plaza";
}

async function refreshAll() {
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
    api(`/notifications?userId=${state.user?.id || 0}`).then((data) => state.notifications = data)
  ]);
}

async function loadDemands() {
  const query = queryString({
    status: "OPEN",
    teacherId: state.user?.role === "TEACHER" ? state.user.id : "",
    subject: $("#filterSubject")?.value || "",
    grade: $("#filterGrade")?.value || "",
    region: $("#filterRegion")?.value || "",
    tag: $("#filterTag")?.value || "",
    sort: $("#filterSort")?.value || "latest"
  });
  state.demands = await api(`/demands?${query}`);
}

function render() {
  renderNav();
  renderHeader();
  renderStats();
  renderNotifications();
  renderPlaza();
  renderAdminDemands();
  renderApplications();
  renderTeachers();
  renderOrders();
  renderRecords();
  renderFeedbacks();
}

function renderNav() {
  const allowed = tabs.filter((tab) => !state.user || tab.roles.includes(state.user.role));
  if (!allowed.some((tab) => tab.id === state.currentTab)) {
    state.currentTab = allowed[0]?.id || "plaza";
  }
  $("#navList").innerHTML = allowed.map((tab) => `
    <button type="button" class="nav-btn ${tab.id === state.currentTab ? "is-active" : ""}"
      data-action="nav" data-tab="${html(tab.id)}">
      <span class="nav-icon" aria-hidden="true">${html(tab.icon)}</span>
      <span class="nav-label">${html(tab.label)}</span>
    </button>
  `).join("");
  document.querySelectorAll(".view").forEach((view) => view.classList.remove("is-active"));
  $(`#view-${state.currentTab}`)?.classList.add("is-active");
}

function renderHeader() {
  const tab = tabs.find((item) => item.id === state.currentTab);
  $("#pageTitle").textContent = tab?.label || "操作台";
  $("#currentUser").innerHTML = state.user ? `
    <span class="role-pill">${html(state.user.roleLabel)}</span>
    <strong>${html(state.profile?.realName || state.user.username)}</strong>
  ` : `<span class="muted">未登录</span>`;
}

function renderStats() {
  const stats = [
    ["待接单需求", state.bootstrap.openDemands],
    ["启用教师", state.bootstrap.activeTeachers],
    ["待审申请", state.bootstrap.pendingApplications],
    ["课程订单", state.bootstrap.courseOrders]
  ];
  $("#statsGrid").innerHTML = stats.map(([label, value]) => `
    <div class="stat">
      <span>${html(label)}</span>
      <strong>${html(value)}</strong>
    </div>
  `).join("");
}

function renderNotifications() {
  const box = $("#notifications");
  if (!state.notifications.length) {
    box.classList.remove("is-active");
    box.innerHTML = "";
    return;
  }
  const latest = state.notifications[0];
  box.classList.add("is-active");
  box.innerHTML = `<p><strong>${html(latest.title)}</strong> ${html(latest.message)}</p>`;
}

function renderPlaza() {
  renderMap();
  const grid = $("#demandGrid");
  if (!state.demands.length) {
    grid.innerHTML = `<div class="empty">暂无匹配需求，建议调整筛选条件。</div>`;
    return;
  }
  grid.innerHTML = state.demands.map((demand) => `
    <article class="demand-card">
      <div class="card-head">
        <div>
          <h3>${html(demand.grade)}${html(demand.subject)}</h3>
          <div class="card-meta">
            <span>${html(demand.region)} · ${html(demand.distanceKm)}km</span>
            <span>${html(demand.salaryRange || "面议")}</span>
            <span>${html(demand.teacherGenderLabel)}</span>
            <span>匹配度 ${html(demand.matchScore)}%</span>
          </div>
        </div>
        <span class="status-pill">${html(demand.statusLabel)}</span>
      </div>
      <p>${html(demand.basicScore || demand.remark)}</p>
      <div class="tag-row">${demand.qualificationTags.map((tag) => `<span class="tag">${html(tag)}</span>`).join("")}</div>
      <div class="action-row">
        ${state.user?.role === "TEACHER" ? `
          <button type="button" class="primary-btn" data-action="open-apply" data-id="${demand.id}" title="申请接单">
            <span aria-hidden="true">→</span>
            申请接单
          </button>
        ` : ""}
      </div>
    </article>
  `).join("");
}

function renderMap() {
  const board = $("#mapBoard");
  if (!state.demands.length) {
    board.innerHTML = "";
    return;
  }
  const longitudes = state.demands.map((demand) => demand.longitude);
  const latitudes = state.demands.map((demand) => demand.latitude);
  const minLon = Math.min(...longitudes);
  const maxLon = Math.max(...longitudes);
  const minLat = Math.min(...latitudes);
  const maxLat = Math.max(...latitudes);
  board.innerHTML = state.demands.map((demand, index) => {
    const x = 12 + scale(demand.longitude, minLon, maxLon) * 76;
    const y = 88 - scale(demand.latitude, minLat, maxLat) * 76;
    return `<button class="marker ${index < 2 ? "is-hot" : ""}" style="left:${x}%;top:${y}%"
      title="${html(demand.grade)}${html(demand.subject)} ${html(demand.region)}">
      <span>${html(demand.subject.slice(0, 1))}</span>
    </button>`;
  }).join("");
}

function scale(value, min, max) {
  if (max === min) return 0.5;
  return (value - min) / (max - min);
}

function renderAdminDemands() {
  $("#adminDemandRows").innerHTML = state.allDemands.map((demand) => `
    <tr>
      <td><strong>${html(demand.grade)}${html(demand.subject)}</strong><br><span class="muted">${html(demand.address)}</span></td>
      <td>${html(demand.region)}</td>
      <td>${html(demand.salaryRange || "面议")}</td>
      <td><span class="status-pill">${html(demand.statusLabel)}</span></td>
      <td>
        ${demand.status !== "CLOSED" ? `
          <button type="button" class="danger-btn mini-btn" data-action="close-demand" data-id="${demand.id}" title="关闭需求">× 关闭</button>
        ` : ""}
      </td>
    </tr>
  `).join("");
}

function renderApplications() {
  const list = $("#applicationList");
  if (!state.applications.length) {
    list.innerHTML = `<div class="empty">暂无申请记录。</div>`;
    return;
  }
  list.innerHTML = state.applications.map((application) => `
    <article class="application-card">
      <div class="card-head">
        <div>
          <h3>${html(application.demandTitle)} · ${html(application.teacherName)}</h3>
          <p>${html(application.selfIntro)}</p>
        </div>
        <span class="status-pill">${html(application.statusLabel)}</span>
      </div>
      <div class="tag-row">${application.teacherTags.map((tag) => `<span class="tag">${html(tag)}</span>`).join("")}</div>
      <div class="action-row">
        ${state.user?.role === "SUPER_ADMIN" && application.status === "PENDING" ? `
          <button type="button" class="primary-btn" data-action="review-application" data-status="APPROVED" data-id="${application.id}" title="通过申请">✓ 通过</button>
          <button type="button" class="danger-btn" data-action="review-application" data-status="REJECTED" data-id="${application.id}" title="拒绝申请">× 拒绝</button>
        ` : ""}
      </div>
    </article>
  `).join("");
}

function renderTeachers() {
  const visibleTeachers = state.user?.role === "TEACHER"
    ? state.teachers.filter((teacher) => teacher.teacherId === state.user.id)
    : state.teachers;
  $("#teacherGrid").innerHTML = visibleTeachers.map((teacher) => `
    <article class="teacher-card">
      <div class="card-head">
        <div>
          <h3>${html(teacher.realName)} · ${html(teacher.genderLabel)}</h3>
          <div class="card-meta">
            <span>${html(teacher.education)}</span>
            <span>${html(teacher.graduateSchool)}</span>
            <span>${html(teacher.subjects.join("、"))}</span>
            <span>评分 ${html(teacher.avgRating)}</span>
          </div>
        </div>
        <span class="status-pill">${teacher.enabled ? "已启用" : "待审核/禁用"}</span>
      </div>
      <p>${html(teacher.teachingExperience || teacher.personalIntro)}</p>
      <div class="tag-row">${teacher.tags.map((tag) => `<span class="tag">${html(tag)}</span>`).join("")}</div>
      <div class="action-row">
        ${state.user?.role === "SUPER_ADMIN" ? `
          <button type="button" class="ghost-btn" data-action="toggle-teacher" data-id="${teacher.teacherId}" data-enabled="${!teacher.enabled}" title="切换启用状态">
            ${teacher.enabled ? "禁用" : "启用"}
          </button>
        ` : ""}
      </div>
    </article>
  `).join("");

  const resumeTeachers = state.user?.role === "TEACHER" ? visibleTeachers : state.teachers;
  $("#resumeTeacher").innerHTML = resumeTeachers.map((teacher) =>
    `<option value="${teacher.teacherId}">${html(teacher.realName)}</option>`).join("");

  const visibleResumes = state.user?.role === "TEACHER"
    ? state.resumes.filter((resume) => resume.teacherId === state.user.id)
    : state.resumes;
  $("#resumeRows").innerHTML = visibleResumes.map((resume) => `
    <tr>
      <td>${html(resume.teacherName)}</td>
      <td>${html(resume.summary)}</td>
      <td>${html(formatDateTime(resume.submitTime))}</td>
      <td><span class="status-pill">${html(resume.statusLabel)}</span></td>
      <td>
        ${state.user?.role !== "TEACHER" ? `
          <button type="button" class="mini-btn" data-action="resume-status" data-id="${resume.id}" data-status="1">标记已查看</button>
          <button type="button" class="mini-btn" data-action="resume-status" data-id="${resume.id}" data-status="2">标记已筛选</button>
        ` : ""}
      </td>
    </tr>
  `).join("");
}

function renderOrders() {
  const grid = $("#orderGrid");
  if (!state.orders.length) {
    grid.innerHTML = `<div class="empty">暂无课程订单。</div>`;
  } else {
    grid.innerHTML = state.orders.map((order) => `
      <article class="order-card">
        <div class="card-head">
          <div>
            <h3>${html(order.courseTitle)} · ${html(order.teacherName)}</h3>
            <div class="card-meta">
              <span>${html(order.address)}</span>
              <span>家长 ${html(order.parentName)} ${html(order.parentPhone)}</span>
              <span>教师 ${html(order.teacherPhone)}</span>
            </div>
          </div>
          <span class="status-pill">${html(order.statusLabel)}</span>
        </div>
      </article>
    `).join("");
  }

  const options = state.orders.map((order) =>
    `<option value="${order.id}">#${order.id} ${html(order.courseTitle)} · ${html(order.teacherName)}</option>`).join("");
  $("#recordOrder").innerHTML = options;
  $("#feedbackOrder").innerHTML = options;
}

function renderRecords() {
  $("#recordRows").innerHTML = state.records.map((record) => `
    <tr>
      <td>#${html(record.orderId)}</td>
      <td>${html(record.lessonDate)}</td>
      <td>${html(record.lessonDuration)}</td>
      <td>${html(record.content)}</td>
      <td>${html(record.studentPerformance)}</td>
    </tr>
  `).join("");
}

function renderFeedbacks() {
  const list = $("#feedbackList");
  if (!state.feedbacks.length) {
    list.innerHTML = `<div class="empty">暂无评价。</div>`;
    return;
  }
  list.innerHTML = state.feedbacks.map((feedback) => `
    <article class="feedback-card">
      <div class="card-head">
        <div>
          <h3>${html(feedback.teacherName)} · ${"★".repeat(feedback.ratingScore)}</h3>
          <p>${html(feedback.commentText)}</p>
        </div>
        <span class="status-pill">${html(feedback.feedbackSourceLabel)}</span>
      </div>
    </article>
  `).join("");
}

function formatDateTime(value) {
  return value ? value.replace("T", " ").slice(0, 16) : "";
}

function formObject(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function handleClick(event) {
  const button = event.target.closest("button[data-action]");
  if (!button) return;
  const action = button.dataset.action;
  try {
    if (action === "demo-login") {
      $("#loginForm").username.value = button.dataset.username;
      $("#loginForm").password.value = button.dataset.password;
      await login(button.dataset.username, button.dataset.password);
    }
    if (action === "nav") {
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
      await api(`/demands/${button.dataset.id}/close`, { method: "POST", body: {} });
      toast("需求已关闭");
      await refreshAll();
      render();
    }
    if (action === "review-application") {
      await api(`/applications/${button.dataset.id}/review`, {
        method: "POST",
        body: { status: button.dataset.status, adminId: state.user.id }
      });
      toast(button.dataset.status === "APPROVED" ? "已通过并生成课程订单" : "已拒绝申请");
      await refreshAll();
      render();
    }
    if (action === "toggle-teacher") {
      await api(`/teachers/${button.dataset.id}/enabled`, {
        method: "POST",
        body: { enabled: button.dataset.enabled === "true" }
      });
      toast("教师状态已更新");
      await refreshAll();
      render();
    }
    if (action === "resume-status") {
      await api(`/resumes/${button.dataset.id}/status`, {
        method: "POST",
        body: { status: Number(button.dataset.status) }
      });
      toast("简历状态已更新");
      await refreshAll();
      render();
    }
  } catch (error) {
    toast(error.message);
  }
}

function attachEvents() {
  document.addEventListener("click", handleClick);

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

  onMediaQueryChange(window.matchMedia("(max-width: 1050px)"), () => {
    applySidebar(preferredSidebar(), { persist: false });
  });

  $("#loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await login(event.currentTarget.username.value, event.currentTarget.password.value);
    } catch (error) {
      toast(error.message);
    }
  });

  ["filterSubject", "filterGrade", "filterRegion", "filterTag", "filterSort"].forEach((id) => {
    $(`#${id}`).addEventListener("change", async () => {
      try {
        await loadDemands();
        renderPlaza();
      } catch (error) {
        toast(error.message);
      }
    });
  });

  $("#demandForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const body = formObject(form);
    body.adminId = state.user.id;
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
      await api("/demands", { method: "POST", body });
      toast("需求已发布");
      await refreshAll();
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#applyForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api(`/demands/${state.selectedDemandId}/applications`, {
        method: "POST",
        body: { teacherId: state.user.id, selfIntro: event.currentTarget.selfIntro.value }
      });
      $("#applyDialog").close();
      toast("申请已提交");
      await refreshAll();
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#closeApply").addEventListener("click", () => $("#applyDialog").close());
  $("#cancelApply").addEventListener("click", () => $("#applyDialog").close());

  $("#resumeForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await api("/resumes", { method: "POST", body: formObject(event.currentTarget) });
      toast("简历已投递");
      await refreshAll();
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#recordForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const body = formObject(event.currentTarget);
    try {
      await api(`/orders/${body.orderId}/records`, { method: "POST", body });
      toast("授课记录已新增");
      await refreshAll();
      render();
    } catch (error) {
      toast(error.message);
    }
  });

  $("#feedbackForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const body = formObject(event.currentTarget);
    body.submitAdminId = state.user.id;
    body.ratingScore = Number(body.ratingScore);
    body.feedbackSource = Number(body.feedbackSource);
    try {
      await api("/feedbacks", { method: "POST", body });
      toast("评价已录入");
      await refreshAll();
      render();
    } catch (error) {
      toast(error.message);
    }
  });
}

async function init() {
  applyTheme(preferredTheme());
  applySidebar(preferredSidebar(), { persist: false });
  attachEvents();
  $("#recordForm").lessonDate.value = new Date().toISOString().slice(0, 10);
  await loadBootstrap();
  await login("teacher", "teacher123");
}

init().catch((error) => {
  console.error(error);
  toast(error.message);
});
