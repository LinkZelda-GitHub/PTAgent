export const state = {
  user: null,
  profile: null,
  bootstrap: null,
  currentTab: "plaza",
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
  { id: "plaza", label: "需求广场", icon: "⌕", roles: ["TEACHER"] },
  { id: "admin-demands", label: "需求发布", icon: "+", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "applications", label: "申请记录", icon: "✓", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "teachers", label: "教师简历", icon: "≡", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "courses", label: "课程记录", icon: "▦", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "feedbacks", label: "评价回访", icon: "★", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "audit", label: "审计日志", icon: "!", roles: ["ADMIN", "SUPER_ADMIN"] }
];

export const shortcuts = [
  { label: "接单需求", description: "打开可接需求", icon: "/assets/icons/clipboard-list.svg", teacherTab: "plaza", adminTab: "admin-demands" },
  { label: "教师", description: "打开教师与简历", icon: "/assets/icons/users.svg", tab: "teachers" },
  { label: "申请", description: "打开申请记录", icon: "/assets/icons/file-check.svg", tab: "applications" },
  { label: "订单", description: "打开课程订单", icon: "/assets/icons/calendar-check.svg", tab: "courses" }
];

export const filterIds = ["filterSubject", "filterGrade", "filterRegion", "filterTag", "filterSort"];

export function defaultTab() {
  if (!state.user) {
    return "plaza";
  }
  const first = tabs.find((tab) => tab.roles.includes(state.user.role));
  return first ? first.id : "plaza";
}
