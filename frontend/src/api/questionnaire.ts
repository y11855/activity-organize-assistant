import { request } from './request'
import type { QuestionnaireVO } from '@/types'

/** 为活动生成问卷 */
export function generateQuestionnaire(activityId: string) {
  return request<string>({ url: `/questionnaires/generate?activityId=${activityId}`, method: 'POST' })
}

/** 获取活动问卷详情 */
export function getQuestionnaire(activityId: string) {
  return request<QuestionnaireVO>({ url: `/questionnaires?activityId=${activityId}`, method: 'GET' })
}

/** 发布问卷 */
export function publishQuestionnaire(id: string) {
  return request<void>({ url: `/questionnaires/${id}/publish`, method: 'PUT' })
}

/** 截止问卷 */
export function closeQuestionnaire(id: string) {
  return request<void>({ url: `/questionnaires/${id}/close`, method: 'PUT' })
}

/** 活动问卷列表 */
export function listQuestionnaires(activityId: string) {
  return request<QuestionnaireVO[]>({ url: `/questionnaires/list?activityId=${activityId}`, method: 'GET' })
}

/** 我的问卷列表（含封面、报名数） */
export function listMyQuestionnaires() {
  return request<QuestionnaireVO[]>({ url: '/questionnaires/mine', method: 'GET' })
}

/** 问卷详情（填写页） */
export function getQuestionnaireById(id: string) {
  return request<QuestionnaireVO>({ url: `/questionnaires/${id}`, method: 'GET' })
}

/** 编辑问卷（含封面） */
export function updateQuestionnaire(
  id: string,
  payload: { title: string; description?: string; coverImage?: string; questions?: string }
) {
  return request<void>({ url: `/questionnaires/${id}`, method: 'PUT', data: payload })
}

/** 提交答卷 */
export function submitResponse(id: string, answer: string) {
  return request<void>({ url: `/questionnaires/${id}/responses`, method: 'POST', data: { answer } })
}
