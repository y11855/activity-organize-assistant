import { request } from './request'
import type {
  ActivityVO,
  ActivitySaveDTO,
  ActivityDraftVO,
  GenerateOptions,
  ActivityEventLogVO,
  PageQuery,
  PageVO
} from '@/types'

/** 创建活动（触发 Agent 全流程） */
export function createActivity(payload: ActivitySaveDTO) {
  return request<string>({ url: '/activities', method: 'POST', data: payload })
}

/** 分页获取我创建的活动列表 */
export function listMyActivities(params: PageQuery = {}) {
  return request<PageVO<ActivityVO>>({ url: '/activities/mine', method: 'GET', params })
}

/** 分页获取我作为参与成员的活动列表 */
export function listMyParticipations(params: PageQuery = {}) {
  return request<PageVO<ActivityVO>>({ url: '/activities/participated', method: 'GET', params })
}

/** 活动详情 */
export function getActivityDetail(id: string) {
  return request<ActivityVO>({ url: `/activities/${id}`, method: 'GET' })
}

/** 取消活动（可选是否删除活动记录；关联问卷自动截止） */
export function cancelActivity(id: string, deleteRecord = false) {
  return request<void>({
    url: `/activities/${id}/cancel?deleteRecord=${deleteRecord}`,
    method: 'PUT'
  })
}

/** 触发 AI 生成活动内容草稿（按勾选生成项） */
export function generateActivityContent(id: string, options: GenerateOptions) {
  return request<void>({ url: `/activities/${id}/generate`, method: 'POST', data: options })
}

/** 获取 AI 生成的内容草稿 */
export function getActivityDraft(id: string) {
  return request<ActivityDraftVO>({ url: `/activities/${id}/draft`, method: 'GET' })
}

/** 保存对 AI 生成内容的编辑 */
export function updateActivityDraft(id: string, draft: ActivityDraftVO) {
  return request<void>({ url: `/activities/${id}/draft`, method: 'PUT', data: draft })
}

/** 确认发布：正式发布任务分工/物料/问卷/提醒 */
export function publishActivity(id: string) {
  return request<void>({ url: `/activities/${id}/publish`, method: 'POST' })
}

/** 生成复盘总结 */
export function generateReview(id: string) {
  return request<string>({ url: `/activities/${id}/review`, method: 'POST' })
}

/** 确认任务分工并通知成员，返回已通知人数 */
export function confirmTasks(id: string) {
  return request<string>({ url: `/activities/${id}/confirm-tasks`, method: 'POST' })
}

/** 活动事件记录列表 */
export function listEventLogs(activityId: string, eventType?: number) {
  const url =
    eventType != null
      ? `/activities/${activityId}/event-logs?eventType=${eventType}`
      : `/activities/${activityId}/event-logs`
  return request<ActivityEventLogVO[]>({ url, method: 'GET' })
}

/** 新增活动事件记录 */
export function addEventLog(activityId: string, payload: { eventType: number; content: string; occurredAt?: string }) {
  return request<string>({ url: `/activities/${activityId}/event-logs`, method: 'POST', data: payload })
}

/** 删除活动事件记录 */
export function deleteEventLog(activityId: string, id: string) {
  return request<void>({ url: `/activities/${activityId}/event-logs/${id}`, method: 'DELETE' })
}
