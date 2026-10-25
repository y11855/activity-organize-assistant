<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { listMyMessages, getUnreadCount, markMessageRead, markAllMessagesRead } from '@/api/message'
import { logout as apiLogout, getCurrentUser } from '@/api/auth'
import type { MessageVO } from '@/types'
import { debounce, formatDate } from '@/utils'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

interface MenuItem {
  path: string
  title: string
  icon?: string
}

/** 一级菜单（无分组，按权限过滤） */
const topMenus = computed(() => {
  const items = router.getRoutes()
    .flatMap((r) => r.children ?? [])
    .filter((r) => !r.meta?.hidden && !r.meta?.group)
    .filter((r) => {
      const permission = r.meta?.permission as string | undefined
      return !permission || userStore.hasPermission(permission)
    })
    .map<MenuItem>((r) => ({
      path: '/' + r.path,
      title: r.meta?.title as string,
      icon: r.meta?.icon as string | undefined
    }))
  return items
})

/** 分组菜单（如「系统管理」），整组无可见项时不渲染 */
const menuGroups = computed(() => {
  const groups: { name: string; items: MenuItem[] }[] = []
  router.getRoutes()
    .flatMap((r) => r.children ?? [])
    .filter((r) => !r.meta?.hidden && r.meta?.group)
    .filter((r) => {
      const permission = r.meta?.permission as string | undefined
      return !permission || userStore.hasPermission(permission)
    })
    .forEach((r) => {
      const name = r.meta?.group as string
      let group = groups.find((g) => g.name === name)
      if (!group) {
        group = { name, items: [] }
        groups.push(group)
      }
      group.items.push({
        path: '/' + r.path,
        title: r.meta?.title as string,
        icon: r.meta?.icon as string | undefined
      })
    })
  return groups
})

async function refreshCurrentUser() {
  try {
    const me = await getCurrentUser()
    userStore.setUserInfo({
      id: me.id,
      username: me.username,
      nickname: me.nickname,
      roles: me.roles ?? [],
      roleNames: me.roleNames ?? [],
      permissions: me.permissions ?? []
    })
  } catch {
    // 静默失败：权限以本地缓存为准，接口侧仍会兜底校验
  }
}

// ===== 消息中心 =====
const drawerVisible = ref(false)
const messages = ref<MessageVO[]>([])
const unreadCount = ref(0)
const messagesLoading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | undefined

async function refreshUnread() {
  try {
    unreadCount.value = Number(await getUnreadCount())
  } catch {
    // 静默失败，避免轮询打扰
  }
}

async function openDrawer() {
  drawerVisible.value = true
  messagesLoading.value = true
  try {
    messages.value = await listMyMessages()
  } finally {
    messagesLoading.value = false
  }
}

async function doReadMessage(msg: MessageVO) {
  if (msg.readStatus === 0) {
    await markMessageRead(msg.id)
    msg.readStatus = 1
    refreshUnread()
  }
}

async function doReadAll() {
  await markAllMessagesRead()
  messages.value.forEach((m) => (m.readStatus = 1))
  refreshUnread()
}

const onReadMessage = debounce(doReadMessage, 300)
const onReadAll = debounce(doReadAll, 300)

function onLogout() {
  apiLogout().catch(() => {})
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  // 进入布局时刷新一次权限，管理员调整授权后可即时生效
  refreshCurrentUser()
  refreshUnread()
  pollTimer = setInterval(refreshUnread, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<template>
  <el-container class="layout">
    <el-aside :width="`var(--app-sidebar-width)`" class="layout__aside">
      <div class="layout__logo">活动管家</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="#606266"
        active-text-color="#4f6280"
      >
        <el-menu-item v-for="m in topMenus" :key="m.path" :index="m.path">
          <el-icon v-if="m.icon">
            <component :is="m.icon" />
          </el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
        <el-sub-menu v-for="g in menuGroups" :key="g.name" :index="g.name">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>{{ g.name }}</span>
          </template>
          <el-menu-item v-for="m in g.items" :key="m.path" :index="m.path">
            <el-icon v-if="m.icon">
              <component :is="m.icon" />
            </el-icon>
            <span>{{ m.title }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout__header">
        <span>{{ route.meta?.title }}</span>
        <div class="layout__right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="layout__bell">
            <el-icon :size="20" style="cursor: pointer" @click="openDrawer"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="onLogout">
            <span class="layout__user">{{ userStore.userInfo?.nickname || '用户' }}</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>

  <el-drawer v-model="drawerVisible" title="消息中心" size="420px">
    <div class="msg__toolbar">
      <span>未读 {{ unreadCount }} 条</span>
      <el-button link type="primary" :disabled="unreadCount === 0" @click="onReadAll">全部已读</el-button>
    </div>
    <div v-loading="messagesLoading">
      <el-empty v-if="!messages.length" description="暂无消息" />
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="msg__item"
        :class="{ 'msg__item--unread': msg.readStatus === 0 }"
        @click="onReadMessage(msg)"
      >
        <div class="msg__title">
          <el-badge v-if="msg.readStatus === 0" is-dot>
            <span>{{ msg.title }}</span>
          </el-badge>
          <span v-else>{{ msg.title }}</span>
          <span class="msg__time">{{ formatDate(msg.createTime) }}</span>
        </div>
        <div class="msg__content">{{ msg.content }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.layout {
  height: 100%;

  &__aside {
    background-color: #fafbfc;
    border-right: 1px solid #ebeef5;
  }

  &__logo {
    height: var(--app-header-height);
    line-height: var(--app-header-height);
    padding-left: 20px;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    letter-spacing: 1px;
    border-bottom: 1px solid #ebeef5;
  }

  :deep(.el-menu) {
    border-right: none;
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 46px;
    line-height: 46px;
    margin: 4px 8px;
    border-radius: 6px;
  }

  :deep(.el-menu-item.is-active) {
    background-color: #edf1f6;
    font-weight: 500;
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background-color: #f2f4f7;
  }

  &__header {
    height: var(--app-header-height);
    line-height: var(--app-header-height);
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #ebeef5;
    color: #303133;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  &__bell {
    line-height: 1;
  }

  &__user {
    cursor: pointer;
    color: var(--el-color-primary);
  }

  &__main {
    background-color: var(--el-bg-color-page);
    padding: 16px;
  }
}

.msg {
  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
    color: #909399;
    font-size: 13px;
  }

  &__item {
    padding: 12px;
    border-radius: 6px;
    cursor: pointer;
    margin-bottom: 8px;
    background: #f5f7fa;

    &--unread {
      background: #eef2f7;
    }

    &:hover {
      background: #e8edf4;
    }
  }

  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
    margin-bottom: 6px;
  }

  &__time {
    color: #c0c4cc;
    font-size: 12px;
    font-weight: 400;
  }

  &__content {
    color: #606266;
    font-size: 13px;
    white-space: pre-wrap;
    line-height: 1.6;
  }
}
</style>
