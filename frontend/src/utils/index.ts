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

type DebouncedFn<T extends (...args: any[]) => any> = ((...args: Parameters<T>) => void) & {
  cancel: () => void
}

/** 事件防抖：短时间内重复触发只执行最后一次 */
export function debounce<T extends (...args: any[]) => any>(fn: T, wait = 300): DebouncedFn<T> {
  let timer: ReturnType<typeof setTimeout> | null = null

  const debounced = ((...args: Parameters<T>) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn(...args)
      timer = null
    }, wait)
  }) as DebouncedFn<T>

  debounced.cancel = () => {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  return debounced
}
