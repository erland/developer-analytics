import { useEffect, useMemo, useState } from 'react'

type Repository = {
  id?: string
  visibility?: 'PUBLIC' | 'PRIVATE' | string
  ownershipRelation?: string
  syncStatus?: string
  lastActivityAt?: string | null
  name?: string
}

type TechnologyAssessment = {
  technologyKey: string
  technologyName: string
  strength: string
  score: number
}

type ProjectCategory = {
  categoryKey: string
  categoryName: string
  confidence: string
}

type SignificantProject = {
  repositoryId: string
  repositoryName: string
  matchReason: string
  significanceScore: number
  involvementScore: number
}

type Timeline = {
  technologyKey: string
  firstObservedAt?: string | null
  lastObservedAt?: string | null
}

type Contribution = {
  occurredAt?: string | null
  type?: string
}

export type OverviewDashboardData = {
  repositoriesAnalysed: number
  ownRepositories: number
  externalRepositories: number
  publicRepositories: number
  privateRepositories: number
  commits: number
  firstActivityAt: string | null
  lastActivityAt: string | null
  activeProjects: number
  keyTechnologies: TechnologyAssessment[]
  projectCategories: ProjectCategory[]
  significantProjects: SignificantProject[]
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: OverviewDashboardData; error: null }
  | { status: 'error'; data: null; error: string }

async function getJson<T>(url: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    signal,
  })
  if (!response.ok) {
    throw new Error(`${url} failed with HTTP ${response.status}`)
  }
  return (await response.json()) as T
}

export function useOverviewDashboard(enabled: boolean): State {
  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })

  useEffect(() => {
    if (!enabled) return

    const controller = new AbortController()

    async function load() {
      try {
        const [
          repositories,
          technologies,
          significantProjects,
          timelines,
        ] = await Promise.all([
          getJson<Repository[]>('/api/me/repositories', controller.signal),
          getJson<TechnologyAssessment[]>('/api/me/technology-assessments', controller.signal),
          getJson<SignificantProject[]>('/api/me/significant-external-projects', controller.signal),
          getJson<Timeline[]>('/api/me/technology-timeline', controller.signal),
        ])

        const categoryResults = await Promise.all(
          repositories.slice(0, 50).map(async (repository) => {
            if (!repository.id) return []
            try {
              return await getJson<ProjectCategory[]>(
                `/api/me/repositories/${repository.id}/project-categories`,
                controller.signal,
              )
            } catch {
              return []
            }
          }),
        )

        let commits = 0
        const contributionDates: Date[] = []

        await Promise.all(
          repositories.slice(0, 50).map(async (repository) => {
            if (!repository.id) return
            try {
              const contributions = await getJson<Contribution[]>(
                `/api/me/repositories/${repository.id}/contributions`,
                controller.signal,
              )
              for (const contribution of contributions) {
                if (contribution.type === 'COMMIT') commits += 1
                if (contribution.occurredAt) {
                  const date = new Date(contribution.occurredAt)
                  if (!Number.isNaN(date.getTime())) contributionDates.push(date)
                }
              }
            } catch {
              // Overview should still render when detailed contribution endpoints
              // are not available for every repository.
            }
          }),
        )

        const repositoryDates = repositories
          .map((repository) => repository.lastActivityAt)
          .filter((value): value is string => Boolean(value))
          .map((value) => new Date(value))
          .filter((value) => !Number.isNaN(value.getTime()))

        const timelineDates = timelines
          .flatMap((timeline) => [timeline.firstObservedAt, timeline.lastObservedAt])
          .filter((value): value is string => Boolean(value))
          .map((value) => new Date(value))
          .filter((value) => !Number.isNaN(value.getTime()))

        const allDates = [...contributionDates, ...repositoryDates, ...timelineDates]
          .sort((a, b) => a.getTime() - b.getTime())

        const uniqueCategories = new Map<string, ProjectCategory>()
        for (const category of categoryResults.flat()) {
          if (!uniqueCategories.has(category.categoryKey)) {
            uniqueCategories.set(category.categoryKey, category)
          }
        }

        const data: OverviewDashboardData = {
          repositoriesAnalysed: repositories.length,
          ownRepositories: repositories.filter(
            (repository) => repository.ownershipRelation === 'OWNED_BY_USER',
          ).length,
          externalRepositories: repositories.filter(
            (repository) => repository.ownershipRelation !== 'OWNED_BY_USER',
          ).length,
          publicRepositories: repositories.filter(
            (repository) => repository.visibility === 'PUBLIC',
          ).length,
          privateRepositories: repositories.filter(
            (repository) => repository.visibility === 'PRIVATE',
          ).length,
          commits,
          firstActivityAt: allDates[0]?.toISOString() ?? null,
          lastActivityAt: allDates.at(-1)?.toISOString() ?? null,
          activeProjects: repositories.filter((repository) => {
            if (!repository.lastActivityAt) return false
            const date = new Date(repository.lastActivityAt)
            if (Number.isNaN(date.getTime())) return false
            return Date.now() - date.getTime() <= 365 * 24 * 60 * 60 * 1000
          }).length,
          keyTechnologies: [...technologies]
            .sort((a, b) => b.score - a.score)
            .slice(0, 6),
          projectCategories: [...uniqueCategories.values()].slice(0, 6),
          significantProjects: [...significantProjects]
            .sort(
              (a, b) =>
                Math.max(b.significanceScore, b.involvementScore) -
                Math.max(a.significanceScore, a.involvementScore),
            )
            .slice(0, 5),
        }

        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          data: null,
          error: error instanceof Error ? error.message : 'Unable to load overview',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [enabled])

  return useMemo(() => state, [state])
}
