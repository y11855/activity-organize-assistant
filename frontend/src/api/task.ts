import { request } from './request'
import type { MyTaskVO, TaskAssignDTO, TaskAssignment } from '@/types'

/** 派发任务 */
export function assignTask(payload: TaskAssignDTO) {
  return request<string>({ url: '/tasks', method: 'POST', data: payload })
}

/** 批量派发任务 */
export function batchAssignTask(activityId: string, tasks: TaskAssignDTO[]) {
  return request<void>({ url: `/tasks/batch?activityId=${activityId}`, method: 'POST', data: tasks })
}

/** 标记任务完成 */
export function completeTask(id: string) {
  return request<void>({ url: `/tasks/${id}/complete`, method: 'PUT' })
}

/** 活动下的任务列表 */
export function listTasks(activityId: string) {
  return request<TaskAssignment[]>({ url: `/tasks?activityId=${activityId}`, method: 'GET' })
}

/** 我的任务（仅分配给自己的，支持状态过滤） */
export function listMyTasks(status?: number) {
  const query = status === undefined || status === null ? '' : `?status=${status}`
  return request<MyTaskVO[]>({ url: `/tasks/mine${query}`, method: 'GET' })
}
