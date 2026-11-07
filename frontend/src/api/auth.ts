import { request } from './request'
import type { LoginVO } from '@/types'

/** 密码登录 */
export function login(payload: { username: string; password: string; captchaKey?: string; captchaCode?: string }) {
  return request<LoginVO>({ url: '/auth/login', method: 'POST', data: payload })
}

/** 发送登录邮箱验证码 */
export function sendEmailCode(email: string) {
  return request<void>({ url: '/auth/email-code', method: 'POST', data: { email } })
}

/** 邮箱验证码登录 */
export function loginByEmailCode(payload: { email: string; code: string }) {
  return request<LoginVO>({ url: '/auth/login-by-email-code', method: 'POST', data: payload })
}

/** 发送绑定邮箱验证码（需登录） */
export function sendBindEmailCode(email: string) {
  return request<void>({ url: '/auth/bind-email-code', method: 'POST', data: { email } })
}

/** 绑定邮箱（需登录） */
export function bindEmail(payload: { email: string; code: string }) {
  return request<LoginVO>({ url: '/auth/bind-email', method: 'POST', data: payload })
}

/** 获取图形验证码 */
export function getCaptcha() {
  return request<{ key: string; imageBase64: string }>({ url: '/auth/captcha', method: 'GET' })
}

/** 注册（密码为 RSA 密文，邮箱可选） */
export function register(payload: { username: string; password: string; nickname?: string; email?: string }) {
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
