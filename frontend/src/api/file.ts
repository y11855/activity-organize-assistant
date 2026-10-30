import type { AxiosProgressEvent } from 'axios'
import service from './request'
import type { ActivityFileVO, ChunkInitVO, ChunkStatusVO, FileUploadVO } from '@/types'

/**
 * 单个小文件直传（≤5MB）。
 * 传活动资料时带 activityId；问卷封面等临时图片可不带，只返回可访问 URL。
 */
export function uploadSmallFile(
  file: File,
  activityId?: string,
  onProgress?: (percent: number) => void,
  signal?: AbortSignal
) {
  const formData = new FormData()
  formData.append('file', file)
  return service.post<unknown, FileUploadVO>('/files/upload', formData, {
    params: activityId ? { activityId } : undefined,
    headers: { 'Content-Type': 'multipart/form-data' },
    signal,
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (event.total) {
        onProgress?.(Math.round((event.loaded / event.total) * 100))
      }
    }
  })
}

/** 兼容旧调用（问卷封面）：只关心返回的 url */
export function uploadFile(file: File) {
  return uploadSmallFile(file)
}

/**
 * 上传活动文件：可自定义文件名称与说明（详情页「活动文件」板块使用）。
 * 请求参数与直传接口保持一致，仅在表单中额外带上 name / description。
 */
export function uploadNamedFile(payload: {
  file: File
  activityId: string
  name?: string
  description?: string
  onProgress?: (percent: number) => void
  signal?: AbortSignal
}) {
  const formData = new FormData()
  formData.append('file', payload.file)
  return service.post<unknown, FileUploadVO>('/files/upload', formData, {
    params: {
      activityId: payload.activityId,
      name: payload.name || undefined,
      description: payload.description || undefined
    },
    headers: { 'Content-Type': 'multipart/form-data' },
    signal: payload.signal,
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (event.total) {
        payload.onProgress?.(Math.round((event.loaded / event.total) * 100))
      }
    }
  })
}

/** 初始化分片上传会话 */
export function initChunkUpload(payload: {
  activityId: string
  fileName: string
  fileSize: number
  description?: string
}) {
  return service.post<unknown, ChunkInitVO>('/files/chunks', payload)
}

/** 查询已上传分片（断点续传） */
export function getChunkStatus(uploadId: string) {
  return service.get<unknown, ChunkStatusVO>(`/files/chunks/${uploadId}`)
}

/** 上传单个分片：sha256 由客户端计算，服务端校验不通过时会要求重传 */
export function uploadChunk(
  uploadId: string,
  index: number,
  chunk: Blob,
  sha256: string,
  onProgress?: (percent: number) => void,
  signal?: AbortSignal
) {
  const formData = new FormData()
  formData.append('chunk', chunk)
  return service.put<unknown, void>(`/files/chunks/${uploadId}/${index}`, formData, {
    params: { sha256 },
    headers: { 'Content-Type': 'multipart/form-data' },
    signal,
    onUploadProgress: (event: AxiosProgressEvent) => {
      if (event.total) {
        onProgress?.(Math.round((event.loaded / event.total) * 100))
      }
    }
  })
}

/** 合并分片（幂等） */
export function mergeChunks(uploadId: string) {
  return service.post<unknown, ActivityFileVO>(`/files/chunks/${uploadId}/merge`)
}

/** 活动资料列表 */
export function listActivityFiles(activityId: string) {
  return service.get<unknown, ActivityFileVO[]>('/files', { params: { activityId } })
}

/** 删除资料文件 */
export function deleteActivityFile(id: string) {
  return service.delete<unknown, void>(`/files/${id}`)
}

/** 单文件下载地址（支持 Range） */
export function fileDownloadUrl(id: string) {
  return `/api/files/${id}/download`
}

/** 整包下载地址（边压缩边输出） */
export function packageDownloadUrl(activityId: string) {
  return `/api/files/package?activityId=${activityId}`
}
