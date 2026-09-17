import { request } from './request'
import type { QuestionnaireVO } from '@/types'

/** 为活动生成问卷 */
export function generateQuestionnaire(activityId: number) {
  return request<number>({ url: `/questionnaires/generate?activityId=${activityId}`, method: 'POST' })
}

/** 获取活动问卷详情 */
export function getQuestionnaire(activityId: number) {
  return request<QuestionnaireVO>({ url: `/questionnaires?activityId=${activityId}`, method: 'GET' })
}

/** 发布问卷 */
export function publishQuestionnaire(id: number) {
  return request<void>({ url: `/questionnaires/${id}/publish`, method: 'PUT' })
}

/** 截止问卷 */
export function closeQuestionnaire(id: number) {
  return request<void>({ url: `/questionnaires/${id}/close`, method: 'PUT' })
}

/** 活动问卷列表 */
export function listQuestionnaires(activityId: number) {
  return request<QuestionnaireVO[]>({ url: `/questionnaires/list?activityId=${activityId}`, method: 'GET' })
}
