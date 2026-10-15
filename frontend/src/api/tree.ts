import { request } from './request'

/** 树节点 */
export interface TreeNode {
  id: string
  parentId: string
  name: string
  type: string
  sort: number
  description: string
  createTime: string
}

/** 树节点（带 children） */
export interface TreeNodeVO extends TreeNode {
  children: TreeNodeVO[]
}

/** 新增/更新节点请求 */
export interface TreeNodeForm {
  parentId?: string
  name: string
  type?: string
  sort?: number
  description?: string
}

/** 获取完整树结构 */
export function getTree() {
  return request<TreeNodeVO[]>({ url: '/tree', method: 'GET' })
}

/** 获取节点详情 */
export function getTreeNode(id: string) {
  return request<TreeNode>({ url: `/tree/${id}`, method: 'GET' })
}

/** 新增节点 */
export function createTreeNode(data: TreeNodeForm) {
  return request<string>({ url: '/tree', method: 'POST', data })
}

/** 更新节点 */
export function updateTreeNode(id: string, data: TreeNodeForm) {
  return request<void>({ url: `/tree/${id}`, method: 'PUT', data })
}

/** 删除节点（含子孙） */
export function deleteTreeNode(id: string) {
  return request<void>({ url: `/tree/${id}`, method: 'DELETE' })
}

/** 移动节点（拖拽调整层级） */
export function moveTreeNode(id: string, parentId: string, sort?: number) {
  const qs = sort != null ? `&sort=${sort}` : ''
  return request<void>({ url: `/tree/${id}/move?parentId=${parentId}${qs}`, method: 'PUT' })
}
