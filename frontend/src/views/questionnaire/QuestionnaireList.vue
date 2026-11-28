<!-- 问卷列表页：全部已发布 / 为你推荐 / 我报名的 / 我点赞的 / 我发布的 -->
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMyQuestionnaires,
  listPublishedQuestionnaires,
  listRegisteredQuestionnaires,
  listLikedQuestionnaires,
  listRecommendedQuestionnaires,
  updateQuestionnaire,
  publishQuestionnaire,
  closeQuestionnaire,
  likeQuestionnaire,
  unlikeQuestionnaire
} from '@/api/questionnaire'
import { uploadFile } from '@/api/file'
import type { QuestionnaireVO } from '@/types'
import { debounce, formatDate } from '@/utils'

type TabKey = 'all' | 'recommend' | 'registered' | 'liked' | 'mine'

interface TabState {
  list: QuestionnaireVO[]
  page: number
  total: number
  loading: boolean
  loaded: boolean
}

const TABS: { key: TabKey; label: string; empty: string }[] = [
  { key: 'all', label: '全部已发布', empty: '暂无已发布的报名问卷' },
  { key: 'recommend', label: '为你推荐', empty: '暂无推荐，先去浏览并点赞感兴趣的活动吧' },
  { key: 'registered', label: '我报名的', empty: '你还没有报名任何活动' },
  { key: 'liked', label: '我点赞的', empty: '你还没有点赞任何问卷' },
  { key: 'mine', label: '我发布的', empty: '暂无问卷，请先在活动详情中生成报名问卷' }
]

const LOADERS: Record<
  TabKey,
  (params: { page: number; size: number }) => Promise<{ records: QuestionnaireVO[]; total: number }>
> = {
  all: listPublishedQuestionnaires,
  recommend: listRecommendedQuestionnaires,
  registered: listRegisteredQuestionnaires,
  liked: listLikedQuestionnaires,
  mine: listMyQuestionnaires
}

const router = useRouter()
const size = 10
const activeTab = ref<TabKey>('all')

/** 每个 tab 独立维护列表与分页，切换 tab 时不互相覆盖 */
const tabStates = reactive<Record<TabKey, TabState>>({
  all: { list: [], page: 1, total: 0, loading: false, loaded: false },
  recommend: { list: [], page: 1, total: 0, loading: false, loaded: false },
  registered: { list: [], page: 1, total: 0, loading: false, loaded: false },
  liked: { list: [], page: 1, total: 0, loading: false, loaded: false },
  mine: { list: [], page: 1, total: 0, loading: false, loaded: false }
})

const current = computed(() => tabStates[activeTab.value])
const currentTab = computed(() => TABS.find((tab) => tab.key === activeTab.value) as (typeof TABS)[number])

async function loadTab(key: TabKey) {
  const state = tabStates[key]
  state.loading = true
  try {
    const res = await LOADERS[key]({ page: state.page, size })
    state.list = res.records
    state.total = res.total
    state.loaded = true
  } finally {
    state.loading = false
  }
}

/** 首次切换到某个 tab 时才请求，避免一次加载全部数据 */
function onTabChange(value: string | number | boolean | undefined) {
  const key = value as TabKey
  if (key && !tabStates[key].loaded) {
    loadTab(key)
  }
}

const dialogVisible = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = ref({ id: '', title: '', description: '', coverImage: '' })

const statusMap: Record<number, string> = { 0: '未发布', 1: '已发布', 2: '已截止' }
const statusType: Record<number, 'info' | 'success' | 'danger'> = { 0: 'info', 1: 'success', 2: 'danger' }

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
  loadTab('mine')
}

const onRemoveCover = debounce(async () => {
  const previous = form.value.coverImage
  form.value.coverImage = ''
  try {
    await persistCover()
    ElMessage.success('封面已删除')
  } catch {
    form.value.coverImage = previous
  }
}, 300)

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
    loadTab('mine')
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    saving.value = false
  }
}, 500)

const onPublish = debounce(async (row: QuestionnaireVO) => {
  if (current.value.loading) return
  await publishQuestionnaire(row.id)
  ElMessage.success('问卷已发布')
  loadTab('mine')
}, 500)

