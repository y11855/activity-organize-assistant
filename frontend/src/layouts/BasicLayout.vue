<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() =>
  router.getRoutes
    .flatMap((r) => r.children ?? [])
    .filter((r) => !r.meta?.hidden)
)

function onLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside :width="`var(--app-sidebar-width)`" class="layout__aside">
      <div class="layout__logo">实训 AI 评价系统</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#fff"
      >
        <el-menu-item v-for="m in menus" :key="m.path" :index="'/' + m.path">
          <el-icon v-if="m.meta?.icon">
            <component :is="m.meta.icon" />
          </el-icon>
          <span>{{ m.meta?.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout__header">
        <span>{{ route.meta?.title }}</span>
        <el-dropdown @command="onLogout">
          <span class="layout__user">{{ userStore.userInfo?.nickname || '用户' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout {
  height: 100%;

  &__aside {
    background-color: #001529;
    color: #fff;
  }

  &__logo {
    height: var(--app-header-height);
    line-height: var(--app-header-height);
    text-align: center;
    font-size: 16px;
    border-bottom: 1px solid #1f1f1f;
  }

  &__header {
    height: var(--app-header-height);
    line-height: var(--app-header-height);
    background-color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #eee;
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
</style>
