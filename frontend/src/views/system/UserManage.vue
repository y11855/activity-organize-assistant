<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAdminUsers,
  createAdminUser,
  assignUserRoles,
  updateUserStatus,
  resetUserPassword,
  deleteAdminUser,
  listRoles
} from '@/api/admin'
import { useUserStore } from '@/stores'
import type { RoleVO, UserAdminVO } from '@/types'
import { debounce, formatDate } from '@/utils'
import { encryptPassword } from '@/utils/loginCrypto'

const userStore = useUserStore()
const canManage = computed(() => userStore.hasPermission('system:user:manage'))

const loading = ref(false)
const list = ref<UserAdminVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref<number | ''>('')

const roles = ref<RoleVO[]>([])

const createVisible = ref(false)
const creating = ref(false)
const createForm = ref({
  username: '',
  nickname: '',
  password: '',
  roleIds: [] as string[],
  status: 1
})

const roleVisible = ref(false)
const roleSaving = ref(false)
const roleTarget = ref<UserAdminVO | null>(null)
const selectedRoleIds = ref<string[]>([])

const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdTarget = ref<UserAdminVO | null>(null)
const newPassword = ref('')

async function loadList() {
  loading.value = true
  try {
    const res = await pageAdminUsers({
      keyword: keyword.value || undefined,
      status: statusFilter.value === '' ? undefined : statusFilter.value,
      page: page.value,
      size: size.value
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  if (roles.value.length) return
  roles.value = await listRoles()
}

function onSearch() {
  page.value = 1
  loadList()
}

function openCreate() {
  loadRoles()
  createForm.value = { username: '', nickname: '', password: '', roleIds: [], status: 1 }
  createVisible.value = true
}

async function doSubmitCreate() {
  const form = createForm.value
  if (!/^[a-zA-Z0-9_]{4,20}$/.test(form.username)) {
    ElMessage.warning('用户名需为 4-20 位字母、数字或下划线')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }
  if (!form.roleIds.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  if (creating.value) return
  creating.value = true
  try {
    const encryptedPassword = await encryptPassword(form.password)
    await createAdminUser({
      username: form.username,
      nickname: form.nickname || undefined,
      password: encryptedPassword,
      roleIds: form.roleIds,
      status: form.status
    })
    ElMessage.success('用户创建成功')
    createVisible.value = false
    onSearch()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    creating.value = false
  }
}

async function openRoles(row: UserAdminVO) {
  await loadRoles()
  roleTarget.value = row
  selectedRoleIds.value = [...row.roleIds]
  roleVisible.value = true
}

async function doSubmitRoles() {
  if (!roleTarget.value) return
  if (!selectedRoleIds.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  roleSaving.value = true
  try {
    await assignUserRoles(roleTarget.value.id, selectedRoleIds.value)
    ElMessage.success('角色已更新')
    roleVisible.value = false
    loadList()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    roleSaving.value = false
  }
}

async function doToggleStatus(row: UserAdminVO) {
  const next = row.status === 1 ? 0 : 1
  if (next === 0) {
    try {
      await ElMessageBox.confirm(`确定禁用账号「${row.username}」？禁用后该用户将立即下线。`, '提示', {
        type: 'warning'
      })
    } catch {
      return
    }
  }
  try {
    await updateUserStatus(row.id, next)
    ElMessage.success(next === 1 ? '账号已启用' : '账号已禁用')
    loadList()
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

function openReset(row: UserAdminVO) {
  pwdTarget.value = row
  newPassword.value = ''
  pwdVisible.value = true
}

async function doSubmitReset() {
  if (!pwdTarget.value) return
  if (newPassword.value.length < 6 || newPassword.value.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }
  pwdSaving.value = true
  try {
    const encryptedPassword = await encryptPassword(newPassword.value)
    await resetUserPassword(pwdTarget.value.id, encryptedPassword)
    ElMessage.success('密码已重置，该用户需重新登录')
    pwdVisible.value = false
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    pwdSaving.value = false
  }
}

async function doDeleteUser(row: UserAdminVO) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」？此操作不可恢复。`, '提示', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await deleteAdminUser(row.id)
    ElMessage.success('用户已删除')
    loadList()
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

const submitCreate = debounce(doSubmitCreate)
const submitRoles = debounce(doSubmitRoles)
const toggleStatus = debounce(doToggleStatus)
const submitReset = debounce(doSubmitReset)
const onDelete = debounce(doDeleteUser)

onMounted(loadList)
</script>

<template>
  <el-card>
    <template #header>
      <div class="user__header">
        <span>用户管理</span>
        <div class="user__toolbar">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名 / 昵称"
            clearable
            style="width: 200px"
            @keyup.enter="onSearch"
          />
          <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="全部状态" :value="''" />
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
          <el-button @click="onSearch">查询</el-button>
          <el-button v-if="canManage" type="primary" @click="openCreate">新建用户</el-button>
        </div>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="name in row.roleNames" :key="name" size="small" class="user__tag">
            {{ name }}
          </el-tag>
          <span v-if="!row.roleNames?.length" class="user__muted">未分配</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="260" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row as UserAdminVO)">分配角色</el-button>
          <el-button link type="warning" @click="toggleStatus(row as UserAdminVO)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="openReset(row as UserAdminVO)">重置密码</el-button>
          <el-button link type="danger" @click="onDelete(row as UserAdminVO)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无用户" />
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

    <el-dialog v-model="createVisible" title="新建用户" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="createForm.username" placeholder="4-20 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="createForm.nickname" placeholder="选填，默认与用户名相同" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="createForm.password" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="createForm.status" :active-value="1" :inactive-value="0" />
          <span class="user__muted" style="margin-left: 8px">
            {{ createForm.status === 1 ? '启用' : '禁用' }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ roleTarget?.nickname }}（{{ roleTarget?.username }}）</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ pwdTarget?.nickname }}（{{ pwdTarget?.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" placeholder="6-20 位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdSaving" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped lang="scss">
.list-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.user {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__toolbar {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  &__tag {
    margin-right: 6px;
  }

  &__muted {
    color: #909399;
    font-size: 12px;
  }
}
</style>
