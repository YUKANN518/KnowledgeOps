<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, Document, HomeFilled, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const initials = computed(() => auth.user?.displayName.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase() || 'KO')
async function signOut() { await auth.logout(); await router.replace('/login') }
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-mark">K</div><div><strong>KnowledgeOps</strong><span>Operations hub</span></div></div>
      <nav class="sidebar-nav" aria-label="Primary navigation">
        <RouterLink to="/dashboard"><el-icon><HomeFilled /></el-icon><span>Dashboard</span></RouterLink>
        <RouterLink to="/tickets"><el-icon><Tickets /></el-icon><span>Tickets</span></RouterLink>
        <RouterLink v-if="auth.user?.permissions.includes('knowledge.read')" to="/knowledge/articles"><el-icon><Collection /></el-icon><span>Knowledge</span></RouterLink>
        <RouterLink v-if="auth.user?.permissions.includes('knowledge.read')" to="/documents"><el-icon><Document /></el-icon><span>Documents</span></RouterLink>
      </nav>
      <div class="sidebar-foot"><span class="environment-dot"></span> Local workspace</div>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div><span class="eyebrow">Knowledge operations</span><h1>{{ route.meta.title || route.name?.toString().replace('-', ' ') }}</h1></div>
        <div class="user-menu">
          <div class="avatar">{{ initials }}</div>
          <div class="user-copy"><strong>{{ auth.user?.displayName }}</strong><span>{{ auth.user?.roles.join(' · ') }}</span></div>
          <el-button text circle aria-label="Log out" @click="signOut"><el-icon><SwitchButton /></el-icon></el-button>
        </div>
      </header>
      <main class="main-content"><RouterView /></main>
    </div>
  </div>
</template>
