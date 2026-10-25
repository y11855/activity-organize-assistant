import { request } from './request'
import type { PageVO, PermissionVO, RoleVO, UserAdminVO } from '@/types'

/** 后台用户分页 */
export function pageAdminUsers(params: {
  keyword?: string
  status?: number
  page?: number
  size?: number
}) {
  return request<PageVO<UserAdminVO>>({ url: '/admin/users', method: 'GET', params })
}

/** 后台新建用户（密码为 RSA 密文） */
export function createAdminUser(payload: {
  username: string
  password: string
  nickname?: string
  roleIds: string[]
  status?: number
}) {
  return request<string>({ url: '/admin/users', method: 'POST', data: payload })
}

/** 分配用户角色（全量替换） */
export function assignUserRoles(id: string, roleIds: string[]) {
  return request<void>({ url: `/admin/users/${id}/roles`, method: 'PUT', data: { roleIds } })
}

/** 启用/禁用账号 */
export function updateUserStatus(id: string, status: number) {
  return request<void>({ url: `/admin/users/${id}/status`, method: 'PUT', data: { status } })
}

/** 重置密码（密码为 RSA 密文） */
export function resetUserPassword(id: string, password: string) {
  return request<void>({ url: `/admin/users/${id}/password`, method: 'PUT', data: { password } })
}

/** 删除用户 */
export function deleteAdminUser(id: string) {
  return request<void>({ url: `/admin/users/${id}`, method: 'DELETE' })
}

/** 角色列表（含权限编码与用户数） */
export function listRoles() {
  return request<RoleVO[]>({ url: '/admin/roles', method: 'GET' })
}

/** 全部权限项 */
export function listPermissions() {
  return request<PermissionVO[]>({ url: '/admin/permissions', method: 'GET' })
}

/** 新建角色 */
export function createRole(payload: { code: string; name: string; description?: string }) {
  return request<string>({ url: '/admin/roles', method: 'POST', data: payload })
}

/** 修改角色（编码不可改） */
export function updateRole(id: string, payload: { code: string; name: string; description?: string }) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'PUT', data: payload })
}

/** 删除角色 */
export function deleteRole(id: string) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'DELETE' })
}

/** 分配角色权限（全量替换） */
export function assignRolePermissions(id: string, permissionCodes: string[]) {
  return request<void>({
    url: `/admin/roles/${id}/permissions`,
    method: 'PUT',
    data: { permissionCodes }
  })
}
