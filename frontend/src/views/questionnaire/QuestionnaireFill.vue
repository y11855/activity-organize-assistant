<!-- 问卷填写页：渲染题目、校验并提交报名问卷 -->
<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getQuestionnaireById,
  submitResponse,
  likeQuestionnaire,
  unlikeQuestionnaire
} from '@/api/questionnaire'
import { reportBehavior } from '@/api/behavior'
import type { QuestionnaireVO } from '@/types'
import { debounce, formatDate } from '@/utils'

interface Question {
  title: string
  type: 'single' | 'multiple' | 'text'
  required?: boolean
  options?: string[]
}

const route = useRoute()
const id = String(route.params.id)
const detail = ref<QuestionnaireVO | null>(null)
const loading = ref(false)
const submitting = ref(false)
const submitted = ref(false)
// value: 单选/填空为 string，多选为 string[]
const answers = ref<Record<number, any>>({})

const questions = computed<Question[]>(() => {
  if (!detail.value?.questions) return []
  try {
    const parsed = JSON.parse(detail.value.questions)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

/** 活动是否已截止（结束时间已到）：截止后不可再报名 */
const expired = computed(() => {
  if (!detail.value?.endTime) return false
  return new Date(detail.value.endTime).getTime() <= Date.now()
})

async function load() {
  loading.value = true
  try {
    detail.value = await getQuestionnaireById(id)
  } finally {
    loading.value = false
  }
}

async function doSubmit() {
  if (submitting.value) return
  // 必填校验
  for (let i = 0; i < questions.value.length; i++) {
    const q = questions.value[i]
    if (q.required) {
      const v = answers.value[i]
      if (v === undefined || v === '' || (Array.isArray(v) && v.length === 0)) {
        ElMessage.warning(`第 ${i + 1} 题「${q.title}」为必答题`)
        return
      }
    }
  }
  submitting.value = true
  try {
    const payload = questions.value.map((q, i) => ({ question: q.title, answer: answers.value[i] ?? '' }))
    await submitResponse(id, JSON.stringify(payload))
    submitted.value = true
    ElMessage.success('提交成功，感谢参与')
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

const onSubmit = debounce(doSubmit, 300)

/** 点赞 / 取消点赞：点赞行为由后端一并上报 Kafka，进入推荐计算 */
const liking = ref(false)

async function toggleLike() {
  if (!detail.value || liking.value) return
  liking.value = true
  const target = detail.value
  try {
    if (target.liked) {
      await unlikeQuestionnaire(target.id)
      target.liked = false
      target.likeCount = Math.max((target.likeCount ?? 1) - 1, 0)
    } else {
      await likeQuestionnaire(target.id)
      target.liked = true
      target.likeCount = (target.likeCount ?? 0) + 1
    }
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    liking.value = false
  }
}

/** 进入页面时间：离开时上报停留时长，供 Flink 计算兴趣得分 */
const enterAt = Date.now()

onBeforeUnmount(() => {
  if (!detail.value) return
  const seconds = Math.round((Date.now() - enterAt) / 1000)
  // 过短的停留无参考价值，不上报
  if (seconds < 3) return
  reportBehavior({ questionnaireId: detail.value.id, type: 'view', durationSeconds: seconds }).catch(() => {})
})

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="fill">
    <el-card v-if="detail" class="fill__card">
      <el-image v-if="detail.coverImage" :src="detail.coverImage" fit="cover" class="fill__cover" />
      <!-- 活动详情：向参与者展示活动是什么、何时何地举行，吸引报名 -->
      <div v-if="detail.activityTitle || detail.introduction" class="fill__activity">
        <h3 v-if="detail.activityTitle" class="fill__activity-title">
          {{ detail.activityTitle }}
          <!-- 活动属性标签：发布后报名页展示 -->
          <el-tag v-if="detail.activityTagName" size="small" effect="plain" class="fill__tag">
            {{ detail.activityTagName }}
          </el-tag>
        </h3>
        <p v-if="detail.introduction" class="fill__activity-intro">{{ detail.introduction }}</p>
        <div class="fill__activity-meta">
          <span v-if="detail.startTime || detail.endTime" class="fill__meta-item">
            🕒 {{ detail.startTime ? formatDate(detail.startTime) : '-' }} ~
            {{ detail.endTime ? formatDate(detail.endTime) : '-' }}
          </span>
          <span v-if="detail.location" class="fill__meta-item">📍 {{ detail.location }}</span>
        </div>
      </div>
      <div class="fill__header">
        <div class="fill__title-row">
          <h2>{{ detail.title }}</h2>
          <!-- 点赞：参与者对感兴趣的活动点赞，可提升同类活动的推荐排序 -->
          <el-button
            round
            size="small"
            :type="detail.liked ? 'danger' : 'default'"
            :loading="liking"
            @click="toggleLike"
          >
            <el-icon v-if="detail.liked"><StarFilled /></el-icon>
            <el-icon v-else><Star /></el-icon>
            {{ detail.liked ? '已点赞' : '点赞' }} {{ detail.likeCount ?? 0 }}
          </el-button>
        </div>
        <p v-if="detail.description" class="fill__desc">{{ detail.description }}</p>
        <el-tag v-if="expired" type="danger">活动已截止，无法报名</el-tag>
        <el-tag v-else-if="detail.status !== 1" type="info">该问卷未发布或已截止</el-tag>
      </div>

      <template v-if="submitted">
        <el-result icon="success" title="提交成功" sub-title="感谢你的参与！" />
      </template>
      <el-form v-else label-position="top" :disabled="detail.status !== 1 || expired">
        <el-form-item
          v-for="(q, i) in questions"
          :key="i"
          :label="`${i + 1}. ${q.title}${q.required ? '（必答）' : ''}`"
          :required="q.required"
        >
          <el-radio-group v-if="q.type === 'single'" v-model="answers[i]">
            <el-radio v-for="opt in q.options ?? []" :key="opt" :value="opt">{{ opt }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="q.type === 'multiple'" v-model="answers[i]">
            <el-checkbox v-for="opt in q.options ?? []" :key="opt" :value="opt">{{ opt }}</el-checkbox>
          </el-checkbox-group>
          <el-input v-else v-model="answers[i]" type="textarea" :rows="3" placeholder="请输入" />
        </el-form-item>

        <el-empty v-if="!questions.length" description="暂无题目" />

        <el-form-item v-if="questions.length && detail.status === 1 && !expired">
          <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.fill {
  display: flex;
  justify-content: center;

  &__card {
    width: 720px;
    max-width: 100%;
  }

  &__cover {
    width: 100%;
    height: 240px;
    border-radius: 4px;
    display: block;
    margin-bottom: 16px;
  }

  &__header {
    margin-bottom: 16px;
  }

  &__title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;

    h2 {
      margin: 0;
    }
  }

  &__tag {
    margin-left: 8px;
    vertical-align: middle;
  }

  &__desc {
    color: #606266;
  }

  &__activity {
    padding: 16px;
    margin-bottom: 16px;
    background: var(--el-fill-color-light);
    border-radius: 6px;
  }

  &__activity-title {
    margin: 0 0 8px;
    font-size: 18px;
  }

  &__activity-intro {
    margin: 0 0 10px;
    color: var(--el-text-color-regular);
    line-height: 1.7;
    white-space: pre-wrap;
  }

  &__activity-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16px;
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}
</style>
