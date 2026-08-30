import { useEffect, useState } from 'react'

export type ActivityPeriod = '12m' | '24m' | '5y' | 'all'

export type ActivityData = {
  commitCount: number
  activeProjects: number
  averageCommitSize: number
  medianCommitSize: number
  additions: number
  deletions: number
  firstActivityAt: string | null
  lastActivityAt: string | null
  commitsPerYear: Array<{ year: number; commits: number }>
  commitsPerMonth: Array<{ month: string; commits: number; activeProjects: number }>
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: ActivityData; error: null }
  | { status: 'error'; data: null; error: string }

export function periodRange(period: ActivityPeriod) {
  if (period === 'all') return { from: null, to: null }

  const to = new Date()
  const from = new Date(to)

  if (period === '12m') from.setMonth(from.getMonth() - 12)
  if (period === '24m') from.setMonth(from.getMonth() - 24)
  if (period === '5y') from.setFullYear(from.getFullYear() - 5)

  return {
    from: formatDate(from),
    to: formatDate(to),
  }
}

function formatDate(date: Date) {
  return date.toISOString().slice(0, 10)
}

export function useActivityView(period: ActivityPeriod): State {
  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()
    const range = periodRange(period)

    async function load() {
      setState({ status: 'loading', data: null, error: null })

      const params = new URLSearchParams()
      if (range.from) params.set('from', range.from)
      if (range.to) params.set('to', range.to)

      try {
        const response = await fetch(
          `/api/me/activity${params.size ? `?${params}` : ''}`,
          {
            credentials: 'include',
            headers: { Accept: 'application/json' },
            signal: controller.signal,
          },
        )

        if (!response.ok) {
          throw new Error(`Activity request failed with HTTP ${response.status}`)
        }

        const data = (await response.json()) as ActivityData
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          data: null,
          error: error instanceof Error ? error.message : 'Unable to load activity',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [period])

  return state
}
