import { useEffect, useState } from 'react'

export type SyncJobSummary = {
  id: string
  jobType: string
  syncMode: string | null
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
  pausedRateLimit: number
  running: number
  completed: number
  failed: number
  totalRepositories: number
  analysisStepsCompleted: number
  analysisStepsTotal: number
  activeJobs: SyncJobSummary[]
}

export type GitHubSyncStatusState = {
  status: 'loading' | 'idle' | 'active' | 'paused' | 'error'
  overview: SyncJobOverview | null
  resumeAt: string | null
}

const POLL_INTERVAL_MS = 15_000

export function deriveGitHubSyncStatus(overview: SyncJobOverview): GitHubSyncStatusState {
  const pausedJobs = overview.activeJobs.filter((job) => job.status === 'PAUSED_RATE_LIMIT')
  const resumeAt = pausedJobs
    .map((job) => job.nextExecutionAt)
    .filter((value): value is string => Boolean(value))
    .map((value) => new Date(value))
    .filter((value) => !Number.isNaN(value.getTime()))
    .sort((a, b) => a.getTime() - b.getTime())[0]

  if (overview.pausedRateLimit > 0 || pausedJobs.length > 0) {
    return {
      status: 'paused',
      overview,
      resumeAt: resumeAt?.toISOString() ?? null,
    }
  }

  if (overview.queued > 0 || overview.waiting > 0 || overview.running > 0) {
    return { status: 'active', overview, resumeAt: null }
  }

  return { status: 'idle', overview, resumeAt: null }
}

export function useGitHubSyncStatus(enabled = true): GitHubSyncStatusState {
  const [state, setState] = useState<GitHubSyncStatusState>({
    status: enabled ? 'loading' : 'idle',
    overview: null,
    resumeAt: null,
  })

  useEffect(() => {
    if (!enabled) return

    let disposed = false
    let currentController: AbortController | null = null

    async function loadStatus() {
      currentController?.abort()
      const controller = new AbortController()
      currentController = controller

      try {
        const response = await fetch('/api/me/sync-jobs', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })
        if (!response.ok) {
          throw new Error(`Sync status request failed with HTTP ${response.status}`)
        }
        const overview = (await response.json()) as SyncJobOverview
        if (!disposed) setState(deriveGitHubSyncStatus(overview))
      } catch {
        if (disposed || controller.signal.aborted) return
        setState({ status: 'error', overview: null, resumeAt: null })
      }
    }

    void loadStatus()
    const interval = window.setInterval(() => void loadStatus(), POLL_INTERVAL_MS)

    return () => {
      disposed = true
      currentController?.abort()
      window.clearInterval(interval)
    }
  }, [enabled])

  return state
}
