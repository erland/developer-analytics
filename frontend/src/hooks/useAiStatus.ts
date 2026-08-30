import { useEffect, useState } from 'react'

type AiStatus =
  | { status: 'loading'; configured: false; providerId: null; message: null }
  | { status: 'ready'; configured: boolean; providerId: string; message: string }
  | { status: 'error'; configured: false; providerId: null; message: string }

export function useAiStatus(): AiStatus {
  const [state, setState] = useState<AiStatus>({
    status: 'loading',
    configured: false,
    providerId: null,
    message: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      try {
        const response = await fetch('/api/me/ai/status', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`AI status request failed with HTTP ${response.status}`)
        }

        const data = (await response.json()) as {
          configured: boolean
          providerId: string
          message: string
        }

        setState({
          status: 'ready',
          configured: data.configured,
          providerId: data.providerId,
          message: data.message,
        })
      } catch (error) {
        if (controller.signal.aborted) return

        setState({
          status: 'error',
          configured: false,
          providerId: null,
          message:
            error instanceof Error
              ? error.message
              : 'Unable to read AI availability',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [])

  return state
}
