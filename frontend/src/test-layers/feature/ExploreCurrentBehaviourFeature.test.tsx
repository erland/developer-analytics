import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TechnologyViews } from '../../components/TechnologyViews'
import { ProjectTypeViews } from '../../components/ProjectTypeViews'
import { TimelineView } from '../../components/TimelineView'

const mocks = vi.hoisted(() => ({
  technologyState: null as unknown,
  projectTypeState: null as unknown,
  activityState: null as unknown,
}))

vi.mock('../../hooks/useTechnologyViews', () => ({
  useTechnologyViews: () => mocks.technologyState,
}))

vi.mock('../../hooks/useProjectTypes', () => ({
  useProjectTypes: () => mocks.projectTypeState,
}))

vi.mock('../../hooks/useActivityView', () => ({
  useActivityView: () => mocks.activityState,
}))

vi.mock('../../hooks/useCorrections', () => ({
  setTechnologySuppressed: vi.fn(),
}))

vi.mock('../../components/ProjectDetailView', () => ({
  ProjectDetailView: ({ repositoryId, onBack }: { repositoryId: string; onBack: () => void }) => (
    <section>
      <h2>Project detail {repositoryId}</h2>
      <button type="button" onClick={onBack}>Back to projects</button>
    </section>
  ),
}))

function technology(key: string, name: string, projectName: string, score: number) {
  return {
    technologyKey: key,
    technologyName: name,
    technologyCategory: 'LANGUAGE',
    evidenceLevel: 'STRONG',
    evidenceScore: score,
    projectCount: 1,
    evidenceCount: 3,
    independentEvidenceTypes: 2,
    firstObservedAt: '2024-01-01T00:00:00Z',
    lastObservedAt: '2026-08-01T00:00:00Z',
    recentProjectCount: 1,
    privacyProvenance: 'PUBLIC_ONLY',
    rationale: {},
    timeline: [{ month: '2026-08-01', commits: 4, changedLines: 40, lineStatisticsCommitCount: 4, projectCount: 1 }],
    representativeProjects: [{
      repositoryId: `repo-${key}`,
      repositoryName: projectName,
      htmlUrl: null,
      visibility: 'PUBLIC',
      ownershipRelation: 'OWNED_BY_USER',
      lastActivityAt: '2026-08-01T00:00:00Z',
      evidenceCount: 3,
    }],
  }
}

function projectType(key: string, name: string, projectName: string) {
  return {
    categoryKey: key,
    categoryName: name,
    projectCount: 1,
    activityCount: 7,
    timeline: [{ month: '2026-08-01', commits: 7, changedLines: 70, lineStatisticsCommitCount: 7, activeProjectCount: 1 }],
    representativeProjects: [{
      repositoryId: `repo-${key}`,
      repositoryName: projectName,
      htmlUrl: null,
      visibility: 'PUBLIC',
      ownershipRelation: 'OWNED_BY_USER',
      lastActivityAt: '2026-08-01T00:00:00Z',
      contributionCount: 7,
    }],
  }
}

describe('current Explore behaviour regression coverage', () => {
  beforeEach(() => {
    mocks.technologyState = {
      status: 'ready',
      data: [
        technology('java', 'Java', 'java-service', 90),
        technology('swift', 'Swift', 'ios-game', 75),
      ],
      error: null,
    }
    mocks.projectTypeState = {
      status: 'ready',
      data: [
        projectType('backend', 'Backend service', 'api-service'),
        projectType('game', 'Game', 'ios-game'),
      ],
      error: null,
    }
    mocks.activityState = {
      status: 'ready',
      error: null,
      data: {
        commitCount: 12,
        activeProjects: 1,
        averageCommitSize: 20,
        medianCommitSize: 15,
        additions: 120,
        deletions: 80,
        firstActivityAt: '2025-01-01T00:00:00Z',
        lastActivityAt: '2026-08-12T00:00:00Z',
        commitSizeStatisticsAvailable: true,
        lineStatisticsCommitCount: 12,
        commitsPerYear: [],
        commitsPerMonth: [],
        commitsPerWeek: [],
        projectsOverTime: [{
          repositoryId: 'repo-1',
          repositoryName: 'developer-analytics',
          firstActivityAt: '2025-01-01T00:00:00Z',
          lastActivityAt: '2026-08-12T00:00:00Z',
          commits: 12,
          projectType: 'Backend service',
          technology: 'Java',
          monthlyActivity: [
            { period: '2025-06', commits: 2, additions: 20, deletions: 10, changedLines: 30, lineStatisticsCommitCount: 2 },
            { period: '2026-08', commits: 10, additions: 100, deletions: 70, changedLines: 170, lineStatisticsCommitCount: 10 },
          ],
          weeklyActivity: [
            { period: '2026-08-03', parentMonth: '2026-08', commits: 4, additions: 40, deletions: 20, changedLines: 60, lineStatisticsCommitCount: 4 },
            { period: '2026-08-10', parentMonth: '2026-08', commits: 6, additions: 60, deletions: 50, changedLines: 110, lineStatisticsCommitCount: 6 },
          ],
        }],
      },
    }
  })

  it('keeps technology selection local, updates its detail, and restores that selection after project detail', async () => {
    const user = userEvent.setup()
    render(<TechnologyViews />)

    expect(screen.getByRole('heading', { name: 'Java' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Swift/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Swift/i }))
    expect(screen.getByRole('heading', { name: 'Swift' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'ios-game' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'ios-game' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-swift' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Swift' })).toBeInTheDocument()
  })

  it('keeps project-type selection local and restores it after project detail', async () => {
    const user = userEvent.setup()
    render(<ProjectTypeViews />)

    expect(screen.getByRole('heading', { name: 'Backend service' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Game/i }))
    expect(screen.getByRole('heading', { name: 'Game' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'ios-game' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'ios-game' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-game' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Game' })).toBeInTheDocument()
  })

  it('drills the timeline from year to month to week and exposes projects for the selected period', async () => {
    const user = userEvent.setup()
    const onOpenProject = vi.fn()
    render(<TimelineView onOpenProject={onOpenProject} />)

    expect(screen.getByRole('button', { name: /2025/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /2026/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /2026/i }))
    expect(screen.getAllByRole('heading', { name: '2026' }).length).toBeGreaterThanOrEqual(1)
    expect(screen.getByRole('button', { name: /August 2026/i })).toBeInTheDocument()
    expect(screen.getByText(/170 changed lines across 1 active project/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /August 2026/i }))
    expect(screen.getByRole('heading', { name: 'August 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Week of Aug 3/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Week of Aug 3/i }))
    expect(screen.getByText(/60 changed lines across 1 active project/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))
    expect(onOpenProject).toHaveBeenCalledWith('repo-1')

    await user.click(screen.getByRole('button', { name: /Back to 2026/i }))
    expect(screen.getAllByRole('heading', { name: '2026' }).length).toBeGreaterThanOrEqual(1)
  })
})
