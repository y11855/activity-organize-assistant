<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getActivityDetail, listEventLogs } from '@/api/activity'
import { listActivityFiles } from '@/api/file'
import { listQuestionnaires } from '@/api/questionnaire'
import type { ActivityEventLogVO, ActivityFileVO, ActivityVO, QuestionnaireVO } from '@/types'
import { formatDate } from '@/utils'

type TagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

/** 树节点：label 显示名称，extra 右侧补充信息，tag 状态标签 */
interface TreeNode {
  key: string
  label: string
  icon?: string
  extra?: string
  tag?: { text: string; type: TagType }
  children?: TreeNode[]
}

const props = defineProps<{ activityId: string }>()

const ACTIVITY_STATUS: Record<number, { text: string; type: TagType }> = {
  0: { text: '草稿', type: 'info' },
  1: { text: '策划中', type: 'primary' },
  2: { text: '执行中', type: 'success' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已取消', type: 'danger' },
  5: { text: '待确认', type: 'warning' }
}

const TASK_STATUS: Record<number, { text: string; type: TagType }> = {
  0: { text: '待开始', type: 'info' },
  1: { text: '进行中', type: 'primary' },
  2: { text: '已完成', type: 'success' },
  3: { text: '已逾期', type: 'danger' }
}

const QUESTIONNAIRE_STATUS: Record<number, { text: string; type: TagType }> = {
  0: { text: '未发布', type: 'info' },
  1: { text: '已发布', type: 'success' },
  2: { text: '已截止', type: 'danger' }
}

const EVENT_TYPE: Record<number, { text: string; type: TagType }> = {
  1: { text: '普通事件', type: 'info' },
  2: { text: '关键节点', type: 'primary' },
  3: { text: '问题', type: 'danger' },
  4: { text: '亮点', type: 'success' }
}

const loading = ref(false)
const detail = ref<ActivityVO | null>(null)
const files = ref<ActivityFileVO[]>([])
const questionnaires = ref<QuestionnaireVO[]>([])
const eventLogs = ref<ActivityEventLogVO[]>([])

/** 四个数据源并行加载，单个失败不影响其它分支展示 */
async function load() {
  if (!props.activityId) return
  loading.value = true
  const [activity, fileList, questionnaireList, logs] = await Promise.allSettled([
    getActivityDetail(props.activityId),
    listActivityFiles(props.activityId),
    listQuestionnaires(props.activityId),
    listEventLogs(props.activityId)
  ])
  detail.value = activity.status === 'fulfilled' ? activity.value : null
  files.value = fileList.status === 'fulfilled' ? fileList.value : []
  questionnaires.value = questionnaireList.status === 'fulfilled' ? questionnaireList.value : []
  eventLogs.value = logs.status === 'fulfilled' ? logs.value : []
  loading.value = false
}

watch(() => props.activityId, load, { immediate: true })

function formatSize(bytes: number) {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)}MB`
  return `${Math.round(bytes / 1024)}KB`
}

const tree = computed<TreeNode[]>(() => {
  const d = detail.value
  if (!d) return []

  const overview: TreeNode[] = [
    {
      key: 'time',
      label: '活动时间',
      icon: 'Clock',
      extra: `${d.startTime ? formatDate(d.startTime) : '-'} ~ ${d.endTime ? formatDate(d.endTime) : '-'}`
    },
    { key: 'location', label: '活动地点', icon: 'Location', extra: d.location || '未填写' },
    { key: 'count', label: '预计人数', icon: 'User', extra: d.expectedCount ? `${d.expectedCount} 人` : '未填写' },
    { key: 'members', label: '参与成员', icon: 'UserFilled', extra: d.memberNames?.length ? d.memberNames.join('、') : '暂无' }
  ]

  const tasks: TreeNode[] = d.tasks?.length
    ? d.tasks.map((t) => ({
        key: `task-${t.id}`,
        label: t.title,
        icon: 'List',
        extra: `负责人：${t.assigneeName || '待分配'} · 截止：${t.deadline ? formatDate(t.deadline) : '-'}`,
        tag: TASK_STATUS[t.status] ?? { text: '未知', type: 'info' }
      }))
    : [{ key: 'task-empty', label: '暂无任务分工' }]

  const materials: TreeNode[] = d.materials?.length
    ? d.materials.map((m) => ({
        key: `material-${m.id}`,
        label: m.name,
        icon: 'Box',
        extra: `${m.quantity}${m.unit || ''}${m.unitPrice != null ? ` · 单价 ¥${m.unitPrice}` : ''}`
      }))
    : [{ key: 'material-empty', label: '暂无物料清单' }]

  const questionnaireNodes: TreeNode[] = questionnaires.value.length
    ? questionnaires.value.map((q) => ({
        key: `questionnaire-${q.id}`,
        label: q.title,
        icon: 'Tickets',
        extra: `报名 ${q.responseCount ?? 0} 人`,
        tag: QUESTIONNAIRE_STATUS[q.status] ?? { text: '未知', type: 'info' }
      }))
    : [{ key: 'questionnaire-empty', label: '暂无报名问卷' }]

  const fileNodes: TreeNode[] = files.value.length
    ? files.value.map((f) => ({
        key: `file-${f.id}`,
        label: f.name,
        icon: 'Document',
        extra: `${formatSize(f.size)} · ${f.uploaderName || '-'}`
      }))
    : [{ key: 'file-empty', label: '暂无资料文件' }]

  const eventNodes: TreeNode[] = eventLogs.value.length
    ? eventLogs.value.map((e) => ({
        key: `event-${e.id}`,
        label: e.content,
        icon: 'Bell',
        extra: e.occurredAt ? formatDate(e.occurredAt) : '未记录时间',
        tag: EVENT_TYPE[e.eventType] ?? { text: '普通事件', type: 'info' }
      }))
    : [{ key: 'event-empty', label: '暂无事件记录' }]

  return [
    {
      key: 'root',
      label: d.title,
      icon: 'Calendar',
      tag: ACTIVITY_STATUS[d.status] ?? { text: '未知', type: 'info' },
      children: [
        { key: 'overview', label: '活动概况', icon: 'InfoFilled', children: overview },
        { key: 'tasks', label: `任务分工（${d.tasks?.length ?? 0}）`, icon: 'List', children: tasks },
        { key: 'materials', label: `物料清单（${d.materials?.length ?? 0}）`, icon: 'Box', children: materials },
        {
          key: 'questionnaires',
          label: `报名问卷（${questionnaires.value.length}）`,
          icon: 'Tickets',
          children: questionnaireNodes
        },
        { key: 'files', label: `资料文件（${files.value.length}）`, icon: 'Folder', children: fileNodes },
        { key: 'events', label: `事件记录（${eventLogs.value.length}）`, icon: 'Bell', children: eventNodes }
      ]
    }
  ]
})
</script>

<template>
  <div v-loading="loading" class="activity-tree">
    <el-tree
      v-if="tree.length"
      :data="tree"
      node-key="key"
      default-expand-all
      :expand-on-click-node="false"
      :props="{ label: 'label', children: 'children' }"
    >
      <template #default="{ data }">
        <span class="tree-node">
          <el-icon v-if="data.icon"><component :is="data.icon" /></el-icon>
          <span class="tree-node__label">{{ data.label }}</span>
          <el-tag v-if="data.tag" size="small" :type="data.tag.type">{{ data.tag.text }}</el-tag>
          <span v-if="data.extra" class="tree-node__extra">{{ data.extra }}</span>
        </span>
      </template>
    </el-tree>
    <el-empty v-else-if="!loading" description="暂无活动数据" :image-size="70" />
  </div>
</template>

<style scoped lang="scss">
.activity-tree {
  min-height: 140px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  &__label {
    font-weight: 500;
  }

  &__extra {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}
</style>
