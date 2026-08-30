import { useCallback, useEffect, useState } from 'react'

export type PrivateRepository = {
  id: string
  name: string
  fullName: string | null
  htmlUrl: string | null
  includedInAnalysis: boolean
  syncStatus: string
}

type State = {
  loading: boolean
  refreshing: boolean
  repositories: PrivateRepository[]
  error: string | null
}

export function usePrivateRepositories(enabled: boolean) {
  const [state, setState] = useState<State>({
    loading: enabled,
    refreshing: false,
    repositories: [],
    error: null,
  })

  const load = useCallback(async () => {
    if (!enabled) return
    setState((current) => ({ ...current, loading: true, error: null }))
    try {
      const response = await fetch('/api/me/private-repositories', {
        credentials: 'include', headers: { Accept: 'application/json' },
      })
      if (!response.ok) throw new Error(`Private repository request failed with HTTP ${response.status}`)
      const repositories = (await response.json()) as PrivateRepository[]
      setState((current) => ({ ...current, loading: false, repositories, error: null }))
    } catch (error) {
      setState((current) => ({ ...current, loading: false, error: error instanceof Error ? error.message : 'Unable to load private repositories' }))
    }
  }, [enabled])

  useEffect(() => { void load() }, [load])

  async function setIncluded(repositoryId: string, included: boolean) {
    const response = await fetch(`/api/me/private-repositories/${repositoryId}/selection`, {
      method: 'PUT', credentials: 'include',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ included }),
    })
    if (!response.ok) throw new Error(`Selection update failed with HTTP ${response.status}`)
    const updated = (await response.json()) as PrivateRepository
    setState((current) => ({ ...current, repositories: current.repositories.map((item) => item.id === updated.id ? updated : item) }))
  }

  async function remove(repositoryId: string) {
    const response = await fetch(`/api/me/private-repositories/${repositoryId}`, {
      method: 'DELETE', credentials: 'include', headers: { Accept: 'application/json' },
    })
    if (!response.ok) throw new Error(`Remove request failed with HTTP ${response.status}`)
    const updated = (await response.json()) as PrivateRepository
    setState((current) => ({ ...current, repositories: current.repositories.map((item) => item.id === updated.id ? updated : item) }))
  }

  async function refresh() {
    setState((current) => ({ ...current, refreshing: true, error: null }))
    try {
      const response = await fetch('/api/me/private-repositories/refresh', {
        method: 'POST', credentials: 'include', headers: { Accept: 'application/json' },
      })
      if (!response.ok) throw new Error(`Permission refresh failed with HTTP ${response.status}`)
      await load()
    } catch (error) {
      setState((current) => ({ ...current, error: error instanceof Error ? error.message : 'Unable to refresh permissions' }))
    } finally {
      setState((current) => ({ ...current, refreshing: false }))
    }
  }

  return { ...state, setIncluded, remove, refresh }
}
