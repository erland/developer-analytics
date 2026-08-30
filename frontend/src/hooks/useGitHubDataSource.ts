import { useEffect, useState } from 'react'

type DataSourceState =
  | {
      status: 'loading'
      privateRepositoriesAuthorised: false
      privateRepositoriesAuthorisedAt: null
      error: null
    }
  | {
      status: 'ready'
      privateRepositoriesAuthorised: boolean
      privateRepositoriesAuthorisedAt: string | null
      error: null
    }
  | {
      status: 'error'
      privateRepositoriesAuthorised: false
      privateRepositoriesAuthorisedAt: null
      error: string
    }

export function useGitHubDataSource(): DataSourceState {
  const [state, setState] = useState<DataSourceState>({
    status: 'loading',
    privateRepositoriesAuthorised: false,
    privateRepositoriesAuthorisedAt: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      try {
        const response = await fetch('/api/me/data-sources/github', {
          credentials: 'include',
          headers: { Accept: 'application/json' },
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error(`GitHub data source request failed with HTTP ${response.status}`)
        }

        const data = (await response.json()) as {
          privateRepositoriesAuthorised: boolean
          privateRepositoriesAuthorisedAt: string | null
        }

        setState({
          status: 'ready',
          privateRepositoriesAuthorised: data.privateRepositoriesAuthorised,
          privateRepositoriesAuthorisedAt: data.privateRepositoriesAuthorisedAt,
          error: null,
        })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          privateRepositoriesAuthorised: false,
          privateRepositoriesAuthorisedAt: null,
          error: error instanceof Error ? error.message : 'Unable to load GitHub settings',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [])

  return state
}
