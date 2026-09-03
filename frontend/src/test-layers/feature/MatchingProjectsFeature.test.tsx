import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAnalysisScope } from '../../analysis/AnalysisScope'
import { MatchingProjects } from '../../components/MatchingProjects'

const mocks = vi.hoisted(() => ({
  calls: [] as Array<{ scope: unknown; page: number; pageSize: number }>,
}))

vi.mock('../../hooks/useMatchingProjects', () => ({
  useMatchingProjects: (scope: unknown, page: number, pageSize: number) => {
    mocks.calls.push({ scope, page, pageSize })
    return {
      status: 'ready',
      error: null,
      data: {
        items: [{
          id: 'repo-1',
          name: 'developer-analytics',
          description: 'Analytics service',
          htmlUrl: null,
          ownershipRelation: 'OWNED_BY_USER',
          visibility: 'PUBLIC',
          lastActivityAt: '2026-08-20T08:00:00Z',
          categories: [{ key: 'backend', name: 'Backend service' }],
          technologies: [
            { key: 'java', name: 'Java' },
            { key: 'typescript', name: 'TypeScript' },
          ],
        }],
        total: 30,
        page,
        pageSize,
        totalPages: 2,
        facets: { technologies: [], projectTypes: [], ownership: [] },
      },
    }
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

describe('MatchingProjects', () => {
  beforeEach(() => {
    mocks.calls = []
    window.history.replaceState(null, '', '/')
  })

  it('queries the project inventory from the supplied analysis scope', () => {
    const scope = createAnalysisScope({
      technologies: ['java'],
      projectTypes: ['backend'],
      year: 2025,
    })

    render(<MatchingProjects scope={scope} />)

    expect(mocks.calls.at(-1)).toMatchObject({ scope, page: 0, pageSize: 25 })
    expect(screen.getByRole('heading', { name: 'Projects matching this selection' })).toBeInTheDocument()
    expect(screen.getByText('30 projects')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'developer-analytics' })).toBeInTheDocument()
    expect(screen.getByText('Java · TypeScript')).toBeInTheDocument()
  })

  it('supports pagination and resets to the first page when scope changes', async () => {
    const user = userEvent.setup()
    const initialScope = createAnalysisScope({ technologies: ['java'] })
    const { rerender } = render(<MatchingProjects scope={initialScope} />)

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(mocks.calls.at(-1)).toMatchObject({ page: 1 })

    const nextScope = createAnalysisScope({ technologies: ['typescript'] })
    rerender(<MatchingProjects scope={nextScope} />)
    expect(mocks.calls.at(-1)).toMatchObject({ scope: nextScope, page: 0 })
  })

  it('returns from project detail to the same matching-project result', async () => {
    const user = userEvent.setup()
    const scope = createAnalysisScope({ technologies: ['java'], year: 2025 })
    render(<MatchingProjects scope={scope} />)

    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-1' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to matching projects' }))
    expect(screen.getByRole('heading', { name: 'Projects matching this selection' })).toBeInTheDocument()
    expect(mocks.calls.at(-1)).toMatchObject({ scope, page: 0 })
  })

  it('browser back from project detail restores the same scoped matching-project result', async () => {
    const user = userEvent.setup()
    window.history.replaceState(null, '', '/?technology=java&year=2025')
    const scope = createAnalysisScope({ technologies: ['java'], year: 2025 })

    render(<MatchingProjects scope={scope} />)

    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))

    let params = new URLSearchParams(window.location.search)
    expect(params.get('project')).toBe('repo-1')
    expect(params.get('technology')).toBe('java')
    expect(params.get('year')).toBe('2025')
    expect(screen.getByRole('heading', { name: 'Project detail repo-1' })).toBeInTheDocument()

    act(() => {
      window.history.back()
    })

    await waitFor(() => {
      expect(new URLSearchParams(window.location.search).get('project')).toBeNull()
    })

    params = new URLSearchParams(window.location.search)
    expect(params.get('technology')).toBe('java')
    expect(params.get('year')).toBe('2025')
    expect(screen.getByRole('heading', { name: 'Projects matching this selection' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'developer-analytics' })).toBeInTheDocument()
    expect(mocks.calls.at(-1)).toMatchObject({ scope, page: 0 })
  })

})
