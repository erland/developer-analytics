import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ContributionsView } from '../../components/ContributionsView'

const mocks = vi.hoisted(() => ({
  scopeSpy: vi.fn(),
  matchingScopeSpy: vi.fn(),
  contributionState: {
    status: 'ready',
    data: {
      total: 9,
      commits: 6,
      pullRequests: 1,
      reviews: 1,
      issues: 1,
      recentProjects: [{
        repositoryId: 'repo-java',
        repositoryName: 'java-service',
        lastActivityAt: '2026-08-10T00:00:00Z',
        contributionCount: 9,
        commitCount: 6,
      }],
    },
  } as const,
}))

vi.mock('../../hooks/useContributions', () => ({
  useContributions: (scope: unknown) => {
    mocks.scopeSpy(scope)
    return mocks.contributionState
  },
}))

vi.mock('../../hooks/useTechnologyViews', () => ({
  useTechnologyViews: () => ({
    status: 'ready',
    error: null,
    data: [
      { technologyKey: 'java', technologyName: 'Java', projectCount: 12 },
      { technologyKey: 'typescript', technologyName: 'TypeScript', projectCount: 8 },
    ],
  }),
}))

vi.mock('../../hooks/useProjectTypes', () => ({
  useProjectTypes: () => ({
    status: 'ready',
    error: null,
    data: [
      { categoryKey: 'backend', categoryName: 'Backend service', projectCount: 10 },
      { categoryKey: 'game', categoryName: 'Game', projectCount: 4 },
    ],
  }),
}))

vi.mock('../../hooks/useMatchingProjects', () => ({
  useMatchingProjects: (scope: unknown, page: number, pageSize: number) => {
    mocks.matchingScopeSpy(scope)
    return ({
    status: 'ready',
    error: null,
    data: {
      items: [{
        id: 'repo-java',
        name: 'java-service',
        description: 'Java service',
        htmlUrl: null,
        ownershipRelation: 'OWNED_BY_USER',
        visibility: 'PUBLIC',
        lastActivityAt: '2026-08-10T00:00:00Z',
        categories: [{ key: 'backend', name: 'Backend service' }],
        technologies: [{ key: 'java', name: 'Java' }],
      }],
      total: 1,
      page,
      pageSize,
      totalPages: 1,
      facets: { technologies: [], projectTypes: [], ownership: [] },
    },
  })
  },
}))

vi.mock('../../components/ProjectDetailView', () => ({
  ProjectDetailView: ({ repositoryId, onBack }: { repositoryId: string; onBack: () => void }) => (
    <section>
      <h2>Project detail {repositoryId}</h2>
      <button type="button" onClick={onBack}>Back to matching projects</button>
    </section>
  ),
}))


describe('Contributions AnalysisScope filtering', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/contributions')
    mocks.scopeSpy.mockClear()
    mocks.matchingScopeSpy.mockClear()
  })

  it('applies shared Explore filters through URL-backed AnalysisScope', async () => {
    const user = userEvent.setup()
    render(<ContributionsView />)

    await user.selectOptions(screen.getByLabelText('Technology'), 'java')
    expect(window.location.search).toContain('technology=java')
    expect(mocks.scopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({ technologies: ['java'] }))

    await user.selectOptions(screen.getByLabelText('Project type'), 'backend')
    expect(window.location.search).toContain('projectType=backend')
    expect(mocks.scopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      technologies: ['java'],
      projectTypes: ['backend'],
    }))

    await user.selectOptions(screen.getByLabelText('Ownership'), 'own')
    expect(window.location.search).toContain('ownership=own')

    await user.selectOptions(screen.getByLabelText('Period'), '12m')
    expect(window.location.search).toContain('from=')
    expect(mocks.scopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      technologies: ['java'],
      projectTypes: ['backend'],
      ownership: 'own',
      from: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    }))
    expect(mocks.matchingScopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      technologies: ['java'],
      projectTypes: ['backend'],
      ownership: 'own',
      from: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
    }))
  })

  it('restores filters from a deep link and browser history', async () => {
    window.history.replaceState(null, '', '/contributions?technology=java&projectType=backend&year=2026')
    render(<ContributionsView />)

    expect(screen.getByLabelText('Technology')).toHaveValue('java')
    expect(screen.getByLabelText('Project type')).toHaveValue('backend')
    expect(mocks.scopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({
      technologies: ['java'],
      projectTypes: ['backend'],
      year: 2026,
    }))

    window.history.pushState(null, '', '/contributions?technology=typescript')
    window.dispatchEvent(new PopStateEvent('popstate'))

    await waitFor(() => {
      expect(screen.getByLabelText('Technology')).toHaveValue('typescript')
    })
    expect(mocks.scopeSpy).toHaveBeenLastCalledWith(expect.objectContaining({ technologies: ['typescript'] }))
  })

  it('shows matching projects for the contribution scope and preserves project-detail back navigation', async () => {
    const user = userEvent.setup()
    render(<ContributionsView />)

    const matchingProjectsHeading = screen.getByRole('heading', { name: 'Projects matching this selection' })
    expect(matchingProjectsHeading).toBeInTheDocument()
    expect(screen.getByText('1 projects')).toBeInTheDocument()
    const contributionStatisticsHeading = screen.getByRole('heading', { name: 'Contribution statistics' })
    expect(matchingProjectsHeading.compareDocumentPosition(contributionStatisticsHeading) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()

    await user.click(screen.getByRole('button', { name: 'java-service' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-java' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to matching projects' }))
    expect(screen.getByRole('heading', { name: 'Projects matching this selection' })).toBeInTheDocument()
  })
})
