import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectTypeViews } from '../../components/ProjectTypeViews'
import { TechnologyViews } from '../../components/TechnologyViews'

const mocks = vi.hoisted(() => ({
  technologyState: null as unknown,
  projectTypeState: null as unknown,
  matchingScopes: [] as Array<Record<string, unknown>>,
}))

vi.mock('../../hooks/useTechnologyViews', () => ({
  useTechnologyViews: () => mocks.technologyState,
}))

vi.mock('../../hooks/useProjectTypes', () => ({
  useProjectTypes: () => mocks.projectTypeState,
}))

vi.mock('../../hooks/useCorrections', () => ({
  setTechnologySuppressed: vi.fn(),
}))

vi.mock('../../hooks/useMatchingProjects', () => ({
  useMatchingProjects: (scope: Record<string, unknown>, page: number, pageSize: number) => {
    mocks.matchingScopes.push({ ...scope })
    const technology = (scope.technologies as string[] | undefined)?.[0]
    const projectType = (scope.projectTypes as string[] | undefined)?.[0]
    const year = scope.year as number | undefined

    const name = technology === 'java'
      ? (year === 2026 ? 'java-2026-service' : 'java-service')
      : projectType === 'game'
        ? (year === 2026 ? 'game-2026-project' : 'game-project')
        : 'other-project'

    return {
      status: 'ready',
      error: null,
      data: {
        items: [{
          id: `repo-${name}`,
          name,
          description: null,
          htmlUrl: null,
          ownershipRelation: 'OWNED_BY_USER',
          visibility: 'PUBLIC',
          lastActivityAt: '2026-08-15T00:00:00Z',
          categories: projectType ? [{ key: projectType, name: projectType === 'game' ? 'Game' : projectType }] : [],
          technologies: technology ? [{ key: technology, name: technology === 'java' ? 'Java' : technology }] : [],
        }],
        total: 1,
        page,
        pageSize,
        totalPages: 1,
        facets: { technologies: [], projectTypes: [], ownership: [] },
      },
    }
  },
}))

vi.mock('../../components/ProjectDetailView', () => ({
  ProjectDetailView: () => <div>Project detail</div>,
}))

function technology(key: string, name: string) {
  return {
    technologyKey: key,
    technologyName: name,
    technologyCategory: 'LANGUAGE',
    evidenceLevel: 'STRONG',
    evidenceScore: 90,
    projectCount: 2,
    evidenceCount: 4,
    independentEvidenceTypes: 2,
    firstObservedAt: '2024-01-01T00:00:00Z',
    lastObservedAt: '2026-08-15T00:00:00Z',
    recentProjectCount: 1,
    privacyProvenance: 'PUBLIC_ONLY',
    rationale: {},
    timeline: key === 'java'
      ? [
          { month: '2024-05-01', commits: 2, changedLines: 20, lineStatisticsCommitCount: 2, projectCount: 1 },
          { month: '2025-01-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 0 },
          { month: '2026-08-01', commits: 4, changedLines: 40, lineStatisticsCommitCount: 4, projectCount: 1 },
        ]
      : [{ month: '2023-03-01', commits: 3, changedLines: 30, lineStatisticsCommitCount: 3, projectCount: 1 }],
    representativeProjects: [],
  }
}

function projectType(key: string, name: string) {
  return {
    categoryKey: key,
    categoryName: name,
    projectCount: 2,
    activityCount: 8,
    timeline: key === 'game'
      ? [
          { month: '2025-02-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, activeProjectCount: 0 },
          { month: '2026-08-01', commits: 8, changedLines: 80, lineStatisticsCommitCount: 8, activeProjectCount: 2 },
        ]
      : [{ month: '2024-04-01', commits: 5, changedLines: 50, lineStatisticsCommitCount: 5, activeProjectCount: 1 }],
    representativeProjects: [],
  }
}

describe('Explore selection consistency', () => {
  beforeEach(() => {
    mocks.matchingScopes = []
    mocks.technologyState = {
      status: 'ready',
      error: null,
      data: [technology('java', 'Java'), technology('swift', 'Swift')],
    }
    mocks.projectTypeState = {
      status: 'ready',
      error: null,
      data: [projectType('backend', 'Backend service'), projectType('game', 'Game')],
    }
    window.history.replaceState(null, '', '/')
  })

  it('shows only active Java periods and Java matching projects when Java is selected', () => {
    window.history.replaceState(null, '', '/?technology=java')
    render(<TechnologyViews />)

    expect(screen.getByRole('heading', { name: 'Activity in projects using Java' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^2024\b/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^2026\b/i })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^2025\b/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'java-service' })).toBeInTheDocument()
    expect(mocks.matchingScopes[mocks.matchingScopes.length - 1]).toMatchObject({ technologies: ['java'] })
  })

  it('uses Java plus the selected year for the matching project list', async () => {
    const user = userEvent.setup()
    window.history.replaceState(null, '', '/?technology=java')
    render(<TechnologyViews />)

    await user.click(screen.getByRole('button', { name: /^2026\b/i }))

    expect(screen.getByRole('button', { name: 'java-2026-service' })).toBeInTheDocument()
    expect(mocks.matchingScopes[mocks.matchingScopes.length - 1]).toMatchObject({ technologies: ['java'], year: 2026 })
    expect(new URLSearchParams(window.location.search).get('year')).toBe('2026')
  })

  it('applies the same project-type and time selection to Over time and matching projects', async () => {
    const user = userEvent.setup()
    window.history.replaceState(null, '', '/?projectType=game')
    render(<ProjectTypeViews />)

    expect(screen.getByRole('heading', { name: 'Activity in Game projects' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^2025\b/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /^2026\b/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'game-project' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^2026\b/i }))

    expect(screen.getByRole('button', { name: 'game-2026-project' })).toBeInTheDocument()
    expect(mocks.matchingScopes[mocks.matchingScopes.length - 1]).toMatchObject({ projectTypes: ['game'], year: 2026 })
  })
})
