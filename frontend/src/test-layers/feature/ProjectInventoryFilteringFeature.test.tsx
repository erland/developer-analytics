import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectInventoryView } from '../../components/ProjectInventoryView'

const mocks = vi.hoisted(() => ({
  lastFilters: null as null | Record<string, unknown>,
}))

vi.mock('../../hooks/useProjectInventory', () => ({
  initialInventoryFilters: {
    page: 0,
    pageSize: 25,
    search: '',
    ownership: '',
    visibility: '',
    activity: '',
    category: '',
    technology: '',
  },
  useProjectInventory: (filters: Record<string, unknown>) => {
    mocks.lastFilters = filters
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
        page: Number(filters.page),
        pageSize: 25,
        totalPages: 2,
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

describe('current project inventory filtering regression coverage', () => {
  beforeEach(() => {
    mocks.lastFilters = null
  })

  it('passes project filters to the inventory hook and resets pagination when a filter changes', async () => {
    const user = userEvent.setup()
    render(<ProjectInventoryView />)

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(mocks.lastFilters).toMatchObject({ page: 1 })

    await user.selectOptions(screen.getByLabelText('Ownership'), 'own')
    expect(mocks.lastFilters).toMatchObject({ page: 0, ownership: 'own' })

    await user.selectOptions(screen.getByLabelText('Technology'), 'java')
    expect(mocks.lastFilters).toMatchObject({ page: 0, ownership: 'own', technology: 'java' })

    await user.type(screen.getByLabelText('Search projects'), 'developer')
    expect(mocks.lastFilters).toMatchObject({ page: 0, search: 'developer' })
  })

  it('derives category and technology filter options from the currently returned inventory items', () => {
    render(<ProjectInventoryView />)

    expect(screen.getByRole('option', { name: 'Backend service' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Java' })).toBeInTheDocument()
  })

  it('returns from project detail to the same project inventory view', async () => {
    const user = userEvent.setup()
    render(<ProjectInventoryView />)

    await user.selectOptions(screen.getByLabelText('Ownership'), 'own')
    await user.click(screen.getByRole('button', { name: 'developer-analytics' }))
    expect(screen.getByRole('heading', { name: 'Project detail repo-1' })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Back to projects' }))
    expect(screen.getByRole('heading', { name: 'Projects' })).toBeInTheDocument()
    expect(screen.getByLabelText('Ownership')).toHaveValue('own')
  })
})
