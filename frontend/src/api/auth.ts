import { request } from './request'
import type { LoginVO } from '@/types'

/** 登录 */
export function login(payload: { username: string; password: string }) {
  return request<LoginVO>({ url: '/auth/login', method: 'POST', data: payload })
}

/** 登出 */
export function logout() {
  return request<void>({ url: '/auth/logout', method: 'POST' })
}

/** 当前登录用户 */
export function getCurrentUser() {
  return request<LoginVO>({ url: '/auth/current', method: 'GET' })
}
