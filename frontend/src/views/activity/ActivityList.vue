<!-- 我创建的活动列表页：发起活动、取消活动、查看活动分解树 -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyActivities, createActivity, cancelActivity } from '@/api/activity'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores'
import ActivityTree from '@/components/ActivityTree.vue'
import type { ActivityVO, ActivitySaveDTO, UserSimpleVO } from '@/types'
import { ACTIVITY_TAGS, debounce, formatDate } from '@/utils'

const router = useRouter()
const userStore = useUserStore()
const list = ref<ActivityVO[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const loading = ref(false)
const creating = ref(false)
const userOptions = ref<UserSimpleVO[]>([])
const form = ref<ActivitySaveDTO>({
  title: '',
  description: '',
  introduction: '',
  startTime: '',
  endTime: '',
  location: '',
  tag: '',
  expectedCount: undefined,
  memberIds: []
})

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '策划中',
  2: '执行中',
  3: '已完成',
  4: '已取消',
  5: '待确认'
}

/** 取消活动对话框 */
const cancelVisible = ref(false)
const cancelTarget = ref('')
const deleteRecord = ref(false)
const cancelling = ref(false)

/** 活动分解树对话框 */
const treeVisible = ref(false)
const treeActivityId = ref('')

function openTree(id: string) {
  treeActivityId.value = id
  treeVisible.value = true
}

function statusTagType(status: number): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 4) return 'danger'
  if (status === 5) return 'warning'
  if (status === 0 || status === 1) return 'info'
  return 'success'
}

async function loadList() {
  loading.value = true
  try {
    const res = await listMyActivities({ page: page.value, size: size.value })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function openDialog() {
  dialogVisible.value = true
  // 成员选择器数据（排除自己），仅首次加载
  if (!userOptions.value.length) {
    try {
      const users = await listUsers()
      userOptions.value = users.filter((u) => String(u.id) !== String(userStore.userInfo?.id))
    } catch {
      // 错误提示已在拦截器统一弹出
    }
  }
}

const onCreate = debounce(async () => {
  if (creating.value) return
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入活动标题')
    return
  }
  if (!form.value.tag) {
    ElMessage.warning('请选择活动属性标签')
    return
  }

  creating.value = true
  try {
    const id = await createActivity(form.value)
    ElMessage.success('活动已创建，请配置需要 AI 生成的内容')
    dialogVisible.value = false
    form.value = {
      title: '',
      description: '',
      introduction: '',
      startTime: '',
      endTime: '',
      location: '',
      tag: '',
      expectedCount: undefined,
      memberIds: []
    }
    router.push(`/activities/${id}`)
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    creating.value = false
  }
}, 500)

function openCancel(id: string) {
  cancelTarget.value = id
  deleteRecord.value = false
  cancelVisible.value = true
}

const onConfirmCancel = debounce(async () => {
  if (cancelling.value) return

  cancelling.value = true
  try {
    await cancelActivity(cancelTarget.value, deleteRecord.value)
    ElMessage.success(deleteRecord.value ? '活动已取消，记录及关联数据已删除' : '活动已取消，关联报名问卷已自动截止')
    cancelVisible.value = false
    loadList()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    cancelling.value = false
  }
}, 500)

/** 删除已取消活动的记录，含级联清除任务/物料/问卷及答案/提醒/消息，不可恢复 */
async function doDeleteRecord(id: string) {
  try {
    await ElMessageBox.confirm(
      '确定删除该活动记录吗？将一并删除其任务、物料、问卷及答案、提醒、消息，且不可恢复。',
      '删除活动记录',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await cancelActivity(id, true)
  ElMessage.success('活动记录已删除')
  loadList()
}

const onDeleteRecord = debounce(doDeleteRecord, 300)

onMounted(loadList)
</script>

<template>
  <el-card>
    <template #header>
      <div class="activity-list__header">
        <span>我的活动</span>
        <el-button type="primary" @click="openDialog">发起活动</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="title" label="活动标题" min-width="160" />
      <el-table-column label="时间" min-width="160">
        <template #default="{ row }">
          {{ row.startTime ? formatDate(row.startTime) : '-' }} ~ {{ row.endTime ? formatDate(row.endTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="110" />
      <el-table-column label="属性标签" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.tagName" size="small" effect="plain">{{ row.tagName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="人数" width="80" align="center">
        <template #default="{ row }">{{ row.expectedCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] ?? '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/activities/${row.id}`)">详情</el-button>
          <el-button link type="primary" @click="openTree(row.id)">分解树</el-button>
          <el-button
            v-if="row.status !== 4"
            link
            type="danger"
            :disabled="row.status === 1"
            @click="openCancel(row.id)"
          >
            取消
          </el-button>
          <el-button v-if="row.status === 4" link type="danger" @click="onDeleteRecord(row.id)">删除记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="list-pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadList"
      />
    </div>
  </el-card>

  <el-dialog v-model="dialogVisible" title="发起活动" width="680px" destroy-on-close>
    <el-form :model="form" label-width="90px" class="create-form">
      <el-form-item label="活动标题" required>
        <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：中秋班级联欢晚会" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 5 }"
          maxlength="500"
          show-word-limit
          placeholder="一句话描述活动，AI 将自动生成策划与分工"
        />
      </el-form-item>
      <el-form-item label="活动介绍">
        <el-input
          v-model="form.introduction"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 8 }"
          maxlength="2000"
          show-word-limit
          placeholder="向参与者介绍活动是做什么的、有什么亮点，吸引报名。可留空，稍后用 AI 生成"
        />
      </el-form-item>
      <div class="create-form__row">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </div>
      <el-form-item label="属性标签" required>
        <el-select v-model="form.tag" placeholder="选择活动类型，用于报名页展示与个性化推荐" style="width: 100%">
          <el-option v-for="t in ACTIVITY_TAGS" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <div class="create-form__row">
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="例如：教学楼 A301" />
        </el-form-item>
        <el-form-item label="预计人数">
          <el-input-number v-model="form.expectedCount" :min="1" :max="10000" placeholder="人数" style="width: 100%" />
        </el-form-item>
      </div>
      <el-form-item label="参与成员">
        <el-select
          v-model="form.memberIds"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择参与分工的成员（AI 将在他们之间分配任务）"
          style="width: 100%"
        >
          <el-option
            v-for="u in userOptions"
            :key="u.id"
            :label="u.nickname || u.username"
            :value="u.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="onCreate">发起</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="cancelVisible" title="取消活动" width="460px">
    <div class="cancel-tip">
      取消后，该活动关联的<strong>报名问卷将自动截止</strong>，新用户无法再提交报名。
    </div>
    <el-checkbox v-model="deleteRecord">同时删除该活动的记录（含任务、物料、问卷及答案、提醒、消息）</el-checkbox>
    <template #footer>
      <el-button @click="cancelVisible = false">再想想</el-button>
      <el-button type="danger" :loading="cancelling" @click="onConfirmCancel">确认取消活动</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="treeVisible" title="活动分解树" width="720px" destroy-on-close>
    <ActivityTree v-if="treeVisible && treeActivityId" :activity-id="treeActivityId" />
  </el-dialog>
</template>

<style scoped lang="scss">
.activity-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cancel-tip {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

/* 发起活动表单：时间/地点等短字段两列并排，界面更紧凑整洁 */
.create-form {
  &__row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    column-gap: 16px;
  }
}

/* 长文本输入：换行展示 + 纵向滚动，避免内容被裁切 */
:deep(.el-textarea__inner) {
  line-height: 1.5;
  word-break: break-all;
  white-space: pre-wrap;
}
</style>
