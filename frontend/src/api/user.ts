// Axios 接口封装：用户列表等用户相关信息请求
import { request } from './request'
import type { UserSimpleVO } from '@/types'

/** 全部用户（成员选择器） */
export function listUsers() {
  return request<UserSimpleVO[]>({ url: '/users', method: 'GET' })
}
