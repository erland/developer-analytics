import { useState } from 'react'
import {
  countActiveAnalysisFilters,
  createAnalysisScope,
  type AnalysisScope,
} from '../analysis/AnalysisScope'

export type AnalysisFilterOption = {
  value: string
  label: string
  count?: number
}

export type AnalysisPeriodOption = AnalysisFilterOption & {
  scope: Partial<Pick<AnalysisScope, 'from' | 'to' | 'year' | 'month' | 'week'>>
}

type AnalysisFiltersProps = {
  scope: AnalysisScope
  onChange: (scope: AnalysisScope) => void
  technologies?: readonly AnalysisFilterOption[]
  projectTypes?: readonly AnalysisFilterOption[]
  periods?: readonly AnalysisPeriodOption[]
  showTechnology?: boolean
  showProjectType?: boolean
  showOwnership?: boolean
  showPeriod?: boolean
}

const PERIOD_KEYS = ['from', 'to', 'year', 'month', 'week'] as const

export function AnalysisFilters({
  scope,
  onChange,
  technologies = [],
  projectTypes = [],
  periods = [],
  showTechnology = false,
  showProjectType = false,
  showOwnership = false,
  showPeriod = false,
}: AnalysisFiltersProps) {
  const [mobileExpanded, setMobileExpanded] = useState(false)
  const activeFilterCount = countActiveAnalysisFilters(scope)
  const hasActiveFilters = scopeHasActiveFilters(scope)
  const hasVisibleControls = showTechnology || showProjectType || showOwnership || showPeriod

  function update(patch: Partial<AnalysisScope>) {
    onChange(createAnalysisScope({ ...scope, ...patch }))
  }

  function setTechnology(value: string) {
    update({ technologies: value ? [value] : [] })
  }

  function setProjectType(value: string) {
    update({ projectTypes: value ? [value] : [] })
  }

  function setPeriod(value: string) {
    const selected = periods.find((period) => period.value === value)
    const next: Partial<AnalysisScope> = {
      from: undefined,
      to: undefined,
      year: undefined,
      month: undefined,
      week: undefined,
      ...(selected?.scope ?? {}),
    }
    update(next)
  }

  return (
    <section className="analysis-filters" aria-label="Analysis filters">
      {hasVisibleControls ? (
        <div className="analysis-filter-mobile-summary">
          <strong>Filters{activeFilterCount > 0 ? ` · ${activeFilterCount} active` : ''}</strong>
          <button
            className="text-button analysis-edit-filters"
            type="button"
            aria-expanded={mobileExpanded}
            aria-controls="analysis-filter-controls"
            onClick={() => setMobileExpanded((expanded) => !expanded)}
          >
            {mobileExpanded ? 'Done' : 'Edit filters'}
          </button>
        </div>
      ) : null}

      <div
        id="analysis-filter-controls"
        className={`analysis-filter-controls${mobileExpanded ? ' is-expanded' : ''}`}
      >
        <div className="analysis-filter-fields">
          {showTechnology ? (
            <FilterSelect
              label="Technology"
              value={scope.technologies[0] ?? ''}
              options={technologies}
              onChange={setTechnology}
            />
          ) : null}

          {showProjectType ? (
            <FilterSelect
              label="Project type"
              value={scope.projectTypes[0] ?? ''}
              options={projectTypes}
              onChange={setProjectType}
            />
          ) : null}

          {showOwnership ? (
            <FilterSelect
              label="Ownership"
              value={scope.ownership ?? ''}
              options={[
                { value: 'own', label: 'Own' },
                { value: 'external', label: 'External' },
              ]}
              onChange={(value) => update({
                ownership: value === 'own' || value === 'external' ? value : undefined,
              })}
            />
          ) : null}

          {showPeriod ? (
            <FilterSelect
              label="Period"
              value={periodValue(scope, periods)}
              options={periods}
              onChange={setPeriod}
            />
          ) : null}
        </div>

        <button
          className="text-button analysis-clear-filters"
          type="button"
          disabled={!hasActiveFilters}
          onClick={() => onChange(createAnalysisScope())}
        >
          Clear all
        </button>
      </div>

      <ActiveFilterChips
        scope={scope}
        technologies={technologies}
        projectTypes={projectTypes}
        periods={periods}
        onRemove={(patch) => update(patch)}
      />
    </section>
  )
}


