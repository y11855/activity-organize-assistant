import { request } from './request'
import type { DashboardOverviewVO } from '@/types'

/** 数据大屏概览（仅当前登录账号相关数据） */
export function getDashboardOverview() {
  return request<DashboardOverviewVO>({ url: '/dashboard/overview', method: 'GET' })
}
