import { useEffect, useState } from 'react'

export type ProjectTypeView = {
  categoryKey: string
  categoryName: string
  projectCount: number
  activityCount: number
  timeline: Array<{
    month: string
    activityCount: number
    activeProjectCount: number
  }>
  representativeProjects: Array<{
    repositoryId: string
    repositoryName: string
    htmlUrl: string | null
    visibility: string
    ownershipRelation: string
    lastActivityAt: string | null
    contributionCount: number
  }>
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: ProjectTypeView[]; error: null }
  | { status: 'error'; data: null; error: string }

export function useProjectTypes(): State {
  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      try {
        const response = await fetch('/api/me/project-types', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`Project type request failed with HTTP ${response.status}`)
        }

        const data = (await response.json()) as ProjectTypeView[]
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          data: null,
          error: error instanceof Error ? error.message : 'Unable to load project types',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [])

  return state
}
