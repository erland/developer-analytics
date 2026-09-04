import { useEffect, useState } from 'react'
import { type AnalysisScope } from '../analysis/AnalysisScope'
import { analysisScopeToSearchParams } from '../analysis/AnalysisScopeUrl'

export type ProjectInventoryQuery = {
  page: number
  pageSize: number
  activity: string
  scope: AnalysisScope
}

export type InventoryItem = {
  id: string
  name: string
  description: string | null
  htmlUrl: string | null
  ownershipRelation: string
  visibility: string
  lastActivityAt: string | null
  codeSizeBytes: number | null
  repositorySizeBytes: number | null
  categories: Array<{ key: string; name: string }>
  technologies: Array<{ key: string; name: string }>
}

export type InventoryFacetValue = {
  key: string
  name: string
  count: number
}

export type InventoryResponse = {
  items: InventoryItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
  facets: {
    technologies: InventoryFacetValue[]
    projectTypes: InventoryFacetValue[]
    ownership: InventoryFacetValue[]
  }
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: InventoryResponse; error: null }
  | { status: 'error'; data: null; error: string }

export const initialProjectInventoryQuery: ProjectInventoryQuery = {
  page: 0,
  pageSize: 25,
  activity: '',
  scope: {
    technologies: [],
    projectTypes: [],
  },
}

export function useProjectInventory(query: ProjectInventoryQuery): State {
  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })
  const params = analysisScopeToSearchParams(query.scope)
  params.set('page', String(query.page))
  params.set('pageSize', String(query.pageSize))
  if (query.activity) params.set('activity', query.activity)
  const requestSearch = params.toString()

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      setState({ status: 'loading', data: null, error: null })

      try {
        const response = await fetch(
          `/api/me/project-inventory?${requestSearch}`,
          {
            credentials: 'include',
            headers: { Accept: 'application/json' },
            signal: controller.signal,
          },
        )

        if (!response.ok) {
          throw new Error(`Project inventory request failed with HTTP ${response.status}`)
        }

        const data = (await response.json()) as InventoryResponse
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          data: null,
          error: error instanceof Error ? error.message : 'Unable to load projects',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [requestSearch])

  return state
}
