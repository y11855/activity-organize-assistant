<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMyParticipations } from '@/api/activity'
import type { ActivityVO } from '@/types'
import { formatDate } from '@/utils'

const router = useRouter()
const list = ref<ActivityVO[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

const statusMap: Record<number, string> = {
  0: '草稿',
  1: '策划中',
  2: '执行中',
  3: '已完成',
  4: '已取消',
  5: '待确认'
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
    const res = await listMyParticipations({ page: page.value, size: size.value })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<template>
  <el-card>
    <template #header>
      <div class="participations__header">
        <span>我的参与</span>
        <span class="participations__tip">这里列出所有由他人发起、并把你选为参与成员的活动</span>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="title" label="活动标题" min-width="160" />
      <el-table-column label="时间" min-width="180">
        <template #default="{ row }">
          {{ row.startTime ? formatDate(row.startTime) : '-' }} ~ {{ row.endTime ? formatDate(row.endTime) : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="110" />
      <el-table-column label="参与成员" min-width="150">
        <template #default="{ row }">
          <template v-if="row.memberNames?.length">
            <el-tag v-for="name in row.memberNames" :key="name" size="small" style="margin-right: 4px">
              {{ name }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusMap[row.status] ?? '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/activities/${row.id}`)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !list.length" description="暂无你参与的活动" :image-size="80" />

    <div class="participations__pagination">
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
</template>

<style scoped lang="scss">
.participations__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.participations__tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.participations__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
