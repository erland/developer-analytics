import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { createAnalysisScope } from '../../analysis/AnalysisScope'
import { AnalysisFilters } from '../../components/AnalysisFilters'

const technologies = [
  { value: 'java', label: 'Java', count: 23 },
  { value: 'typescript', label: 'TypeScript', count: 17 },
]

const projectTypes = [
  { value: 'backend', label: 'Backend', count: 12 },
  { value: 'game', label: 'Game', count: 4 },
]

const periods = [
  { value: '2025', label: '2025', scope: { year: 2025 } },
  { value: '2026', label: '2026', scope: { year: 2026 } },
]

describe('component layer: AnalysisFilters', () => {
  it('renders only configured filter dimensions and emits an updated AnalysisScope', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <AnalysisFilters
        scope={createAnalysisScope()}
        onChange={onChange}
        technologies={technologies}
        projectTypes={projectTypes}
        periods={periods}
        showTechnology
        showProjectType
        showOwnership
        showPeriod
      />,
    )

    expect(screen.getByRole('combobox', { name: 'Technology' })).toBeVisible()
    expect(screen.getByRole('combobox', { name: 'Project type' })).toBeVisible()
    expect(screen.getByRole('combobox', { name: 'Ownership' })).toBeVisible()
    expect(screen.getByRole('combobox', { name: 'Period' })).toBeVisible()

    await user.selectOptions(screen.getByRole('combobox', { name: 'Technology' }), 'java')

    expect(onChange).toHaveBeenLastCalledWith(createAnalysisScope({ technologies: ['java'] }))
  })

  it('maps a configured period to time scope and clears previous time drill-down values', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <AnalysisFilters
        scope={createAnalysisScope({
          technologies: ['java'],
          year: 2026,
          month: '2026-08',
          week: '2026-W32',
        })}
        onChange={onChange}
        periods={periods}
        showPeriod
      />,
    )

    await user.selectOptions(screen.getByRole('combobox', { name: 'Period' }), '2025')

    expect(onChange).toHaveBeenLastCalledWith(createAnalysisScope({
      technologies: ['java'],
      year: 2025,
    }))
  })

  it('clears the complete analysis scope', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <AnalysisFilters
        scope={createAnalysisScope({
          technologies: ['java'],
          projectTypes: ['backend'],
          ownership: 'own',
          year: 2026,
          search: 'analytics',
        })}
        onChange={onChange}
        technologies={technologies}
        showTechnology
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Clear all' }))

    expect(onChange).toHaveBeenLastCalledWith(createAnalysisScope())
  })

  it('keeps unconfigured filters out of the UI', () => {
    render(
      <AnalysisFilters
        scope={createAnalysisScope()}
        onChange={() => undefined}
        technologies={technologies}
        showTechnology
      />,
    )

    expect(screen.getByRole('combobox', { name: 'Technology' })).toBeVisible()
    expect(screen.queryByRole('combobox', { name: 'Project type' })).not.toBeInTheDocument()
    expect(screen.queryByRole('combobox', { name: 'Ownership' })).not.toBeInTheDocument()
    expect(screen.queryByRole('combobox', { name: 'Period' })).not.toBeInTheDocument()
  })
})

describe('component layer: AnalysisFilters compact controls', () => {
  it('exposes a compact Edit filters toggle with the active-filter count', async () => {
    const user = userEvent.setup()

    render(
      <AnalysisFilters
        scope={createAnalysisScope({ technologies: ['java'], year: 2026 })}
        onChange={() => undefined}
        technologies={technologies}
        periods={periods}
        showTechnology
        showPeriod
      />,
    )

    const edit = screen.getByRole('button', { name: 'Edit filters' })
    expect(edit).toHaveAttribute('aria-expanded', 'false')
    expect(screen.getByText('Filters · 2 active')).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Active filters' })).toHaveTextContent('Technology: Java')

    await user.click(edit)

    expect(screen.getByRole('button', { name: 'Done' })).toHaveAttribute('aria-expanded', 'true')
  })
})

describe('component layer: AnalysisFilters active chips', () => {
  it('shows the active scope as removable chips, including filters without visible fields', () => {
    render(
      <AnalysisFilters
        scope={createAnalysisScope({
          technologies: ['java'],
          projectTypes: ['backend'],
          ownership: 'external',
          visibility: 'private',
          year: 2026,
          search: 'analytics',
        })}
        onChange={() => undefined}
        technologies={technologies}
        projectTypes={projectTypes}
        periods={periods}
        showTechnology
      />,
    )

    const activeFilters = screen.getByRole('group', { name: 'Active filters' })
    expect(activeFilters).toHaveTextContent('Technology: Java')
    expect(activeFilters).toHaveTextContent('Project type: Backend')
    expect(activeFilters).toHaveTextContent('Ownership: External')
    expect(activeFilters).toHaveTextContent('Visibility: Private')
    expect(activeFilters).toHaveTextContent('Period: 2026')
    expect(activeFilters).toHaveTextContent('Search: analytics')
  })

  it('removes only the selected filter dimension', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()
    const scope = createAnalysisScope({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend'],
      ownership: 'own',
      year: 2026,
    })

    render(
      <AnalysisFilters
        scope={scope}
        onChange={onChange}
        technologies={technologies}
        projectTypes={projectTypes}
        periods={periods}
        showTechnology
        showProjectType
        showOwnership
        showPeriod
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Remove Technology: Java' }))

    expect(onChange).toHaveBeenLastCalledWith(createAnalysisScope({
      technologies: ['typescript'],
      projectTypes: ['backend'],
      ownership: 'own',
      year: 2026,
    }))
  })

  it('removes the complete time selection while preserving non-time filters', async () => {
    const user = userEvent.setup()
    const onChange = vi.fn()

    render(
      <AnalysisFilters
        scope={createAnalysisScope({
          technologies: ['java'],
          year: 2026,
          month: '2026-08',
          week: '2026-W32',
        })}
        onChange={onChange}
        technologies={technologies}
        showTechnology
      />,
    )

    await user.click(screen.getByRole('button', { name: 'Remove Period: 2026-W32' }))

    expect(onChange).toHaveBeenLastCalledWith(createAnalysisScope({
      technologies: ['java'],
    }))
  })
})
