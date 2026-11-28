<!-- 日志中心：概览统计 + 操作/登录/接口/异常/审计日志查询，行展开查看请求与响应明细 -->
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageLogs, getLogOverview, listLogModules, clearLogs } from '@/api/log'
import { useUserStore } from '@/stores'
import type { LogOverviewVO, LogQuery, LogType, SysLogVO } from '@/types'
import { debounce, formatDate } from '@/utils'

const userStore = useUserStore()
/** 清空日志属于高危操作，仅 log:manage 可见 */
const canManage = computed(() => userStore.hasPermission('log:manage'))

/** Tab 定义：value 为空表示概览（不按类型过滤） */
const TABS: { label: string; value: '' | LogType }[] = [
  { label: '概览', value: '' },
  { label: '操作日志', value: 'operation' },
  { label: '登录日志', value: 'login' },
  { label: '接口调用', value: 'api' },
  { label: '异常日志', value: 'exception' },
  { label: '审计日志', value: 'audit' }
]

const activeTab = ref<'' | LogType>('')
const overview = ref<LogOverviewVO>({
  totalCalls: 0,
  todayCalls: 0,
  exceptionCount: 0,
  slowCount: 0,
  loginFailCount: 0,
  onlineUsers: 0
})
const modules = ref<string[]>([])
const list = ref<SysLogVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

// ===== 筛选条件 =====
const keyword = ref('')
const moduleFilter = ref('')
const dateRange = ref<[string, string] | null>(null)

/** 统计卡片配置：颜色区分指标性质，数值实时取概览接口 */
const stats = computed(() => [
  { label: '接口调用总量', value: overview.value.totalCalls, icon: 'DataLine', tone: 'blue' },
  { label: '今日接口调用', value: overview.value.todayCalls, icon: 'Calendar', tone: 'green' },
  { label: '异常日志数', value: overview.value.exceptionCount, icon: 'Warning', tone: 'red' },
  { label: '慢请求数', value: overview.value.slowCount, icon: 'Timer', tone: 'orange' },
  { label: '登录失败次数', value: overview.value.loginFailCount, icon: 'Lock', tone: 'purple' },
  { label: '当前在线用户', value: overview.value.onlineUsers, icon: 'User', tone: 'cyan' }
])

async function loadOverview() {
  overview.value = await getLogOverview()
}

async function loadModules() {
  modules.value = await listLogModules()
}

