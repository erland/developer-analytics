import { describe, expect, it } from 'vitest'
import {
  countActiveAnalysisFilters,
  createAnalysisScope,
  emptyAnalysisScope,
  type AnalysisScope,
} from '../../analysis/AnalysisScope'

describe('AnalysisScope foundation', () => {
  it('represents an unfiltered analysis without view-specific presentation state', () => {
    expect(emptyAnalysisScope).toEqual({
      technologies: [],
      projectTypes: [],
    })

    expect('metric' in emptyAnalysisScope).toBe(false)
    expect('colourBy' in emptyAnalysisScope).toBe(false)
    expect('sort' in emptyAnalysisScope).toBe(false)
  })

  it('can represent the common selection dimensions planned for Explore views', () => {
    const scope: AnalysisScope = createAnalysisScope({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend'],
      ownership: 'own',
      visibility: 'private',
      from: '2025-01-01',
      to: '2025-12-31',
      year: 2025,
      month: '2025-08',
      week: '2025-W32',
      search: 'analytics',
    })

    expect(scope).toEqual({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend'],
      ownership: 'own',
      visibility: 'private',
      from: '2025-01-01',
      to: '2025-12-31',
      year: 2025,
      month: '2025-08',
      week: '2025-W32',
      search: 'analytics',
    })
  })

  it('counts active filter chips while treating a composed time selection as one period filter', () => {
    expect(countActiveAnalysisFilters(createAnalysisScope({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend'],
      ownership: 'own',
      visibility: 'private',
      year: 2026,
      month: '2026-08',
      search: 'analytics',
    }))).toBe(7)

    expect(countActiveAnalysisFilters(createAnalysisScope())).toBe(0)
  })

  it('creates independent technology and project-type collections', () => {
    const first = createAnalysisScope()
    const second = createAnalysisScope()

    expect(first.technologies).not.toBe(second.technologies)
    expect(first.projectTypes).not.toBe(second.projectTypes)
  })
})
