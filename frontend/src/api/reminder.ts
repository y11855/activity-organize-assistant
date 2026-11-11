import { request } from './request'
import type { ReminderVO, ReminderCreateDTO } from '@/types'

/** 发给我的提醒列表 */
export function listMyReminders() {
  return request<ReminderVO[]>({ url: '/reminders/my', method: 'GET' })
}

/** 活动下的提醒列表 */
export function listRemindersByActivity(activityId: string) {
  return request<ReminderVO[]>({ url: `/reminders?activityId=${activityId}`, method: 'GET' })
}

/** 创建提醒 */
export function createReminder(data: ReminderCreateDTO) {
  return request<string>({ url: '/reminders', method: 'POST', data })
}

/** 取消提醒 */
export function cancelReminder(id: string) {
  return request<void>({ url: `/reminders/${id}`, method: 'DELETE' })
}
