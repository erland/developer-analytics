import { describe, expect, it } from 'vitest'
import { createAnalysisScope } from '../../analysis/AnalysisScope'
import {
  analysisScopeFromSearchParams,
  analysisScopeToSearchParams,
} from '../../analysis/AnalysisScopeUrl'

describe('AnalysisScope URL serialisation', () => {
  it('round-trips all supported analysis selection dimensions', () => {
    const scope = createAnalysisScope({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend', 'web'],
      ownership: 'own',
      visibility: 'private',
      from: '2025-01-01',
      to: '2025-12-31',
      year: 2025,
      month: '2025-08',
      week: '2025-W32',
      search: 'developer analytics',
    })

    const params = analysisScopeToSearchParams(scope)

    expect(params.getAll('technology')).toEqual(['java', 'typescript'])
    expect(params.getAll('projectType')).toEqual(['backend', 'web'])
    expect(analysisScopeFromSearchParams(params)).toEqual(scope)
  })

  it('omits empty values and de-duplicates repeated selections', () => {
    const params = analysisScopeToSearchParams(createAnalysisScope({
      technologies: ['java', 'java', '  ', 'typescript'],
      projectTypes: ['', 'backend', 'backend'],
      search: '   ',
    }))

    expect(params.toString()).toBe(
      'technology=java&technology=typescript&projectType=backend',
    )
  })

  it('ignores unknown parameters and invalid enum/year values safely', () => {
    const scope = analysisScopeFromSearchParams(
      '?technology=java&technology=&projectType=backend&ownership=team&visibility=secret&year=20x5&unknown=value',
    )

    expect(scope).toEqual({
      technologies: ['java'],
      projectTypes: ['backend'],
      ownership: undefined,
      visibility: undefined,
      from: undefined,
      to: undefined,
      year: undefined,
      month: undefined,
      week: undefined,
      search: undefined,
    })
  })

  it('accepts a query string with or without a leading question mark', () => {
    expect(analysisScopeFromSearchParams('technology=java&year=2026')).toEqual(
      analysisScopeFromSearchParams('?technology=java&year=2026'),
    )
  })
})
