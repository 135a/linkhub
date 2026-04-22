import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import LogQuery from '../views/LogQuery.vue'
import TraceView from '../views/TraceView.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  {
    path: '/',
    name: 'LogQuery',
    component: LogQuery,
    meta: { requiresAuth: true }
  },
  {
    path: '/trace/:traceId?',
    name: 'TraceView',
    component: TraceView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.matched.some(r => r.meta.requiresAuth) && !localStorage.getItem('token')) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
