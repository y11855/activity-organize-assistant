import { computed } from 'vue'
import { useUserStore } from '@/stores'

/**
 * 认证相关组合式函数
 */
export function useAuth() {
  const userStore = useUserStore()
  const isLoggedIn = computed(() => !!userStore.token)
  const userInfo = computed(() => userStore.userInfo)

  function logout() {
    userStore.logout()
  }

  return { isLoggedIn, userInfo, logout }
}
