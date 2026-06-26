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
  notifications: [],
  selectedDemandId: null
};

export const tabs = [
  { id: "plaza", label: "需求广场", icon: "⌕", roles: ["TEACHER"] },
  { id: "admin-demands", label: "需求发布", icon: "+", roles: ["ADMIN", "SUPER_ADMIN"] },
  { id: "applications", label: "申请记录", icon: "✓", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "teachers", label: "教师简历", icon: "≡", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "courses", label: "课程记录", icon: "▦", roles: ["TEACHER", "ADMIN", "SUPER_ADMIN"] },
  { id: "feedbacks", label: "评价回访", icon: "★", roles: ["ADMIN", "SUPER_ADMIN"] }
];

export const filterIds = ["filterSubject", "filterGrade", "filterRegion", "filterTag", "filterSort"];

export function defaultTab() {
  if (!state.user) {
    return "plaza";
  }
  const first = tabs.find((tab) => tab.roles.includes(state.user.role));
  return first ? first.id : "plaza";
}
