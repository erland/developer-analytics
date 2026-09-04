import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ActivityView } from '../../components/ActivityView'

const mocks = vi.hoisted(() => ({
  activitySpy: vi.fn(),
}))

vi.mock('../../hooks/useActivityView', async () => {
  const actual = await vi.importActual<typeof import('../../hooks/useActivityView')>('../../hooks/useActivityView')
  return {
    ...actual,
    useActivityView: (period: string, scope: unknown) => {
      mocks.activitySpy(period, scope)
      return {
        status: 'ready',
        error: null,
        data: {
          commitCount: 0,
          activeProjects: 0,
          averageCommitSize: 0,
          medianCommitSize: 0,
          additions: 0,
          deletions: 0,
          firstActivityAt: null,
          lastActivityAt: null,
          commitSizeStatisticsAvailable: false,
          lineStatisticsCommitCount: 0,
          commitsPerYear: [],
          commitsPerMonth: [],
          commitsPerWeek: [],
          projectsOverTime: [],
        },
      }
    },
  }
})

describe('Activity AnalysisScope continuity', () => {
  beforeEach(() => {
    mocks.activitySpy.mockClear()
    window.history.replaceState(null, '', '/?technology=java&projectType=backend&ownership=own&year=2026')
  })

  it('inherits Explore scope from the URL, displays it, and uses it for activity loading', async () => {
    const user = userEvent.setup()
    render(<ActivityView />)

    expect(screen.getByText('Analysis scope carried from Explore')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Remove Technology: java' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Remove Project type: backend' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Remove Ownership: Own' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Remove Period: 2026' })).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Activity window' })).toHaveValue('12m')

    expect(mocks.activitySpy).toHaveBeenLastCalledWith('12m', expect.objectContaining({
      technologies: ['java'],
      projectTypes: ['backend'],
      ownership: 'own',
      year: 2026,
    }))

    await user.click(screen.getByRole('button', { name: 'Remove Technology: java' }))
    expect(new URLSearchParams(window.location.search).get('technology')).toBeNull()
    expect(new URLSearchParams(window.location.search).get('projectType')).toBe('backend')
    expect(mocks.activitySpy).toHaveBeenLastCalledWith('12m', expect.objectContaining({
      technologies: [],
      projectTypes: ['backend'],
      year: 2026,
    }))
  })
})
