// Axios 接口封装：用户行为采集（停留时长 / 点赞），后端转投 Kafka 由 Flink 计算推荐
import { request } from './request'

/** 上报用户行为 */
export function reportBehavior(payload: {
  questionnaireId: string
  type: 'view' | 'like'
  durationSeconds?: number
}) {
  return request<void>({ url: '/behaviors', method: 'POST', data: payload })
}
