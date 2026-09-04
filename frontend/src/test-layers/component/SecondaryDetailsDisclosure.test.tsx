import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SyncMonitoringPanel } from '../../components/SyncMonitoringPanel'
import { useSyncMonitoring } from '../../hooks/useSyncMonitoring'

vi.mock('../../hooks/useSyncMonitoring', () => ({
  useSyncMonitoring: vi.fn(),
}))

const mockedUseSyncMonitoring = vi.mocked(useSyncMonitoring)

describe('secondary details disclosures', () => {
  beforeEach(() => {
    mockedUseSyncMonitoring.mockReturnValue({
      status: 'ready',
      jobs: {
        queued: 1,
        waiting: 0,
        running: 1,
        completed: 18,
        failed: 2,
        totalRepositories: 22,
        analysisStepsCompleted: 74,
        analysisStepsTotal: 88,
        activeJobs: [],
      },
      errors: [],
      contributionRuns: [],
    })
  })

  it('keeps synchronisation summary visible while operational details start collapsed', async () => {
    const user = userEvent.setup()
    render(<SyncMonitoringPanel />)

    const details = screen.getByText('Analysis progress').closest('details')
    expect(details).not.toHaveAttribute('open')
    expect(screen.getByText('18/22 repositories completed · 74/88 analysis steps · 1 running · 2 failed')).toBeInTheDocument()

    await user.click(screen.getByText('Analysis progress'))
    expect(details).toHaveAttribute('open')
    expect(screen.getByText('74 of 88 steps completed')).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Analysis pipeline progress' })).toHaveAttribute('value', '74')
    expect(screen.getByText('Recent synchronisation errors')).toBeInTheDocument()
  })
})