const onClose = debounce(async (row: QuestionnaireVO) => {
  if (current.value.loading) return
  await ElMessageBox.confirm('确定截止该问卷吗？截止后将无法提交答卷。', '提示', { type: 'warning' })
  await closeQuestionnaire(row.id)
  ElMessage.success('问卷已截止')
  loadTab('mine')
}, 500)

/** 点赞 / 取消点赞，乐观更新卡片上的点赞数与状态 */
async function toggleLike(q: QuestionnaireVO) {
  try {
    if (q.liked) {
      await unlikeQuestionnaire(q.id)
      q.liked = false
      q.likeCount = Math.max((q.likeCount ?? 1) - 1, 0)
      // 「我点赞的」列表取消点赞后立即移除该卡片
      if (activeTab.value === 'liked') {
        current.value.list = current.value.list.filter((item) => item.id !== q.id)
        current.value.total = Math.max(current.value.total - 1, 0)
      }
    } else {
      await likeQuestionnaire(q.id)
      q.liked = true
      q.likeCount = (q.likeCount ?? 0) + 1
    }
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

onMounted(() => {
  loadTab('all')
})
</script>

<template>
  <div v-loading="current.loading">
    <div class="qn-toolbar">
      <el-radio-group v-model="activeTab" @change="onTabChange">
        <el-radio-button v-for="tab in TABS" :key="tab.key" :value="tab.key">{{ tab.label }}</el-radio-button>
      </el-radio-group>
      <span class="qn-toolbar__tip">「为你推荐」依据你的浏览时长与点赞行为实时计算</span>
    </div>

    <el-empty v-if="!current.list.length" :description="currentTab.empty" />

    <div v-else class="qn-grid">
      <el-card v-for="q in current.list" :key="q.id" class="qn-card" shadow="hover">
        <div class="qn-card__cover" @click="router.push(`/questionnaires/${q.id}/fill`)">
          <el-image v-if="q.coverImage" :src="q.coverImage" fit="cover" class="qn-card__img" />
          <div v-else class="qn-card__placeholder">
            <el-icon :size="40"><Picture /></el-icon>
            <span>暂无封面</span>
          </div>
        </div>
        <div class="qn-card__body">
          <div class="qn-card__title">
            <span class="qn-card__name">{{ q.title }}</span>
            <el-tag :type="statusType[q.status]" size="small">{{ statusMap[q.status] }}</el-tag>
          </div>
          <div class="qn-card__meta">
            所属活动：{{ q.activityTitle || '-' }}
            <!-- 活动属性标签：参与者一眼识别活动类型 -->
            <el-tag v-if="q.activityTagName" size="small" effect="plain" class="qn-card__tag">
              {{ q.activityTagName }}
            </el-tag>
            <el-tag v-if="q.registered" size="small" type="success" class="qn-card__tag">已报名</el-tag>
          </div>
          <div class="qn-card__meta">报名人数：{{ q.responseCount ?? 0 }} · {{ formatDate(q.createTime) }}</div>
          <div class="qn-card__actions">
            <el-button link type="primary" @click="router.push(`/questionnaires/${q.id}/fill`)">填写</el-button>
            <el-button link :type="q.liked ? 'danger' : 'primary'" @click="toggleLike(q)">
              {{ q.liked ? '已点赞' : '点赞' }} {{ q.likeCount ?? 0 }}
            </el-button>
            <template v-if="activeTab === 'mine'">
              <el-button link type="primary" @click="onEdit(q)">编辑/封面</el-button>
              <el-button v-if="q.status !== 1" link type="success" @click="onPublish(q)">发布</el-button>
              <el-button v-if="q.status === 1" link type="danger" @click="onClose(q)">截止</el-button>
            </template>
          </div>
        </div>
      </el-card>
    </div>

    <div v-if="current.total > 0" class="list-pagination">
      <el-pagination
        v-model:current-page="current.page"
        :page-size="size"
        :total="current.total"
        layout="total, prev, pager, next"
        @change="loadTab(activeTab)"
      />
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
.qn-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;

  &__tip {
    color: #909399;
    font-size: 12px;
  }
}

.qn-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.list-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
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
    gap: 8px;
    font-weight: 600;
    margin-bottom: 8px;
  }

  &__name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    color: #909399;
    font-size: 12px;
    line-height: 1.8;
  }

  &__tag {
    margin-left: 6px;
  }

  &__actions {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
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
