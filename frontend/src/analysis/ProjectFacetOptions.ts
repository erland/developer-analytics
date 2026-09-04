export type ProjectFacetValue = {
  key: string
  name: string
  count: number
}

export type AnalysisFilterOption = {
  value: string
  label: string
  count: number
}

/**
 * Converts server-side project facets to AnalysisFilters options without
 * deriving anything from the currently paginated project rows.
 */
export function projectFacetOptions(facets: readonly ProjectFacetValue[]): AnalysisFilterOption[] {
  return facets.map((facet) => ({
    value: facet.key,
    label: facet.name,
    count: facet.count,
  }))
}
