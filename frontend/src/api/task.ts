import { request } from './request'
import type { TaskAssignDTO, TaskAssignment } from '@/types'

/** 派发任务 */
export function assignTask(payload: TaskAssignDTO) {
  return request<number>({ url: '/tasks', method: 'POST', data: payload })
}

/** 批量派发任务 */
export function batchAssignTask(activityId: number, tasks: TaskAssignDTO[]) {
  return request<void>({ url: `/tasks/batch?activityId=${activityId}`, method: 'POST', data: tasks })
}

/** 标记任务完成 */
export function completeTask(id: number) {
  return request<void>({ url: `/tasks/${id}/complete`, method: 'PUT' })
}

/** 活动下的任务列表 */
export function listTasks(activityId: number) {
  return request<TaskAssignment[]>({ url: `/tasks?activityId=${activityId}`, method: 'GET' })
}
