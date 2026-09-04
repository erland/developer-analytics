import { describe, expect, it } from 'vitest'
import { buildCumulativeSeries } from './CumulativeContributionChart'

describe('buildCumulativeSeries', () => {
  it('accumulates additions minus deletions in chronological order', () => {
    expect(buildCumulativeSeries([
      { period: '2026-02', additions: 50, deletions: 20 },
      { period: '2026-01', additions: 100, deletions: 10 },
      { period: '2026-03', additions: 15, deletions: 80 },
    ])).toEqual([
      { period: '2026-01', value: 90, additionsTotal: 100, deletionsTotal: 10 },
      { period: '2026-02', value: 120, additionsTotal: 150, deletionsTotal: 30 },
      { period: '2026-03', value: 55, additionsTotal: 165, deletionsTotal: 110 },
    ])
  })
})
