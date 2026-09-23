let accessToken = ''
let refreshHandler: (() => Promise<void>) | null = null
let failureHandler: (() => void) | null = null
let refreshPromise: Promise<void> | null = null

export const getAccessToken = () => accessToken
export const setAccessToken = (value: string) => { accessToken = value }
export function configureSession(refresh: () => Promise<void>, failure: () => void) {
  refreshHandler = refresh
  failureHandler = failure
}
export async function refreshOnce() {
  if (!refreshHandler) throw new Error('Authentication session is not configured')
  if (!refreshPromise) refreshPromise = refreshHandler().finally(() => { refreshPromise = null })
  try { await refreshPromise } catch (error) { failureHandler?.(); throw error }
}
