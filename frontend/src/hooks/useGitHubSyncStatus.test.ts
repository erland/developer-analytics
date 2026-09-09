import { describe, expect, it } from 'vitest'
import { deriveGitHubSyncStatus, type SyncJobOverview } from './useGitHubSyncStatus'

function overview(overrides: Partial<SyncJobOverview> = {}): SyncJobOverview {
  return {
    queued: 0,
    waiting: 0,
    pausedRateLimit: 0,
    running: 0,
    completed: 0,
    failed: 0,
    totalRepositories: 2,
    analysisStepsCompleted: 3,
    analysisStepsTotal: 8,
    activeJobs: [],
    ...overrides,
  }
}

describe('deriveGitHubSyncStatus', () => {
  it('returns paused with the earliest automatic resume time', () => {
    const result = deriveGitHubSyncStatus(overview({
      pausedRateLimit: 2,
      running: 1,
      activeJobs: [
        {
          id: '1', jobType: 'A', status: 'PAUSED_RATE_LIMIT', repositoryId: null,
          repositoryName: null, attemptCount: 0, maxAttempts: 5, analysisStep: null,
          analysisStepsTotal: null, progressPercent: null, lastError: null, createdAt: null,
          nextExecutionAt: '2026-09-09T15:45:00Z', startedAt: null, completedAt: null,
        },
        {
          id: '2', jobType: 'B', status: 'PAUSED_RATE_LIMIT', repositoryId: null,
          repositoryName: null, attemptCount: 0, maxAttempts: 5, analysisStep: null,
          analysisStepsTotal: null, progressPercent: null, lastError: null, createdAt: null,
          nextExecutionAt: '2026-09-09T15:30:00Z', startedAt: null, completedAt: null,
        },
      ],
    }))

    expect(result.status).toBe('paused')
    expect(result.resumeAt).toBe('2026-09-09T15:30:00.000Z')
  })

  it('returns active for ordinary queued, waiting or running work', () => {
    expect(deriveGitHubSyncStatus(overview({ running: 1 })).status).toBe('active')
    expect(deriveGitHubSyncStatus(overview({ queued: 1 })).status).toBe('active')
    expect(deriveGitHubSyncStatus(overview({ waiting: 1 })).status).toBe('active')
  })

  it('returns idle when there is no active work', () => {
    expect(deriveGitHubSyncStatus(overview()).status).toBe('idle')
  })
})
