export type ActivityPeriod = {
  commits: number
  changedLines: number
  lineStatisticsCommitCount: number
  projectCount?: number
  activeProjectCount?: number
}

/**
 * Returns true when a timeline period contains observable user activity or at
 * least one matching project. Keeping this predicate central prevents Explore
 * views from drifting in how they decide whether a period should be visible.
 */
export function hasActivityInPeriod(period: ActivityPeriod): boolean {
  return period.commits > 0
    || period.changedLines > 0
    || period.lineStatisticsCommitCount > 0
    || (period.projectCount ?? 0) > 0
    || (period.activeProjectCount ?? 0) > 0
}

export function nonEmptyActivityPeriods<T extends ActivityPeriod>(periods: readonly T[]): T[] {
  return periods.filter(hasActivityInPeriod)
}
