export const state = {
  user: null,
  profile: null,
  bootstrap: null,
  currentTab: "plaza",
  authMode: "login",
  registrationResult: null,
  theme: "light",
  sidebarCollapsed: false,
  loading: {
    demands: false,
    refresh: false
  },
  demands: [],
  allDemands: [],
  applications: [],
  teachers: [],
  resumes: [],
  orders: [],
  records: [],
  feedbacks: [],
  auditLogs: [],
  notifications: [],
  selectedDemandId: null
};

export const tabs = [
  { id: "plaza", label: "需求广场", icon: "/assets/icons/clipboard-list.svg", roles: ["TEACHER"] },
  { id: "admin-demands", label: "需求发布", icon: "/assets/icons/file-plus.svg", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "applications", label: "申请记录", icon: "/assets/icons/file-check.svg", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "teachers", label: "教师简历", icon: "/assets/icons/users.svg", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "courses", label: "课程记录", icon: "/assets/icons/calendar-check.svg", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "feedbacks", label: "评价回访", icon: "/assets/icons/message-square.svg", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "audit", label: "审计日志", icon: "/assets/icons/shield-check.svg", roles: ["ADMIN", "SUPER_ADMIN"] }
];

export const filterIds = ["filterSubject", "filterGrade", "filterRegion", "filterTag", "filterSort"];

export function defaultTab() {
  if (!state.user) {
    return "plaza";
  }
  const first = tabs.find((tab) => tab.roles.includes(state.user.role));
  return first ? first.id : "plaza";
}
