<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getQuestionnaireById, submitResponse } from '@/api/questionnaire'
import type { QuestionnaireVO } from '@/types'

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

async function load() {
  loading.value = true
  try {
    detail.value = await getQuestionnaireById(id)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
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

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="fill">
    <el-card v-if="detail" class="fill__card">
      <el-image v-if="detail.coverImage" :src="detail.coverImage" fit="cover" class="fill__cover" />
      <div class="fill__header">
        <h2>{{ detail.title }}</h2>
        <p v-if="detail.description" class="fill__desc">{{ detail.description }}</p>
        <el-tag v-if="detail.status !== 1" type="info">该问卷未发布或已截止</el-tag>
      </div>

      <template v-if="submitted">
        <el-result icon="success" title="提交成功" sub-title="感谢你的参与！" />
      </template>
      <el-form v-else label-position="top" :disabled="detail.status !== 1">
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

        <el-form-item v-if="questions.length && detail.status === 1">
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

  &__desc {
    color: #606266;
  }
}
</style>
