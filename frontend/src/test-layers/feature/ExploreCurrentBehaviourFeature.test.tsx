import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TechnologyViews } from '../../components/TechnologyViews'
import { ProjectTypeViews } from '../../components/ProjectTypeViews'
import { ActivityView } from '../../components/ActivityView'

const mocks = vi.hoisted(() => ({
  technologyState: null as unknown,
  projectTypeState: null as unknown,
  activityState: null as unknown,
  matchingProjectsSpy: vi.fn(),
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

vi.mock('../../hooks/useMatchingProjects', () => ({
  useMatchingProjects: (scope: { technologies: string[]; projectTypes?: string[]; year?: number; month?: string }, page: number, pageSize: number) => {
    mocks.matchingProjectsSpy(scope, page, pageSize)
    const swift = scope.technologies[0] === 'swift'
    const game = scope.projectTypes?.[0] === 'game'
    const backend = scope.projectTypes?.[0] === 'backend'
    const id = game ? 'repo-game' : backend ? 'repo-backend' : swift ? 'repo-swift' : 'repo-java'
    const name = game ? 'ios-game' : backend ? 'api-service' : swift ? 'ios-game' : 'java-service'
    return {
      status: 'ready',
      error: null,
      data: {
        items: [{
          id,
          name,
          description: null,
          htmlUrl: null,
          ownershipRelation: 'OWNED_BY_USER',
          visibility: 'PUBLIC',
          lastActivityAt: '2026-08-01T00:00:00Z',
          categories: [],
          technologies: [{ key: swift ? 'swift' : 'java', name: swift ? 'Swift' : 'Java' }],
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
    timeline: [
      { month: '2025-01-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 0 },
      { month: '2026-08-01', commits: 4, changedLines: 40, lineStatisticsCommitCount: 4, projectCount: 1 },
    ],
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
    timeline: [
      { month: '2025-01-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, activeProjectCount: 0 },
      { month: '2026-08-01', commits: 7, changedLines: 70, lineStatisticsCommitCount: 7, activeProjectCount: 1 },
    ],
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
    window.history.replaceState(null, '', '/')
    mocks.matchingProjectsSpy.mockClear()
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
          projectTypes: ['Backend service'],
          technologies: ['Java', 'Quarkus'],
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

  it('stores technology selection in AnalysisScope URL state and restores it after project detail', async () => {
    const user = userEvent.setup()
    render(<TechnologyViews />)

    expect(screen.getByRole('heading', { name: 'Java' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Activity in projects using Java' })).toBeInTheDocument()
    expect(screen.getByText(/does not imply that every commit or changed line in the period used Java/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^2025\b/i })).not.toBeInTheDocument()
    const yearButton = screen.getByRole('button', { name: /^2026\b/i })
    expect(yearButton).toBeInTheDocument()

    await user.click(yearButton)
    let paramsAfterTimeSelection = new URLSearchParams(window.location.search)
    expect(paramsAfterTimeSelection.getAll('technology')).toEqual(['java'])
    expect(paramsAfterTimeSelection.get('year')).toBe('2026')
    expect(paramsAfterTimeSelection.get('month')).toBeNull()
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ technologies: ['java'], year: 2026, month: undefined }),
      0,
      25,
    )

    const monthButton = screen.getByRole('button', { name: /Aug 2026|August 2026/i })
    await user.click(monthButton)
    paramsAfterTimeSelection = new URLSearchParams(window.location.search)
    expect(paramsAfterTimeSelection.getAll('technology')).toEqual(['java'])
    expect(paramsAfterTimeSelection.get('year')).toBe('2026')
    expect(paramsAfterTimeSelection.get('month')).toBe('2026-08')
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ technologies: ['java'], year: 2026, month: '2026-08' }),
      0,
      25,
    )
    expect(screen.getByRole('button', { name: /Remove Period: 2026-08/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Back to years/i }))
    paramsAfterTimeSelection = new URLSearchParams(window.location.search)
    expect(paramsAfterTimeSelection.getAll('technology')).toEqual(['java'])
    expect(paramsAfterTimeSelection.get('year')).toBeNull()
    expect(paramsAfterTimeSelection.get('month')).toBeNull()
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ technologies: ['java'], year: undefined, month: undefined }),
      0,
      25,
    )

    expect(screen.getByLabelText('Technology summary statistics')).toHaveTextContent('1 project')
    expect(screen.getByLabelText('Technology summary statistics')).toHaveTextContent('3 evidence items')
    expect(screen.queryByLabelText('Technology statistics')).not.toBeInTheDocument()
    const evidenceSummary = screen.getByText('Evidence and statistics')
    const evidenceDetails = evidenceSummary.closest('details')
    expect(evidenceDetails).not.toBeNull()
    expect(evidenceDetails).not.toHaveAttribute('open')
    await user.click(evidenceSummary)
    expect(evidenceDetails).toHaveAttribute('open')
    expect(within(evidenceDetails as HTMLElement).getByText('Evidence types')).toBeInTheDocument()
    expect(within(evidenceDetails as HTMLElement).getByText('Public data only')).toBeInTheDocument()

    const advancedSummary = screen.getByText('Advanced')
    const advancedDetails = advancedSummary.closest('details')
    expect(advancedDetails).not.toBeNull()
    expect(advancedDetails).not.toHaveAttribute('open')
    await user.click(advancedSummary)
    expect(advancedDetails).toHaveAttribute('open')
    expect(screen.getByRole('button', { name: 'Suppress technology inference' })).toBeInTheDocument()

    const technologyFilter = screen.getByRole('combobox', { name: 'Technology' })
    expect(technologyFilter).toHaveValue('java')
    expect(new URLSearchParams(window.location.search).getAll('technology')).toEqual(['java'])

    await user.selectOptions(technologyFilter, 'swift')
    expect(screen.getByRole('heading', { name: 'Swift' })).toBeInTheDocument()
    expect(new URLSearchParams(window.location.search).getAll('technology')).toEqual(['swift'])
    expect(screen.getByRole('button', { name: 'ios-game' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'ios-game' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-swift' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Swift' })).toBeInTheDocument()
    expect(new URLSearchParams(window.location.search).getAll('technology')).toEqual(['swift'])
  })

  it('restores technology selection from URL navigation without dropping other scope parameters', async () => {
    window.history.replaceState(null, '', '/?technology=swift&projectType=game&year=2026&month=2026-08')
    render(<TechnologyViews />)

    expect(screen.getByRole('heading', { name: 'Swift' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Aug 2026|August 2026/i })).toHaveAttribute('aria-pressed', 'true')
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ technologies: ['swift'], projectTypes: ['game'], year: 2026, month: '2026-08' }),
      0,
      25,
    )

    window.history.pushState(null, '', '/?technology=java&projectType=game&year=2026')
    window.dispatchEvent(new PopStateEvent('popstate'))

    expect(await screen.findByRole('heading', { name: 'Java' })).toBeInTheDocument()
    const params = new URLSearchParams(window.location.search)
    expect(params.get('projectType')).toBe('game')
    expect(params.get('year')).toBe('2026')
    expect(params.get('month')).toBeNull()
  })

  it('stores project-type selection in AnalysisScope URL and restores it after project detail', async () => {
    const user = userEvent.setup()
    render(<ProjectTypeViews />)

    expect(screen.getByRole('heading', { name: 'Backend service' })).toBeInTheDocument()
    expect(new URLSearchParams(window.location.search).getAll('projectType')).toEqual(['backend'])

    expect(screen.getByRole('heading', { name: 'Activity in Backend service projects' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /2025/i })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /2026/i })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Projects matching this selection/i })).toBeInTheDocument()
    expect(screen.getByText('1 project')).toBeInTheDocument()
    expect(screen.getByText('7 observed commits')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Project type'), 'game')
    expect(screen.getByRole('heading', { name: 'Game' })).toBeInTheDocument()
    expect(new URLSearchParams(window.location.search).getAll('projectType')).toEqual(['game'])
    expect(screen.getByRole('heading', { name: 'Activity in Game projects' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'ios-game' })).toBeInTheDocument()
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ projectTypes: ['game'] }),
      0,
      25,
    )

    await user.click(screen.getByRole('button', { name: /2026/i }))
    expect(new URLSearchParams(window.location.search).get('year')).toBe('2026')
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ projectTypes: ['game'], year: 2026 }),
      0,
      25,
    )

    await user.click(screen.getByRole('button', { name: /Aug 2026|August 2026/i }))
    const monthParams = new URLSearchParams(window.location.search)
    expect(monthParams.get('year')).toBe('2026')
    expect(monthParams.get('month')).toBe('2026-08')
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ projectTypes: ['game'], year: 2026, month: '2026-08' }),
      0,
      25,
    )

    await user.click(screen.getByRole('button', { name: /Back to years/i }))
    const clearedTimeParams = new URLSearchParams(window.location.search)
    expect(clearedTimeParams.get('projectType')).toBe('game')
    expect(clearedTimeParams.get('year')).toBeNull()
    expect(clearedTimeParams.get('month')).toBeNull()

    const statistics = screen.getByText('Classification statistics').closest('details')
    expect(statistics).not.toBeNull()
    expect(statistics).not.toHaveAttribute('open')

    await user.click(screen.getByRole('button', { name: 'ios-game' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-game' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Game' })).toBeInTheDocument()
    expect(new URLSearchParams(window.location.search).getAll('projectType')).toEqual(['game'])
  })

  it('restores project-type selection from URL navigation without dropping other scope parameters', async () => {
    window.history.replaceState(null, '', '/?projectType=game&technology=swift&year=2026&month=2026-08')
    render(<ProjectTypeViews />)

    expect(screen.getByRole('heading', { name: 'Game' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Aug 2026|August 2026/i })).toHaveAttribute('aria-pressed', 'true')
    expect(mocks.matchingProjectsSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ projectTypes: ['game'], technologies: ['swift'], year: 2026, month: '2026-08' }),
      0,
      25,
    )

    window.history.pushState(null, '', '/?projectType=backend&technology=swift&year=2026')
    window.dispatchEvent(new PopStateEvent('popstate'))

    expect(await screen.findByRole('heading', { name: 'Backend service' })).toBeInTheDocument()
    const params = new URLSearchParams(window.location.search)
    expect(params.get('technology')).toBe('swift')
    expect(params.get('year')).toBe('2026')
  })

  it('explains an empty Technology over-time result and lets the user clear the period', async () => {
    const user = userEvent.setup()
    const emptyJava = technology('java', 'Java', 'java-service', 90)
    emptyJava.timeline = [
      { month: '2025-01-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 0 },
    ]
    mocks.technologyState = { status: 'ready', data: [emptyJava], error: null }
    window.history.replaceState(null, '', '/?technology=java&year=2025')

    render(<TechnologyViews />)

    expect(screen.getByRole('heading', { name: 'No activity over time for this selection.' })).toBeInTheDocument()
    expect(screen.getByText(/No recorded activity was found in projects where Java has been observed during the selected period/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Technology: Java')
    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Period: 2025')

    await user.click(screen.getByRole('button', { name: 'Clear period' }))
    const params = new URLSearchParams(window.location.search)
    expect(params.get('technology')).toBe('java')
    expect(params.get('year')).toBeNull()
  })

  it('explains an empty Project type over-time result and lets the user clear the period', async () => {
    const user = userEvent.setup()
    const emptyGame = projectType('game', 'Game', 'ios-game')
    emptyGame.timeline = [
      { month: '2025-01-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, activeProjectCount: 0 },
    ]
    mocks.projectTypeState = { status: 'ready', data: [emptyGame], error: null }
    window.history.replaceState(null, '', '/?projectType=game&month=2025-01&year=2025')

    render(<ProjectTypeViews />)

    expect(screen.getByRole('heading', { name: 'No activity over time for this selection.' })).toBeInTheDocument()
    expect(screen.getByText(/No recorded activity was found in Game projects during the selected period/i)).toBeInTheDocument()
    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Project type: Game')
    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Period: 2025-01')

    await user.click(screen.getByRole('button', { name: 'Clear period' }))
    const params = new URLSearchParams(window.location.search)
    expect(params.get('projectType')).toBe('game')
    expect(params.get('year')).toBeNull()
    expect(params.get('month')).toBeNull()
  })

  it('uses the shared year-to-month-to-week timeline inside Activity and opens projects from the selected period', async () => {
    const user = userEvent.setup()
    const onOpenProject = vi.fn()
    render(<ActivityView onOpenProject={onOpenProject} />)

    expect(screen.getByRole('combobox', { name: 'Activity window' })).toHaveValue('12m')
    expect(screen.getByRole('combobox', { name: 'Measure' })).toHaveValue('lines')
    expect(screen.getByRole('combobox', { name: 'Colour by' })).toHaveValue('projectType')
    expect(screen.getByRole('button', { name: /2026/i })).toBeInTheDocument()
    const overTimeHeading = screen.getByRole('heading', { name: 'Changed lines over time' })
    const activityStatisticsHeading = screen.getByRole('heading', { name: 'Activity statistics' })
    expect(overTimeHeading.compareDocumentPosition(activityStatisticsHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()

    await user.click(screen.getByRole('button', { name: /2026/i }))
    expect(screen.getByRole('button', { name: /August 2026/i })).toBeInTheDocument()
    expect(screen.getByText(/170 changed lines across 1 active project/i)).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /August 2026/i }))
    expect(screen.getByRole('button', { name: /Week of Aug 3/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /Week of Aug 3/i }))
    expect(screen.getByText(/60 changed lines across 1 active project/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))
    expect(onOpenProject).toHaveBeenCalledWith('repo-1')

    await user.selectOptions(screen.getByRole('combobox', { name: 'Colour by' }), 'technology')
    expect(screen.getByText('Java')).toBeInTheDocument()
  })

})
