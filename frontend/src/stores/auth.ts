import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { rawApi } from '../api/client'
import { configureSession, setAccessToken } from '../api/session'
import type { AuthTokens, User } from '../types'

const CSRF_KEY = 'knowledgeops.csrf'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const ready = ref(false)
  const loading = ref(false)
  const csrfToken = ref(sessionStorage.getItem(CSRF_KEY) || '')
  const authenticated = computed(() => Boolean(user.value))

  function apply(tokens: AuthTokens) {
    setAccessToken(tokens.accessToken)
    csrfToken.value = tokens.csrfToken
    sessionStorage.setItem(CSRF_KEY, tokens.csrfToken)
    user.value = tokens.user
  }
  function clear() {
    setAccessToken('')
    csrfToken.value = ''
    sessionStorage.removeItem(CSRF_KEY)
    user.value = null
  }
  async function login(email: string, password: string) {
    loading.value = true
    try { apply((await rawApi.post<AuthTokens>('/auth/login', { email, password })).data) }
    finally { loading.value = false }
  }
  async function refresh() {
    if (!csrfToken.value) throw new Error('No restorable session')
    const response = await rawApi.post<AuthTokens>('/auth/refresh', null, { headers: { 'X-CSRF-Token': csrfToken.value } })
    apply(response.data)
  }
  async function restore() {
    if (ready.value) return authenticated.value
    try { await refresh() } catch { clear() }
    finally { ready.value = true }
    return authenticated.value
  }
  async function logout() {
    try {
      if (csrfToken.value) await rawApi.post('/auth/logout', null, { headers: { 'X-CSRF-Token': csrfToken.value } })
    } finally { clear() }
  }
  configureSession(refresh, () => {
    clear()
    if (window.location.pathname !== '/login') window.location.assign('/login')
  })
  return { user, ready, loading, authenticated, login, restore, logout, clear }
})
