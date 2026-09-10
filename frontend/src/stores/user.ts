import { defineStore } from 'pinia'
import type { UserInfo } from '@/types'

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
    roles: (state) => state.userInfo?.roles ?? []
  },
  actions: {
    setToken(token: string) {
      this.token = token
    },
    setUserInfo(info: UserInfo) {
      this.userInfo = info
    },
    logout() {
      this.token = ''
      this.userInfo = null
    }
  },
  persist: {
    key: 'training-eval-user',
    paths: ['token', 'userInfo']
  }
})
