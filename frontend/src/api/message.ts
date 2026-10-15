import { request } from './request'
import type { MessageVO } from '@/types'

/** 我的消息列表 */
export function listMyMessages() {
  return request<MessageVO[]>({ url: '/messages/mine', method: 'GET' })
}

/** 未读消息数 */
export function getUnreadCount() {
  return request<string>({ url: '/messages/unread-count', method: 'GET' })
}

/** 标记已读 */
export function markMessageRead(id: string) {
  return request<void>({ url: `/messages/${id}/read`, method: 'PUT' })
}

/** 全部已读 */
export function markAllMessagesRead() {
  return request<void>({ url: '/messages/read-all', method: 'PUT' })
}
