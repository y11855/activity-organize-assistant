import { request } from './request'
import type { ReminderVO, ReminderCreateDTO } from '@/types'

/** 创建提醒 */
export function createReminder(payload: ReminderCreateDTO) {
  return request<number>({ url: '/reminders', method: 'POST', data: payload })
}

/** 取消提醒 */
export function cancelReminder(id: number) {
  return request<void>({ url: `/reminders/${id}`, method: 'DELETE' })
}

/** 活动下的提醒列表 */
export function listReminders(activityId: number) {
  return request<ReminderVO[]>({ url: `/reminders?activityId=${activityId}`, method: 'GET' })
}
