<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listQuestionnaires } from '@/api/questionnaire'
import { listResponses, getStatistics, exportResponses } from '@/api/questionnaire'
import type { QuestionnaireVO, QuestionnaireResponseVO, ResponseStatisticsVO } from '@/types'
import { formatDate } from '@/utils'

const route = useRoute()
const activityId = String(route.params.id)

const questionnaires = ref<QuestionnaireVO[]>([])
const regQ = ref<QuestionnaireVO | null>(null)
const responses = ref<QuestionnaireResponseVO[]>([])
const stats = ref<ResponseStatisticsVO | null>(null)
const loading = ref(false)
const statusFilter = ref<number | undefined>(undefined)
const exporting = ref(false)

const statusMap: Record<number, { label: string; type: 'success' | 'info' | 'danger' | 'warning' }> = {
  1: { label: '已报名', type: 'success' },
  2: { label: '已取消', type: 'info' }
}

const totalSigned = computed(() => stats.value?.byStatus?.[1] ?? 0)
const totalCanceled = computed(() => stats.value?.byStatus?.[2] ?? 0)

async function load() {
  loading.value = true
  try {
    questionnaires.value = await listQuestionnaires(activityId)
    regQ.value = questionnaires.value.find((q) => !q.type || q.type === 1) ?? null
    if (regQ.value) {
      await Promise.all([loadResponses(), loadStats()])
    }
  } finally {
    loading.value = false
  }
}

async function loadResponses() {
  if (!regQ.value) return
  responses.value = await listResponses(regQ.value.id, statusFilter.value)
}

async function loadStats() {
  if (!regQ.value) return
  stats.value = await getStatistics(regQ.value.id)
}

function onStatusChange() {
  loadResponses()
}

async function onExport() {
  if (!regQ.value) return
  exporting.value = true
  try {
    const blob = await exportResponses(regQ.value.id)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `报名人员名单_${regQ.value.title}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // 错误已在拦截器处理
  } finally {
    exporting.value = false
  }
}

/** 解析 answer JSON，取可读摘要 */
function answerSummary(answer?: string): string {
  if (!answer) return '-'
  try {
    const obj = JSON.parse(answer)
    if (Array.isArray(obj)) {
      return obj.map((it: any) => `${it.title ?? ''}: ${it.value ?? it.answer ?? ''}`).join('；')
    }
    return answer
  } catch {
    return answer
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card v-if="!regQ">
      <el-empty description="该活动暂无报名问卷" />
    </el-card>

    <template v-else>
      <el-card style="margin-bottom: 16px">
        <template #header>
          <div class="board__header">
            <div>
              <div style="font-size: 18px; font-weight: 600">{{ regQ.title }}</div>
              <div style="color: var(--el-text-color-secondary); font-size: 13px; margin-top: 4px">
                {{ regQ.description }}
              </div>
            </div>
            <el-button type="primary" :loading="exporting" @click="onExport">导出 Excel</el-button>
          </div>
        </template>

        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic title="报名总人数" :value="stats?.total ?? 0" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="已报名" :value="totalSigned">
              <template #suffix><el-tag type="success" size="small" style="margin-left: 6px">有效</el-tag></template>
            </el-statistic>
          </el-col>
          <el-col :span="6">
            <el-statistic title="已取消" :value="totalCanceled" />
          </el-col>
          <el-col :span="6">
            <div class="status-stat">
              <div class="status-stat__label">问卷状态</div>
              <el-tag :type="regQ.status === 1 ? 'success' : regQ.status === 2 ? 'info' : 'warning'" size="large">
                {{ regQ.status === 1 ? '已发布' : regQ.status === 2 ? '已截止' : '未发布' }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-row :gutter="16">
        <el-col :span="16">
          <el-card>
            <template #header>报名人员明细</template>
            <div style="margin-bottom: 12px">
              <el-radio-group v-model="statusFilter" @change="onStatusChange">
                <el-radio-button :value="undefined">全部</el-radio-button>
                <el-radio-button :value="1">已报名</el-radio-button>
                <el-radio-button :value="2">已取消</el-radio-button>
              </el-radio-group>
            </div>
            <el-table :data="responses" border stripe>
              <el-table-column label="序号" width="70" align="center">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column prop="respondentName" label="姓名" width="140" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusMap[row.status]?.type ?? 'info'">
                    {{ statusMap[row.status]?.label ?? '已报名' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="回答内容" min-width="240">
                <template #default="{ row }">{{ answerSummary(row.answer) }}</template>
              </el-table-column>
              <el-table-column label="提交时间" width="170">
                <template #default="{ row }">{{ row.submitTime ? formatDate(row.submitTime) : '-' }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card>
            <template #header>报名时间线</template>
            <div v-if="stats?.timeline?.length" class="timeline">
              <div v-for="item in stats.timeline" :key="item.date" class="timeline__item">
                <div class="timeline__date">{{ item.date }}</div>
                <div class="timeline__bar">
                  <div
                    class="timeline__fill"
                    :style="{ width: (item.count / (stats.timeline[0]?.count || 1)) * 100 + '%' }"
                  ></div>
                </div>
                <div class="timeline__count">{{ item.count }} 人</div>
              </div>
            </div>
            <el-empty v-else description="暂无报名数据" :image-size="80" />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<style scoped lang="scss">
.board__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline__item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.timeline__date {
  width: 92px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.timeline__bar {
  flex: 1;
  height: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  overflow: hidden;
}

.timeline__fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 8px;
  min-width: 4px;
}

.timeline__count {
  width: 60px;
  text-align: right;
  color: var(--el-text-color-regular);
  flex-shrink: 0;
}

.status-stat {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-stat__label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style>
