import { request } from './request'
import type {
  QuestionnaireVO,
  QuestionnaireResponseVO,
  ResponseStatisticsVO,
  PageQuery,
  PageVO
} from '@/types'

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
  return request<QuestionnaireVO[]>({ url: `/questionnaires?activityId=${activityId}`, method: 'GET' })
}

/** 分页获取我的问卷列表（含封面、报名数） */
export function listMyQuestionnaires(params: PageQuery = {}) {
  return request<PageVO<QuestionnaireVO>>({ url: '/questionnaires/mine', method: 'GET', params })
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

/** 报名人员明细列表 */
export function listResponses(id: string, status?: number) {
  const url = status != null ? `/questionnaires/${id}/responses?status=${status}` : `/questionnaires/${id}/responses`
  return request<QuestionnaireResponseVO[]>({ url, method: 'GET' })
}

/** 报名统计看板 */
export function getStatistics(id: string) {
  return request<ResponseStatisticsVO>({ url: `/questionnaires/${id}/statistics`, method: 'GET' })
}

/** 导出报名人员 Excel */
export function exportResponses(id: string) {
  return request<Blob>({
    url: `/questionnaires/${id}/responses/export`,
    method: 'GET',
    responseType: 'blob'
  })
}
