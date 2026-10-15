import service from './request'

/** 上传图片（jpg/png/webp，≤5MB），返回可访问 URL */
export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return service.post<unknown, { url: string }>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
