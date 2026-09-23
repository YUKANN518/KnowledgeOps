<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Document, HomeFilled, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { displayLabel } from '../utils/format'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const initials = computed(() => auth.user?.displayName.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase() || 'KO')
async function signOut() { await auth.logout(); await router.replace('/login') }
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-mark">K</div><div><strong>KnowledgeOps</strong><span>企业知识协作平台</span></div></div>
      <nav class="sidebar-nav" aria-label="主导航">
        <RouterLink to="/dashboard"><el-icon><HomeFilled /></el-icon><span>工作台</span></RouterLink>
        <RouterLink to="/tickets"><el-icon><Tickets /></el-icon><span>工单</span></RouterLink>
        <RouterLink v-if="auth.user?.permissions.includes('knowledge.read')" to="/knowledge/articles"><el-icon><Collection /></el-icon><span>知识文章</span></RouterLink>
        <RouterLink v-if="auth.user?.permissions.includes('knowledge.read')" to="/documents"><el-icon><Document /></el-icon><span>知识文档</span></RouterLink>
      </nav>
      <div class="sidebar-foot"><span class="environment-dot"></span> 本地演示环境</div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div><span class="eyebrow">知识协作</span><h1>{{ route.meta.title }}</h1></div>
        <div class="user-menu">
          <div class="avatar">{{ initials }}</div>
          <div class="user-copy"><strong>{{ auth.user?.displayName }}</strong><span>{{ auth.user?.roles.map(displayLabel).join(' · ') }}</span></div>
          <el-button text circle aria-label="退出登录" title="退出登录" @click="signOut"><el-icon><SwitchButton /></el-icon></el-button>
        </div>
      </header>
      <main class="main-content"><RouterView /></main>
    </div>
  </div>
</template>
