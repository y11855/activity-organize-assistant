import { request } from './request'
import type { ActivityVO, ActivitySaveDTO, ActivityDraftVO, GenerateOptions } from '@/types'

/** 活动分解树节点 */
export interface BreakdownNode {
  id: string
  name: string
  type: 'activity' | 'category' | 'task' | 'material' | 'questionnaire' | 'reminder'
  status: number | null
  meta: Record<string, any> | null
  children: BreakdownNode[]
}

/** 创建活动（触发 Agent 全流程） */
export function createActivity(payload: ActivitySaveDTO) {
  return request<string>({ url: '/activities', method: 'POST', data: payload })
}

/** 我创建的活动列表 */
export function listMyActivities() {
  return request<ActivityVO[]>({ url: '/activities/mine', method: 'GET' })
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

/** 活动分解树（活动→任务/物料/问卷/提醒） */
export function getActivityBreakdownTree() {
  return request<BreakdownNode[]>({ url: '/activities/breakdown-tree', method: 'GET' })
}

/** 确认任务分工并通知成员，返回已通知人数 */
export function confirmTasks(id: string) {
  return request<string>({ url: `/activities/${id}/confirm-tasks`, method: 'POST' })
}
