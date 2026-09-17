<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getActivityDetail, generateReview } from '@/api/activity'
import type { ActivityVO } from '@/types'
import { formatDate } from '@/utils'

const route = useRoute()
const id = Number(route.params.id)
const detail = ref<ActivityVO | null>(null)
const loading = ref(false)
const reviewing = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getActivityDetail(id)
  } finally {
    loading.value = false
  }
}

async function onReview() {
  reviewing.value = true
  try {
    const content = await generateReview(id)
    if (detail.value) detail.value.reviewContent = content
    ElMessage.success('复盘生成完成')
  } finally {
    reviewing.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card v-if="detail">
      <template #header>
        <div style="display: flex; justify-content: space-between">
          <h2>{{ detail.title }}</h2>
          <el-button type="primary" :loading="reviewing" @click="onReview">生成复盘总结</el-button>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="描述">{{ detail.description }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ detail.startTime ? formatDate(detail.startTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime ? formatDate(detail.endTime) : '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail" style="margin-top: 16px">
      <template #header>AI 策划方案</template>
      <div v-html="detail.planContent" style="white-space: pre-wrap"></div>
    </el-card>

    <el-card v-if="detail?.materials?.length" style="margin-top: 16px">
      <template #header>物料清单</template>
      <el-table :data="detail.materials" border>
        <el-table-column prop="name" label="物料" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-card v-if="detail?.tasks?.length" style="margin-top: 16px">
      <template #header>任务分工</template>
      <el-table :data="detail.tasks" border>
        <el-table-column prop="title" label="任务" />
        <el-table-column prop="assigneeName" label="负责人" width="120" />
        <el-table-column label="截止时间" width="180">
          <template #default="{ row }">{{ row.deadline ? formatDate(row.deadline) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" />
      </el-table>
    </el-card>

    <el-card v-if="detail?.reviewContent" style="margin-top: 16px">
      <template #header>复盘总结</template>
      <div style="white-space: pre-wrap">{{ detail.reviewContent }}</div>
    </el-card>
  </div>
</template>
