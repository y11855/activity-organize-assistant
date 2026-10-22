<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMyQuestionnaires,
  updateQuestionnaire,
  publishQuestionnaire,
  closeQuestionnaire
} from '@/api/questionnaire'
import { uploadFile } from '@/api/file'
import type { QuestionnaireVO } from '@/types'
import { debounce, formatDate } from '@/utils'

const router = useRouter()
const list = ref<QuestionnaireVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = ref({ id: '', title: '', description: '', coverImage: '' })

const statusMap: Record<number, string> = { 0: '未发布', 1: '已发布', 2: '已截止' }
const statusType: Record<number, 'info' | 'success' | 'danger'> = { 0: 'info', 1: 'success', 2: 'danger' }

async function loadList() {
  loading.value = true
  try {
    list.value = await listMyQuestionnaires()
  } finally {
    loading.value = false
  }
}

function onEdit(row: QuestionnaireVO) {
  form.value = {
    id: row.id,
    title: row.title,
    description: row.description ?? '',
    coverImage: row.coverImage ?? ''
  }
  dialogVisible.value = true
}

/** 前端校验 + 上传封面：格式 JPG/PNG/WebP，大小 ≤5MB */
async function onFileChange(file: { raw?: File }) {
  const raw = file.raw
  if (!raw) return
  const ext = raw.name.includes('.') ? raw.name.split('.').pop()!.toLowerCase() : ''
  if (!['jpg', 'jpeg', 'png', 'webp'].includes(ext)) {
    ElMessage.error('仅支持 JPG、PNG、WebP 格式的图片')
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.error(`图片大小不能超过 5MB，当前图片 ${(raw.size / 1024 / 1024).toFixed(1)}MB`)
    return
  }
  uploading.value = true
  const previous = form.value.coverImage
  try {
    const res = await uploadFile(raw)
    form.value.coverImage = res.url
    // 上传成功后立即落库，无需再点「保存」
    await persistCover()
    ElMessage.success('封面上传成功')
  } catch {
    // 失败则回滚本地预览，保持与数据库一致（错误提示已在拦截器统一弹出）
    form.value.coverImage = previous
  } finally {
    uploading.value = false
  }
}

/** 把封面（连同当前标题、描述）立即写入数据库 */
async function persistCover() {
  await updateQuestionnaire(form.value.id, {
    title: form.value.title,
    description: form.value.description,
    coverImage: form.value.coverImage
  })
  loadList()
}

async function onRemoveCover() {
  const previous = form.value.coverImage
  form.value.coverImage = ''
  try {
    await persistCover()
    ElMessage.success('封面已删除')
  } catch {
    form.value.coverImage = previous
  }
}

const onSave = debounce(async () => {
  if (saving.value) return
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入问卷标题')
    return
  }

  saving.value = true
  try {
    await updateQuestionnaire(form.value.id, {
      title: form.value.title,
      description: form.value.description,
      coverImage: form.value.coverImage
    })
    ElMessage.success('问卷已保存')
    dialogVisible.value = false
    loadList()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    saving.value = false
  }
}, 500)

const onPublish = debounce(async (row: QuestionnaireVO) => {
  if (loading.value) return
  await publishQuestionnaire(row.id)
  ElMessage.success('问卷已发布')
  loadList()
}, 500)

const onClose = debounce(async (row: QuestionnaireVO) => {
  if (loading.value) return
  await ElMessageBox.confirm('确定截止该问卷吗？截止后将无法提交答卷。', '提示', { type: 'warning' })
  await closeQuestionnaire(row.id)
  ElMessage.success('问卷已截止')
  loadList()
}, 500)

onMounted(loadList)
</script>

<template>
  <div v-loading="loading">
    <el-empty v-if="!list.length" description="暂无问卷，请先在活动详情中生成报名问卷" />

    <div v-else class="qn-grid">
      <el-card v-for="q in list" :key="q.id" class="qn-card" shadow="hover">
        <div class="qn-card__cover" @click="router.push(`/questionnaires/${q.id}/fill`)">
          <el-image v-if="q.coverImage" :src="q.coverImage" fit="cover" class="qn-card__img" />
          <div v-else class="qn-card__placeholder">
            <el-icon :size="40"><Picture /></el-icon>
            <span>暂无封面</span>
          </div>
        </div>
        <div class="qn-card__body">
          <div class="qn-card__title">
            <span>{{ q.title }}</span>
            <el-tag :type="statusType[q.status]" size="small">{{ statusMap[q.status] }}</el-tag>
          </div>
          <div class="qn-card__meta">所属活动：{{ q.activityTitle || '-' }}</div>
          <div class="qn-card__meta">报名人数：{{ q.responseCount ?? 0 }} · {{ formatDate(q.createTime) }}</div>
          <div class="qn-card__actions">
            <el-button link type="primary" @click="onEdit(q)">编辑/封面</el-button>
            <el-button link type="primary" @click="router.push(`/questionnaires/${q.id}/fill`)">填写页</el-button>
            <el-button v-if="q.status !== 1" link type="success" @click="onPublish(q)">发布</el-button>
            <el-button v-if="q.status === 1" link type="danger" @click="onClose(q)">截止</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" title="编辑问卷" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="问卷标题" required>
          <el-input v-model="form.title" placeholder="请输入问卷标题" />
        </el-form-item>
        <el-form-item label="问卷描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入问卷描述" />
        </el-form-item>
        <el-form-item label="封面图片">
          <div class="cover-edit">
            <template v-if="form.coverImage">
              <el-image :src="form.coverImage" fit="cover" class="cover-edit__preview" />
              <div class="cover-edit__ops">
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  accept=".jpg,.jpeg,.png,.webp"
                  :on-change="onFileChange"
                >
                  <el-button :loading="uploading">替换</el-button>
                </el-upload>
                <el-button type="danger" plain @click="onRemoveCover">删除</el-button>
              </div>
            </template>
            <el-upload
              v-else
              drag
              :auto-upload="false"
              :show-file-list="false"
              accept=".jpg,.jpeg,.png,.webp"
              :on-change="onFileChange"
            >
              <el-icon :size="32"><Plus /></el-icon>
              <div>点击或拖拽图片上传</div>
              <div class="cover-edit__tip">支持 JPG / PNG / WebP，单张不超过 5MB</div>
            </el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.qn-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.qn-card {
  overflow: hidden;

  :deep(.el-card__body) {
    padding: 0;
  }

  &__cover {
    height: 160px;
    cursor: pointer;
    background: #f5f7fa;
  }

  &__img {
    width: 100%;
    height: 160px;
    display: block;
  }

  &__placeholder {
    height: 160px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
    gap: 8px;
  }

  &__body {
    padding: 12px 16px;
  }

  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    margin-bottom: 8px;
  }

  &__meta {
    color: #909399;
    font-size: 12px;
    line-height: 1.8;
  }

  &__actions {
    margin-top: 8px;
  }
}

.cover-edit {
  width: 100%;

  &__preview {
    width: 240px;
    height: 135px;
    border-radius: 4px;
    display: block;
    margin-bottom: 8px;
  }

  &__ops {
    display: flex;
    gap: 8px;
  }

  &__tip {
    color: #909399;
    font-size: 12px;
  }
}
</style>
