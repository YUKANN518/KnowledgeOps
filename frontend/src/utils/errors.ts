import axios from 'axios'
import type { ApiErrorBody } from '../types'

const statusMessages: Record<number, string> = {
  400: 'The request could not be understood.',
  401: 'Your session has expired. Please sign in again.',
  403: 'Permission denied. You cannot perform this action.',
  404: 'The requested item was not found.',
  409: 'This item changed since you opened it. Refresh and try again.',
  422: 'Please review the information and try again.',
  500: 'The service could not complete the request.',
}

export function apiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return 'Something went wrong. Please try again.'
  const status = error.response?.status
  const body = error.response?.data
  if (body?.details?.length) return body.details.map((item) => `${item.field}: ${item.message}`).join('; ')
  if (status === 403 || status === 409 || status === 422) return body?.message || statusMessages[status]
  return status ? statusMessages[status] || 'The service could not complete the request.' : 'Cannot reach the server.'
}
