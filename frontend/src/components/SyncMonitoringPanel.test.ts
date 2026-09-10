import { describe, expect, it } from 'vitest'
import type { SyncJobOverview } from '../hooks/useSyncMonitoring'
import { syncHeadline } from './SyncMonitoringPanel'

function jobs(overrides: Partial<SyncJobOverview> = {}): SyncJobOverview {
  return {
    queued: 0,
    waiting: 0,
    running: 0,
    completed: 220,
    failed: 0,
    totalRepositories: 220,
    analysisStepsCompleted: 804,
    analysisStepsTotal: 804,
    activeJobs: [],
    ...overrides,
  }
}

describe('syncHeadline', () => {
  it('makes a fully idle completed pipeline unambiguously complete', () => {
    expect(syncHeadline(jobs())).toEqual({
      kind: 'complete',
      title: 'Analysis complete',
      detail: 'No jobs are queued, running, waiting to retry, or failed.',
    })
  })

  it('shows waiting retries as ongoing work rather than completion', () => {
    expect(syncHeadline(jobs({ waiting: 2 }))).toMatchObject({
      kind: 'working',
      title: 'Analysis in progress',
      detail: '2 waiting to retry',
    })
  })

  it('shows a terminal failure ahead of completed work', () => {
    expect(syncHeadline(jobs({ failed: 1 }))).toEqual({
      kind: 'attention',
      title: 'Needs attention',
      detail: '1 job has stopped after all retry attempts.',
    })
  })

  it('shows an idle state when no analysis has been scheduled', () => {
    expect(syncHeadline(jobs({ completed: 0, totalRepositories: 0, analysisStepsCompleted: 0, analysisStepsTotal: 0 }))).toMatchObject({
      kind: 'idle',
      title: 'No analysis yet',
    })
  })
})
