import { renderDemandMap } from "./map.js";
import { shortcuts, state, tabs } from "./state.js";
import { $, formatDateTime, html } from "./view.js";

export function render() {
  renderAuthState();
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
  renderAuditLogs();
}

export function renderDemoAccounts() {
  $("#demoAccounts").innerHTML = state.bootstrap.demoAccounts.map((account) => `
    <button type="button" class="demo-btn" data-action="demo-login"
      data-username="${html(account.username)}" data-password="${html(account.password)}">
      <span aria-hidden="true">•</span>
      ${html(account.role)}：${html(account.username)}
    </button>
  `).join("");
}

function renderNav() {
  const allowed = state.user ? tabs.filter((tab) => tab.roles.includes(state.user.role)) : [];
  if (!allowed.some((tab) => tab.id === state.currentTab)) {
    state.currentTab = allowed[0]?.id || "plaza";
  }
  $("#navList").innerHTML = allowed.map((tab) => `
    <button type="button" class="nav-btn ${tab.id === state.currentTab ? "is-active" : ""}"
      data-action="nav" data-tab="${html(tab.id)}" title="${html(tab.label)}">
      <span class="nav-icon" aria-hidden="true">${html(tab.icon)}</span>
      <span class="nav-label">${html(tab.label)}</span>
    </button>
  `).join("");
  document.querySelectorAll(".view").forEach((view) => view.classList.remove("is-active"));
  if (state.user) {
    $(`#view-${state.currentTab}`)?.classList.add("is-active");
  }
}

function renderAuthState() {
  const authenticated = Boolean(state.user);
  document.body.classList.remove("auth-pending");
  document.body.classList.toggle("is-authenticated", authenticated);
  $("#insightPanel").hidden = authenticated;
  $("#signedOutState").hidden = authenticated;
  $("#focusStrip").hidden = !authenticated;
  $("#sidebarUser").hidden = !authenticated;
  $("#logoutButton").hidden = !authenticated;
}

function renderHeader() {
  const tab = tabs.find((item) => item.id === state.currentTab);
  $("#pageTitle").textContent = state.user ? (tab?.label || "操作台") : "账号登录";
  $("#currentUser").innerHTML = `<strong>登录 PTAgent</strong><span class="muted">家教资源工作台</span>`;
  $("#sidebarUser").innerHTML = state.user ? `
    <span class="sidebar-user-avatar" aria-hidden="true">${html((state.profile?.realName || state.user.username).slice(0, 1))}</span>
    <span class="sidebar-user-copy">
      <strong>${html(state.profile?.realName || state.user.username)}</strong>
      <small>${html(state.user.roleLabel)}</small>
    </span>
  ` : "";
}

function renderStats() {
  if (!state.user) {
    $("#statsGrid").innerHTML = "";
    return;
  }
  $("#statsGrid").innerHTML = shortcuts.map((shortcut) => {
    const tab = shortcut.tab || (state.user.role === "TEACHER" ? shortcut.teacherTab : shortcut.adminTab);
    return `
    <button type="button" class="shortcut-button" data-action="nav" data-tab="${html(tab)}"
      data-tooltip="${html(shortcut.description)}" aria-label="${html(shortcut.description)}">
      <img src="${html(shortcut.icon)}" alt="" width="22" height="22" aria-hidden="true">
    </button>
  `;
  }).join("");
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

export function renderPlaza() {
  renderMap();
  const grid = $("#demandGrid");
  if (state.loading.demands) {
    grid.innerHTML = loadingBlock("正在加载需求...");
    return;
  }
  if (!state.demands.length) {
    grid.innerHTML = emptyBlock("暂无匹配需求，建议调整筛选条件。");
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
  if (state.loading.demands) {
    board.innerHTML = `<div class="map-loading">加载中</div>`;
    return;
  }
  if (!state.demands.length) {
    board.innerHTML = "";
    return;
  }
  renderDemandMap(board, state.demands);
}

function renderAdminDemands() {
  $("#adminDemandRows").innerHTML = state.allDemands.length ? state.allDemands.map((demand) => `
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
  `).join("") : `<tr><td colspan="5">${emptyBlock("暂无需求记录。")}</td></tr>`;
}

function renderApplications() {
  const list = $("#applicationList");
  const visibleApplications = state.user?.role === "TEACHER"
    ? state.applications.filter((application) => application.teacherId === state.user.id)
    : state.applications;
  if (!visibleApplications.length) {
    list.innerHTML = emptyBlock("暂无申请记录。");
    return;
  }
  list.innerHTML = visibleApplications.map((application) => `
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
  $("#teacherGrid").innerHTML = visibleTeachers.length ? visibleTeachers.map((teacher) => `
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
  `).join("") : emptyBlock("暂无教师资料。");

  const resumeTeachers = state.user?.role === "TEACHER" ? visibleTeachers : state.teachers;
  $("#resumeTeacher").innerHTML = resumeTeachers.map((teacher) =>
    `<option value="${teacher.teacherId}">${html(teacher.realName)}</option>`).join("");

  const visibleResumes = state.user?.role === "TEACHER"
    ? state.resumes.filter((resume) => resume.teacherId === state.user.id)
    : state.resumes;
  $("#resumeRows").innerHTML = visibleResumes.length ? visibleResumes.map((resume) => `
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
  `).join("") : `<tr><td colspan="5">${emptyBlock("暂无简历记录。")}</td></tr>`;
}

function renderOrders() {
  const grid = $("#orderGrid");
  if (!state.orders.length) {
    grid.innerHTML = emptyBlock("暂无课程订单。");
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
  $("#recordForm").hidden = state.user?.role !== "TEACHER";
  $("#recordOrder").innerHTML = options;
  $("#feedbackOrder").innerHTML = options;
}

function renderRecords() {
  $("#recordRows").innerHTML = state.records.length ? state.records.map((record) => `
    <tr>
      <td>#${html(record.orderId)}</td>
      <td>${html(record.lessonDate)}</td>
      <td>${html(record.lessonDuration)}</td>
      <td>${html(record.content)}</td>
      <td>${html(record.studentPerformance)}</td>
    </tr>
  `).join("") : `<tr><td colspan="5">${emptyBlock("暂无授课记录。")}</td></tr>`;
}

function renderFeedbacks() {
  const list = $("#feedbackList");
  if (!state.feedbacks.length) {
    list.innerHTML = emptyBlock("暂无评价。");
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

function renderAuditLogs() {
  const rows = $("#auditRows");
  if (!rows) {
    return;
  }
  rows.innerHTML = state.auditLogs.length ? state.auditLogs.map((log) => `
    <tr>
      <td>${html(formatDateTime(log.createTime))}</td>
      <td><strong>${html(log.action)}</strong><br><span class="muted">${html(log.detail || "")}</span></td>
      <td>${html(log.targetType)} #${html(log.targetId)}</td>
      <td>#${html(log.actorId)} ${html(log.actorRole)}</td>
    </tr>
  `).join("") : `<tr><td colspan="4">${emptyBlock("暂无审计日志。")}</td></tr>`;
}

function loadingBlock(message) {
  return `<div class="empty loading-state"><span class="loading-dot"></span>${html(message)}</div>`;
}

function emptyBlock(message) {
  return `<div class="empty">${html(message)}</div>`;
}
