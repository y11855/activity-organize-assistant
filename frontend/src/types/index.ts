/** 用户信息 */
export interface UserInfo {
  id: string
  username: string
  nickname: string
  /** 已绑定邮箱，未绑定为空 */
  email?: string
  roles: string[]
  roleNames?: string[]
  permissions: string[]
}

/** 统一分页查询参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/** 统一分页响应结构 */
export interface PageVO<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 活动创建请求 */
export interface ActivitySaveDTO {
  title: string
  description?: string
  startTime?: string
  endTime?: string
  location?: string
  expectedCount?: number
  memberIds?: string[]
}

/** 物料项 */
export interface MaterialVO {
  id: string
  name: string
  quantity: number
  unit: string
  unitPrice?: number
  remark?: string
}

/** 任务 */
export interface TaskVO {
  id: string
  title: string
  description?: string
  assigneeId?: string
  assigneeName?: string
  deadline?: string
  status: number
}

/** 活动详情 */
export interface ActivityVO {
  id: string
  creatorId?: string
  creatorid: string
  title: string
  description?: string
  startTime?: string
  endTime?: string
  location?: string
  expectedCount?: number
  memberNames?: string[]
  status: number
  draftStatus?: number
  planContent?: string
  reviewContent?: string
  materials?: MaterialVO[]
  tasks?: TaskVO[]
  createTime: string
}

/** 任务派发请求 */
export interface TaskAssignDTO {
  activityId?: string
  title: string
  description?: string
  assigneeId?: string
  assigneeName?: string
  deadline?: string
}

/** 问卷 */
export interface QuestionnaireVO {
  id: string
  activityid: string
  activityTitle?: string
  type?: number
  title: string
  description?: string
  coverImage?: string
  questions?: string
  status: number
  responseCount?: number
  createTime: string
}

/** 报名人员明细 */
export interface QuestionnaireResponseVO {
  id: string
  respondentId?: string
  respondentName?: string
  answer?: string
  status: number
  submitTime?: string
}

/** 报名统计看板 */
export interface ResponseStatisticsVO {
  total: number
  byStatus: Record<number, number>
  timeline: { date: string; count: number }[]
}

/** 活动事件记录 */
export interface ActivityEventLogVO {
  id: string
  activityId: string
  eventType: number
  content: string
  occurredAt?: string
  recorderId?: string
  recorderName?: string
  createTime?: string
}

/** 我的任务（个人分工） */
export interface MyTaskVO {
  id: string
  activityid: string
  activityTitle?: string
  title: string
  description?: string
  assigneeId?: string
  assigneeName?: string
  deadline?: string
  status: number
  createTime: string
}

/** 站内消息 */
export interface MessageVO {
  id: string
  userid: string
  title: string
  content?: string
  activityId?: string
  readStatus: number
  createTime: string
}

/** 定时提醒 */
export interface ReminderVO {
  id: string
  activityId: string
  title: string
  content?: string
  targetUserName?: string
  triggerTime: string
  /** 0待触发 1已发送 2已取消 3发送失败 */
  status: number
  createTime: string
}

/** 创建提醒请求 */
export interface ReminderCreateDTO {
  activityId: string
  title: string
  content?: string
  targetUserId?: string
  targetUserName?: string
  triggerTime: string
}

/** 用户（成员选择器） */
export interface UserSimpleVO {
  id: string
  username: string
  nickname: string
  status?: number
}

/** 登录响应 */
export interface LoginVO {
  token: string
  id: string
  username: string
  nickname: string
  /** 已绑定邮箱，未绑定为空 */
  email?: string
  roles: string[]
  roleNames: string[]
  permissions: string[]
}

/** 后台用户列表项 */
export interface UserAdminVO {
  id: string
  username: string
  nickname: string
  status: number
  roleIds: string[]
  roleCodes: string[]
  roleNames: string[]
  createTime: string
}

/** 角色 */
export interface RoleVO {
  id: string
  code: string
  name: string
  description?: string
  builtin: number
  permissionCodes: string[]
  userCount: number
}

/** 权限项 */
export interface PermissionVO {
  code: string
  name: string
  groupName: string
  sort: number
}

/** AI 生成内容配置（发起人勾选需要 AI 生成的内容项） */
export interface GenerateOptions {
  tasks?: boolean
  materials?: boolean
  questionnaire?: boolean
  reminders?: boolean
}

/** 问卷题目草稿 */
export interface QuestionDraft {
  title: string
  type: 'single' | 'multiple' | 'text'
  required?: boolean
  options?: string[]
}

/** 提醒规则草稿 */
export interface ReminderDraft {
  title: string
  content?: string
  targetUserId?: string
  targetUserName?: string
  triggerTime?: string
}

/** AI 生成内容草稿（待发起人确认/编辑） */
export interface ActivityDraftVO {
  options?: GenerateOptions
  planContent?: string
  materials?: MaterialVO[]
  questions?: QuestionDraft[]
  tasks?: TaskAssignDTO[]
  reminders?: ReminderDraft[]
}

/** 活动资料文件 */
export interface ActivityFileVO {
  id: string
  activityId: string
  name: string
  description?: string
  ext?: string
  size: number
  uploaderId?: string
  uploaderName?: string
  sha256?: string
  url: string
  /** 当前用户是否可删除（创建人可删全部，成员仅可删自传） */
  canDelete?: boolean
  createTime: string
}

/** 单文件直传结果 */
export interface FileUploadVO {
  id?: string
  name: string
  size: number
  url: string
}

/** 分片上传会话 */
export interface ChunkInitVO {
  uploadId: string
  chunkSize: number
  totalChunks: number
}

/** 分片上传进度（断点续传查询结果） */
export interface ChunkStatusVO {
  uploadId: string
  fileName: string
  fileSize: number
  chunkSize: number
  totalChunks: number
  uploadedChunks: number[]
  merged: boolean
}

/** 大屏活动状态分布项 */
export interface DashboardStatusItem {
  name: string
  value: number
}

/** 大屏报名趋势项 */
export interface DashboardTrendItem {
  date: string
  count: number
}

/** 大屏实时动态项（由 Redis Stream 消息生成） */
export interface DashboardEventItem {
  type: string
  content: string
  time: string
}

/** 数据大屏概览（仅当前登录账号相关数据） */
export interface DashboardOverviewVO {
  activityTotal: number
  runningCount: number
  completedCount: number
  pendingCount: number
  cancelledCount: number
  registrationTotal: number
  taskTotal: number
  taskCompleted: number
  taskCompletionRate: number
  myTodoCount: number
  reminderCount: number
  unreadMessages: number
  statusDistribution: DashboardStatusItem[]
  registrationTrend: DashboardTrendItem[]
  recentEvents: DashboardEventItem[]
}
