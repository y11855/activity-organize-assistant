/** 用户信息 */
export interface UserInfo {
  id: string
  username: string
  nickname: string
  roles: string[]
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

/** 任务实体 */
export interface TaskAssignment {
  id: string
  activityid: string
  title: string
  description?: string
  assigneeId?: string
  assigneeName?: string
  deadline?: string
  status: number
  createTime: string
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

/** 用户（成员选择器） */
export interface UserSimpleVO {
  id: string
  username: string
  nickname: string
  role: string
}

/** 登录响应 */
export interface LoginVO {
  token: string
  id: string
  username: string
  nickname: string
  role: string
}

/** 提醒创建请求 */
export interface ReminderCreateDTO {
  activityId: string
  title: string
  content?: string
  targetUserId?: string
  targetUserName?: string
  triggerTime: string
}

/** 提醒 */
export interface ReminderVO {
  id: string
  activityId: string
  title: string
  content?: string
  targetUserName?: string
  triggerTime: string
  status: number
  createTime: string
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
