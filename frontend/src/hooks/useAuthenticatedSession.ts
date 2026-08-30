import { useEffect, useState } from 'react'

export type SessionUser = {
  authenticated: true
  provider: string
  login: string
  displayName: string
}

type SessionState =
  | { status: 'loading'; user: null; error: null }
  | { status: 'anonymous'; user: null; error: null }
  | { status: 'authenticated'; user: SessionUser; error: null }
  | { status: 'error'; user: null; error: string }

export function useAuthenticatedSession(): SessionState {
  const [state, setState] = useState<SessionState>({
    status: 'loading',
    user: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function loadSession() {
      try {
        const response = await fetch('/api/auth/session', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })

        if (response.status === 401) {
          setState({ status: 'anonymous', user: null, error: null })
          return
        }

        if (!response.ok) {
          throw new Error(`Session request failed with HTTP ${response.status}`)
        }

        const payload = (await response.json()) as SessionUser

        if (!payload.authenticated) {
          setState({ status: 'anonymous', user: null, error: null })
          return
        }

        setState({
          status: 'authenticated',
          user: payload,
          error: null,
        })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          user: null,
          error: error instanceof Error ? error.message : 'Unable to load session',
        })
      }
    }

    void loadSession()
    return () => controller.abort()
  }, [])

  return state
}
