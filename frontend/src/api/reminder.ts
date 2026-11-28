// Axios 接口封装：定时提醒的创建、查询、更新与取消
import { request } from './request'
import type { ReminderVO, ReminderCreateDTO, ReminderUpdateDTO } from '@/types'

/** 创建定时提醒（活动创建者自定义提醒对象与时间） */
export function createReminder(dto: ReminderCreateDTO) {
  return request<string>({ url: '/reminders', method: 'POST', data: dto })
}

/** 发给我的提醒列表 */
export function listMyReminders() {
  return request<ReminderVO[]>({ url: '/reminders/my', method: 'GET' })
}

/** 某活动下的提醒列表（创建者/负责人查看与管理） */
export function listReminders(activityId: string) {
  return request<ReminderVO[]>({ url: `/reminders?activityId=${activityId}`, method: 'GET' })
}

/** 更新提醒（标题/内容/触发时间），创建者自定义提醒时间 */
export function updateReminder(id: string, dto: ReminderUpdateDTO) {
  return request<void>({ url: `/reminders/${id}`, method: 'PUT', data: dto })
}

/** 取消提醒 */
export function cancelReminder(id: string) {
  return request<void>({ url: `/reminders/${id}`, method: 'DELETE' })
}
