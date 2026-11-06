import { request } from './request'
import type { LoginVO } from '@/types'

/** 密码登录 */
export function login(payload: { username: string; password: string; captchaKey?: string; captchaCode?: string }) {
  return request<LoginVO>({ url: '/auth/login', method: 'POST', data: payload })
}

/** 验证码登录 */
export function loginByCode(payload: { username: string; code: string }) {
  return request<LoginVO>({ url: '/auth/login-by-code', method: 'POST', data: payload })
}

/** 获取图形验证码 */
export function getCaptcha() {
  return request<{ key: string; imageBase64: string }>({ url: '/auth/captcha', method: 'GET' })
}

/** 发送短信验证码 */
export function sendCode(username: string) {
  return request<{ code: string }>({ url: '/auth/send-code', method: 'POST', data: { username } })
}

/** 注册（密码为 RSA 密文） */
export function register(payload: { username: string; password: string; nickname?: string }) {
  return request<LoginVO>({ url: '/auth/register', method: 'POST', data: payload })
}

/** 登出 */
export function logout() {
  return request<void>({ url: '/auth/logout', method: 'POST' })
}

/** 当前登录用户 */
export function getCurrentUser() {
  return request<LoginVO>({ url: '/auth/current', method: 'GET' })
}
