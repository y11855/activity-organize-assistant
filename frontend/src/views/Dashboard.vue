<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { getDashboardOverview } from '@/api/dashboard'
import { useUserStore } from '@/stores'
import type { DashboardOverviewVO } from '@/types'

const userStore = useUserStore()
const data = ref<DashboardOverviewVO>()
const loading = ref(true)
const clock = ref('')
const statusChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

let statusChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let pollTimer: number | undefined
let clockTimer: number | undefined

const kpis = computed(() => {
  const d = data.value
  return [
    {
      label: '我创建的活动',
      value: Number(d?.activityTotal ?? 0),
      hint: `已完成 ${Number(d?.completedCount ?? 0)} · 待处理 ${Number(d?.pendingCount ?? 0)}`
    },
    {
      label: '进行中活动',
      value: Number(d?.runningCount ?? 0),
      hint: `已取消 ${Number(d?.cancelledCount ?? 0)}`
    },
    {
      label: '报名总人数',
      value: Number(d?.registrationTotal ?? 0),
      hint: '我创建活动的有效报名'
    },
    {
      label: '任务完成率',
      value: Number(d?.taskCompletionRate ?? 0),
      unit: '%',
      hint: `${Number(d?.taskCompleted ?? 0)} / ${Number(d?.taskTotal ?? 0)} 项任务`
    },
    {
      label: '我的待办',
      value: Number(d?.myTodoCount ?? 0),
      hint: '分配给我且未完成'
    },
    {
      label: '未读消息',
      value: Number(d?.unreadMessages ?? 0),
      hint: `发给我的提醒 ${Number(d?.reminderCount ?? 0)} 条`
    }
  ]
})

function updateClock() {
  clock.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

const STATUS_COLORS: Record<string, string> = {
  待策划: '#c0c4cc',
  策划中: '#a9b6cc',
  待确认: '#f0c78a',
  进行中: '#6d84a8',
  已完成: '#7fb8a4',
  已取消: '#d99a9a'
}

function renderCharts() {
  const d = data.value
  if (!d) {
    return
  }
  // 只展示有数据的状态，避免 0 值扇区把标签堆在图表顶部
  const statusItems = d.statusDistribution
    .map((item) => ({ name: item.name, value: Number(item.value) }))
    .filter((item) => item.value > 0)

  if (statusItems.length === 0) {
    statusChart?.setOption(
      {
        title: {
          text: '暂无活动数据',
          left: 'center',
          top: 'middle',
          textStyle: { color: '#909399', fontSize: 13, fontWeight: 'normal' }
        },
        legend: { show: false },
        series: []
      },
      true
    )
  } else {
    statusChart?.setOption(
      {
        tooltip: { trigger: 'item' },
        legend: {
          bottom: 0,
          textStyle: { color: '#606266' },
          itemWidth: 10,
          itemHeight: 10
        },
        series: [
          {
            type: 'pie',
            radius: ['50%', '72%'],
            center: ['50%', '44%'],
            avoidLabelOverlap: true,
            itemStyle: { borderColor: '#ffffff', borderWidth: 3 },
            label: { color: '#606266', formatter: '{b} {c}' },
            labelLine: { length: 10, length2: 8 },
            data: statusItems.map((item) => ({
              ...item,
              itemStyle: { color: STATUS_COLORS[item.name] ?? '#a9b6cc' }
            }))
          }
        ]
      },
      true
    )
  }

  const trendValues = d.registrationTrend.map((item) => Number(item.count))
  const hasTrend = trendValues.some((value) => value > 0)

  trendChart?.setOption(
    {
      graphic: hasTrend
        ? []
        : [
            {
              type: 'text',
              left: 'center',
              top: 'middle',
              style: { text: '近 7 天暂无报名', fill: '#909399', fontSize: 13 }
            }
          ],
      grid: { left: 36, right: 18, top: 24, bottom: 28 },
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: d.registrationTrend.map((item) => item.date),
        axisLine: { lineStyle: { color: '#e4e7ed' } },
        axisLabel: { color: '#909399' }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        splitLine: { lineStyle: { color: '#f0f2f5' } },
        axisLabel: { color: '#909399' }
      },
      series: [
        {
          name: '报名人数',
          type: 'line',
          smooth: true,
          symbolSize: 8,
          data: trendValues,
          lineStyle: { width: 3, color: '#6d84a8' },
          itemStyle: { color: '#6d84a8' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(109, 132, 168, .28)' },
              { offset: 1, color: 'rgba(109, 132, 168, 0)' }
            ])
          }
        }
      ]
    },
    true
  )
}

