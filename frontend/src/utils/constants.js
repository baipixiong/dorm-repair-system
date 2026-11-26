export const STATUS_OPTIONS = [
  { value: 'pending', label: '待派单', type: 'info' },
  { value: 'assigned', label: '已派单', type: 'warning' },
  { value: 'working', label: '维修中', type: 'primary' },
  { value: 'review', label: '待验收', type: 'warning' },
  { value: 'completed', label: '已完成', type: 'success' },
  { value: 'cancelled', label: '已取消', type: 'info' }
]

export const CATEGORY_OPTIONS = [
  { value: 'water', label: '水电维修' },
  { value: 'electric', label: '电路故障' },
  { value: 'door', label: '门窗维修' },
  { value: 'furniture', label: '家具维修' },
  { value: 'other', label: '其他问题' }
]

export const PRIORITY_OPTIONS = [
  { value: 'low', label: '低', type: 'info' },
  { value: 'normal', label: '普通', type: 'primary' },
  { value: 'high', label: '紧急', type: 'warning' },
  { value: 'urgent', label: '特急', type: 'danger' }
]

export const ROLE_OPTIONS = [
  { value: 'student', label: '学生', type: 'primary' },
  { value: 'technician', label: '维修人员', type: 'success' },
  { value: 'admin', label: '管理员', type: 'danger' }
]

export const ACTION_LABELS = {
  create: '提交报修',
  assign: '派单',
  start: '开始维修',
  finish: '提交完工',
  accept: '验收通过',
  reject: '申请返修',
  cancel: '取消报修',
  comment: '添加备注',
  classify: '修改分类',
  evaluate: '评价'
}

export const STATUS_MAP = Object.fromEntries(STATUS_OPTIONS.map((item) => [item.value, item]))
export const CATEGORY_MAP = Object.fromEntries(CATEGORY_OPTIONS.map((item) => [item.value, item]))
export const PRIORITY_MAP = Object.fromEntries(PRIORITY_OPTIONS.map((item) => [item.value, item]))
export const ROLE_MAP = Object.fromEntries(ROLE_OPTIONS.map((item) => [item.value, item]))

export function statusLabel(status) {
  return STATUS_MAP[status]?.label || status || '-'
}

export function statusType(status) {
  return STATUS_MAP[status]?.type || 'info'
}

export function categoryLabel(category) {
  return CATEGORY_MAP[category]?.label || category || '-'
}

export function priorityLabel(priority) {
  return PRIORITY_MAP[priority]?.label || priority || '-'
}

export function priorityType(priority) {
  return PRIORITY_MAP[priority]?.type || 'info'
}

export function roleLabel(role) {
  return ROLE_MAP[role]?.label || role || '-'
}

export function roleType(role) {
  return ROLE_MAP[role]?.type || 'info'
}

export function locationText(ticket) {
  if (!ticket) return '-'
  const building = ticket.buildingName || ''
  const room = ticket.roomNo || ''
  return `${building} ${room}`.trim() || '-'
}
