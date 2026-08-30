import { useEffect, useState } from 'react'

export type InventoryFilters = {
  page: number
  pageSize: number
  search: string
  ownership: string
  visibility: string
  activity: string
  category: string
  technology: string
}

export type InventoryItem = {
  id: string
  name: string
  description: string | null
  htmlUrl: string | null
  ownershipRelation: string
  visibility: string
  lastActivityAt: string | null
  categories: Array<{ key: string; name: string }>
  technologies: Array<{ key: string; name: string }>
}

export type InventoryResponse = {
  items: InventoryItem[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: InventoryResponse; error: null }
  | { status: 'error'; data: null; error: string }

export const initialInventoryFilters: InventoryFilters = {
  page: 0,
  pageSize: 25,
  search: '',
  ownership: '',
  visibility: '',
  activity: '',
  category: '',
  technology: '',
}

export function useProjectInventory(filters: InventoryFilters): State {
  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()
    const params = new URLSearchParams()

    params.set('page', String(filters.page))
    params.set('pageSize', String(filters.pageSize))

    for (const [key, value] of Object.entries(filters)) {
      if (key === 'page' || key === 'pageSize') continue
      if (value) params.set(key, String(value))
    }

    async function load() {
      setState({ status: 'loading', data: null, error: null })

      try {
        const response = await fetch(
          `/api/me/project-inventory?${params}`,
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
  }, [filters])

  return state
}
