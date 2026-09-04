import type { AnalysisPeriodOption } from '../components/AnalysisFilters'

/**
 * Common rolling-period presets for Explore views that use AnalysisScope.
 * "All" is supplied by AnalysisFilters itself, so only constrained periods are
 * returned here.
 */
export function rollingAnalysisPeriodOptions(reference = new Date()): AnalysisPeriodOption[] {
  return [
    { value: '12m', label: '12 months', scope: { from: monthsBefore(reference, 12) } },
    { value: '24m', label: '24 months', scope: { from: monthsBefore(reference, 24) } },
    { value: '5y', label: '5 years', scope: { from: monthsBefore(reference, 60) } },
  ]
}

function monthsBefore(reference: Date, months: number): string {
  const value = new Date(Date.UTC(
    reference.getUTCFullYear(),
    reference.getUTCMonth(),
    reference.getUTCDate(),
  ))
  value.setUTCMonth(value.getUTCMonth() - months)
  return value.toISOString().slice(0, 10)
}
