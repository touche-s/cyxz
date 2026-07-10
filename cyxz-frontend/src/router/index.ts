import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/following',
    name: 'Following',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/user/:id',
    name: 'UserCenter',
    component: () => import('@/views/UserCenter.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/creator',
    name: 'CreatorCenter',
    component: () => import('@/views/CreatorCenter.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/post/create',
    name: 'PostCreate',
    component: () => import('@/views/PostCreate.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/post/edit/:id',
    name: 'PostEdit',
    component: () => import('@/views/PostCreate.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue'),
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('token')
    if (!token) {
      next('/')
      return
    }
  }
  next()
})

export default router
