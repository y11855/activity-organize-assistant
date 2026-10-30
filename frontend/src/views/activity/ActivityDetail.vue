<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActivityDetail,
  generateReview,
  confirmTasks,
  generateActivityContent,
  getActivityDraft,
  updateActivityDraft,
  publishActivity,
  listEventLogs,
  addEventLog,
  deleteEventLog
} from '@/api/activity'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores'
import {
  deleteActivityFile,
  fileDownloadUrl,
  listActivityFiles,
  uploadNamedFile
} from '@/api/file'
import { CHUNK_THRESHOLD, ResumableDownload, uploadChunked } from '@/utils/fileTransfer'
import type {
  ActivityVO,
  ActivityDraftVO,
  ActivityFileVO,
  GenerateOptions,
  QuestionDraft,
  TaskAssignDTO,
  UserSimpleVO,
  ActivityEventLogVO
} from '@/types'
import { debounce, formatDate } from '@/utils'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const id = String(route.params.id)
const detail = ref<ActivityVO | null>(null)
const loading = ref(false)
const reviewing = ref(false)
const confirming = ref(false)
const publishing = ref(false)
const savingDraft = ref(false)
const generating = ref(false)

/** 当前用户是否为活动创建者：决定编辑/发布等入口是否可见 */
const isCreator = computed(
  () => String(detail.value?.creatorId ?? '') === String(userStore.userInfo?.id ?? '')
)

/** AI 生成内容草稿（待确认状态下的预览/编辑数据） */
const draft = ref<ActivityDraftVO | null>(null)
const draftLoading = ref(false)
/** 生成项配置对话框 */
const optionVisible = ref(false)
const options = ref<GenerateOptions>({
  tasks: true,
  materials: true,
  questionnaire: true,
  reminders: true
})
/** 成员账号（用于修改任务负责人） */
const userOptions = ref<UserSimpleVO[]>([])

/** 配置表勾选项 ↔ options 的映射 */
const checkedOptions = computed<string[]>({
  get() {
    const list: string[] = []
    if (options.value.tasks) list.push('tasks')
    if (options.value.materials) list.push('materials')
    if (options.value.questionnaire) list.push('questionnaire')
    if (options.value.reminders) list.push('reminders')
    return list
  },
  set(value: string[]) {
    options.value = {
      tasks: value.includes('tasks'),
      materials: value.includes('materials'),
      questionnaire: value.includes('questionnaire'),
      reminders: value.includes('reminders')
    }
  }
})

const taskStatusMap: Record<number, string> = { 0: '待开始', 1: '进行中', 2: '已完成', 3: '已逾期' }
const taskStatusType: Record<number, 'info' | 'primary' | 'success' | 'danger'> = {
  0: 'info',
  1: 'primary',
  2: 'success',
  3: 'danger'
}

let pollTimer: ReturnType<typeof setInterval> | undefined

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function load() {
  loading.value = true
  try {
    const d = await getActivityDetail(id)
    detail.value = d
    if (d.status === 1) {
      startPolling()
    } else {
      stopPolling()
      if (d.status === 5) {
        await loadDraft()
      }
    }
  } finally {
    loading.value = false
  }
}

/** AI 生成中：轮询详情，生成结束后加载待确认草稿 */
function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(async () => {
    try {
      const d = await getActivityDetail(id)
      detail.value = d
      if (d.status !== 1) {
        stopPolling()
        if (d.status === 5) {
          await loadDraft()
          ElMessage.success('AI 内容已生成，请预览并确认')
        }
      }
    } catch {
      // 轮询失败静默处理，下一轮重试
    }
  }, 3000)
}

async function loadDraft() {
  draftLoading.value = true
  try {
    draft.value = await getActivityDraft(id)
    if (draft.value.options) {
      options.value = { ...options.value, ...draft.value.options }
    }
    normalizeDraftAssignees()
  } finally {
    draftLoading.value = false
  }
}

/**
 * 草稿任务负责人归一化：ID 统一为字符串；只有 ID 匹配不到时按姓名回填；
 * 仍无法对应真实账号（如 AI 曾返回序号）则清空，让发起人重新选择，避免下拉框只显示数字。
 */
