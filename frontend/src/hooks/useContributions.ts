import { useEffect, useState } from 'react'

export type ContributionSummary = {
  total: number
  commits: number
  pullRequests: number
  reviews: number
  issues: number
  recent: Array<{
    id: string
    repositoryId: string
    repositoryName: string
    type: string
    title: string | null
    occurredAt: string
  }>
}

type State =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; data: ContributionSummary }

export function useContributions(): State {
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()

    fetch('/api/me/contributions?limit=100', {
      credentials: 'include',
      headers: { Accept: 'application/json' },
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Contributions request failed with HTTP ${response.status}`)
        }
        const raw = (await response.json()) as Partial<ContributionSummary>
        setState({
          status: 'ready',
          data: {
            total: raw.total ?? 0,
            commits: raw.commits ?? 0,
            pullRequests: raw.pullRequests ?? 0,
            reviews: raw.reviews ?? 0,
            issues: raw.issues ?? 0,
            recent: raw.recent ?? [],
          },
        })
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setState({
            status: 'error',
            error: error instanceof Error ? error.message : 'Unable to load contributions',
          })
        }
      })

    return () => controller.abort()
  }, [])

  return state
}
