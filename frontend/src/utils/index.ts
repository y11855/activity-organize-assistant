import dayjs from 'dayjs'

/** 时间格式化 */
export function formatDate(date: string | Date, format = 'YYYY-MM-DD HH:mm:ss') {
  return dayjs(date).format(format)
}

/** 下载 Blob 文件 */
export function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}
