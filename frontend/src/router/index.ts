import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores'

const Layout = () => import('@/layouts/BasicLayout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
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
        meta: { title: '我的活动', icon: 'Calendar' }
      },
      {
        path: 'activities/:id',
        name: 'activity-detail',
        component: () => import('@/views/activity/ActivityDetail.vue'),
        meta: { title: '活动详情', hidden: true }
      },
      {
        path: 'tasks',
        name: 'task-list',
        component: () => import('@/views/task/TaskList.vue'),
        meta: { title: '任务分工', icon: 'List' }
      },
      {
        path: 'questionnaires',
        name: 'questionnaire-list',
        component: () => import('@/views/questionnaire/QuestionnaireList.vue'),
        meta: { title: '报名问卷', icon: 'Tickets' }
      },
      {
        path: 'reminders',
        name: 'reminder-list',
        component: () => import('@/views/reminder/ReminderList.vue'),
        meta: { title: '定时提醒', icon: 'Bell' }
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

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const title = to.meta.title as string
  document.title = title ? `${title} - 活动管家` : '活动管家'

  if (to.meta.public) {
    return next()
  }
  if (!userStore.token) {
    return next({ name: 'login' })
  }
  next()
})

export default router
