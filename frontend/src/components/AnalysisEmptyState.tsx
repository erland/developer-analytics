import { countActiveAnalysisFilters, createAnalysisScope, type AnalysisScope } from '../analysis/AnalysisScope'

type AnalysisEmptyStateProps = {
  title: string
  description: string
  scope: AnalysisScope
  onScopeChange?: (scope: AnalysisScope) => void
  className?: string
  extraAction?: {
    label: string
    onClick: () => void
  }
}

export function AnalysisEmptyState({
  title,
  description,
  scope,
  onScopeChange,
  className = '',
  extraAction,
}: AnalysisEmptyStateProps) {
  const filters = describeScope(scope)
  const hasPeriod = Boolean(scope.from || scope.to || scope.year !== undefined || scope.month || scope.week)
  const hasFilters = countActiveAnalysisFilters(scope) > 0

  return (
    <section className={`analysis-empty-state ${className}`.trim()}>
      <h3>{title}</h3>
      <p>{description}</p>

      {filters.length ? (
        <div className="analysis-empty-selection" aria-label="Current selection">
          <span className="card-kicker">Current selection</span>
          <div className="analysis-empty-filter-list">
            {filters.map((filter) => <span key={filter}>{filter}</span>)}
          </div>
        </div>
      ) : null}

      {(extraAction || (onScopeChange && hasFilters)) ? (
        <div className="analysis-empty-actions">
          {extraAction ? (
            <button type="button" className="secondary-action" onClick={extraAction.onClick}>{extraAction.label}</button>
          ) : null}
          {onScopeChange && hasPeriod ? (
            <button
              type="button"
              className="secondary-action"
              onClick={() => onScopeChange(createAnalysisScope({
                ...scope,
                from: undefined,
                to: undefined,
                year: undefined,
                month: undefined,
                week: undefined,
              }))}
            >
              Clear period
            </button>
          ) : null}
          {onScopeChange && hasFilters ? (
            <button type="button" className="text-button" onClick={() => onScopeChange(createAnalysisScope())}>
              Clear all filters
            </button>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}

function describeScope(scope: AnalysisScope): string[] {
  const result: string[] = []
  result.push(...scope.technologies.map((value) => `Technology: ${humanize(value)}`))
  result.push(...scope.projectTypes.map((value) => `Project type: ${humanize(value)}`))
  if (scope.ownership) result.push(`Ownership: ${scope.ownership === 'own' ? 'Own' : 'External'}`)
  if (scope.visibility) result.push(`Visibility: ${scope.visibility === 'public' ? 'Public' : 'Private'}`)
  if (scope.search?.trim()) result.push(`Search: ${scope.search.trim()}`)

  const period = describePeriod(scope)
  if (period) result.push(`Period: ${period}`)
  return result
}

function describePeriod(scope: AnalysisScope): string | undefined {
  if (scope.week) return scope.week
  if (scope.month) return scope.month
  if (scope.year !== undefined) return String(scope.year)
  if (scope.from && scope.to) return `${scope.from} – ${scope.to}`
  if (scope.from) return `from ${scope.from}`
  if (scope.to) return `through ${scope.to}`
  return undefined
}

function humanize(value: string): string {
  return value
    .replace(/[-_]+/g, ' ')
    .replace(/\b\w/g, (character) => character.toUpperCase())
}
