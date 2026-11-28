// TypeScript 类型定义：业务实体、DTO 与接口返回值
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
  /** 面向参与者的活动介绍（可手写，留空则可由 AI 生成） */
  introduction?: string
  startTime?: string
  endTime?: string
  location?: string
  /** 活动属性标签: sports运动 art艺术 academic学术 public_welfare公益 social社交 entertainment娱乐 other其他 */
  tag: string
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
  /** 面向参与者的活动介绍 */
  introduction?: string
  startTime?: string
  endTime?: string
  location?: string
  expectedCount?: number
  /** 活动属性标签编码 */
  tag?: string
  /** 活动属性标签中文名 */
  tagName?: string
  memberNames?: string[]
  status: number
  draftStatus?: number
  /** 任务分工是否已通知成员: 0否 1是（全局标志，通知后按钮置灰） */
  tasksNotified?: number
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
  /** 面向参与者的活动介绍（报名页展示） */
  introduction?: string
  startTime?: string
  endTime?: string
  location?: string
  type?: number
  title: string
  description?: string
  coverImage?: string
  questions?: string
  status: number
  responseCount?: number
  /** 活动属性标签编码 */
  activityTag?: string
  /** 活动属性标签中文名 */
  activityTagName?: string
  /** 点赞数 */
  likeCount?: number
  /** 当前用户是否已点赞 */
  liked?: boolean
  /** 当前用户是否已报名 */
  registered?: boolean
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

/** 提醒更新请求（创建者自定义提醒时间/标题/内容，字段可选，非空才更新） */
export interface ReminderUpdateDTO {
  title?: string
  content?: string
  triggerTime?: string
}

/** 提醒创建请求（活动创建者自定义定时提醒） */
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
  introduction?: boolean
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
  /** 面向参与者的活动介绍（AI 生成，可在确认前编辑） */
  introduction?: string
  materials?: MaterialVO[]
  questions?: QuestionDraft[]
  /** 报名问卷封面图 URL（创建者在草稿阶段直接设置） */
  coverImage?: string
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

/** 大屏实时动态项（由 Kafka 消息生成） */
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

/** 活动群聊消息 */
export interface ChatMessageVO {
  id: string
  activityId: string
  senderId: string
  senderName: string
  content: string
  createTime: string
}

/** 日志类型 */
export type LogType = 'operation' | 'login' | 'api' | 'exception' | 'audit'

/** 系统日志行 */
export interface SysLogVO {
  id: string
  logType: LogType
  module?: string
  operation?: string
  method?: string
  requestUri?: string
  /** 请求参数（已脱敏） */
  requestParams?: string
  /** 响应内容（截断） */
  responseBody?: string
  userId?: string
  userName?: string
  ip?: string
  client?: string
  statusCode?: number
  /** 耗时（毫秒） */
  duration?: number
  /** 1 成功 0 失败 */
  success: number
  errorMsg?: string
  createTime: string
}

/** 日志概览统计 */
export interface LogOverviewVO {
  totalCalls: number
  todayCalls: number
  exceptionCount: number
  slowCount: number
  loginFailCount: number
  onlineUsers: number
}

/** 日志分页查询参数 */
export interface LogQuery extends PageQuery {
  logType?: string
  module?: string
  keyword?: string
  /** yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** yyyy-MM-dd HH:mm:ss */
  endTime?: string
}
