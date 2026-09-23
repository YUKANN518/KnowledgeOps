import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppLayout from '../layouts/AppLayout.vue'
import { canAccessRoute } from '../utils/permissions'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { public: true, title: '登录' } },
    { path: '/', component: AppLayout, children: [
      { path: '', redirect: '/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/DashboardView.vue'), meta: { title: '工作台' } },
      { path: 'tickets', name: 'tickets', component: () => import('../views/tickets/TicketListView.vue'), meta: { title: '工单' } },
      { path: 'tickets/new', name: 'ticket-create', component: () => import('../views/tickets/TicketCreateView.vue'), meta: { title: '新建工单', permissions: ['ticket.create'] } },
      { path: 'tickets/:id', name: 'ticket-detail', component: () => import('../views/tickets/TicketDetailView.vue'), meta: { title: '工单详情' } },
      { path: 'knowledge/articles', name: 'articles', component: () => import('../views/knowledge/ArticleListView.vue'), meta: { title: '知识文章', permissions: ['knowledge.read'] } },
      { path: 'knowledge/articles/new', name: 'article-create', component: () => import('../views/knowledge/ArticleFormView.vue'), meta: { title: '新建文章', permissions: ['knowledge.write', 'knowledge.admin'], anyPermission: true } },
      { path: 'knowledge/articles/:id', name: 'article-detail', component: () => import('../views/knowledge/ArticleDetailView.vue'), meta: { title: '文章详情', permissions: ['knowledge.read'] } },
      { path: 'knowledge/articles/:id/edit', name: 'article-edit', component: () => import('../views/knowledge/ArticleFormView.vue'), meta: { title: '编辑文章', permissions: ['knowledge.write', 'knowledge.admin'], anyPermission: true } },
      { path: 'documents', name: 'documents', component: () => import('../views/knowledge/DocumentListView.vue'), meta: { title: '知识文档', permissions: ['knowledge.read'] } },
    ]},
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.ready) await auth.restore()
  if (to.meta.public) return auth.authenticated ? '/dashboard' : true
  if (!auth.authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  const required = (to.meta.permissions as string[] | undefined) || []
  if (!canAccessRoute(auth.user, required, Boolean(to.meta.anyPermission))) return '/dashboard'
  return true
})

export default router
