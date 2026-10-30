import { useUserStore } from '@/stores'
import { downloadBlob } from '@/utils'
import { getChunkStatus, initChunkUpload, mergeChunks, uploadChunk } from '@/api/file'
import type { ActivityFileVO, ChunkInitVO, ChunkStatusVO } from '@/types'

/** 超过该大小走分片上传，否则一次直传 */
export const CHUNK_THRESHOLD = 5 * 1024 * 1024
/** 单个分片失败后的重试次数 */
const MAX_CHUNK_ATTEMPTS = 3

/** 用户主动暂停（不是失败），页面据此把任务标为「已暂停」 */
export class UploadPausedError extends Error {
  constructor() {
    super('上传已暂停')
    this.name = 'UploadPausedError'
  }
}

/** 计算 SHA-256（十六进制），用于分片与整体校验；浏览器原生实现，无需额外依赖 */
export async function sha256Hex(data: BufferSource): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', data)
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

export interface ChunkProgress {
  percent: number
  uploadedChunks: number
  totalChunks: number
}

export interface ChunkUploadOptions {
  activityId: string
  signal: AbortSignal
  /** 续传时传入已有会话 ID，跳过 init 直接查已传分片 */
  uploadId?: string
  /** 自定义文件名称与说明（详情页上传时填写） */
  name?: string
  description?: string
  /** 演示用：让指定分片第一次上传时数据损坏，触发服务端校验失败与自动重传 */
  corruptIndex?: number
  onInit?: (info: ChunkInitVO) => void
  onProgress?: (progress: ChunkProgress) => void
  /** 某个分片失败（success=false）或重传成功（success=true） */
  onRetry?: (index: number, success: boolean) => void
  onPhase?: (phase: 'uploading' | 'merging' | 'paused') => void
}

/**
 * 分片上传：init → 查询已传分片 → 逐片上传（服务端 SHA-256 校验）→ 合并。
 * 单文件内串行上传，便于展示「第 N / M 片」进度；批量上传的并发由页面队列控制。
 */
export async function uploadChunked(file: File, options: ChunkUploadOptions): Promise<ActivityFileVO> {
  let init: ChunkInitVO
  let status: ChunkStatusVO
  if (options.uploadId) {
    status = await getChunkStatus(options.uploadId)
    init = {
      uploadId: status.uploadId,
      // 后端 Long 序列化为字符串，转成数字后再参与分片计算
      chunkSize: Number(status.chunkSize),
      totalChunks: status.totalChunks
    }
  } else {
    init = await initChunkUpload({
      activityId: options.activityId,
      fileName: options.name || file.name,
      fileSize: file.size,
      description: options.description
    })
    init = { ...init, chunkSize: Number(init.chunkSize) }
    status = await getChunkStatus(init.uploadId)
  }
  options.onInit?.(init)
  // 服务端已合并完成：直接取回记录，避免重复上传全部分片
  if (status.merged) {
    return mergeChunks(init.uploadId)
  }

  const done = new Set(status.uploadedChunks)
  let loadedBytes = 0
  done.forEach((index) => {
    loadedBytes += chunkLength(file, init, index)
  })

  const report = (inflight = 0) => {
    const percent = file.size ? Math.min(100, Math.round(((loadedBytes + inflight) / file.size) * 100)) : 0
    options.onProgress?.({ percent, uploadedChunks: done.size, totalChunks: init.totalChunks })
  }
  report()
  options.onPhase?.('uploading')

  for (let index = 0; index < init.totalChunks; index++) {
    if (options.signal.aborted) {
      throw new UploadPausedError()
    }
    if (done.has(index)) {
      continue
    }
    const chunk = chunkOf(file, init, index)
    const buffer = await chunk.arrayBuffer()
    const hash = await sha256Hex(buffer)
    let attempts = 0
    while (true) {
      if (options.signal.aborted) {
        throw new UploadPausedError()
      }
      // 只有第一次尝试注入坏数据，重传时使用原始分片
      const payload = attempts === 0 && options.corruptIndex === index ? damage(buffer) : chunk
      try {
        await uploadChunk(init.uploadId, index, payload, hash, (p) => report((p / 100) * chunk.size), options.signal)
        if (attempts > 0) {
          options.onRetry?.(index, true)
        }
        done.add(index)
        loadedBytes += chunk.size
        report()
        break
      } catch (e) {
        if (options.signal.aborted) {
          throw new UploadPausedError()
        }
        attempts++
        if (attempts >= MAX_CHUNK_ATTEMPTS) {
          throw e
        }
        options.onRetry?.(index, false)
        await sleep(300)
      }
    }
  }

  options.onPhase?.('merging')
  return mergeChunks(init.uploadId)
}

