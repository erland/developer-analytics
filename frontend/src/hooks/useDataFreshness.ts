import { useEffect, useState } from 'react'

export type FreshnessState = {
  status: 'loading' | 'fresh' | 'stale' | 'unknown' | 'error'
  label: string
  timestamp: string | null
}

type SyncRun = {
  status?: string
  completedAt?: string | null
  createdAt?: string | null
}

export function useDataFreshness(enabled: boolean): FreshnessState {
  const [state, setState] = useState<FreshnessState>({
    status: enabled ? 'loading' : 'unknown',
    label: enabled ? 'Checking data freshness…' : 'Data freshness unavailable',
    timestamp: null,
  })

  useEffect(() => {
    if (!enabled) return

    const controller = new AbortController()

    async function loadFreshness() {
      try {
        const response = await fetch('/api/me/sync-runs', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`Freshness request failed with HTTP ${response.status}`)
        }

        const runs = (await response.json()) as SyncRun[]
        const latest = runs
          .map((run) => run.completedAt ?? run.createdAt ?? null)
          .filter((value): value is string => Boolean(value))
          .map((value) => new Date(value))
          .filter((value) => !Number.isNaN(value.getTime()))
          .sort((a, b) => b.getTime() - a.getTime())[0]

        if (!latest) {
          setState({
            status: 'unknown',
            label: 'No completed sync yet',
            timestamp: null,
          })
          return
        }

        const ageMs = Date.now() - latest.getTime()
        const ageHours = ageMs / 3_600_000
        const fresh = ageHours <= 24

        setState({
          status: fresh ? 'fresh' : 'stale',
          label: fresh ? 'Data updated recently' : 'Data may be stale',
          timestamp: latest.toISOString(),
        })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          label: 'Could not determine data freshness',
          timestamp: null,
        })
      }
    }

    void loadFreshness()
    return () => controller.abort()
  }, [enabled])

  return state
}
