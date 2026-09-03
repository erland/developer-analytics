import {
  createAnalysisScope,
  type AnalysisOwnership,
  type AnalysisScope,
  type AnalysisVisibility,
} from './AnalysisScope'

const PARAMS = {
  technology: 'technology',
  projectType: 'projectType',
  ownership: 'ownership',
  visibility: 'visibility',
  from: 'from',
  to: 'to',
  year: 'year',
  month: 'month',
  week: 'week',
  search: 'search',
} as const

function trimmed(value: string | null): string | undefined {
  const result = value?.trim()
  return result ? result : undefined
}

function parseOwnership(value: string | null): AnalysisOwnership | undefined {
  return value === 'own' || value === 'external' ? value : undefined
}

function parseVisibility(value: string | null): AnalysisVisibility | undefined {
  return value === 'public' || value === 'private' ? value : undefined
}

function parseYear(value: string | null): number | undefined {
  if (!value || !/^\d{4}$/.test(value)) {
    return undefined
  }

  const year = Number(value)
  return Number.isInteger(year) && year >= 1970 && year <= 9999 ? year : undefined
}

function uniqueNonEmpty(values: readonly string[]): string[] {
  return [...new Set(values.map((value) => value.trim()).filter(Boolean))]
}

/**
 * Serialises the data-selection portion of an Explore analysis into stable URL
 * search parameters. Empty values are omitted and collection values are emitted
 * as repeated query parameters to preserve multi-selection without custom
 * escaping rules.
 */
export function analysisScopeToSearchParams(scope: AnalysisScope): URLSearchParams {
  const params = new URLSearchParams()

  uniqueNonEmpty(scope.technologies).forEach((technology) => {
    params.append(PARAMS.technology, technology)
  })
  uniqueNonEmpty(scope.projectTypes).forEach((projectType) => {
    params.append(PARAMS.projectType, projectType)
  })

  if (scope.ownership) params.set(PARAMS.ownership, scope.ownership)
  if (scope.visibility) params.set(PARAMS.visibility, scope.visibility)
  if (trimmed(scope.from ?? null)) params.set(PARAMS.from, scope.from!.trim())
  if (trimmed(scope.to ?? null)) params.set(PARAMS.to, scope.to!.trim())
  if (scope.year !== undefined) params.set(PARAMS.year, String(scope.year))
  if (trimmed(scope.month ?? null)) params.set(PARAMS.month, scope.month!.trim())
  if (trimmed(scope.week ?? null)) params.set(PARAMS.week, scope.week!.trim())
  if (trimmed(scope.search ?? null)) params.set(PARAMS.search, scope.search!.trim())

  return params
}

/**
 * Parses known AnalysisScope query parameters while safely ignoring unknown or
 * invalid values. The returned scope is always complete enough for Explore
 * views to consume directly (arrays are present even for an unfiltered scope).
 */
export function analysisScopeFromSearchParams(
  source: URLSearchParams | string,
): AnalysisScope {
  const params = typeof source === 'string'
    ? new URLSearchParams(source.startsWith('?') ? source.slice(1) : source)
    : source

  return createAnalysisScope({
    technologies: uniqueNonEmpty(params.getAll(PARAMS.technology)),
    projectTypes: uniqueNonEmpty(params.getAll(PARAMS.projectType)),
    ownership: parseOwnership(params.get(PARAMS.ownership)),
    visibility: parseVisibility(params.get(PARAMS.visibility)),
    from: trimmed(params.get(PARAMS.from)),
    to: trimmed(params.get(PARAMS.to)),
    year: parseYear(params.get(PARAMS.year)),
    month: trimmed(params.get(PARAMS.month)),
    week: trimmed(params.get(PARAMS.week)),
    search: trimmed(params.get(PARAMS.search)),
  })
}
