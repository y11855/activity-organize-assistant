import { request } from './request'
import type { ActivityVO, ActivitySaveDTO } from '@/types'

/** 创建活动（触发 Agent 全流程） */
export function createActivity(payload: ActivitySaveDTO) {
  return request<number>({ url: '/activities', method: 'POST', data: payload })
}

/** 我创建的活动列表 */
export function listMyActivities() {
  return request<ActivityVO[]>({ url: '/activities/mine', method: 'GET' })
}

/** 活动详情 */
export function getActivityDetail(id: number) {
  return request<ActivityVO>({ url: `/activities/${id}`, method: 'GET' })
}

/** 取消活动 */
export function cancelActivity(id: number) {
  return request<void>({ url: `/activities/${id}/cancel`, method: 'PUT' })
}

/** 生成复盘总结 */
export function generateReview(id: number) {
  return request<string>({ url: `/activities/${id}/review`, method: 'POST' })
}
