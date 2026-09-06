import { renderHook, waitFor } from '@testing-library/react'
import { useOverviewDashboard } from '../../hooks/useOverviewDashboard'

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('API error-state layer: overview dashboard', () => {
  it('preserves the pre-R-009 HTTP error message contract', async () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation((input: RequestInfo | URL) => {
      const url = String(input)
      if (url === '/api/me/activity') {
        return Promise.resolve(new Response(null, { status: 503 }))
      }
      if (url === '/api/me/repositories') return Promise.resolve(jsonResponse([]))
      if (url === '/api/me/technologies') return Promise.resolve(jsonResponse([]))
      if (url === '/api/me/project-types') return Promise.resolve(jsonResponse([]))
      if (url === '/api/me/significant-external-projects') return Promise.resolve(jsonResponse([]))
      return Promise.reject(new Error(`Unexpected URL: ${url}`))
    }))

    const { result } = renderHook(() => useOverviewDashboard(true))

    await waitFor(() => {
      expect(result.current).toEqual({
        status: 'error',
        data: null,
        error: '/api/me/activity failed with HTTP 503',
      })
    })

    vi.unstubAllGlobals()
  })
})
