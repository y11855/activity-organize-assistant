// Axios 接口封装：定时提醒的查询与取消
import { request } from './request'
import type { ReminderVO } from '@/types'

/** 发给我的提醒列表 */
export function listMyReminders() {
  return request<ReminderVO[]>({ url: '/reminders/my', method: 'GET' })
}

/** 取消提醒 */
export function cancelReminder(id: string) {
  return request<void>({ url: `/reminders/${id}`, method: 'DELETE' })
}
