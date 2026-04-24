import { createRouter, createWebHistory } from 'vue-router'
import { isNotEmpty } from '@/utils/plugins'
import { getToken, setToken, setUsername } from '@/core/auth' // 验权
import user from '@/api/modules/user'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/home'
    },
    {
      path: '/login',
      name: 'LoginIndex',
      component: () => import('@/views/login/LoginIndex.vue'),
      meta: { title: '登录 · LinkHub' }
    },
    {
      path: '/home',
      name: 'LayoutIndex',
      redirect: '/home/space',
      component: () => import('@/views/home/HomeIndex.vue'),
      meta: { title: 'LinkHub – 高性能 SaaS 短链接平台' },
      children: [
        {
          // 前面不能加/
          path: 'space',
          name: 'MySpace',
          component: () => import('@/views/mySpace/MySpaceIndex.vue'),
          meta: { title: '我的空间 · LinkHub' }
        },
        {
          path: 'recycleBin',
          name: 'RecycleBin',
          component: () => import('@/views/recycleBin/RecycleBinIndex.vue'),
          meta: { title: '回收站 · LinkHub' }
        },
        {
          path: 'account',
          name: 'Mine',
          component: () => import('@/views/mine/MineIndex.vue'),
          meta: { title: '个人中心 · LinkHub' }
        }
      ]
    }
  ]
})

// eslint-disable-next-line no-unused-vars
router.beforeEach(async (to, from, next) => {
  // 从localstorage中先获取token，并赋给chookies，如果还存在token，而且还处于正常登录状态就直接将token和username赋给cookies，用户徐的数据请求
  setToken(localStorage.getItem('token'))
  setUsername(localStorage.getItem('username'))
  const token = getToken()
  if (to.path === '/login') {
    next()
  }
  if (isNotEmpty(token)) {
    next()
  } else {
    next('/login')
  }
})

export default router
