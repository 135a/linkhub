import { createRouter, createWebHistory } from 'vue-router'
import { isNotEmpty } from '@/utils/plugins'
import { getToken, setToken, setUsername, removeKey, removeUsername } from '@/core/auth' // 验权
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
      component: () => import('@/views/login/LoginIndex.vue')
    },
    {
      path: '/home',
      name: 'LayoutIndex',
      redirect: '/home/space',
      component: () => import('@/views/home/HomeIndex.vue'),
      children: [
        {
          // 前面不能加/
          path: 'space',
          name: 'MySpace',
          component: () => import('@/views/mySpace/MySpaceIndex.vue'),
          meta: { title: '我的空间' }
        },
        {
          path: 'recycleBin',
          name: 'RecycleBin',
          component: () => import('@/views/recycleBin/RecycleBinIndex.vue'),
          meta: { title: '账户设置' }
        },
        {
          path: 'account',
          name: 'Mine',
          component: () => import('@/views/mine/MineIndex.vue'),
          meta: { title: '个人中心' }
        }
      ]
    }
  ]
})

// eslint-disable-next-line no-unused-vars
router.beforeEach(async (to, from, next) => {
  const lsToken = localStorage.getItem('token')
  const lsUsername = localStorage.getItem('username')

  if (isNotEmpty(lsToken) && isNotEmpty(lsUsername)) {
    // localStorage 有登录态，同步到 cookies
    setToken(lsToken)
    setUsername(lsUsername)
  } else {
    // localStorage 已无登录态，同步清空 cookies
    removeKey()
    removeUsername()
  }

  const token = getToken()

  if (to.path === '/login') {
    return next()
  }
  if (isNotEmpty(token)) {
    return next()
  } else {
    return next('/login')
  }
})

export default router
