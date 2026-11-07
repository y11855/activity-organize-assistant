import { defineStore } from 'pinia'
import type { LoginVO, UserInfo } from '@/types'

interface UserState {
  token: string
  userInfo: UserInfo | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    roles: (state) => state.userInfo?.roles ?? [],
    permissions: (state) => state.userInfo?.permissions ?? []
  },
  actions: {
    setToken(token: string) {
      this.token = token
    },
    setUserInfo(info: UserInfo) {
      this.userInfo = info
    },
    /** 登录/注册成功后写入完整用户信息（含角色与权限） */
    setLoginInfo(res: LoginVO) {
      this.token = res.token
      this.userInfo = {
        id: res.id,
        username: res.username,
        nickname: res.nickname,
        email: res.email,
        roles: res.roles ?? [],
        roleNames: res.roleNames ?? [],
        permissions: res.permissions ?? []
      }
    },
    /** 是否拥有指定权限 */
    hasPermission(code: string) {
      return this.permissions.includes(code)
    },
    logout() {
      this.token = ''
      this.userInfo = null
    }
  },
  // 会话存 sessionStorage：每个标签页一份登录态，同窗口多标签可分别登录不同账号
  persist: {
    key: 'activity-assistant-user',
    storage: sessionStorage,
    paths: ['token', 'userInfo']
  }
})
