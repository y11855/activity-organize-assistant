import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 登录过期：清空本地凭据并回到登录页
function handleUnauthorized() {
  const userStore = useUserStore()
  userStore.logout()
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

/**
 * 状态码兜底文案：仅在后端未返回 message 时使用。
 * 后端返回的 message 更具体（例如「你不是该活动的负责人，无权管理」），优先展示它。
 */
function errorMessage(status?: number): string {
  switch (status) {
    case 400:
      return '请求参数错误'
    case 401:
      return '未登录或登录已过期'
    case 403:
      return '无权限访问该资源'
    case 404:
      return '请求的资源不存在'
    case 405:
      return '请求方法不支持'
    case 409:
      return '当前状态不允许该操作'
    case 429:
      return '操作过于频繁，请稍后再试'
    case 500:
      return '服务器异常，请稍后重试'
    case 503:
      return '服务暂时不可用，请稍后重试'
    default:
      return '网络异常，请稍后重试'
  }
}

/**
 * 响应拦截器。
 *
 * 后端采用「HTTP 状态码 + 业务码 code」双轨：状态码表达结果类别，code 表达业务细分。
 * 因此成功走 success 分支、失败走 error 分支，两处都需要处理。
 */
service.interceptors.response.use(
  (response) => {
    // 文件下载等二进制响应直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }
    // 204 No Content（删除、置状态等无返回体的成功响应）
    if (response.status === 204 || response.data === '') {
      return undefined
    }
    const { code, message, data } = response.data ?? {}
    // 非统一结构（例如第三方回调）直接透传
    if (code === undefined) {
      return response.data
    }
    if (code === 200) {
      return data ?? response.data
    }
    // 兜底：HTTP 2xx 但业务码非成功（正常不应出现，防御性处理）
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || 'Error'))
  },
  (error) => {
    const status: number | undefined = error.response?.status
    const payload = error.response?.data
    const body = (payload && typeof payload === 'object' ? payload : {}) as {
      code?: number
      message?: string
    }
    const message: string = body.message || errorMessage(status)

    // 401：token 失效。只做跳转，不弹提示，避免页面上叠出多个错误框
    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }

    // 429：限流。后端带 Retry-After 头，提示用户等待秒数
    if (status === 429) {
      const retryAfter = error.response?.headers?.['retry-after']
      ElMessage.warning(
        retryAfter ? `${message}（约 ${retryAfter} 秒后可重试）` : message
      )
      return Promise.reject(error)
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request<unknown, T>(config) as Promise<T>
}

export default service
