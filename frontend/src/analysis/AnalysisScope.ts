/**
 * Describes the data set currently being analysed across Explore views.
 *
 * This deliberately contains only selection/filter state. Presentation state
 * such as metric, colour mode, sort order and grouping belongs in view-specific
 * options instead.
 */
export type AnalysisOwnership = 'own' | 'external'
export type AnalysisVisibility = 'public' | 'private'

export type AnalysisScope = {
  technologies: readonly string[]
  projectTypes: readonly string[]
  ownership?: AnalysisOwnership
  visibility?: AnalysisVisibility
  from?: string
  to?: string
  year?: number
  month?: string
  week?: string
  search?: string
}

export const emptyAnalysisScope: AnalysisScope = {
  technologies: [],
  projectTypes: [],
}

/**
 * Creates a scope with independent collection values so callers can safely use
 * the shared empty scope as a template without sharing mutable array instances.
 */
export function createAnalysisScope(
  overrides: Partial<AnalysisScope> = {},
): AnalysisScope {
  return {
    ...emptyAnalysisScope,
    ...overrides,
    technologies: [...(overrides.technologies ?? emptyAnalysisScope.technologies)],
    projectTypes: [...(overrides.projectTypes ?? emptyAnalysisScope.projectTypes)],
  }
}
/**
 * Counts the visible filter chips represented by an AnalysisScope. Multiple
 * technologies/project types count individually, while a time selection counts
 * as one period filter regardless of whether it is represented by year/month/
 * week or an explicit from/to range.
 */
export function countActiveAnalysisFilters(scope: AnalysisScope): number {
  const hasPeriod = Boolean(scope.from || scope.to || scope.year || scope.month || scope.week)

  return (
    scope.technologies.length +
    scope.projectTypes.length +
    (scope.ownership ? 1 : 0) +
    (scope.visibility ? 1 : 0) +
    (scope.search?.trim() ? 1 : 0) +
    (hasPeriod ? 1 : 0)
  )
}

