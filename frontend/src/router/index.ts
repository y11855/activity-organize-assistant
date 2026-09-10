import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

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
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'House' }
      },
      {
        path: 'trainings',
        name: 'training-list',
        component: () => import('@/views/training/TrainingList.vue'),
        meta: { title: '实训管理', icon: 'Document' }
      },
      {
        path: 'trainings/:id',
        name: 'training-detail',
        component: () => import('@/views/training/TrainingDetail.vue'),
        meta: { title: '实训详情', hidden: true }
      },
      {
        path: 'evaluation',
        name: 'evaluation',
        component: () => import('@/views/evaluation/EvaluationConfig.vue'),
        meta: { title: '评价指标', icon: 'SetUp' }
      },
      {
        path: 'reports',
        name: 'reports',
        component: () => import('@/views/report/ReportView.vue'),
        meta: { title: '统计报表', icon: 'DataAnalysis' }
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
  document.title = title ? `${title} - 实训 AI 评价系统` : '实训 AI 评价系统'

  if (to.meta.public) {
    return next()
  }
  if (!userStore.token) {
    return next({ name: 'login' })
  }
  next()
})

export default router
