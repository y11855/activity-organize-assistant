<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyTasks, completeTask } from '@/api/task'
import type { MyTaskVO } from '@/types'
import { formatDate } from '@/utils'

const list = ref<MyTaskVO[]>([])
const loading = ref(false)
const statusFilter = ref<number | ''>('')
const sortOrder = ref<'asc' | 'desc'>('asc')

const statusMap: Record<number, string> = { 0: '待开始', 1: '进行中', 2: '已完成', 3: '已逾期' }
const statusType: Record<number, 'info' | 'primary' | 'success' | 'danger'> = {
  0: 'info',
  1: 'primary',
  2: 'success',
  3: 'danger'
}

const sortedList = computed(() => {
  const arr = [...list.value]
  arr.sort((a, b) => {
    if (!a.deadline && !b.deadline) return 0
    if (!a.deadline) return 1 // 无截止时间排最后
    if (!b.deadline) return -1
    const diff = new Date(a.deadline).getTime() - new Date(b.deadline).getTime()
    return sortOrder.value === 'asc' ? diff : -diff
  })
  return arr
})

async function loadList() {
  loading.value = true
  try {
    list.value = await listMyTasks(statusFilter.value === '' ? undefined : statusFilter.value)
  } finally {
    loading.value = false
  }
}

function onStatusChange() {
  loadList()
}

async function onComplete(row: MyTaskVO) {
  await ElMessageBox.confirm(`确定将任务「${row.title}」标记为已完成吗？`, '提示', { type: 'info' })
  await completeTask(row.id)
  ElMessage.success('任务已标记完成')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <el-card>
    <template #header>
      <div class="task__header">
        <span>我的分工（仅显示分配给我的任务）</span>
        <div class="task__toolbar">
          <el-select
            v-model="statusFilter"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="onStatusChange"
          >
            <el-option label="全部状态" :value="''" />
            <el-option label="待开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已逾期" :value="3" />
          </el-select>
          <el-radio-group v-model="sortOrder">
            <el-radio-button value="asc">截止升序</el-radio-button>
            <el-radio-button value="desc">截止降序</el-radio-button>
          </el-radio-group>
          <el-button @click="loadList">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="sortedList" v-loading="loading" border>
      <el-table-column label="任务内容" min-width="220">
        <template #default="{ row }">
          <div class="task__title">{{ row.title }}</div>
          <div v-if="row.description" class="task__desc">交付要求：{{ row.description }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="activityTitle" label="所属活动" min-width="140">
        <template #default="{ row }">{{ row.activityTitle || '-' }}</template>
      </el-table-column>
      <el-table-column label="截止时间" width="170">
        <template #default="{ row }">{{ row.deadline ? formatDate(row.deadline) : '-' }}</template>
      </el-table-column>
      <el-table-column label="完成状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType[row.status]">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="success"
            :disabled="row.status === 2"
            @click="onComplete(row as MyTaskVO)"
          >
            标记完成
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无分配给你的任务" />
      </template>
    </el-table>
  </el-card>
</template>

<style scoped lang="scss">
.task {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__title {
    font-weight: 600;
  }

  &__desc {
    color: #909399;
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>