function chunkLength(file: File, init: ChunkInitVO, index: number) {
  return Math.min(init.chunkSize, file.size - index * init.chunkSize)
}

function chunkOf(file: File, init: ChunkInitVO, index: number): Blob {
  const start = index * init.chunkSize
  return file.slice(start, start + chunkLength(file, init, index))
}

/** 翻转分片首字节，模拟传输损坏 */
function damage(buffer: ArrayBuffer): Blob {
  const bytes = new Uint8Array(buffer.slice(0))
  if (bytes.length) {
    bytes[0] ^= 0xff
  }
  return new Blob([bytes])
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * 可暂停/可续传的下载器：用 fetch + Range 读取响应流。
 * 暂停时保留已下载数据，再次调用 start() 会带 Range 从断点继续。
 */
export class ResumableDownload {
  private chunks: Uint8Array[] = []
  private loaded = 0
  private total = 0
  private controller: AbortController | null = null

  constructor(
    private readonly url: string,
    private readonly fileName: string,
    private readonly onProgress: (loaded: number, total: number) => void
  ) {}

  pause() {
    this.controller?.abort()
  }

  /** 返回 true 表示下载完成，false 表示被暂停 */
  async start(): Promise<boolean> {
    while (true) {
      this.controller = new AbortController()
      const headers: Record<string, string> = {}
      const token = useUserStore().token
      if (token) {
        headers.Authorization = `Bearer ${token}`
      }
      if (this.loaded > 0) {
        headers.Range = `bytes=${this.loaded}-`
      }

      let response: Response
      try {
        response = await fetch(this.url, { headers, signal: this.controller.signal })
      } catch {
        if (this.controller.signal.aborted) {
          return false
        }
        throw new Error('下载失败，请检查网络')
      }
      if (!response.ok) {
        throw new Error(`下载失败（HTTP ${response.status}）`)
      }
      // 服务端不支持 Range 时返回 200 全量数据，此时必须清空已下载内容重新接收
      if (response.status === 200 && this.loaded > 0) {
        this.chunks = []
        this.loaded = 0
      }
      this.total = parseTotal(response, this.total, this.loaded)
      this.onProgress(this.loaded, this.total)

      const before = this.loaded
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('浏览器不支持流式下载')
      }
      try {
        while (true) {
          const { done, value } = await reader.read()
          if (done) {
            break
          }
          if (value) {
            this.chunks.push(value)
            this.loaded += value.length
            this.onProgress(this.loaded, this.total)
          }
        }
      } catch {
        if (this.controller.signal.aborted) {
          return false
        }
        throw new Error('下载中断，请重试')
      }
      if (this.total <= 0) {
        // 服务端未返回 Content-Length（如边压缩边输出的 zip），无法得知总量也不支持 Range 续传，
        // 响应流读完即视为下载完成，避免重复请求陷入死循环
        break
      }
      if (this.loaded >= this.total) {
        break
      }
      if (this.loaded === before) {
        break
      }
      // 服务端提前结束但未传完：循环继续，下一次请求带 Range 续传
    }

    downloadBlob(new Blob(this.chunks as BlobPart[]), this.fileName)
    return true
  }
}

function parseTotal(response: Response, fallback: number, loaded: number): number {
  const contentRange = response.headers.get('Content-Range')
  if (contentRange && contentRange.includes('/')) {
    const total = Number(contentRange.split('/')[1])
    if (!Number.isNaN(total)) {
      return total
    }
  }
  const contentLength = Number(response.headers.get('Content-Length'))
  if (!Number.isNaN(contentLength) && contentLength > 0) {
    return response.status === 200 ? contentLength : loaded + contentLength
  }
  return fallback
}