async function load() {
  try {
    data.value = await getDashboardOverview()
    renderCharts()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function resizeCharts() {
  statusChart?.resize()
  trendChart?.resize()
}

onMounted(async () => {
  statusChart = echarts.init(statusChartRef.value!)
  trendChart = echarts.init(trendChartRef.value!)
  updateClock()
  clockTimer = window.setInterval(updateClock, 1000)
  await load()
  // 大屏自动刷新：MQ 事件会主动失效后端缓存，这里 30 秒拉取一次兜底
  pollTimer = window.setInterval(load, 30000)
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.clearInterval(pollTimer)
  window.clearInterval(clockTimer)
  window.removeEventListener('resize', resizeCharts)
  statusChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <header class="screen-header">
      <div>
        <h1>活动管家 · 数据大屏</h1>
        <p>
          当前账号：{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '-' }}
          · 仅展示与我相关的数据
        </p>
      </div>
      <div class="clock">{{ clock }}</div>
    </header>

    <section class="kpi-grid">
      <div v-for="item in kpis" :key="item.label" class="kpi">
        <span class="kpi-label">{{ item.label }}</span>
        <strong class="kpi-value">
          {{ item.value }}<em v-if="item.unit">{{ item.unit }}</em>
        </strong>
        <span class="kpi-hint">{{ item.hint }}</span>
      </div>
    </section>

    <section class="content-grid">
      <div class="panel">
        <h3>活动状态分布</h3>
        <div ref="statusChartRef" class="chart"></div>
      </div>
      <div class="panel">
        <h3>近 7 天报名趋势</h3>
        <div ref="trendChartRef" class="chart"></div>
      </div>
      <div class="panel">
        <h3>实时动态 <span class="badge">Redis Stream</span></h3>
        <ul v-if="data?.recentEvents?.length" class="feed">
          <li v-for="(item, index) in data.recentEvents" :key="index">
            <span class="dot"></span>
            <div>
              <p>{{ item.content }}</p>
              <small>{{ item.type }} · {{ item.time }}</small>
            </div>
          </li>
        </ul>
        <div v-else class="feed-empty">暂无动态，报名、任务完成等操作会实时推送</div>
      </div>
    </section>

    <p class="footer">数据每 30 秒自动刷新 · 事件经由 Redis Stream 异步更新</p>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.screen-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;

  h1 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    letter-spacing: 1px;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .clock {
    padding: 5px 12px;
    font-family: Consolas, monospace;
    font-size: 13px;
    color: var(--el-text-color-regular);
    border: 1px solid var(--el-border-color);
    border-radius: 16px;
  }
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.kpi {
  padding: 16px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;

  .kpi-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .kpi-value {
    display: block;
    margin-top: 8px;
    font-family: Consolas, monospace;
    font-size: 28px;
    line-height: 1;
    color: var(--el-text-color-primary);

    em {
      margin-left: 4px;
      font-size: 14px;
      font-style: normal;
      color: var(--el-text-color-secondary);
    }
  }

  .kpi-hint {
    display: block;
    margin-top: 10px;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
  }
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1.4fr 1fr;
  gap: 14px;
}

.panel {
  padding: 16px 18px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;

  h3 {
    display: flex;
    gap: 8px;
    align-items: center;
    margin: 0 0 10px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .chart {
    height: 280px;
  }
}

.badge {
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 400;
  color: var(--el-color-primary-dark-2);
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
}

.feed {
  height: 280px;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  list-style: none;

  li {
    display: flex;
    gap: 10px;
    padding: 10px 0;
    border-bottom: 1px solid var(--el-border-color-lighter);

    .dot {
      flex: none;
      width: 7px;
      height: 7px;
      margin-top: 6px;
      background: var(--el-color-primary);
      border-radius: 50%;
    }

    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.5;
      color: var(--el-text-color-regular);
    }

    small {
      font-size: 11px;
      color: var(--el-text-color-secondary);
    }
  }
}

.feed-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 280px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.footer {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  text-align: right;
}

@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
