<template>
  <div class="tree-view">
    <!-- 左侧：活动分解树 -->
    <div class="tree-panel">
      <div class="tree-toolbar">
        <el-button size="small" :icon="Refresh" @click="loadTree">刷新</el-button>
        <span class="tree-tip">点击节点查看详情 · 展开活动查看任务/物料/问卷/提醒</span>
      </div>

      <el-tree
        ref="treeRef"
        :data="treeData"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        :expand-on-click-node="false"
        :highlight-current="true"
        @node-click="handleNodeClick"
      >
        <template #default="{ data }">
          <div class="tree-node" :class="'node-' + data.type">
            <el-icon class="node-icon">
              <component :is="iconFor(data.type)" />
            </el-icon>
            <span class="node-label">{{ data.name }}</span>
            <el-tag v-if="data.status != null" size="small" :type="statusTagType(data.type, data.status)" effect="light" class="node-status">
              {{ statusLabel(data.type, data.status) }}
            </el-tag>
          </div>
        </template>
      </el-tree>

      <el-empty v-if="!loading && treeData.length === 0" description="暂无活动，去「我的活动」创建一个吧" />
    </div>

    <!-- 右侧：节点详情 -->
    <div class="detail-panel">
      <div class="detail-title">
        <el-icon><InfoFilled /></el-icon>
        <span>节点详情</span>
      </div>
      <div v-if="currentNode" class="detail-content">
        <div class="detail-name">{{ currentNode.name }}</div>
        <div class="detail-type">{{ typeLabel(currentNode.type) }}</div>
        <el-descriptions v-if="currentNode.meta && Object.keys(currentNode.meta).length" :column="1" border size="small" class="detail-meta">
          <el-descriptions-item v-for="(val, key) in currentNode.meta" :key="key" :label="key">
            {{ val }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-else class="detail-empty">该节点无额外详情</div>
      </div>
      <el-empty v-else description="点击左侧节点查看详情" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh, InfoFilled, Folder, Files, List, Tickets, Bell, Calendar } from '@element-plus/icons-vue'
import { getActivityBreakdownTree, type BreakdownNode } from '@/api/activity'

const treeRef = ref()
const treeData = ref<BreakdownNode[]>([])
const currentNode = ref<BreakdownNode | null>(null)
const loading = ref(false)

async function loadTree() {
  loading.value = true
  try {
    treeData.value = await getActivityBreakdownTree()
  } finally {
    loading.value = false
  }
}

function handleNodeClick(data: BreakdownNode) {
  currentNode.value = data
}

/** 根据节点类型返回图标 */
function iconFor(type: string) {
  return (
    {
      activity: Calendar,
      category: Folder,
      task: List,
      material: Files,
      questionnaire: Tickets,
      reminder: Bell
    } as Record<string, any>
  )[type] || Folder
}

function typeLabel(type: string) {
  return (
    {
      activity: '活动',
      category: '分类',
      task: '任务',
      material: '物料',
      questionnaire: '问卷',
      reminder: '提醒'
    } as Record<string, string>
  )[type] || '节点'
}

/** 状态文案 */
function statusLabel(type: string, status: number) {
  const maps: Record<string, Record<number, string>> = {
    activity: { 0: '待策划', 1: '策划中', 2: '进行中', 3: '已完成', 4: '已取消' },
    task: { 0: '待开始', 1: '进行中', 2: '已完成', 3: '已逾期' },
    questionnaire: { 0: '未发布', 1: '已发布', 2: '已截止' },
    reminder: { 0: '待触发', 1: '已发送', 2: '已取消', 3: '失败' }
  }
  return maps[type]?.[status] ?? '未知'
}

/** 状态标签颜色 */
function statusTagType(type: string, status: number) {
  // 统一规则：完成/成功=success，进行中/待触发=primary，待开始/未发布=info，逾期/失败/取消=danger
  if (type === 'activity') {
    return status === 3 ? 'success' : status === 4 ? 'danger' : status === 2 ? 'primary' : 'info'
  }
  if (type === 'task') {
    return status === 2 ? 'success' : status === 3 ? 'danger' : status === 1 ? 'primary' : 'info'
  }
  if (type === 'questionnaire') {
    return status === 1 ? 'success' : status === 2 ? 'info' : 'warning'
  }
  if (type === 'reminder') {
    return status === 1 ? 'success' : status === 3 ? 'danger' : 'primary'
  }
  return 'info'
}

onMounted(loadTree)
</script>

<style scoped>
.tree-view {
  display: flex;
  gap: 16px;
  height: 100%;
}
.tree-panel {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  overflow: auto;
}
.tree-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.tree-tip {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  padding-right: 8px;
}
.node-icon {
  color: var(--el-color-primary);
}
.node-activity .node-icon { color: #722ed1; }
.node-task .node-icon { color: #409eff; }
.node-material .node-icon { color: #e6a23c; }
.node-questionnaire .node-icon { color: #67c23a; }
.node-reminder .node-icon { color: #f56c6c; }
.node-category .node-icon { color: #909399; }
.node-label {
  font-size: 14px;
}
.node-status {
  margin-left: auto;
  transform: scale(0.85);
}
.detail-panel {
  width: 340px;
  flex-shrink: 0;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
}
.detail-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-color-primary);
}
.detail-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}
.detail-type {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}
.detail-meta {
  margin-top: 8px;
}
.detail-empty {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  padding: 16px 0;
  text-align: center;
}
</style>
