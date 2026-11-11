<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { listMyReminders, cancelReminder } from '@/api/reminder'
import type { ReminderVO } from '@/types'

const router = useRouter()
const reminders = ref<ReminderVO[]>([])
const loading = ref(false)

const statusMap: Record<number, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  0: { label: '待触发', type: 'warning' },
  1: { label: '已发送', type: 'success' },
  2: { label: '已取消', type: 'info' },
  3: { label: '发送失败', type: 'danger' }
}

const formatTime = (t: string) => (t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '')

async function load() {
  loading.value = true
  try {
    reminders.value = await listMyReminders()
  } finally {
    loading.value = false
  }
}

const onCancel = async (r: ReminderVO) => {
  if (r.status !== 0) {
    ElMessage.warning('只有待触发的提醒可以取消')
    return
  }
  try {
    await ElMessageBox.confirm(`确认取消提醒「${r.title}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await cancelReminder(r.id)
  ElMessage.success('已取消')
  load()
}

onMounted(load)
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div class="reminder__header">
        <span>发给我的提醒</span>
        <el-button type="primary" link @click="router.push('/activities')">去活动中创建提醒</el-button>
      </div>
    </template>

    <el-empty v-if="!reminders.length" description="暂无提醒，可在活动详情中给自己或成员创建定时提醒" />

    <el-timeline v-else>
      <el-timeline-item
        v-for="r in reminders"
        :key="r.id"
        :type="r.status === 0 ? 'warning' : r.status === 1 ? 'success' : r.status === 3 ? 'danger' : 'info'"
        placement="top"
      >
        <el-card shadow="hover" class="reminder__card">
          <div class="reminder__row">
            <span class="reminder__title">{{ r.title }}</span>
            <el-tag :type="statusMap[r.status]?.type" size="small" effect="light">
              {{ statusMap[r.status]?.label }}
            </el-tag>
          </div>
          <div v-if="r.content" class="reminder__content">{{ r.content }}</div>
          <div class="reminder__footer">
            <span class="reminder__time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(r.triggerTime) }}
            </span>
            <el-button
              v-if="r.status === 0"
              type="danger"
              link
              size="small"
              @click="onCancel(r)"
            >取消</el-button>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<style scoped lang="scss">
.reminder {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__card {
    margin-bottom: 4px;
  }
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  &__title {
    font-weight: 600;
    font-size: 15px;
  }
  &__content {
    margin-top: 8px;
    color: #606266;
    font-size: 13px;
    line-height: 1.6;
  }
  &__footer {
    margin-top: 10px;
    padding-top: 8px;
    border-top: 1px solid #f0f0f0;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__time {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 12px;
  }
}
</style>
