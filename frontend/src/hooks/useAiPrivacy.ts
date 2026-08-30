import { useEffect, useState } from 'react'

export type AiPrivacyPolicy =
  | 'PUBLIC_ONLY'
  | 'PRIVATE_METADATA_ALLOWED'
  | 'PRIVATE_AI_DISABLED'

type State = {
  status: 'loading' | 'ready' | 'saving' | 'error'
  policy: AiPrivacyPolicy | null
  error: string | null
}

export function useAiPrivacy() {
  const [state, setState] = useState<State>({
    status: 'loading',
    policy: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      try {
        const response = await fetch('/api/me/ai/privacy', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })
        if (!response.ok) {
          throw new Error(`AI privacy request failed with HTTP ${response.status}`)
        }
        const data = (await response.json()) as { policy: AiPrivacyPolicy }
        setState({ status: 'ready', policy: data.policy, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          policy: null,
          error: error instanceof Error ? error.message : 'Unable to load AI privacy settings',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [])

  async function update(policy: AiPrivacyPolicy) {
    setState((current) => ({ ...current, status: 'saving', error: null }))
    try {
      const response = await fetch('/api/me/ai/privacy', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ policy }),
      })
      if (!response.ok) {
        throw new Error(`AI privacy update failed with HTTP ${response.status}`)
      }
      const data = (await response.json()) as { policy: AiPrivacyPolicy }
      setState({ status: 'ready', policy: data.policy, error: null })
    } catch (error) {
      setState((current) => ({
        ...current,
        status: 'error',
        error: error instanceof Error ? error.message : 'Unable to save AI privacy settings',
      }))
    }
  }

  return { ...state, update }
}
