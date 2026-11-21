import { request } from './request'
import type { ChatMessageVO } from '@/types'

/** 拉取活动聊天记录（按时间正序） */
export function listChatMessages(activityId: string, limit = 50) {
  return request<ChatMessageVO[]>({
    url: `/activities/${activityId}/chat/messages`,
    method: 'GET',
    params: { limit }
  })
}

/** 发送聊天消息（后端保存后通过 WebSocket 广播） */
export function sendChatMessage(activityId: string, content: string) {
  return request<ChatMessageVO>({
    url: `/activities/${activityId}/chat/messages`,
    method: 'POST',
    data: { content }
  })
}
