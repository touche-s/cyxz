import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
  },
  {
    path: '/square',
    name: 'Square',
    component: () => import('@/views/Square.vue'),
  },
  {
    path: '/discover',
    name: 'Discover',
    component: () => import('@/views/Discover.vue'),
  },
  {
    path: '/following',
    name: 'Following',
    component: () => import('@/views/Following.vue'),
  },
  {
    path: '/circle/:id',
    name: 'CircleDetail',
    component: () => import('@/views/CircleDetail.vue'),
  },
  {
    path: '/user/:id',
    name: 'Profile',
    component: () => import('@/views/ProfilePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user-center',
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
    component: () => import('@/views/PublishFlow.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/post/edit/:id',
    name: 'PostEdit',
    component: () => import('@/views/PublishFlow.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: () => import('@/views/PostDetail.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/views/ArticleDetail.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/SearchPage.vue'),
  },
  {
    path: '/messages',
    name: 'MessageCenter',
    component: () => import('@/views/MessageCenter.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/guidelines',
    name: 'Guidelines',
    component: () => import('@/views/Guidelines.vue'),
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/messages/chat',
    name: 'PrivateMessages',
    component: () => import('@/views/PrivateMessages.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const { requireLogin } = useAuth()
    if (!requireLogin()) {
      return '/'
    }
  }
  return true
})

export default router
