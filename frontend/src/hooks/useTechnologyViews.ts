import { useEffect, useState } from 'react'

export type TechnologyView = {
  technologyKey: string
  technologyName: string
  technologyCategory: string
  evidenceLevel: string
  evidenceScore: number
  projectCount: number
  evidenceCount: number
  independentEvidenceTypes: number
  firstObservedAt: string | null
  lastObservedAt: string | null
  recentProjectCount: number
  privacyProvenance: 'PUBLIC_ONLY' | 'INCLUDES_PRIVATE' | 'PRIVATE_AGGREGATE'
  rationale: Record<string, unknown>
  timeline: Array<{
    month: string
    commits: number
    changedLines: number
    lineStatisticsCommitCount: number
    projectCount: number
  }>
  representativeProjects: Array<{
    repositoryId: string
    repositoryName: string
    htmlUrl: string | null
    visibility: string
    ownershipRelation: string
    lastActivityAt: string | null
    evidenceCount: number
  }>
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: TechnologyView[]; error: null }
  | { status: 'error'; data: null; error: string }

export function useTechnologyViews(): State {
  const [state, setState] = useState<State>({ status: 'loading', data: null, error: null })

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      try {
        const response = await fetch('/api/me/technologies', {
          credentials: 'include', headers: { Accept: 'application/json' }, signal: controller.signal,
        })
        if (!response.ok) throw new Error(`Technology request failed with HTTP ${response.status}`)
        const data = (await response.json()) as TechnologyView[]
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({ status: 'error', data: null, error: error instanceof Error ? error.message : 'Unable to load technologies' })
      }
    }
    void load()
    return () => controller.abort()
  }, [])

  return state
}
