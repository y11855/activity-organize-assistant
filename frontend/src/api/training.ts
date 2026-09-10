import { request } from './request'
import type { TrainingVO } from '@/types'

/** 提交实训成果 */
export function submitTraining(payload: { title: string; fileUrl: string }) {
  return request<number>({ url: '/trainings', method: 'POST', data: payload })
}

/** 获取实训详情 */
export function getTrainingDetail(id: number) {
  return request<TrainingVO>({ url: `/trainings/${id}`, method: 'GET' })
}

/** 触发 AI 校验 */
export function evaluateTraining(id: number) {
  return request<void>({ url: `/trainings/${id}/evaluate`, method: 'POST' })
}
