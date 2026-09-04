import { afterEach, describe, expect, it, vi } from 'vitest'
import { getJson } from './request'

describe('getJson', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('uses the authenticated JSON request contract and returns the decoded body', async () => {
    const signal = new AbortController().signal
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: vi.fn().mockResolvedValue({ value: 42 }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(getJson<{ value: number }>('/api/me/example', {
      signal,
      errorMessage: 'Example request failed',
    })).resolves.toEqual({ value: 42 })

    expect(fetchMock).toHaveBeenCalledWith('/api/me/example', {
      credentials: 'include',
      headers: { Accept: 'application/json' },
      signal,
    })
  })

  it('preserves the caller-specific HTTP error message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: vi.fn(),
    }))

    await expect(getJson('/api/me/example', {
      errorMessage: 'Example request failed',
    })).rejects.toThrow('Example request failed with HTTP 503')
  })
})
