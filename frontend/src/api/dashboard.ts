// Axios 接口封装：数据大屏概览统计请求
import { request } from './request'
import type { DashboardOverviewVO } from '@/types'

/** 数据大屏概览（仅当前登录账号相关数据） */
export function getDashboardOverview() {
  return request<DashboardOverviewVO>({ url: '/dashboard/overview', method: 'GET' })
}
