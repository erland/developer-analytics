import { describe, expect, it } from 'vitest'
import { countActiveAnalysisFilters, createAnalysisScope } from '../../analysis/AnalysisScope'
import { analysisScopeFromSearchParams, analysisScopeToSearchParams } from '../../analysis/AnalysisScopeUrl'
import { nonEmptyActivityPeriods } from '../../analysis/AnalysisTimeline'
import { projectFacetOptions } from '../../analysis/ProjectFacetOptions'

describe('analysis model unit contracts', () => {
  it('combines multiple selections without losing independent filter dimensions', () => {
    const scope = createAnalysisScope({
      technologies: ['java', 'typescript'],
      projectTypes: ['backend', 'cli'],
      ownership: 'own',
      visibility: 'private',
      year: 2026,
      month: '2026-08',
      search: 'analytics',
    })

    expect(countActiveAnalysisFilters(scope)).toBe(8)

    const roundTripped = analysisScopeFromSearchParams(analysisScopeToSearchParams(scope))
    expect(roundTripped).toEqual(scope)
  })

  it('normalises project facets without depending on paginated project rows', () => {
    expect(projectFacetOptions([
      { key: 'java', name: 'Java', count: 23 },
      { key: 'typescript', name: 'TypeScript', count: 48 },
    ])).toEqual([
      { value: 'java', label: 'Java', count: 23 },
      { value: 'typescript', label: 'TypeScript', count: 48 },
    ])
  })

  it('removes timeline periods with no observable activity or matching projects', () => {
    const periods = [
      { month: '2025-01', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 0 },
      { month: '2025-02', commits: 1, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 1 },
      { month: '2025-03', commits: 0, changedLines: 0, lineStatisticsCommitCount: 2, projectCount: 0 },
      { month: '2025-04', commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, projectCount: 1 },
    ]

    expect(nonEmptyActivityPeriods(periods).map((period) => period.month)).toEqual([
      '2025-02',
      '2025-03',
      '2025-04',
    ])
  })

  it('supports project-type timeline rows that use activeProjectCount instead of projectCount', () => {
    expect(nonEmptyActivityPeriods([
      { commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, activeProjectCount: 0 },
      { commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, activeProjectCount: 2 },
    ])).toHaveLength(1)
  })
})
