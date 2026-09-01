import { useEffect, useState } from 'react'

export type SyncJob = {
  id: string
  jobType: string
  status: string
  repositoryId: string | null
  repositoryName: string | null
  attemptCount: number
  maxAttempts: number
  progressPercent: number | null
  lastError: string | null
  createdAt: string | null
  nextExecutionAt: string | null
  startedAt: string | null
  completedAt: string | null
}

export type SyncJobOverview = {
  queued: number
  waiting: number
  running: number
  completed: number
  failed: number
  activeJobs: SyncJob[]
}

export type ContributionSyncRun = {
  id: string
  repositoryId: string
  repositoryName: string
  provider: string
  status: string
  contributionsSeen: number
  contributionsCreated: number
  contributionsUpdated: number
  pagesProcessed: number
  rateLimitRemaining: number | null
  rateLimitResetAt: string | null
  startedAt: string | null
  completedAt: string | null
  lastError: string | null
}

type State =
  | { status: 'loading' }
  | { status: 'error'; error: string }
  | { status: 'ready'; jobs: SyncJobOverview; errors: SyncJob[]; contributionRuns: ContributionSyncRun[] }

export function useSyncMonitoring(repositoryId?: string) {
  const [state, setState] = useState<State>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const contributionUrl = repositoryId
          ? `/api/me/contribution-sync-runs?repositoryId=${encodeURIComponent(repositoryId)}`
          : '/api/me/contribution-sync-runs'
        const [jobsResponse, errorsResponse, runsResponse] = await Promise.all([
          fetch('/api/me/sync-jobs?limit=200', { credentials: 'include' }),
          fetch('/api/me/sync-jobs/errors?limit=50', { credentials: 'include' }),
          fetch(contributionUrl, { credentials: 'include' }),
        ])
        if (!jobsResponse.ok || !errorsResponse.ok || !runsResponse.ok) {
          throw new Error('Synchronisation status could not be loaded')
        }
        const jobs = await jobsResponse.json() as SyncJobOverview
        const errors = await errorsResponse.json() as SyncJob[]
        const contributionRuns = await runsResponse.json() as ContributionSyncRun[]
        if (!cancelled) setState({ status: 'ready', jobs, errors, contributionRuns })
      } catch (error) {
        if (!cancelled) {
          setState({ status: 'error', error: error instanceof Error ? error.message : 'Synchronisation status could not be loaded' })
        }
      }
    }

    void load()
    const timer = window.setInterval(() => void load(), 5000)
    return () => {
      cancelled = true
      window.clearInterval(timer)
    }
  }, [repositoryId])

  return state
}
