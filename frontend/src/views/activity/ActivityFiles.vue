<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteActivityFile,
  fileDownloadUrl,
  getChunkStatus,
  listActivityFiles,
  packageDownloadUrl,
  uploadSmallFile
} from '@/api/file'
import { CHUNK_THRESHOLD, ResumableDownload, UploadPausedError, uploadChunked } from '@/utils/fileTransfer'
import type { ActivityFileVO } from '@/types'
import { formatDate } from '@/utils'

const route = useRoute()
const router = useRouter()
const activityId = String(route.params.id)

/** 分片上传允许的最大文件（与后端 app.max-file-size 一致） */
const MAX_FILE_SIZE = 1024 * 1024 * 1024
/** 批量上传逐个进行：每成功传完一个文件，总进度前进 1/N，进度条节奏更直观 */
const MAX_CONCURRENT = 1

/* ===================== 资料列表 ===================== */
const files = ref<ActivityFileVO[]>([])
const loading = ref(false)

async function loadFiles() {
  loading.value = true
  try {
    files.value = await listActivityFiles(activityId)
  } finally {
    loading.value = false
  }
}

async function removeFile(file: ActivityFileVO) {
  try {
    await ElMessageBox.confirm(`确认删除「${file.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteActivityFile(file.id)
    ElMessage.success('已删除')
    await loadFiles()
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

/* ===================== 上传任务 ===================== */
type TaskStatus = 'uploading' | 'paused' | 'merging' | 'done' | 'error'

interface UploadTask {
  key: string
  name: string
  size: number
  percent: number
  status: TaskStatus
  tip: string
  chunkText: string
  chunked: boolean
  file?: File
  uploadId?: string
  totalChunks?: number
  uploadedChunks?: number
  controller?: AbortController
}

const tasks = ref<UploadTask[]>([])
const demoCorrupt = ref(false)

const statusText: Record<TaskStatus, string> = {
  uploading: '上传中',
  paused: '已暂停',
  merging: '合并中',
  done: '已完成',
  error: '失败'
}

const statusType: Record<TaskStatus, 'primary' | 'warning' | 'info' | 'success' | 'danger'> = {
  uploading: 'primary',
  paused: 'info',
  merging: 'warning',
  done: 'success',
  error: 'danger'
}

/** 本次上传总进度：按文件个数平均，5 个文件每传完一个前进 20% */
const totalPercent = computed(() => {
  if (!tasks.value.length) return 0
  const sum = tasks.value.reduce((acc, task) => acc + (task.status === 'done' ? 100 : task.percent), 0)
  return Math.round(sum / tasks.value.length)
})

const jobQueue: Array<() => Promise<void>> = []
let runningJobs = 0

function enqueue(job: () => Promise<void>) {
  jobQueue.push(job)
  runNextJob()
}

function runNextJob() {
  while (runningJobs < MAX_CONCURRENT && jobQueue.length) {
    const job = jobQueue.shift()!
    runningJobs++
    job()
      .catch(() => undefined)
      .finally(() => {
        runningJobs--
        runNextJob()
      })
  }
}

function onUploadChange(uploadItem: { raw?: File }) {
  if (uploadItem.raw) {
    addFiles([uploadItem.raw])
  }
}

function addFiles(list: File[]) {
  list.forEach((file) => {
    if (!file.size) {
      ElMessage.warning(`${file.name} 是空文件，已跳过`)
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      ElMessage.error(`${file.name} 超过 1GB，已跳过`)
      return
    }
    // 小文件走单请求直传，大文件自动分片
    const chunked = file.size > CHUNK_THRESHOLD
    // 用 reactive 包装任务对象：后续直接修改 task.percent 才能触发总进度等 computed 更新
    const task: UploadTask = reactive({
      key: `${file.name}-${file.size}-${Date.now()}-${Math.random()}`,
      name: file.name,
      size: file.size,
      percent: 0,
      status: 'uploading',
      tip: chunked ? '准备分片...' : '上传中...',
      chunkText: '',
      chunked,
      file
    })
    tasks.value.unshift(task)
    enqueue(() => (chunked ? runChunkTask(task) : runSmallTask(task)))
  })
}

async function runSmallTask(task: UploadTask) {
  task.status = 'uploading'
  task.tip = '上传中...'
  try {
    await uploadSmallFile(task.file!, activityId, (percent) => {
      task.percent = percent
    })
    task.percent = 100
    task.status = 'done'
    task.tip = '上传完成'
    ElMessage.success(`${task.name} 上传成功`)
    await loadFiles()
  } catch {
    task.status = 'error'
    task.tip = '上传失败，点击重试'
  }
}

async function runChunkTask(task: UploadTask, uploadId?: string) {
  task.status = 'uploading'
  task.tip = uploadId
    ? `正在续传，已传 ${task.uploadedChunks ?? 0}/${task.totalChunks ?? 0} 片...`
    : '正在准备分片...'
  const controller = new AbortController()
  task.controller = controller
  try {
    const fileVO = await uploadChunked(task.file!, {
      activityId,
      uploadId,
      signal: controller.signal,
      corruptIndex: demoCorrupt.value ? 2 : undefined,
      onInit: (info) => {
        task.uploadId = info.uploadId
        task.totalChunks = info.totalChunks
        persistSessions()
      },
      onPhase: (phase) => {
        if (phase === 'merging') {
          task.status = 'merging'
          task.tip = '服务端正在合并分片并做整体校验...'
        }
      },
      onProgress: (progress) => {
        task.percent = progress.percent
        task.uploadedChunks = progress.uploadedChunks
        task.chunkText = `${progress.uploadedChunks}/${progress.totalChunks} 片`
      },
      onRetry: (index, success) => {
        const no = index + 1
        if (success) {
          task.tip = `第 ${no} 片重传成功`
          ElMessage.success(`第 ${no} 片重传成功`)
        } else {
          task.tip = `第 ${no} 片校验失败，正在重传...`
        }
      }
    })
    task.percent = 100
    task.status = 'done'
    task.chunkText = ''
    task.tip = fileVO.sha256 ? `合并完成 · SHA-256 ${fileVO.sha256.slice(0, 8)}` : '合并完成'
    ElMessage.success(`${task.name} 上传成功`)
    removeSession(task.uploadId)
    await loadFiles()
  } catch (e) {
    if (e instanceof UploadPausedError) {
      task.status = 'paused'
      task.tip = `已暂停，已完成 ${task.uploadedChunks ?? 0}/${task.totalChunks ?? 0} 片`
      persistSessions()
      return
    }
    task.status = 'error'
    task.tip = '上传失败，点击重试'
  } finally {
    task.controller = undefined
  }
}

function pauseTask(task: UploadTask) {
  task.controller?.abort()
}

function resumeTask(task: UploadTask) {
  enqueue(() => runChunkTask(task, task.uploadId))
}

function retryTask(task: UploadTask) {
  enqueue(() => (task.chunked ? runChunkTask(task, task.uploadId) : runSmallTask(task)))
}

function removeTask(task: UploadTask) {
  task.controller?.abort()
  tasks.value = tasks.value.filter((item) => item.key !== task.key)
}

function progressStatus(task: UploadTask) {
  if (task.status === 'done') return 'success'
  if (task.status === 'error') return 'exception'
  if (task.status === 'paused' || task.status === 'merging') return 'warning'
  return ''
}

/* ===================== 断点续传：会话持久化 ===================== */
interface SavedSession {
  uploadId: string
  fileName: string
  fileSize: number
  totalChunks: number
  uploaded: number
}

const sessionKey = computed(() => `activity-file-sessions:${activityId}`)
const pendingSessions = ref<SavedSession[]>([])
const resumeInput = ref<HTMLInputElement>()
const resumeTarget = ref<SavedSession | null>(null)

/** 上传中/暂停时把会话写进 sessionStorage，刷新页面后仍能找回来（按标签页隔离，避免多账号串会话） */
function persistSessions() {
  const list: SavedSession[] = tasks.value
    .filter((task) => task.uploadId && task.status !== 'done')
    .map((task) => ({
      uploadId: task.uploadId!,
      fileName: task.name,
      fileSize: task.size,
      totalChunks: task.totalChunks ?? 0,
      uploaded: task.uploadedChunks ?? 0
    }))
  sessionStorage.setItem(sessionKey.value, JSON.stringify(list))
}

function removeSession(uploadId?: string) {
  if (!uploadId) return
  const raw = sessionStorage.getItem(sessionKey.value)
  if (!raw) return
  const list = JSON.parse(raw) as SavedSession[]
  sessionStorage.setItem(sessionKey.value, JSON.stringify(list.filter((item) => item.uploadId !== uploadId)))
}

/** 打开页面时向服务端查询真实进度，而不是相信本地记录 */
async function loadPendingSessions() {
  const raw = sessionStorage.getItem(sessionKey.value)
  if (!raw) return
  const saved = JSON.parse(raw) as SavedSession[]
  const alive: SavedSession[] = []
  for (const session of saved) {
    try {
      const status = await getChunkStatus(session.uploadId)
      if (status.merged) {
        continue
      }
      alive.push({
        uploadId: status.uploadId,
        fileName: status.fileName,
        // 后端 Long 统一序列化为字符串，这里转回数字，否则续传时 file.size 严格比较会不相等
        fileSize: Number(status.fileSize),
        totalChunks: status.totalChunks,
        uploaded: status.uploadedChunks.length
      })
    } catch {
      // 会话已过期，直接丢弃
    }
  }
  pendingSessions.value = alive
  sessionStorage.setItem(sessionKey.value, JSON.stringify(alive))
}

function pickResume(session: SavedSession) {
  resumeTarget.value = session
  resumeInput.value?.click()
}

function onResumePicked(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  const target = resumeTarget.value
  input.value = ''
  resumeTarget.value = null
  if (!file || !target) {
    return
  }
  if (file.name !== target.fileName || file.size !== target.fileSize) {
    ElMessage.error('请选择同一个文件（文件名和大小需完全一致）')
    return
  }
  pendingSessions.value = pendingSessions.value.filter((item) => item.uploadId !== target.uploadId)
  removeSession(target.uploadId)
  // 续传任务直接从上次进度起算，避免进度条先归零再跳回
  const resumedPercent = target.totalChunks
    ? Math.round((target.uploaded / target.totalChunks) * 100)
    : 0
  const task: UploadTask = reactive({
    key: `${file.name}-${file.size}-${Date.now()}`,
    name: file.name,
    size: file.size,
    percent: resumedPercent,
    status: 'uploading',
    tip: `正在续传，已传 ${target.uploaded}/${target.totalChunks} 片...`,
    chunkText: `${target.uploaded}/${target.totalChunks} 片`,
    chunked: true,
    file,
    uploadId: target.uploadId,
    totalChunks: target.totalChunks,
    uploadedChunks: target.uploaded
  })
  tasks.value.unshift(task)
  enqueue(() => runChunkTask(task, target.uploadId))
}

function dropSession(session: SavedSession) {
  pendingSessions.value = pendingSessions.value.filter((item) => item.uploadId !== session.uploadId)
  removeSession(session.uploadId)
}

/* ===================== 下载：进度 + 暂停 / 续传 ===================== */
interface DownloadState {
  percent: number
  loaded: number
  total: number
  paused: boolean
  done: boolean
  instance?: ResumableDownload
}

const downloads = reactive<Record<string, DownloadState>>({})

function beginDownload(key: string, url: string, name: string) {
  if (downloads[key]?.done) {
    delete downloads[key]
  }
  const state = (downloads[key] ??= { percent: 0, loaded: 0, total: 0, paused: false, done: false })
  if (!state.instance) {
    state.instance = new ResumableDownload(url, name, (loaded, total) => {
      state.loaded = loaded
      state.total = Math.max(total, state.total)
      state.percent = state.total ? Math.round((loaded / state.total) * 100) : 0
    })
  }
  runDownload(key, name)
}

function runDownload(key: string, name: string) {
  const state = downloads[key]
  if (!state?.instance) {
    return
  }
  state.paused = false
  state.instance
    .start()
    .then((finished) => {
      if (finished) {
        state.percent = 100
        state.done = true
        ElMessage.success(`${name} 下载完成`)
      }
    })
    .catch((e: Error) => {
      if (!state.paused) {
        ElMessage.error(e.message || '下载失败')
      }
    })
}

function pauseDownload(key: string) {
  const state = downloads[key]
  if (!state?.instance) {
    return
  }
  state.instance.pause()
  state.paused = true
}

function downloadFile(file: ActivityFileVO) {
  beginDownload(file.id, fileDownloadUrl(file.id), file.name)
}

function downloadPackage() {
  beginDownload('package', packageDownloadUrl(activityId), '活动资料.zip')
}

/* ===================== 其它 ===================== */
function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  }
  return `${Math.round(bytes / 1024)}KB`
}

function backToActivity() {
  router.push({ name: 'activity-detail', params: { id: activityId } })
}

onMounted(async () => {
  await loadFiles()
  await loadPendingSessions()
})
</script>

<template>
  <div>
    <el-card>
      <template #header>
        <div class="page-header">
          <div>
            <h2>活动资料</h2>
            <div class="page-tip">大文件（PPT / 视频等）自动分片上传，小文件可整批拖入；上传与下载都支持断点续传。</div>
          </div>
          <div>
            <el-button @click="backToActivity">返回活动详情</el-button>
            <el-button type="primary" :disabled="!files.length" @click="downloadPackage">
              一键打包下载 .zip
            </el-button>
          </div>
        </div>
      </template>

      <!-- 整包下载进度 -->
      <div v-if="downloads.package && !downloads.package.done" class="download-row">
        <span class="download-row__label">活动资料.zip</span>
        <!-- zip 由服务端边压缩边输出，拿不到总大小，用不确定进度条表示正在打包下载 -->
        <el-progress
          :percentage="downloads.package.percent"
          :indeterminate="downloads.package.percent === 0"
          :show-text="downloads.package.percent > 0"
          :stroke-width="10"
          style="flex: 1"
        />
        <span class="download-row__tip">{{ downloads.package.paused ? '已暂停' : '打包下载中...' }}</span>
        <el-button link type="primary" @click="downloads.package.paused ? runDownload('package', '活动资料.zip') : pauseDownload('package')">
          {{ downloads.package.paused ? '继续下载' : '暂停' }}
        </el-button>
      </div>

      <!-- 刷新后找回未完成的分片上传 -->
      <div v-if="pendingSessions.length" class="resume">
        <div class="resume__title">检测到未完成的分片上传（进度已保存在服务端）</div>
        <div v-for="session in pendingSessions" :key="session.uploadId" class="resume__item">
          <span class="resume__name">{{ session.fileName }}</span>
          <span class="resume__info">{{ formatSize(session.fileSize) }} · 已传 {{ session.uploaded }}/{{ session.totalChunks }} 片</span>
          <el-button link type="primary" @click="pickResume(session)">继续上传</el-button>
          <el-button link type="danger" @click="dropSession(session)">放弃</el-button>
        </div>
      </div>

      <!-- 上传区：整块可拖拽，支持多选 -->
      <el-upload
        drag
        multiple
        :auto-upload="false"
        :show-file-list="false"
        :on-change="onUploadChange"
        class="uploader"
      >
        <el-icon class="uploader__icon"><UploadFilled /></el-icon>
        <div class="uploader__text">把文件拖到这里，或<em>点击选择</em>（可多选）</div>
        <div class="uploader__hint">单个 ≤5MB 直接上传；大于 5MB 自动分片，可暂停、可续传</div>
      </el-upload>
      <div class="demo-switch">
        <el-checkbox v-model="demoCorrupt">演示坏块：让第 3 片首次上传时数据损坏，观察「校验失败 → 自动重传成功」</el-checkbox>
      </div>

      <!-- 上传任务：每个文件一条，底部汇总总进度 -->
      <div v-if="tasks.length" class="tasks">
        <div v-for="task in tasks" :key="task.key" class="task">
          <div class="task__head">
            <span class="task__name">{{ task.name }}</span>
            <span class="task__meta">{{ formatSize(task.size) }}</span>
            <el-tag size="small" :type="statusType[task.status]">{{ statusText[task.status] }}</el-tag>
            <span v-if="task.chunkText" class="task__meta">{{ task.chunkText }}</span>
            <div class="task__ops">
              <el-button v-if="task.status === 'uploading' && task.chunked" link type="primary" @click="pauseTask(task)">暂停</el-button>
              <el-button v-if="task.status === 'paused'" link type="primary" @click="resumeTask(task)">继续</el-button>
              <el-button v-if="task.status === 'error'" link type="primary" @click="retryTask(task)">重试</el-button>
              <el-button link @click="removeTask(task)">移除</el-button>
            </div>
          </div>
          <el-progress :percentage="task.percent" :status="progressStatus(task)" :stroke-width="10" />
          <div v-if="task.tip" class="task__tip">{{ task.tip }}</div>
        </div>
        <div class="tasks__total">
          <span>本次上传总进度</span>
          <el-progress :percentage="totalPercent" :stroke-width="12" style="flex: 1" />
        </div>
      </div>

      <input ref="resumeInput" type="file" class="hidden-input" @change="onResumePicked" />
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>资料列表（{{ files.length }}）</template>
      <el-table v-loading="loading" :data="files" border>
        <el-table-column prop="name" label="文件名" min-width="180" />
        <el-table-column label="文件说明" min-width="200">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="110" />
        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" align="center">
          <template #default="{ row }">
            <template v-if="downloads[row.id] && !downloads[row.id].done">
              <el-progress :percentage="downloads[row.id].percent" :stroke-width="8" style="width: 110px; display: inline-block" />
              <el-button link type="primary" @click="downloads[row.id].paused ? runDownload(row.id, row.name) : pauseDownload(row.id)">
                {{ downloads[row.id].paused ? '继续' : '暂停' }}
              </el-button>
            </template>
            <el-button v-else link type="primary" @click="downloadFile(row as ActivityFileVO)">
              {{ downloads[row.id]?.done ? '重新下载' : '下载' }}
            </el-button>
            <el-button v-if="row.canDelete" link type="danger" @click="removeFile(row as ActivityFileVO)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !files.length" description="还没有资料，上传第一个文件吧" :image-size="80" />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0 0 4px;
  }
}

.page-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.download-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  &__label {
    width: 120px;
    color: var(--el-text-color-regular);
  }

  &__tip {
    color: var(--el-text-color-secondary);
    font-size: 13px;
    white-space: nowrap;
  }
}

.resume {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #fdf6ec;

  &__title {
    font-weight: 600;
    margin-bottom: 6px;
    color: var(--el-color-warning-dark-2);
  }

  &__item {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;
  }

  &__name {
    min-width: 220px;
  }

  &__info {
    color: var(--el-text-color-secondary);
  }
}

.uploader {
  &__icon {
    font-size: 40px;
    color: var(--el-color-primary);
    margin-bottom: 8px;
  }

  &__text em {
    color: var(--el-color-primary);
    font-style: normal;
  }

  &__hint {
    margin-top: 6px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.demo-switch {
  margin-top: 8px;
}

.tasks {
  margin-top: 12px;

  &__total {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed var(--el-border-color);
    color: var(--el-text-color-regular);
  }
}

.task {
  padding: 10px 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);

  &__head {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 6px;
  }

  &__name {
    font-weight: 600;
    max-width: 320px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }

  &__ops {
    margin-left: auto;
  }

  &__tip {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.hidden-input {
  display: none;
}
</style>
