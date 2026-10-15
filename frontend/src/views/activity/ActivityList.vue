<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyActivities, createActivity, cancelActivity } from '@/api/activity'
import { listUsers } from '@/api/user'
import { useUserStore } from '@/stores'
import type { ActivityVO, ActivitySaveDTO, UserSimpleVO } from '@/types'
import { formatDate } from '@/utils'

const router = useRouter()
const userStore = useUserStore()
const list = ref<ActivityVO[]>([])
const dialogVisible = ref(false)
const loading = ref(false)
const creating = ref(false)
const userOptions = ref<UserSimpleVO[]>([])
const form = ref<ActivitySaveDTO>({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  location: '',
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

function statusTagType(status: number): 'success' | 'danger' | 'warning' | 'info' {
  if (status === 4) return 'danger'
  if (status === 5) return 'warning'
  if (status === 0 || status === 1) return 'info'
  return 'success'
}

async function loadList() {
  loading.value = true
  try {
    list.value = await listMyActivities()
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

async function onCreate() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入活动标题')
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
      startTime: '',
      endTime: '',
      location: '',
      expectedCount: undefined,
      memberIds: []
    }
    router.push(`/activities/${id}`)
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    creating.value = false
  }
}

function openCancel(id: string) {
  cancelTarget.value = id
  deleteRecord.value = false
  cancelVisible.value = true
}

async function onConfirmCancel() {
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
}

/** 删除已取消活动的记录，含级联清除任务/物料/问卷及答案/提醒/消息，不可恢复 */
async function onDeleteRecord(id: string) {
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
      <el-table-column label="人数" width="80" align="center">
        <template #default="{ row }">{{ row.expectedCount ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] ?? '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/activities/${row.id}`)">详情</el-button>
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
  </el-card>

  <el-dialog v-model="dialogVisible" title="发起活动" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="活动标题" required>
        <el-input v-model="form.title" placeholder="例如：中秋班级联欢晚会" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="一句话描述活动，AI 将自动生成策划与分工" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
      </el-form-item>
      <el-form-item label="地点">
        <el-input v-model="form.location" placeholder="例如：教学楼 A301" />
      </el-form-item>
      <el-form-item label="预计人数">
        <el-input-number v-model="form.expectedCount" :min="1" :max="10000" placeholder="人数" />
      </el-form-item>
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
</template>

<style scoped lang="scss">
.activity-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cancel-tip {
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
</style>
