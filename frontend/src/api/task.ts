// Axios 接口封装：任务分工的查询与标记完成
import { request } from './request'
import type { MyTaskVO, PageQuery, PageVO } from '@/types'

/** 标记任务完成 */
export function completeTask(id: string) {
  return request<void>({ url: `/tasks/${id}/complete`, method: 'PUT' })
}

/** 分页获取我的任务（仅分配给自己的，支持状态过滤） */
export function listMyTasks(status?: number, params: PageQuery = {}) {
  return request<PageVO<MyTaskVO>>({
    url: '/tasks/mine',
    method: 'GET',
    params: { ...params, status: status ?? undefined }
  })
}
