<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyActivities, createActivity, cancelActivity } from '@/api/activity'
import type { ActivityVO, ActivitySaveDTO } from '@/types'
import { formatDate } from '@/utils'

const router = useRouter()
const list = ref<ActivityVO[]>([])
const dialogVisible = ref(false)
const loading = ref(false)
const form = ref<ActivitySaveDTO>({ title: '', description: '', startTime: '', endTime: '', location: '' })

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '策划中',
  2: '执行中',
  3: '已完成',
  4: '已取消'
}

async function loadList() {
  loading.value = true
  try {
    list.value = await listMyActivities()
  } finally {
    loading.value = false
  }
}

async function onCreate() {
  try {
    const id = await createActivity(form.value)
    ElMessage.success('活动已发起，AI 正在自动策划...')
    dialogVisible.value = false
    router.push(`/activities/${id}`)
  } catch {
    // 错误已在拦截器处理
  }
}

async function onCancel(id: number) {
  await ElMessageBox.confirm('确定取消该活动吗？', '提示', { type: 'warning' })
  await cancelActivity(id)
  ElMessage.success('已取消')
  loadList()
}

onMounted(loadList)
</script>

<template>
  <el-card>
    <template #header>
      <div class="activity-list__header">
        <span>我的活动</span>
        <el-button type="primary" @click="dialogVisible = true">一句话发起活动</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="title" label="活动标题" min-width="180" />
      <el-table-column label="时间" min-width="160">
        <template #default="{ row }">
          {{ row.startTime ? formatDate(row.startTime) : '-' }} ~ {{ row.endTime ? formatDate(row.endTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 4 ? 'danger' : 'success'">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/activities/${row.id}`)">详情</el-button>
          <el-button link type="danger" :disabled="row.status === 4" @click="onCancel(row.id)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="发起活动" width="500px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="活动标题" required>
        <el-input v-model="form.title" placeholder="例如：中秋班级联欢晚会" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="一句话描述活动，AI 将自动生成策划" />
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
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="onCreate">发起</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.activity-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
