import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configureSession, getAccessToken, refreshOnce, setAccessToken } from '../src/api/session'

describe('authentication session', () => {
  beforeEach(() => setAccessToken(''))

  it('keeps the access token in module memory', () => {
    setAccessToken('access-token')
    expect(getAccessToken()).toBe('access-token')
  })

  it('coalesces concurrent refresh attempts', async () => {
    let release!: () => void
    const gate = new Promise<void>((resolve) => { release = resolve })
    const refresh = vi.fn(() => gate)
    const failure = vi.fn()
    configureSession(refresh, failure)

    const first = refreshOnce()
    const second = refreshOnce()
    expect(refresh).toHaveBeenCalledTimes(1)
    release()
    await Promise.all([first, second])
    expect(failure).not.toHaveBeenCalled()
  })

  it('clears the session when refresh fails', async () => {
    const failure = vi.fn()
    configureSession(() => Promise.reject(new Error('expired')), failure)
    await expect(refreshOnce()).rejects.toThrow('expired')
    expect(failure).toHaveBeenCalledOnce()
  })
})
