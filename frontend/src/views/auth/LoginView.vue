<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { apiErrorMessage } from '../../utils/errors'

const auth = useAuthStore(); const router = useRouter(); const route = useRoute()
const form = reactive({ email: '', password: '' }); const formRef = ref<FormInstance>()
const rules: FormRules = { email: [{ required: true, message: 'Enter your email', trigger: 'blur' }, { type: 'email', message: 'Enter a valid email', trigger: 'blur' }], password: [{ required: true, message: 'Enter your password', trigger: 'blur' }] }
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
      <div class="story-content"><p class="story-kicker">One workspace. Clear ownership.</p><h1>Keep service work and team knowledge moving together.</h1><p>Resolve requests, publish trusted guidance, and keep operational context within reach.</p><div class="story-proof"><span>01</span><div><strong>Secure by default</strong><small>Role-aware access backed by Spring Security</small></div></div><div class="story-proof"><span>02</span><div><strong>Built for the daily queue</strong><small>Tickets and knowledge in one focused workspace</small></div></div></div>
      <div class="story-foot">KnowledgeOps · Internal operations workspace</div>
    </section>
    <section class="login-panel"><div class="login-card"><div class="mobile-brand">KnowledgeOps</div><span class="eyebrow">Welcome back</span><h2>Sign in to your workspace</h2><p class="muted">Use your organization account to continue.</p><el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit"><el-form-item label="Email address" prop="email"><el-input v-model="form.email" size="large" placeholder="you@company.com" :prefix-icon="Message" /></el-form-item><el-form-item label="Password" prop="password"><el-input v-model="form.password" size="large" type="password" show-password placeholder="Your password" :prefix-icon="Lock" /></el-form-item><el-button class="login-button" type="primary" size="large" :loading="auth.loading" @click="submit">Sign in <el-icon><ArrowRight /></el-icon></el-button></el-form><div class="login-security"><span>●</span> Protected session with rotating refresh tokens</div></div></section>
  </div>
</template>
