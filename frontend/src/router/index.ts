import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores'
import { getCurrentUser } from '@/api/auth'

const Layout = () => import('@/layouts/BasicLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/activities',
    children: [
      {
        path: 'activities',
        name: 'activity-list',
        component: () => import('@/views/activity/ActivityList.vue'),
        meta: { title: '我的活动', icon: 'Calendar', permission: 'activity:view' }
      },
      {
        path: 'activities/:id',
        name: 'activity-detail',
        component: () => import('@/views/activity/ActivityDetail.vue'),
        meta: { title: '活动详情', hidden: true, permission: 'activity:view' }
      },
      {
        path: 'activities/:id/registration',
        name: 'activity-registration',
        component: () => import('@/views/activity/RegistrationBoard.vue'),
        meta: { title: '报名人员看板', hidden: true, permission: 'questionnaire:view' }
      },
      {
        path: 'tasks',
        name: 'task-list',
        component: () => import('@/views/task/TaskList.vue'),
        meta: { title: '任务分工', icon: 'List', permission: 'task:view' }
      },
      {
        path: 'questionnaires',
        name: 'questionnaire-list',
        component: () => import('@/views/questionnaire/QuestionnaireList.vue'),
        meta: { title: '报名问卷', icon: 'Tickets', permission: 'questionnaire:view' }
      },
      {
        path: 'questionnaires/:id/fill',
        name: 'questionnaire-fill',
        component: () => import('@/views/questionnaire/QuestionnaireFill.vue'),
        meta: { title: '填写问卷', hidden: true, permission: 'questionnaire:view' }
      },
      {
        path: 'reminders',
        name: 'reminder-list',
        component: () => import('@/views/reminder/ReminderList.vue'),
        meta: { title: '定时提醒', icon: 'Bell', permission: 'reminder:view' }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserManage.vue'),
        meta: { title: '用户管理', icon: 'User', group: '系统管理', permission: 'system:user:view' }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleManage.vue'),
        meta: { title: '角色权限', icon: 'Key', group: '系统管理', permission: 'system:role:view' }
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/Forbidden.vue'),
        meta: { title: '无权限', hidden: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFound.vue'),
    meta: { public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore()
  const title = to.meta.title as string
  document.title = title ? `${title} - 活动管家` : '活动管家'

  if (to.meta.public) {
    return true
  }
  if (!userStore.token) {
    return { name: 'login' }
  }
  // 旧会话（持久化数据不含权限）先刷新一次当前用户，避免误判为无权限
  if (!userStore.userInfo || !Array.isArray(userStore.userInfo.permissions)) {
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
      userStore.logout()
      return { name: 'login' }
    }
  }
  const permission = to.meta.permission as string | undefined
  if (permission && !userStore.hasPermission(permission)) {
    return { name: 'forbidden' }
  }
  return true
})

export default router
