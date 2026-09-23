<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { apiErrorMessage } from '../../utils/errors'

const auth = useAuthStore(); const router = useRouter(); const route = useRoute()
const form = reactive({ email: '', password: '' }); const formRef = ref<FormInstance>()
const rules: FormRules = { email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }], password: [{ required: true, message: '请输入密码', trigger: 'blur' }] }
async function submit() {
  if (!await formRef.value?.validate().catch(() => false)) return
  try { await auth.login(form.email, form.password); await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard') }
  catch (error) { ElMessage.error(apiErrorMessage(error)) }
}
</script>
<template>
  <div class="login-page">
    <section class="login-story">
      <div class="login-brand"><span>K</span> KnowledgeOps</div>
      <div class="story-content"><p class="story-kicker">一个工作空间，职责清晰可见</p><h1>让服务工单与团队知识顺畅协作。</h1><p>集中处理请求、发布可靠指南，让日常工作所需信息触手可及。</p><div class="story-proof"><span>01</span><div><strong>默认安全</strong><small>基于 Spring Security 的角色权限控制</small></div></div><div class="story-proof"><span>02</span><div><strong>面向日常协作</strong><small>在一个工作空间处理工单与知识</small></div></div></div>
      <div class="story-foot">KnowledgeOps · 企业知识协作平台</div>
    </section>
    <section class="login-panel"><div class="login-card"><div class="mobile-brand">KnowledgeOps</div><span class="eyebrow">欢迎回来</span><h2>登录工作空间</h2><p class="muted">请使用组织账号继续。</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit"><el-form-item label="邮箱" prop="email"><el-input v-model="form.email" size="large" placeholder="name@company.com" :prefix-icon="Message" /></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" /></el-form-item><el-button class="login-button" type="primary" size="large" :loading="auth.loading" @click="submit">登录 <el-icon><ArrowRight /></el-icon></el-button></el-form><div class="login-security"><span>●</span> Refresh Token 轮换保护会话安全</div></div></section>
  </div>
</template>
