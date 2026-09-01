import { useEffect, useState } from 'react'

export type ActivityPeriod = '12m' | '24m' | '5y' | 'all'
export type ActivityMetric = 'lines' | 'commits'

export type ActivityPeriodPoint = {
  commits: number
  additions: number
  deletions: number
  changedLines: number
  lineStatisticsCommitCount: number
  activeProjects: number
  projects: string[]
}

export type ProjectPeriodActivity = {
  period: string
  commits: number
  additions: number
  deletions: number
  changedLines: number
  lineStatisticsCommitCount: number
}

export type ProjectLifecycle = {
  repositoryId: string
  repositoryName: string
  firstActivityAt: string
  lastActivityAt: string
  commits: number
  projectType: string
  technology: string
  monthlyActivity: ProjectPeriodActivity[]
  weeklyActivity: ProjectPeriodActivity[]
}

export type ActivityData = {
  commitCount: number
  activeProjects: number
  averageCommitSize: number
  medianCommitSize: number
  additions: number
  deletions: number
  firstActivityAt: string | null
  lastActivityAt: string | null
  commitSizeStatisticsAvailable: boolean
  lineStatisticsCommitCount: number
  commitsPerYear: Array<ActivityPeriodPoint & { year: number }>
  commitsPerMonth: Array<ActivityPeriodPoint & { month: string }>
  commitsPerWeek: Array<ActivityPeriodPoint & { week: string }>
  projectsOverTime: ProjectLifecycle[]
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: ActivityData; error: null }
  | { status: 'error'; data: null; error: string }

export function periodRange(period: ActivityPeriod) {
  if (period === 'all') return { from: null, to: null }
  const to = new Date()
  const from = new Date(to)
  if (period === '12m') from.setMonth(from.getMonth() - 12)
  if (period === '24m') from.setMonth(from.getMonth() - 24)
  if (period === '5y') from.setFullYear(from.getFullYear() - 5)
  return { from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10) }
}

const normalizePeriod = <T extends Record<string, unknown>>(value: T) => ({
  ...value,
  commits: Number(value.commits ?? 0),
  additions: Number(value.additions ?? 0),
  deletions: Number(value.deletions ?? 0),
  changedLines: Number(value.changedLines ?? (Number(value.additions ?? 0) + Number(value.deletions ?? 0))),
  lineStatisticsCommitCount: Number(value.lineStatisticsCommitCount ?? 0),
  activeProjects: Number(value.activeProjects ?? 0),
  projects: Array.isArray(value.projects) ? value.projects as string[] : [],
})

export function useActivityView(period: ActivityPeriod): State {
  const [state, setState] = useState<State>({ status: 'loading', data: null, error: null })

  useEffect(() => {
    const controller = new AbortController()
    const range = periodRange(period)

    async function load() {
      setState({ status: 'loading', data: null, error: null })
      const query = new URLSearchParams()
      if (range.from) query.set('from', range.from)
      if (range.to) query.set('to', range.to)

      try {
        const response = await fetch(`/api/me/activity${query.size ? `?${query}` : ''}`, {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })
        if (!response.ok) throw new Error(`Activity request failed with HTTP ${response.status}`)

        const raw = await response.json() as Partial<ActivityData>
        const statisticsAvailable = raw.commitSizeStatisticsAvailable
          ?? (raw.averageCommitSize !== undefined || raw.additions !== undefined || raw.deletions !== undefined)

        const data: ActivityData = {
          commitCount: raw.commitCount ?? 0,
          activeProjects: raw.activeProjects ?? 0,
          averageCommitSize: raw.averageCommitSize ?? 0,
          medianCommitSize: raw.medianCommitSize ?? 0,
          additions: raw.additions ?? 0,
          deletions: raw.deletions ?? 0,
          firstActivityAt: raw.firstActivityAt ?? null,
          lastActivityAt: raw.lastActivityAt ?? null,
          commitSizeStatisticsAvailable: statisticsAvailable,
          lineStatisticsCommitCount: raw.lineStatisticsCommitCount ?? 0,
          commitsPerYear: (raw.commitsPerYear ?? []).map(value => normalizePeriod(value) as ActivityData['commitsPerYear'][number]),
          commitsPerMonth: (raw.commitsPerMonth ?? []).map(value => normalizePeriod(value) as ActivityData['commitsPerMonth'][number]),
          commitsPerWeek: (raw.commitsPerWeek ?? []).map(value => normalizePeriod(value) as ActivityData['commitsPerWeek'][number]),
          projectsOverTime: (raw.projectsOverTime ?? []).map(project => ({
            repositoryId: project.repositoryId,
            repositoryName: project.repositoryName,
            firstActivityAt: project.firstActivityAt,
            lastActivityAt: project.lastActivityAt,
            commits: project.commits ?? 0,
            projectType: project.projectType ?? 'Unclassified',
            technology: project.technology ?? 'Unclassified',
            monthlyActivity: (project.monthlyActivity ?? []).map(value => ({
              period: value.period ?? (value as { month?: string }).month ?? '',
              commits: value.commits ?? 0,
              additions: value.additions ?? 0,
              deletions: value.deletions ?? 0,
              changedLines: value.changedLines ?? ((value.additions ?? 0) + (value.deletions ?? 0)),
              lineStatisticsCommitCount: value.lineStatisticsCommitCount ?? 0,
            })),
            weeklyActivity: (project.weeklyActivity ?? []).map(value => ({
              period: value.period ?? '',
              commits: value.commits ?? 0,
              additions: value.additions ?? 0,
              deletions: value.deletions ?? 0,
              changedLines: value.changedLines ?? ((value.additions ?? 0) + (value.deletions ?? 0)),
              lineStatisticsCommitCount: value.lineStatisticsCommitCount ?? 0,
            })),
          })),
        }
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (!controller.signal.aborted) {
          setState({
            status: 'error',
            data: null,
            error: error instanceof Error ? error.message : 'Unable to load activity',
          })
        }
      }
    }

    void load()
    return () => controller.abort()
  }, [period])

  return state
}