function normalizeDraftAssignees() {
  if (!userOptions.value.length) return
  draft.value?.tasks?.forEach((task) => {
    if (task.assigneeId !== undefined && task.assigneeId !== null) {
      task.assigneeId = String(task.assigneeId)
    }
    const matched =
      (task.assigneeId && userOptions.value.find((u) => u.id === task.assigneeId)) ||
      (task.assigneeName &&
        userOptions.value.find((u) => (u.nickname || u.username) === task.assigneeName))
    if (matched) {
      task.assigneeId = String(matched.id)
      task.assigneeName = matched.nickname || matched.username
    } else {
      task.assigneeId = undefined
    }
  })
}

async function loadUsers() {
  if (userOptions.value.length) return
  try {
    userOptions.value = await listUsers()
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

function openOptions() {
  optionVisible.value = true
  loadUsers()
}

/** 触发 AI 生成（仅草稿/待确认状态可用） */
const onGenerate = debounce(async () => {
  if (generating.value) return
  if (!options.value.tasks && !options.value.materials && !options.value.questionnaire && !options.value.reminders) {
    ElMessage.warning('请至少勾选一项需要 AI 生成的内容')
    return
  }
  generating.value = true
  try {
    await generateActivityContent(id, options.value)
    optionVisible.value = false
    ElMessage.success('AI 正在生成内容，请稍候...')
    if (detail.value) detail.value.status = 1
    startPolling()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    generating.value = false
  }
}, 500)

/** 保存对草稿的编辑 */
const onSaveDraft = debounce(async () => {
  if (savingDraft.value || !draft.value) return
  savingDraft.value = true
  try {
    await updateActivityDraft(id, draft.value)
    ElMessage.success('修改已保存')
  } finally {
    savingDraft.value = false
  }
}, 500)

/** 确认发布：先保存编辑，再正式发布任务/问卷等内容 */
const onPublish = debounce(async () => {
  if (publishing.value || !draft.value) return
  try {
    await ElMessageBox.confirm(
      '确认发布该方案吗？确认后将正式发布任务分工、报名问卷、物料清单与定时提醒，且不可再修改草稿。',
      '确认发布',
      { type: 'warning' }
    )
  } catch {
    return
  }
  publishing.value = true
  try {
    await updateActivityDraft(id, draft.value)
    await publishActivity(id)
    ElMessage.success('内容已确认发布')
    draft.value = null
    await load()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    publishing.value = false
  }
}, 500)

/** 任务负责人变更时同步昵称 */
function onAssigneeChange(task: TaskAssignDTO, value: string | number) {
  const userId = String(value)
  const user = userOptions.value.find((u) => u.id === userId)
  task.assigneeId = userId
  task.assigneeName = user ? user.nickname || user.username : ''
}

/** 问卷选项编辑：逗号分隔字符串 ↔ 数组 */
function optionsText(q: QuestionDraft): string {
  return (q.options ?? []).join('、')
}

function onOptionsTextChange(q: QuestionDraft, text: string) {
  q.options = text
    .split(/[、,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function addTask() {
  draft.value?.tasks?.push({ title: '', description: '', assigneeName: '', deadline: '' })
}

function addMaterial() {
  draft.value?.materials?.push({ id: '', name: '', quantity: 1, unit: '', unitPrice: undefined, remark: '' })
}

function addQuestion() {
  draft.value?.questions?.push({ title: '', type: 'text', required: false, options: [] })
}

function addReminder() {
  draft.value?.reminders?.push({ title: '', content: '', triggerTime: '' })
}

const onReview = debounce(async () => {
  if (reviewing.value) return
  reviewing.value = true
  try {
    const content = await generateReview(id)
    if (detail.value && content) detail.value.reviewContent = content
    ElMessage.success('复盘生成完成')
    await load()
  } finally {
    reviewing.value = false
  }
}, 500)

/** 确认分工：向被分配的成员发送站内消息 */
const onConfirmTasks = debounce(async () => {
  if (confirming.value) return
  try {
    await ElMessageBox.confirm(
      '确认当前任务分工吗？确认后将立即向各任务负责人发送消息通知。',
      '确认分工',
      { type: 'info' }
    )
  } catch {
    return
  }
  confirming.value = true
  try {
    const count = await confirmTasks(id)
    ElMessage.success(`分工已确认，已通知 ${count} 位成员`)
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    confirming.value = false
  }
}, 500)

onMounted(loadAll)
onUnmounted(stopPolling)

/* ===================== 报名人员看板 ===================== */
function goRegistration() {
  router.push({ name: 'activity-registration', params: { id } })
}

/* ===================== 活动资料 ===================== */
function goFiles() {
  router.push({ name: 'activity-files', params: { id } })
}

/* ===================== 活动文件（详情页内嵌，参与成员可上传/下载） ===================== */
const activityFiles = ref<ActivityFileVO[]>([])
const fileLoading = ref(false)
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadForm = ref<{ name: string; description: string; file?: File; percent: number }>({
  name: '',
  description: '',
  file: undefined,
  percent: 0
})
const downloadPercents = ref<Record<string, number>>({})

async function loadFiles() {
  fileLoading.value = true
  try {
    activityFiles.value = await listActivityFiles(id)
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    fileLoading.value = false
  }
}

function openUpload() {
  uploadForm.value = { name: '', description: '', file: undefined, percent: 0 }
  uploadVisible.value = true
}

function onUploadFileChange(uploadItem: { raw?: File }) {
  const file = uploadItem.raw
  if (!file) {
    return
  }
  uploadForm.value.file = file
  // 默认用原始文件名作为「文件名称」，用户可修改但需保留扩展名
  if (!uploadForm.value.name) {
    uploadForm.value.name = file.name
  }
}

/** 自定义名称缺少扩展名时补上原文件扩展名，避免后端无法识别类型 */
function ensureExtension(name: string, original: string) {
  const dot = original.lastIndexOf('.')
  if (dot < 0 || name.includes('.')) {
    return name
  }
  return name + original.slice(dot)
}

const submitUpload = debounce(async () => {
  const form = uploadForm.value
  if (!form.file) {
    ElMessage.warning('请选择要上传的文件')
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning('请填写文件名称')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请填写文件说明')
    return
  }
  uploading.value = true
  try {
    const name = ensureExtension(form.name.trim(), form.file.name)
    const description = form.description.trim()
    if (form.file.size > CHUNK_THRESHOLD) {
      await uploadChunked(form.file, {
        activityId: id,
        name,
        description,
        signal: new AbortController().signal,
        onProgress: (progress) => (form.percent = progress.percent)
      })
    } else {
      await uploadNamedFile({
        file: form.file,
        activityId: id,
        name,
        description,
        onProgress: (percent) => (form.percent = percent)
      })
    }
    ElMessage.success('文件上传成功，已同步给活动创建者和其他参与成员')
    uploadVisible.value = false
    await loadFiles()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    uploading.value = false
  }
}, 500)

async function downloadFile(file: ActivityFileVO) {
  downloadPercents.value[file.id] = 0
  try {
    const download = new ResumableDownload(fileDownloadUrl(file.id), file.name, (loaded, total) => {
      downloadPercents.value[file.id] = total ? Math.round((loaded / total) * 100) : 0
    })
    const finished = await download.start()
    if (finished) {
      ElMessage.success(`${file.name} 下载完成`)
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '下载失败，请稍后重试')
  } finally {
    delete downloadPercents.value[file.id]
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
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch {
    // 错误提示已在拦截器统一弹出（无权限时提示「普通成员只能删除自己上传的文件」）
  }
}

function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) {
    return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  }
  return `${Math.round(bytes / 1024)}KB`
}

/* ===================== 活动事件记录 ===================== */
const eventLogs = ref<ActivityEventLogVO[]>([])
const eventLoading = ref(false)
const eventFormVisible = ref(false)
const eventTypeOptions: {
  label: string
  value: number
  type: 'success' | 'info' | 'danger' | 'warning' | 'primary'
}[] = [
  { label: '普通事件', value: 1, type: 'info' },
  { label: '关键节点', value: 2, type: 'primary' },
  { label: '问题', value: 3, type: 'danger' },
  { label: '亮点', value: 4, type: 'success' }
]
const eventForm = ref({ eventType: 1, content: '', occurredAt: '' })

async function loadEventLogs() {
  eventLoading.value = true
  try {
    eventLogs.value = await listEventLogs(id)
  } finally {
    eventLoading.value = false
  }
}

function openEventForm() {
  eventForm.value = { eventType: 1, content: '', occurredAt: '' }
  eventFormVisible.value = true
}

const submitEvent = debounce(async () => {
  if (!eventForm.value.content.trim()) {
    ElMessage.warning('请填写事件内容')
    return
  }
  try {
    await addEventLog(id, eventForm.value)
    ElMessage.success('事件已记录')
    eventFormVisible.value = false
    await loadEventLogs()
  } catch {
    // 错误已在拦截器处理
  }
}, 500)

const removeEvent = debounce(async (eid: string) => {
  try {
    await ElMessageBox.confirm('确认删除该事件记录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteEventLog(id, eid)
  ElMessage.success('已删除')
  await loadEventLogs()
}, 500)

const eventTypeTag = (t: number) => eventTypeOptions.find((o) => o.value === t)

// 活动已发布（含进行中/已完成）时展示事件记录与复盘
const showOpsPanel = computed(() => detail.value && (detail.value.status === 2 || detail.value.status === 3))

/* ===================== 复盘结构化解析 ===================== */
interface ReviewStruct {
  summary: string
  highlights: string[]
  issues: string[]
  suggestions: string[]
}
const reviewStruct = computed<ReviewStruct | null>(() => {
  const raw = detail.value?.reviewContent
  if (!raw) return null
  try {
    const obj = JSON.parse(raw)
    return {
      summary: obj.summary || '',
      highlights: Array.isArray(obj.highlights) ? obj.highlights : [],
      issues: Array.isArray(obj.issues) ? obj.issues : [],
      suggestions: Array.isArray(obj.suggestions) ? obj.suggestions : []
    }
  } catch {
    return { summary: raw, highlights: [], issues: [], suggestions: [] }
  }
})

// 事件记录在活动加载后一并拉取
async function loadAll() {
  // 先加载成员账号，草稿里的负责人才有下拉选项可映射（刷新页面后同样生效）
  await loadUsers()
  await load()
  await loadFiles()
  if (showOpsPanel.value) {
    loadEventLogs()
  }
}

</script>

<template>
  <div v-loading="loading">
    <el-card v-if="detail">
      <template #header>
        <div class="detail__header">
          <h2>{{ detail.title }}</h2>
          <div>
            <el-button @click="goFiles">活动资料</el-button>
            <el-button v-if="isCreator && detail.status === 0" type="primary" @click="openOptions">
              配置 AI 生成内容
            </el-button>
            <template v-if="isCreator && detail.status === 5">
              <el-button :loading="generating" @click="openOptions">重新生成</el-button>
              <el-button type="primary" :loading="savingDraft" @click="onSaveDraft">保存修改</el-button>
              <el-button type="success" :loading="publishing" @click="onPublish">确认发布</el-button>
            </template>
            <template v-if="isCreator && detail.status === 2">
              <el-button
                v-if="detail.tasks?.length"
                type="success"
                :loading="confirming"
                @click="onConfirmTasks"
              >
                确认分工并通知成员
              </el-button>
              <el-button @click="goRegistration">报名人员看板</el-button>
              <el-button type="primary" :loading="reviewing" @click="onReview">生成复盘总结</el-button>
            </template>
            <template v-if="isCreator && detail.status === 3">
              <el-button @click="goRegistration">报名人员看板</el-button>
              <el-button type="primary" :loading="reviewing" @click="onReview">重新生成复盘</el-button>
            </template>
          </div>
        </div>
      </template>

      <el-alert
        v-if="detail.status === 1"
        type="info"
        :closable="false"
        show-icon
        title="AI 正在生成内容，请稍候..."
        style="margin-bottom: 16px"
      />
      <el-alert
        v-else-if="detail.status === 5"
        type="warning"
        :closable="false"
        show-icon
        title="AI 内容已生成，请预览并可直接编辑，确认发布后才会正式生效"
        style="margin-bottom: 16px"
      />
      <el-alert
        v-else-if="detail.status === 4"
        type="error"
        :closable="false"
        show-icon
        title="该活动已取消，关联报名问卷已自动截止"
        style="margin-bottom: 16px"
      />
      <el-alert
        v-if="!isCreator"
        type="info"
        :closable="false"
        show-icon
        title="你正在以参与成员身份查看该活动：可查看全部公开信息、下载与上传活动文件；编辑与发布等操作仅活动创建者可用。"
        style="margin-bottom: 16px"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="描述">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.startTime ? formatDate(detail.startTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime ? formatDate(detail.endTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计人数">{{ detail.expectedCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="参与成员">
          <template v-if="detail.memberNames?.length">
            <el-tag v-for="name in detail.memberNames" :key="name" size="small" style="margin-right: 6px">
              {{ name }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 待确认：AI 生成内容预览与编辑 -->
    <template v-if="detail?.status === 5 && draft">
      <el-card v-loading="draftLoading" style="margin-top: 16px">
        <template #header>策划方案（可编辑）</template>
        <el-input v-model="draft.planContent" type="textarea" :rows="6" placeholder="活动流程与任务完成计划" />
      </el-card>

      <el-card v-if="options.tasks" style="margin-top: 16px">
        <template #header>
          <div class="detail__header">
            <span>任务分工明细（可编辑）</span>
            <el-button link type="primary" @click="addTask">添加任务</el-button>
          </div>
        </template>
        <el-table :data="draft.tasks ?? []" border>
          <el-table-column label="任务内容" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.title" placeholder="任务标题" />
            </template>
          </el-table-column>
          <el-table-column label="交付要求" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.description" placeholder="交付要求" />
            </template>
          </el-table-column>
          <el-table-column label="负责人员" width="170">
            <template #default="{ row }">
              <el-select
                :model-value="row.assigneeId"
                placeholder="选择成员"
                style="width: 100%"
                @change="(v: string | number) => onAssigneeChange(row as TaskAssignDTO, v)"
              >
                <el-option
                  v-for="u in userOptions"
                  :key="u.id"
                  :label="u.nickname || u.username"
                  :value="u.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.deadline"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="截止时间"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="draft.tasks?.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="options.materials" style="margin-top: 16px">
        <template #header>
          <div class="detail__header">
            <span>物料采购清单（可编辑）</span>
            <el-button link type="primary" @click="addMaterial">添加物料</el-button>
          </div>
        </template>
        <el-table :data="draft.materials ?? []" border>
          <el-table-column label="物料名称" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.name" placeholder="物料名称" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="0" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="100">
            <template #default="{ row }">
              <el-input v-model="row.unit" placeholder="单位" />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.unitPrice" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="draft.materials?.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="options.questionnaire" style="margin-top: 16px">
        <template #header>
          <div class="detail__header">
            <span>报名问卷配置（可编辑）</span>
            <el-button link type="primary" @click="addQuestion">添加题目</el-button>
          </div>
        </template>
        <el-table :data="draft.questions ?? []" border>
          <el-table-column label="题目" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.title" placeholder="题目" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-select v-model="row.type" style="width: 100%">
                <el-option label="单选" value="single" />
                <el-option label="多选" value="multiple" />
                <el-option label="填空" value="text" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="是否必填" width="100" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.required" />
            </template>
          </el-table-column>
          <el-table-column label="选项（顿号分隔）" min-width="180">
            <template #default="{ row }">
              <el-input
                :model-value="optionsText(row as QuestionDraft)"
                placeholder="选项一、选项二"
                :disabled="row.type === 'text'"
                @update:model-value="(v: string) => onOptionsTextChange(row as QuestionDraft, v)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="draft.questions?.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card v-if="options.reminders" style="margin-top: 16px">
        <template #header>
          <div class="detail__header">
            <span>定时提醒规则（可编辑）</span>
            <el-button link type="primary" @click="addReminder">添加提醒</el-button>
          </div>
        </template>
        <el-table :data="draft.reminders ?? []" border>
          <el-table-column label="提醒标题" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.title" placeholder="提醒标题" />
            </template>
          </el-table-column>
          <el-table-column label="提醒内容" min-width="200">
            <template #default="{ row }">
              <el-input v-model="row.content" placeholder="提醒内容" />
            </template>
          </el-table-column>
          <el-table-column label="触发时间" width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.triggerTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="触发时间"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" @click="draft.reminders?.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 已发布：只读展示 -->
    <el-card v-if="detail?.planContent && detail?.status !== 5" style="margin-top: 16px">
      <template #header>AI 策划方案</template>
      <div style="white-space: pre-wrap">{{ detail.planContent }}</div>
    </el-card>

    <el-card v-if="detail?.materials?.length && detail?.status !== 5" style="margin-top: 16px">
      <template #header>物料清单</template>
      <el-table :data="detail.materials" border>
        <el-table-column prop="name" label="物料" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-card v-if="detail?.tasks?.length && detail?.status !== 5" style="margin-top: 16px">
      <template #header>任务分工</template>
      <el-table :data="detail.tasks" border>
        <el-table-column prop="title" label="任务" min-width="140" />
        <el-table-column prop="description" label="交付要求" min-width="180" />
        <el-table-column prop="assigneeName" label="负责人" width="100" />
        <el-table-column label="截止时间" width="170">
          <template #default="{ row }">{{ row.deadline ? formatDate(row.deadline) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="taskStatusType[row.status]">{{ taskStatusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 活动文件：参与成员可查看/下载全部文件，并可上传自己的文件 -->
    <el-card style="margin-top: 16px">
      <template #header>
        <div class="detail__header">
          <span>活动文件（{{ activityFiles.length }}）</span>
          <div>
            <el-button size="small" @click="goFiles">进入资料中心</el-button>
            <el-button type="primary" size="small" @click="openUpload">上传文件</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="fileLoading" :data="activityFiles" border>
        <el-table-column prop="name" label="文件名" min-width="180" />
        <el-table-column label="文件说明" min-width="200">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="大小" width="90">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="100" />
        <el-table-column label="上传时间" width="165">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <template v-if="downloadPercents[row.id] !== undefined">
              <el-progress
                :percentage="downloadPercents[row.id]"
                :stroke-width="8"
                style="width: 90px; display: inline-block"
              />
            </template>
            <el-button v-else link type="primary" @click="downloadFile(row as ActivityFileVO)">下载</el-button>
            <el-button v-if="row.canDelete" link type="danger" @click="removeFile(row as ActivityFileVO)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty
        v-if="!fileLoading && !activityFiles.length"
        description="还没有活动文件，上传第一个文件吧"
        :image-size="80"
      />
    </el-card>

    <!-- 活动事件记录（已发布后可记录，用于 AI 复盘） -->
    <el-card v-if="showOpsPanel" style="margin-top: 16px">
      <template #header>
        <div class="detail__header">
          <span>活动事件记录（关键节点 / 亮点 / 问题，将用于 AI 复盘）</span>
          <el-button v-if="isCreator" type="primary" size="small" @click="openEventForm">记录事件</el-button>
        </div>
      </template>
      <el-table v-loading="eventLoading" :data="eventLogs" border>
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="eventTypeTag(row.eventType)?.type ?? 'info'">
              {{ eventTypeTag(row.eventType)?.label ?? '普通事件' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="事件内容" min-width="280" />
        <el-table-column label="发生时间" width="170">
          <template #default="{ row }">{{ row.occurredAt ? formatDate(row.occurredAt) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="recorderName" label="记录人" width="120" />
        <el-table-column v-if="isCreator" label="操作" width="80" align="center">
          <template #default="{ row }">
            <el-button link type="danger" @click="removeEvent(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!eventLoading && !eventLogs.length" description="暂无事件记录，点击右上角记录" :image-size="80" />
    </el-card>

    <!-- 复盘总结（结构化展示） -->
    <el-card v-if="reviewStruct" style="margin-top: 16px">
      <template #header>复盘总结</template>
      <el-alert
        v-if="reviewStruct.summary"
        :title="reviewStruct.summary"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-row :gutter="16">
        <el-col :span="8" v-if="reviewStruct.highlights.length">
          <div class="review-block">
            <div class="review-block__title">
              <el-tag type="success">亮点</el-tag>
            </div>
            <ul class="review-list">
              <li v-for="(h, i) in reviewStruct.highlights" :key="`h${i}`">{{ h }}</li>
            </ul>
          </div>
        </el-col>
        <el-col :span="8" v-if="reviewStruct.issues.length">
          <div class="review-block">
            <div class="review-block__title">
              <el-tag type="danger">问题</el-tag>
            </div>
            <ul class="review-list">
              <li v-for="(it, i) in reviewStruct.issues" :key="`i${i}`">{{ it }}</li>
            </ul>
          </div>
        </el-col>
        <el-col :span="8" v-if="reviewStruct.suggestions.length">
          <div class="review-block">
            <div class="review-block__title">
              <el-tag type="warning">改进建议</el-tag>
            </div>
            <ul class="review-list">
              <li v-for="(s, i) in reviewStruct.suggestions" :key="`s${i}`">{{ s }}</li>
            </ul>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>

  <!-- 事件记录表单 -->
  <el-dialog v-model="eventFormVisible" title="记录活动事件" width="520px">
    <el-form label-width="80px">
      <el-form-item label="事件类型">
        <el-select v-model="eventForm.eventType" style="width: 100%">
          <el-option v-for="o in eventTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发生时间">
        <el-date-picker
          v-model="eventForm.occurredAt"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="默认当前时间"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="事件内容">
        <el-input v-model="eventForm.content" type="textarea" :rows="4" placeholder="描述事件、问题或亮点..." />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="eventFormVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEvent">保存</el-button>
    </template>
  </el-dialog>

  <!-- 上传活动文件 -->
  <el-dialog v-model="uploadVisible" title="上传活动文件" width="540px">
    <el-form label-width="80px">
      <el-form-item label="选择文件" required>
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="true"
          :on-change="onUploadFileChange"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.csv,.txt,.md,.zip,.jpg,.jpeg,.png,.webp,.gif,.mp4,.mov"
          style="width: 100%"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">点击或拖拽文件到这里</div>
          <div class="upload-hint">支持 PDF / Word / Excel / PPT / 图片 / 压缩包等；大于 5MB 自动分片上传</div>
        </el-upload>
      </el-form-item>
      <el-form-item label="文件名称" required>
        <el-input v-model="uploadForm.name" placeholder="默认取原始文件名，可修改（请保留扩展名）" />
      </el-form-item>
      <el-form-item label="文件说明" required>
        <el-input
          v-model="uploadForm.description"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="简要说明文件用途，便于创建者和其他成员了解"
        />
      </el-form-item>
      <el-progress v-if="uploading" :percentage="uploadForm.percent" :stroke-width="10" />
    </el-form>
    <template #footer>
      <el-button :disabled="uploading" @click="uploadVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="submitUpload">上传</el-button>
    </template>
  </el-dialog>

  <!-- AI 生成内容配置表 -->
  <el-dialog v-model="optionVisible" title="AI 生成内容配置" width="480px">
    <div class="option-tip">请勾选需要 AI 自动生成的活动内容（可多选）：</div>
    <el-checkbox-group v-model="checkedOptions" class="option-group">
      <el-checkbox label="tasks">任务分工明细</el-checkbox>
      <el-checkbox label="materials">物料采购清单</el-checkbox>
      <el-checkbox label="questionnaire">报名问卷配置</el-checkbox>
      <el-checkbox label="reminders">定时提醒规则</el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="optionVisible = false">取消</el-button>
      <el-button type="primary" :loading="generating" @click="onGenerate">开始生成</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-tip {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-block__title {
  margin-bottom: 8px;
}

.review-list {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.upload-icon {
  font-size: 36px;
  color: var(--el-color-primary);
  margin-bottom: 6px;
}

.upload-text {
  color: var(--el-text-color-regular);
}

.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
