// Axios 接口封装：日志中心（概览统计、日志分页、模块筛选与清空）
import { request } from './request'
import type { LogOverviewVO, LogQuery, PageVO, SysLogVO } from '@/types'

/** 日志分页查询 */
export function pageLogs(params: LogQuery) {
  return request<PageVO<SysLogVO>>({ url: '/logs/page', method: 'GET', params })
}

/** 日志概览统计 */
export function getLogOverview() {
  return request<LogOverviewVO>({ url: '/logs/overview', method: 'GET' })
}

/** 已产生日志的业务模块（筛选项） */
export function listLogModules() {
  return request<string[]>({ url: '/logs/modules', method: 'GET' })
}

/** 清空全部日志 */
export function clearLogs() {
  return request<void>({ url: '/logs', method: 'DELETE' })
}
