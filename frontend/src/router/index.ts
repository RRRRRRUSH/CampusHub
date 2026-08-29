import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // --- 基础页 ---
    {
      path: '/login',
      name: 'student-login',
      component: () => import('@/views/student/login/Login.vue')
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/login/Login.vue')
    },

    // --- 管理端路由 ---
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAdmin: true },
      children: [
        { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },

        // 1. 竞赛管理
        {
          path: 'competition/internal',
          component: () => import('@/views/admin/CompetitionManage.vue'),
          props: { pageType: 'internal' }
        },
        {
          path: 'competition/external',
          component: () => import('@/views/admin/CompetitionManage.vue'),
          props: { pageType: 'external' }
        },
        { path: 'competition/publish', component: () => import('@/views/admin/CompetitionPublish.vue') },
        { path: 'competition/manage/:id', component: () => import('@/views/admin/CompetitionDetail.vue') },

        // 2. 认定审核
        { path: 'audit/competition', component: () => import('@/views/admin/CompetitionAudit.vue') },
        { path: 'audit/volunteer', component: () => import('@/views/admin/ActivityAudit.vue') },

        // 3. 活动管理
        {
          path: 'activity/internal',
          component: () => import('@/views/admin/ActivityManage.vue'),
          props: { pageType: 'internal' }
        },
        {
          path: 'activity/external',
          component: () => import('@/views/admin/ActivityManage.vue'),
          props: { pageType: 'external' }
        },
        { path: 'activity/publish', component: () => import('@/views/admin/ActivityPublish.vue') },

        // 🟢 核心修正点：将 'detail/:id' 改为 'manage/:id'，与列表跳转保持一致
        {
          path: 'activity/manage/:id',
          component: () => import('@/views/admin/ActivityDetail.vue')
        },

        // 4. 用户管理
        { path: 'users', component: () => import('@/views/admin/UserManage.vue') },
        { path: 'users/cadre', component: () => import('@/views/admin/CadreSetting.vue') }
      ]
    },

    // --- 学生端路由 ---
    {
      path: '/',
      component: () => import('@/layouts/UserLayout.vue'),
      redirect: '/home',
      children: [
        { path: 'home', component: () => import('@/views/student/Home.vue') },
        { path: 'competitions', component: () => import('@/views/student/CompetitionList.vue') },
        { path: 'mycompetitions', component: () => import('@/views/student/MyCompetitions.vue') },
        { path: 'activities', component: () => import('@/views/student/ActivityList.vue') },
        { path: 'notices', component: () => import('@/views/student/NoticeList.vue') },
        { path: 'my-participations', component: () => import('@/views/student/MyParticipations.vue') },
        { path: 'application', component: () => import('@/views/student/MyApplication.vue') },
        { path: 'message', component: () => import('@/views/student/StudentMessage.vue') },
        { path: 'profile', component: () => import('@/views/student/Profile.vue') },
        { path: 'competition/cockpit/:id',name:'CompetitionCockpit',component: () => import('@/views/student/CompetitionCockpit.vue') }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path === '/admin/login' || to.path === '/login') return next()

  if ((to.path.startsWith('/admin') || to.path.startsWith('/home')) && !userStore.id) {
    if (to.path.startsWith('/admin')) return next('/admin/login')
    return next('/login')
  }

  // 管理端权限控制
  if (to.path.startsWith('/admin')) {
    // 学生干部权限控制
    if (userStore.role === 'STUDENT' && userStore.isCadre) {
      const allowedPrefixes = [
        '/admin/audit/competition',
        '/admin/audit/volunteer',
        '/admin/dashboard',
        '/admin/login',
        '/admin/users'
      ]

      const blockedPaths = [
        '/admin/users/cadre',
        '/admin/competition/publish',
        '/admin/activity/publish'
      ]

      if (blockedPaths.some(p => to.path.startsWith(p))) {
        ElMessage.error('权限拒绝：学生干部无权访问此页面')
        return next('/admin/users')
      }

      const isAllowed = allowedPrefixes.some(p => to.path === p || to.path.startsWith(p + '/'))
      if (!isAllowed) {
        ElMessage.warning('学生干部仅限访问认定审核及查询页面')
        return next('/admin/audit/competition')
      }
    }

    if (userStore.role === 'TEACHER') {
      if (to.path.startsWith('/admin/audit')) {
        ElMessage.warning('普通教师无权访问认定中心')
        return next('/admin/competition/internal')
      }
    }
  }

  next()
})

export default router
