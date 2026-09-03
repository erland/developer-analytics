import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SummaryFacts } from '../../components/SummaryFacts'

describe('SummaryFacts', () => {
  it('renders compact labelled facts without legacy metric cards', () => {
    const { container } = render(
      <SummaryFacts
        ariaLabel="Activity summary"
        items={[
          { label: 'Commits', value: '12,430' },
          { label: 'Active projects', value: '84' },
          { label: 'Activity period', value: 'Aug 2010 – Sep 2026' },
        ]}
      />,
    )

    const summary = screen.getByLabelText('Activity summary')
    expect(summary).toHaveTextContent('Commits')
    expect(summary).toHaveTextContent('12,430')
    expect(summary).toHaveTextContent('Active projects')
    expect(summary).toHaveTextContent('84')
    expect(summary).toHaveTextContent('Aug 2010 – Sep 2026')
    expect(container.querySelector('.metric-grid')).not.toBeInTheDocument()
    expect(container.querySelector('.metric-card')).not.toBeInTheDocument()
  })
})
