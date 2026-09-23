import axios from 'axios'
import type { ApiErrorBody } from '../types'

const statusMessages: Record<number, string> = {
  400: '请求内容有误，请检查后重试。',
  401: '登录状态已失效，请重新登录。',
  403: '你没有权限执行此操作。',
  404: '未找到所需内容。',
  409: '内容已被其他操作更新，请刷新后重试。',
  422: '请检查填写内容后重试。',
  500: '服务暂时无法完成请求，请稍后重试。',
}

const codeMessages: Record<string, string> = {
  VALIDATION_ERROR: '请检查填写内容后重试。',
  BAD_REQUEST: '请求内容有误，请检查后重试。',
  AUTHENTICATION_REQUIRED: '请先登录后再继续。',
  INVALID_CREDENTIALS: '邮箱或密码错误。',
  TOKEN_REVOKED: '登录状态已失效，请重新登录。',
  FORBIDDEN: '你没有权限执行此操作。',
  RESOURCE_NOT_FOUND: '未找到所需内容。',
  EMAIL_CONFLICT: '该邮箱已被使用。',
  STATE_CONFLICT: '当前状态已发生变化，请刷新后重试。',
  TICKET_NOT_FOUND: '未找到该工单。',
  TICKET_ACCESS_DENIED: '你没有权限查看该工单。',
  INVALID_TICKET_STATUS: '当前工单状态不支持此操作。',
  INVALID_ASSIGNEE: '请选择符合条件的支持人员。',
  TICKET_VERSION_CONFLICT: '工单已被更新，请刷新后重试。',
  KNOWLEDGE_ARTICLE_NOT_FOUND: '未找到该知识文章。',
  KNOWLEDGE_DOCUMENT_NOT_FOUND: '未找到该知识文档。',
  KNOWLEDGE_ACCESS_DENIED: '你没有权限访问该知识内容。',
  UNSUPPORTED_FILE_TYPE: '不支持该文件类型。',
  FILE_TOO_LARGE: '文件大小不能超过 20 MiB。',
  FILE_STORAGE_ERROR: '文件保存失败，请稍后重试。',
  ARTICLE_VERSION_CONFLICT: '文章已被更新，请刷新后重试。',
  AUTH_DEPENDENCY_UNAVAILABLE: '认证服务暂时不可用，请稍后重试。',
  INTERNAL_ERROR: '服务暂时无法完成请求，请稍后重试。',
}

export function apiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return '操作失败，请稍后重试。'
  const status = error.response?.status
  const body = error.response?.data
  if (body?.code && codeMessages[body.code]) return codeMessages[body.code]
  return status ? statusMessages[status] || '服务暂时无法完成请求，请稍后重试。' : '无法连接服务器，请检查网络后重试。'
}