function ActiveFilterChips({
  scope,
  technologies,
  projectTypes,
  periods,
  onRemove,
}: {
  scope: AnalysisScope
  technologies: readonly AnalysisFilterOption[]
  projectTypes: readonly AnalysisFilterOption[]
  periods: readonly AnalysisPeriodOption[]
  onRemove: (patch: Partial<AnalysisScope>) => void
}) {
  const chips: Array<{ key: string; label: string; remove: () => void }> = []

  for (const technology of scope.technologies) {
    chips.push({
      key: `technology:${technology}`,
      label: `Technology: ${optionLabel(technology, technologies)}`,
      remove: () => onRemove({
        technologies: scope.technologies.filter((value) => value !== technology),
      }),
    })
  }

  for (const projectType of scope.projectTypes) {
    chips.push({
      key: `projectType:${projectType}`,
      label: `Project type: ${optionLabel(projectType, projectTypes)}`,
      remove: () => onRemove({
        projectTypes: scope.projectTypes.filter((value) => value !== projectType),
      }),
    })
  }

  if (scope.ownership) {
    chips.push({
      key: 'ownership',
      label: `Ownership: ${scope.ownership === 'own' ? 'Own' : 'External'}`,
      remove: () => onRemove({ ownership: undefined }),
    })
  }

  if (scope.visibility) {
    chips.push({
      key: 'visibility',
      label: `Visibility: ${scope.visibility === 'public' ? 'Public' : 'Private'}`,
      remove: () => onRemove({ visibility: undefined }),
    })
  }

  const activePeriod = periodLabel(scope, periods)
  if (activePeriod) {
    chips.push({
      key: 'period',
      label: `Period: ${activePeriod}`,
      remove: () => onRemove({
        from: undefined,
        to: undefined,
        year: undefined,
        month: undefined,
        week: undefined,
      }),
    })
  }

  if (scope.search) {
    chips.push({
      key: 'search',
      label: `Search: ${scope.search}`,
      remove: () => onRemove({ search: undefined }),
    })
  }

  if (chips.length === 0) return null

  return (
    <div className="analysis-active-filters" role="group" aria-label="Active filters">
      {chips.map((chip) => (
        <button
          className="analysis-filter-chip"
          type="button"
          key={chip.key}
          aria-label={`Remove ${chip.label}`}
          onClick={chip.remove}
        >
          <span>{chip.label}</span>
          <span aria-hidden="true">×</span>
        </button>
      ))}
    </div>
  )
}

function optionLabel(value: string, options: readonly AnalysisFilterOption[]): string {
  return options.find((option) => option.value === value)?.label ?? value
}

function periodLabel(
  scope: AnalysisScope,
  periods: readonly AnalysisPeriodOption[],
): string | undefined {
  const configured = periods.find((period) => PERIOD_KEYS.every((key) => {
    return scope[key] === period.scope[key]
  }))
  if (configured && PERIOD_KEYS.some((key) => scope[key] !== undefined)) {
    return configured.label
  }

  if (scope.week) return scope.week
  if (scope.month) return scope.month
  if (scope.year !== undefined) return String(scope.year)
  if (scope.from && scope.to) return `${scope.from} – ${scope.to}`
  if (scope.from) return `from ${scope.from}`
  if (scope.to) return `through ${scope.to}`
  return undefined
}

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string
  value: string
  options: readonly AnalysisFilterOption[]
  onChange: (value: string) => void
}) {
  return (
    <label className="analysis-filter-field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        <option value="">All</option>
        {options.map((option) => (
          <option value={option.value} key={option.value}>
            {option.label}{option.count === undefined ? '' : ` (${option.count})`}
          </option>
        ))}
      </select>
    </label>
  )
}

function periodValue(
  scope: AnalysisScope,
  periods: readonly AnalysisPeriodOption[],
): string {
  const matching = periods.find((period) => PERIOD_KEYS.every((key) => {
    return scope[key] === period.scope[key]
  }))

  return matching?.value ?? ''
}

function scopeHasActiveFilters(scope: AnalysisScope): boolean {
  return scope.technologies.length > 0
    || scope.projectTypes.length > 0
    || Boolean(scope.ownership)
    || Boolean(scope.visibility)
    || Boolean(scope.from)
    || Boolean(scope.to)
    || scope.year !== undefined
    || Boolean(scope.month)
    || Boolean(scope.week)
    || Boolean(scope.search)
}
