import { useEffect, useState } from 'react'

export type ProjectDetail = {
  metadata: {
    id: string
    provider: string
    name: string
    fullName: string | null
    description: string | null
    htmlUrl: string | null
    visibility: string
    ownershipRelation: string
    ownerLogin: string | null
    fork: boolean
    archived: boolean
    topics: string[]
    lastActivityAt: string | null
    excludedFromAiProfile: boolean
  }
  activity: {
    commits: number
    pullRequests: number
    reviews: number
    issues: number
    additions: number
    deletions: number
    firstActivityAt: string | null
    lastActivityAt: string | null
    excludedFromAiProfile: boolean
    timeline: Array<{
      month: string
      commits: number
      additions: number
      deletions: number
      changedLines: number
      lineStatisticsCommitCount: number
    }>
  }
  technologies: Array<{ technologyKey: string; technologyName: string; strength: string }>
  categories: Array<{
    categoryKey: string
    categoryName: string
    source: string
    confidence: string
    rationale: Record<string, unknown>
    privacyProvenance: string
    rejectedByUser: boolean
  }>
  assessment: null | {
    significanceLevel: string
    significanceScore: number
    significanceRationale: Record<string, unknown>
    involvementLevel: string
    involvementScore: number
    involvementRationale: Record<string, unknown>
    calculatedAt: string
    privacyProvenance: string
  }
  synchronisation: { status: string; lastSeenAt: string | null; error: string | null }
  contributors: { total: number | null; humans: number | null; bots: number | null; userCommits: number | null }
}

type State =
  | { status: 'idle'; data: null; error: null }
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: ProjectDetail; error: null }
  | { status: 'error'; data: null; error: string }

export function useProjectDetail(repositoryId: string | null): State {
  const [state, setState] = useState<State>({ status: 'idle', data: null, error: null })

  useEffect(() => {
    if (!repositoryId) {
      setState({ status: 'idle', data: null, error: null })
      return
    }

    const controller = new AbortController()
    async function load() {
      setState({ status: 'loading', data: null, error: null })
      try {
        const response = await fetch(`/api/me/projects/${repositoryId}`, {
          credentials: 'include', headers: { Accept: 'application/json' }, signal: controller.signal,
        })
        if (!response.ok) throw new Error(`Project detail request failed with HTTP ${response.status}`)
        const raw = (await response.json()) as ProjectDetail
        const data: ProjectDetail = {
          ...raw,
          activity: {
            ...raw.activity,
            timeline: (raw.activity.timeline ?? []).map(point => ({
              ...point,
              additions: Number(point.additions ?? 0),
              deletions: Number(point.deletions ?? 0),
              changedLines: Number(point.changedLines ?? (Number(point.additions ?? 0) + Number(point.deletions ?? 0))),
            })),
          },
          contributors: raw.contributors ?? { total: null, humans: null, bots: null, userCommits: null },
        }
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({ status: 'error', data: null, error: error instanceof Error ? error.message : 'Unable to load project' })
      }
    }
    void load()
    return () => controller.abort()
  }, [repositoryId])

  return state
}
