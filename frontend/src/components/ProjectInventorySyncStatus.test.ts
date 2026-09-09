import { describe, expect, it } from 'vitest'
import type { SyncJobSummary } from '../hooks/useGitHubSyncStatus'
import { repositorySyncIndicator } from './ProjectInventoryView'

function job(status: string, overrides: Partial<SyncJobSummary> = {}): SyncJobSummary {
  return {
    id: 'job-1',
    jobType: 'GITHUB_CONTRIBUTION_DISCOVERY',
    status,
    repositoryId: 'repo-1',
    repositoryName: 'repo-one',
    attemptCount: 0,
    maxAttempts: 5,
    analysisStep: 1,
    analysisStepsTotal: 4,
    progressPercent: null,
    lastError: null,
    createdAt: '2026-09-09T12:00:00Z',
    nextExecutionAt: null,
    startedAt: null,
    completedAt: null,
    ...overrides,
  }
}

describe('repositorySyncIndicator', () => {
  it('shows a rate-limit pause ahead of other active states', () => {
    const indicator = repositorySyncIndicator('repo-1', [
      job('RUNNING'),
      job('PAUSED_RATE_LIMIT', { id: 'job-2', nextExecutionAt: '2026-09-09T14:30:00Z' }),
    ])

    expect(indicator?.kind).toBe('paused')
    expect(indicator?.label).toBe('Paused · GitHub rate limit')
    expect(indicator?.detail).toContain('continue automatically')
  })

  it('shows running before waiting and queued', () => {
    const indicator = repositorySyncIndicator('repo-1', [
      job('QUEUED'),
      job('WAITING', { id: 'job-2' }),
      job('RUNNING', { id: 'job-3' }),
    ])

    expect(indicator).toMatchObject({ kind: 'running', label: 'Syncing' })
  })

  it('shows waiting before queued', () => {
    const indicator = repositorySyncIndicator('repo-1', [
      job('QUEUED'),
      job('WAITING', { id: 'job-2' }),
    ])

    expect(indicator).toMatchObject({ kind: 'waiting', label: 'Waiting' })
  })

  it('ignores jobs belonging to another repository', () => {
    expect(repositorySyncIndicator('repo-1', [job('PAUSED_RATE_LIMIT', { repositoryId: 'repo-2' })])).toBeNull()
  })
})
