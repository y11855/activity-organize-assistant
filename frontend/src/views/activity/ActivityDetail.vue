<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActivityDetail,
  generateReview,
  confirmTasks,
  generateActivityContent,
  getActivityDraft,
  updateActivityDraft,
  publishActivity
} from '@/api/activity'
import { listUsers } from '@/api/user'
import type {
  ActivityVO,
  ActivityDraftVO,
  GenerateOptions,
  QuestionDraft,
  TaskAssignDTO,
  UserSimpleVO
} from '@/types'
import { formatDate } from '@/utils'

const route = useRoute()
const id = String(route.params.id)
const detail = ref<ActivityVO | null>(null)
const loading = ref(false)
const reviewing = ref(false)
const confirming = ref(false)
const publishing = ref(false)
const savingDraft = ref(false)
const generating = ref(false)

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
  } finally {
    draftLoading.value = false
  }
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
async function onGenerate() {
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
}

/** 保存对草稿的编辑 */
async function onSaveDraft() {
  if (!draft.value) return
  savingDraft.value = true
  try {
    await updateActivityDraft(id, draft.value)
    ElMessage.success('修改已保存')
  } finally {
    savingDraft.value = false
  }
}

/** 确认发布：先保存编辑，再正式发布任务/问卷等内容 */
async function onPublish() {
  if (!draft.value) return
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
}

/** 任务负责人变更时同步昵称 */
function onAssigneeChange(task: TaskAssignDTO, userId: string) {
  const user = userOptions.value.find((u) => String(u.id) === String(userId))
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

async function onReview() {
  reviewing.value = true
  try {
    const content = await generateReview(id)
    if (detail.value && content) detail.value.reviewContent = content
    ElMessage.success('复盘生成完成')
    await load()
  } finally {
    reviewing.value = false
  }
}

/** 确认分工：向被分配的成员发送站内消息 */
async function onConfirmTasks() {
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
}

onMounted(load)
onUnmounted(stopPolling)
</script>

<template>
  <div v-loading="loading">
    <el-card v-if="detail">
      <template #header>
        <div class="detail__header">
          <h2>{{ detail.title }}</h2>
          <div>
            <el-button v-if="detail.status === 0" type="primary" @click="openOptions">
              配置 AI 生成内容
            </el-button>
            <template v-if="detail.status === 5">
              <el-button :loading="generating" @click="openOptions">重新生成</el-button>
              <el-button type="primary" :loading="savingDraft" @click="onSaveDraft">保存修改</el-button>
              <el-button type="success" :loading="publishing" @click="onPublish">确认发布</el-button>
            </template>
            <template v-if="detail.status === 2">
              <el-button
                v-if="detail.tasks?.length"
                type="success"
                :loading="confirming"
                @click="onConfirmTasks"
              >
                确认分工并通知成员
              </el-button>
              <el-button type="primary" :loading="reviewing" @click="onReview">生成复盘总结</el-button>
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
                @change="(v: string) => onAssigneeChange(row as TaskAssignDTO, v)"
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

    <el-card v-if="detail?.reviewContent" style="margin-top: 16px">
      <template #header>复盘总结</template>
      <div style="white-space: pre-wrap">{{ detail.reviewContent }}</div>
    </el-card>
  </div>

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
</style>
