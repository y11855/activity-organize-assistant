<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listRoles,
  listPermissions,
  createRole,
  updateRole,
  deleteRole,
  assignRolePermissions
} from '@/api/admin'
import { useUserStore } from '@/stores'
import type { PermissionVO, RoleVO } from '@/types'
import { debounce } from '@/utils'

const userStore = useUserStore()
const canManage = computed(() => userStore.hasPermission('system:role:manage'))

const loading = ref(false)
const roles = ref<RoleVO[]>([])
const permissions = ref<PermissionVO[]>([])

const formVisible = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const form = ref({ code: '', name: '', description: '' })

const permVisible = ref(false)
const permSaving = ref(false)
const permTarget = ref<RoleVO | null>(null)
const checkedCodes = ref<string[]>([])

const groupedPermissions = computed(() => {
  const groups: { name: string; items: PermissionVO[] }[] = []
  for (const p of permissions.value) {
    let group = groups.find((g) => g.name === p.groupName)
    if (!group) {
      group = { name: p.groupName, items: [] }
      groups.push(group)
    }
    group.items.push(p)
  }
  return groups
})

const isAdminRole = computed(() => permTarget.value?.code === 'admin')

async function loadAll() {
  loading.value = true
  try {
    const [roleList, permissionList] = await Promise.all([listRoles(), listPermissions()])
    roles.value = roleList
    permissions.value = permissionList
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = { code: '', name: '', description: '' }
  formVisible.value = true
}

function openEdit(row: RoleVO) {
  editingId.value = row.id
  form.value = { code: row.code, name: row.name, description: row.description ?? '' }
  formVisible.value = true
}

async function doSubmitForm() {
  if (!editingId.value && !/^[a-z][a-z0-9_]{2,31}$/.test(form.value.code)) {
    ElMessage.warning('角色编码需为 3-32 位小写字母、数字或下划线，且以字母开头')
    return
  }
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入角色名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      code: form.value.code,
      name: form.value.name.trim(),
      description: form.value.description || undefined
    }
    if (editingId.value) {
      await updateRole(editingId.value, payload)
      ElMessage.success('角色已更新')
    } else {
      await createRole(payload)
      ElMessage.success('角色已创建')
    }
    formVisible.value = false
    loadAll()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    saving.value = false
  }
}

async function doDeleteRole(row: RoleVO) {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRole(row.id)
    ElMessage.success('角色已删除')
    loadAll()
  } catch {
    // 错误提示已在拦截器统一弹出
  }
}

function openPermissions(row: RoleVO) {
  permTarget.value = row
  checkedCodes.value = [...row.permissionCodes]
  permVisible.value = true
}

async function doSubmitPermissions() {
  if (!permTarget.value || permSaving.value) return
  permSaving.value = true
  try {
    await assignRolePermissions(permTarget.value.id, checkedCodes.value)
    ElMessage.success('权限已保存')
    permVisible.value = false
    loadAll()
  } catch {
    // 错误提示已在拦截器统一弹出
  } finally {
    permSaving.value = false
  }
}

const submitForm = debounce(doSubmitForm)
const onDelete = debounce(doDeleteRole)
const submitPermissions = debounce(doSubmitPermissions)

onMounted(loadAll)
</script>

<template>
  <el-card>
    <template #header>
      <div class="role__header">
        <span>角色权限</span>
        <el-button v-if="canManage" type="primary" @click="openCreate">新建角色</el-button>
      </div>
    </template>

    <el-table :data="roles" v-loading="loading" border>
      <el-table-column prop="name" label="角色名称" min-width="140" />
      <el-table-column prop="code" label="角色编码" min-width="140" />
      <el-table-column label="说明" min-width="200">
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="用户数" width="80" align="center">
        <template #default="{ row }">{{ row.userCount }}</template>
      </el-table-column>
      <el-table-column label="权限数" width="80" align="center">
        <template #default="{ row }">
          {{ row.code === 'admin' ? permissions.length : row.permissionCodes.length }}
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.builtin === 1 ? 'info' : 'success'">
            {{ row.builtin === 1 ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canManage" label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row as RoleVO)">编辑</el-button>
          <el-button
            link
            type="primary"
            :disabled="row.code === 'admin'"
            @click="openPermissions(row as RoleVO)"
          >
            分配权限
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.builtin === 1"
            @click="onDelete(row as RoleVO)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无角色" />
      </template>
    </el-table>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑角色' : '新建角色'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色编码">
          <el-input
            v-model="form.code"
            :disabled="!!editingId"
            placeholder="如 auditor，创建后不可修改"
          />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.name" placeholder="如 审核员" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" title="分配权限" width="560px">
      <el-alert
        v-if="isAdminRole"
        title="内置管理员角色始终拥有全部权限，无需分配"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <div v-for="group in groupedPermissions" :key="group.name" class="perm__group">
        <div class="perm__group-title">{{ group.name }}</div>
        <el-checkbox-group v-model="checkedCodes" :disabled="isAdminRole">
          <el-checkbox v-for="p in group.items" :key="p.code" :value="p.code">
            {{ p.name }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="permSaving"
          :disabled="isAdminRole"
          @click="submitPermissions"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped lang="scss">
.role {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
}

.perm {
  &__group {
    margin-bottom: 16px;
  }

  &__group-title {
    font-weight: 600;
    margin-bottom: 8px;
  }
}
</style>
