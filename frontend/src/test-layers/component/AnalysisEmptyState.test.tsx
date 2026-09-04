import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { createAnalysisScope } from '../../analysis/AnalysisScope'
import { AnalysisEmptyState } from '../../components/AnalysisEmptyState'

describe('AnalysisEmptyState', () => {
  it('explains the current selection and can clear only the period', async () => {
    const user = userEvent.setup()
    const onScopeChange = vi.fn()
    const scope = createAnalysisScope({ technologies: ['java'], year: 2025, month: '2025-08' })

    render(
      <AnalysisEmptyState
        title="No projects match the current selection."
        description="Broaden the analysis selection to see projects again."
        scope={scope}
        onScopeChange={onScopeChange}
      />,
    )

    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Technology: Java')
    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Period: 2025-08')

    await user.click(screen.getByRole('button', { name: 'Clear period' }))
    expect(onScopeChange).toHaveBeenLastCalledWith(expect.objectContaining({
      technologies: ['java'],
      year: undefined,
      month: undefined,
      week: undefined,
    }))
  })

  it('can clear the complete analysis selection', async () => {
    const user = userEvent.setup()
    const onScopeChange = vi.fn()

    render(
      <AnalysisEmptyState
        title="No activity matches the current selection."
        description="Broaden the analysis selection."
        scope={createAnalysisScope({ projectTypes: ['backend-service'], ownership: 'own' })}
        onScopeChange={onScopeChange}
      />,
    )

    expect(screen.getByLabelText('Current selection')).toHaveTextContent('Project type: Backend Service')
    await user.click(screen.getByRole('button', { name: 'Clear all filters' }))
    expect(onScopeChange).toHaveBeenLastCalledWith(createAnalysisScope())
  })
})
