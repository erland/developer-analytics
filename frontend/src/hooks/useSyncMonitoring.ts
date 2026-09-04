import { useEffect, useState } from 'react'

export type SyncJob = {
  id: string
  jobType: string
  status: string
  repositoryId: string | null
  repositoryName: string | null
  attemptCount: number
  maxAttempts: number
  analysisStep: number | null
  analysisStepsTotal: number | null
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
  totalRepositories: number
  analysisStepsCompleted: number
  analysisStepsTotal: number
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

const emptyJobs: SyncJobOverview = {
  queued: 0,
  waiting: 0,
  running: 0,
  completed: 0,
  failed: 0,
  totalRepositories: 0,
  analysisStepsCompleted: 0,
  analysisStepsTotal: 0,
  activeJobs: [],
}

function normalizeJobs(value: unknown): SyncJobOverview {
  if (!value || typeof value !== 'object') return emptyJobs
  const jobs = value as Partial<SyncJobOverview>
  const queued = typeof jobs.queued === 'number' ? jobs.queued : 0
  const waiting = typeof jobs.waiting === 'number' ? jobs.waiting : 0
  const running = typeof jobs.running === 'number' ? jobs.running : 0
  const completed = typeof jobs.completed === 'number' ? jobs.completed : 0
  const failed = typeof jobs.failed === 'number' ? jobs.failed : 0
  const totalRepositories = typeof jobs.totalRepositories === 'number'
    ? jobs.totalRepositories
    : queued + waiting + running + completed + failed
  return {
    queued,
    waiting,
    running,
    completed,
    failed,
    totalRepositories,
    analysisStepsCompleted: typeof jobs.analysisStepsCompleted === 'number'
      ? jobs.analysisStepsCompleted
      : completed * 4,
    analysisStepsTotal: typeof jobs.analysisStepsTotal === 'number'
      ? jobs.analysisStepsTotal
      : totalRepositories * 4,
    activeJobs: Array.isArray(jobs.activeJobs) ? jobs.activeJobs : [],
  }
}

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
        const jobs = normalizeJobs(await jobsResponse.json())
        const rawErrors = await errorsResponse.json() as unknown
        const rawContributionRuns = await runsResponse.json() as unknown
        const errors = Array.isArray(rawErrors) ? rawErrors as SyncJob[] : []
        const contributionRuns = Array.isArray(rawContributionRuns)
          ? rawContributionRuns as ContributionSyncRun[]
          : []
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