/** 按当前 Tab 与筛选条件加载日志 */
async function loadList() {
  loading.value = true
  try {
    const query: LogQuery = {
      page: page.value,
      size: size.value,
      logType: activeTab.value || undefined,
      module: moduleFilter.value || undefined,
      keyword: keyword.value.trim() || undefined
    }
    if (dateRange.value?.length === 2) {
      query.startTime = `${dateRange.value[0]} 00:00:00`
      query.endTime = `${dateRange.value[1]} 23:59:59`
    }
    const res = await pageLogs(query)
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 切换 Tab：回到第一页并按类型重新查询 */
function onTabChange() {
  page.value = 1
  loadList()
}

/** 搜索 / 重置 */
function onSearch() {
  page.value = 1
  loadList()
}

function onReset() {
  keyword.value = ''
  moduleFilter.value = ''
  dateRange.value = null
  onSearch()
}

/** 刷新统计、模块选项与列表 */
async function refresh() {
  await Promise.all([loadOverview(), loadModules(), loadList()])
}

/** 清空全部日志（后端会保留本次清空操作的审计记录） */
async function doClear() {
  if (!canManage.value) return
  try {
    await ElMessageBox.confirm('确定清空全部日志？此操作不可恢复。', '清空日志', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await clearLogs()
    ElMessage.success('日志已清空')
    await refresh()
  } catch {
    // 错误提示已在请求拦截器统一弹出
  }
}

const onClear = debounce(doClear, 300)

/** 格式化 JSON 展示；非 JSON 文本原样返回 */
function prettyText(text?: string) {
  if (!text) return '—'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
  }
}

/** 耗时是否达到慢请求阈值 */
function isSlow(duration?: number) {
  return (duration ?? 0) >= 1000
}

onMounted(refresh)
</script>

<template>
  <el-card>
    <template #header>
      <div class="log__header">
        <div class="log__heading">
          <span class="log__title">日志中心</span>
          <span class="log__subtitle">接口调用 · 登录 · 异常 · 审计全链路留痕</span>
        </div>
        <div class="log__toolbar">
          <el-button :loading="loading" @click="refresh">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
          <el-button v-if="canManage" type="danger" plain @click="onClear">清空日志</el-button>
        </div>
      </div>
    </template>

    <!-- 概览统计卡 -->
    <div class="log__stats">
      <div v-for="item in stats" :key="item.label" class="log__stat">
        <div class="log__stat-icon" :class="`log__stat-icon--${item.tone}`">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div class="log__stat-body">
          <div class="log__stat-value">{{ item.value.toLocaleString() }}</div>
          <div class="log__stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 功能说明 -->
    <div class="log__intro">
      <div class="log__intro-title">这套日志系统做什么</div>
      <p>
        系统在每次接口调用结束后自动采集操作人、请求路径、参数、响应、耗时与结果，
        按「操作 / 登录 / 接口 / 异常 / 审计」分类入库。登录失败、慢请求、异常响应会被
        单独统计，便于快速定位问题；后台管理类操作额外标记为审计日志，方便追溯关键变更。
      </p>
    </div>

    <!-- 类型 Tab -->
    <div class="log__tabs">
      <el-radio-group v-model="activeTab" @change="onTabChange">
        <el-radio-button v-for="tab in TABS" :key="tab.value" :value="tab.value">
          {{ tab.label }}
        </el-radio-button>
      </el-radio-group>
    </div>

    <!-- 筛选栏 -->
    <div class="log__filters">
      <el-select v-model="moduleFilter" placeholder="全部模块" clearable style="width: 140px">
        <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="用户名 / 请求路径 / 操作"
        clearable
        style="width: 240px"
        @keyup.enter="onSearch"
      />
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 260px"
      />
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <!-- 日志表格：展开行查看请求参数、响应与异常 -->
    <el-table :data="list" v-loading="loading" border>
      <el-table-column type="expand" width="40">
        <template #default="{ row }">
          <div class="log__detail">
            <div class="log__detail-block">
              <div class="log__detail-title">请求参数</div>
              <pre class="log__detail-code">{{ prettyText(row.requestParams) }}</pre>
            </div>
            <div class="log__detail-block">
              <div class="log__detail-title">响应内容</div>
              <pre class="log__detail-code">{{ prettyText(row.responseBody) }}</pre>
            </div>
            <div class="log__detail-block">
              <div class="log__detail-title">异常信息</div>
              <pre class="log__detail-code log__detail-code--error">{{ row.errorMsg || '—' }}</pre>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="用户" width="120">
        <template #default="{ row }">
          {{ row.userName || (row.userId ? `#${row.userId}` : '匿名') }}
        </template>
      </el-table-column>
      <el-table-column label="请求路径 / 操作" min-width="240">
        <template #default="{ row }">
          <div class="log__path">{{ row.operation || row.method }}</div>
          <div class="log__muted">{{ row.method }} {{ row.requestUri }}</div>
        </template>
      </el-table-column>
      <el-table-column label="模块" width="100">
        <template #default="{ row }">
          <el-tag size="small" type="info" effect="plain">{{ row.module || '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="返回码" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.success === 1 ? 'success' : 'danger'">
            {{ row.statusCode ?? '—' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'log__slow': isSlow(row.duration) }">{{ row.duration ?? 0 }} ms</span>
        </template>
      </el-table-column>
      <el-table-column label="客户端 / IP" min-width="170">
        <template #default="{ row }">
          <div>{{ row.ip || '—' }}</div>
          <el-tooltip :content="row.client || '未知客户端'" placement="top">
            <div class="log__muted log__ellipsis">{{ row.client || '未知客户端' }}</div>
          </el-tooltip>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无日志" />
      </template>
    </el-table>

    <div v-if="total > 0" class="list-pagination">
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
.list-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.log {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__heading {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
  }

  &__subtitle {
    color: #909399;
    font-size: 12px;
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__stats {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 12px;
  }

  &__stat {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    background: #fbfcfd;
  }

  &__stat-icon {
    width: 38px;
    height: 38px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;

    &--blue {
      color: #6d84a8;
      background: #edf1f6;
    }

    &--green {
      color: #52946f;
      background: #ebf5ef;
    }

    &--red {
      color: #c45656;
      background: #fcefef;
    }

    &--orange {
      color: #c98a3c;
      background: #fbf3e8;
    }

    &--purple {
      color: #8a6bb3;
      background: #f2edf9;
    }

    &--cyan {
      color: #4a8f9c;
      background: #eaf5f7;
    }
  }

  &__stat-body {
    min-width: 0;
  }

  &__stat-value {
    font-size: 20px;
    font-weight: 600;
    line-height: 1.2;
    color: #303133;
  }

  &__stat-label {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
    white-space: nowrap;
  }

  &__intro {
    margin-top: 16px;
    padding: 14px 16px;
    border-left: 3px solid var(--el-color-primary);
    border-radius: 4px;
    background: #f7f9fc;

    p {
      margin: 6px 0 0;
      color: #606266;
      font-size: 13px;
      line-height: 1.7;
    }
  }

  &__intro-title {
    font-weight: 600;
    color: #303133;
  }

  &__tabs {
    margin: 16px 0 12px;
  }

  &__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    margin-bottom: 12px;
  }

  &__path {
    color: #303133;
  }

  &__muted {
    color: #909399;
    font-size: 12px;
  }

  &__ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__slow {
    color: #c98a3c;
    font-weight: 600;
  }

  &__detail {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    padding: 12px 16px;
    background: #fafbfc;
  }

  &__detail-block {
    min-width: 0;
  }

  &__detail-title {
    margin-bottom: 6px;
    font-size: 12px;
    font-weight: 600;
    color: #606266;
  }

  &__detail-code {
    margin: 0;
    max-height: 220px;
    overflow: auto;
    padding: 10px 12px;
    border-radius: 6px;
    background: #fff;
    border: 1px solid #ebeef5;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12px;
    line-height: 1.6;
    color: #303133;
    white-space: pre-wrap;
    word-break: break-all;

    &--error {
      color: #c45656;
    }
  }
}

@media (max-width: 1400px) {
  .log__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .log__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .log__detail {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
