/** 用户信息 */
export interface UserInfo {
  id: number
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
}

/** 物料项 */
export interface MaterialVO {
  id: number
  name: string
  quantity: number
  unit: string
  unitPrice?: number
  remark?: string
}

/** 任务 */
export interface TaskVO {
  id: number
  title: string
  description?: string
  assigneeId?: number
  assigneeName?: string
  deadline?: string
  status: number
}

/** 活动详情 */
export interface ActivityVO {
  id: number
  creatorId: number
  title: string
  description?: string
  startTime?: string
  endTime?: string
  location?: string
  status: number
  planContent?: string
  reviewContent?: string
  materials?: MaterialVO[]
  tasks?: TaskVO[]
  createTime: string
}

/** 任务派发请求 */
export interface TaskAssignDTO {
  activityId?: number
  title: string
  description?: string
  assigneeId?: number
  assigneeName?: string
  deadline?: string
}

/** 任务实体 */
export interface TaskAssignment {
  id: number
  activityId: number
  title: string
  description?: string
  assigneeId?: number
  assigneeName?: string
  deadline?: string
  status: number
  createTime: string
}

/** 问卷 */
export interface QuestionnaireVO {
  id: number
  activityId: number
  title: string
  description?: string
  questions?: string
  status: number
  responseCount?: number
  createTime: string
}

/** 提醒创建请求 */
export interface ReminderCreateDTO {
  activityId: number
  title: string
  content?: string
  targetUserId?: number
  targetUserName?: string
  triggerTime: string
}

/** 提醒 */
export interface ReminderVO {
  id: number
  activityId: number
  title: string
  content?: string
  targetUserName?: string
  triggerTime: string
  status: number
  createTime: string
}
