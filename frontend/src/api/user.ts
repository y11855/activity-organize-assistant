import { request } from './request'
import type { UserSimpleVO } from '@/types'

/** 全部用户（成员选择器） */
export function listUsers() {
  return request<UserSimpleVO[]>({ url: '/users', method: 'GET' })
}
