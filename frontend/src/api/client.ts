import axios, { type InternalAxiosRequestConfig } from 'axios'
import { getAccessToken, refreshOnce } from './session'

export const rawApi = axios.create({ baseURL: '/api/v1', withCredentials: true })
export const api = axios.create({ baseURL: '/api/v1', withCredentials: true })

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

interface RetryConfig extends InternalAxiosRequestConfig { _retried?: boolean }
api.interceptors.response.use(undefined, async (error) => {
  const config = error.config as RetryConfig | undefined
  if (error.response?.status === 401 && config && !config._retried && !config.url?.startsWith('/auth/')) {
    config._retried = true
    await refreshOnce()
    config.headers.Authorization = `Bearer ${getAccessToken()}`
    return api.request(config)
  }
  return Promise.reject(error)
})
