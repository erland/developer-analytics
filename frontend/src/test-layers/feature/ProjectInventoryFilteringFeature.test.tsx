import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectInventoryView } from '../../components/ProjectInventoryView'

const mocks = vi.hoisted(() => ({
  lastQuery: null as null | Record<string, unknown>,
}))

vi.mock('../../hooks/useProjectInventory', () => ({
  useProjectInventory: (query: Record<string, unknown>) => {
    mocks.lastQuery = query
    const scope = query.scope as Record<string, unknown>
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
          technologies: [{ key: 'java', name: 'Java' }],
        }],
        total: 30,
        page: Number(query.page),
        pageSize: 25,
        totalPages: 2,
        facets: {
          technologies: [
            { key: 'java', name: 'Java', count: 30 },
            { key: 'typescript', name: 'TypeScript', count: 18 },
          ],
          projectTypes: [
            { key: 'backend', name: 'Backend service', count: 24 },
            { key: 'cli', name: 'CLI tool', count: 6 },
          ],
          ownership: [
            { key: 'own', name: 'Own', count: 20 },
            { key: 'external', name: 'External', count: 10 },
          ],
        },
        _scope: scope,
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

describe('project inventory AnalysisScope filtering', () => {
  beforeEach(() => {
    mocks.lastQuery = null
    window.history.replaceState(null, '', '/projects')
  })

  it('uses AnalysisScope for shared filters, writes them to the URL, and resets pagination', async () => {
    const user = userEvent.setup()
    render(<ProjectInventoryView />)

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(mocks.lastQuery).toMatchObject({ page: 1 })

    await user.selectOptions(screen.getByLabelText('Ownership'), 'own')
    expect(mocks.lastQuery).toMatchObject({
      page: 0,
      scope: expect.objectContaining({ ownership: 'own' }),
    })
    expect(window.location.search).toContain('ownership=own')

    await user.selectOptions(screen.getByLabelText('Technology'), 'java')
    expect(mocks.lastQuery).toMatchObject({
      page: 0,
      scope: expect.objectContaining({ technologies: ['java'], ownership: 'own' }),
    })
    expect(window.location.search).toContain('technology=java')

    await user.selectOptions(screen.getByLabelText('Project type'), 'backend')
    expect(mocks.lastQuery).toMatchObject({
      scope: expect.objectContaining({ projectTypes: ['backend'] }),
    })
    expect(window.location.search).toContain('projectType=backend')

    await user.type(screen.getByLabelText('Search projects'), 'developer')
    expect(mocks.lastQuery).toMatchObject({
      page: 0,
      scope: expect.objectContaining({ search: 'developer' }),
    })
    expect(window.location.search).toContain('search=developer')
  })

  it('restores shared filters from a deep link and browser navigation', async () => {
    window.history.replaceState(null, '', '/projects?technology=java&projectType=backend&ownership=external&visibility=private&year=2025&search=api')
    render(<ProjectInventoryView />)

    expect(screen.getByLabelText('Technology')).toHaveValue('java')
    expect(screen.getByLabelText('Project type')).toHaveValue('backend')
    expect(screen.getByLabelText('Ownership')).toHaveValue('external')
    expect(screen.getByLabelText('Visibility')).toHaveValue('private')
    expect(screen.getByLabelText('Search projects')).toHaveValue('api')
    expect(mocks.lastQuery).toMatchObject({
      scope: expect.objectContaining({ year: 2025 }),
    })

    window.history.pushState(null, '', '/projects?technology=typescript')
    fireEvent.popState(window)
    expect(screen.getByLabelText('Technology')).toHaveValue('typescript')
    expect(screen.getByLabelText('Project type')).toHaveValue('')
    expect(mocks.lastQuery).toMatchObject({ page: 0 })
  })

  it('separates project scope filters from project-list-only options', async () => {
    const user = userEvent.setup()
    render(<ProjectInventoryView />)

    expect(screen.getByRole('heading', { name: 'Project filters' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Project list options' })).toBeInTheDocument()
    expect(screen.getByText(/part of the analysis selection/i)).toBeInTheDocument()
    expect(screen.getByText(/not part of the analysis scope/i)).toBeInTheDocument()

    await user.type(screen.getByLabelText('Search projects'), 'api')
    await user.selectOptions(screen.getByLabelText('Visibility'), 'private')
    expect(mocks.lastQuery).toMatchObject({
      scope: expect.objectContaining({ search: 'api', visibility: 'private' }),
    })
    expect(window.location.search).toContain('search=api')
    expect(window.location.search).toContain('visibility=private')

    await user.selectOptions(screen.getByLabelText('Activity'), 'active')
    expect(mocks.lastQuery).toMatchObject({ activity: 'active' })
    expect((mocks.lastQuery?.scope as Record<string, unknown>)).not.toHaveProperty('activity')
    expect(window.location.search).not.toContain('activity=')
  })

  it('derives project type and technology options from server facets rather than the current page', () => {
    render(<ProjectInventoryView />)

    expect(screen.getByRole('option', { name: 'Backend service (24)' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'CLI tool (6)' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Java (30)' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'TypeScript (18)' })).toBeInTheDocument()
  })

  it('returns from project detail with the same AnalysisScope selection', async () => {
    const user = userEvent.setup()
    render(<ProjectInventoryView />)

    await user.selectOptions(screen.getByLabelText('Ownership'), 'own')
    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-1' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Projects' })).toBeInTheDocument()
    expect(screen.getByLabelText('Ownership')).toHaveValue('own')
    expect(window.location.search).toContain('ownership=own')
  })
})
