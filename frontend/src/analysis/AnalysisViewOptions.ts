/**
 * Describes how an analysis result should be presented.
 *
 * These options deliberately do not change which projects or observations are
 * part of the current analysis. Selection/filter state belongs in AnalysisScope.
 */
export type AnalysisMetric = 'changed-lines' | 'commits'
export type AnalysisColourBy = 'none' | 'technology' | 'project-type'
export type AnalysisSortDirection = 'asc' | 'desc'

export type AnalysisSort = {
  key: string
  direction: AnalysisSortDirection
}

export type AnalysisViewOptions = {
  metric?: AnalysisMetric
  colourBy?: AnalysisColourBy
  sort?: AnalysisSort
  groupBy?: string
}

export const emptyAnalysisViewOptions: AnalysisViewOptions = {}

export function createAnalysisViewOptions(
  overrides: Partial<AnalysisViewOptions> = {},
): AnalysisViewOptions {
  return {
    ...emptyAnalysisViewOptions,
    ...overrides,
    sort: overrides.sort ? { ...overrides.sort } : undefined,
  }
}
