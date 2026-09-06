import { useEffect, useMemo, useState } from 'react'
import type { AnalysisScope } from '../analysis/AnalysisScope'
import { analysisScopeToSearchParams } from '../analysis/AnalysisScopeUrl'
import { getJson } from '../api/request'
import type { InventoryResponse } from './useProjectInventory'

type State =
  | { status: 'loading'; data: null; error: null }
  | { status: 'ready'; data: InventoryResponse; error: null }
  | { status: 'error'; data: null; error: string }

export function useMatchingProjects(
  scope: AnalysisScope,
  page: number,
  pageSize = 25,
): State {
  const paramsKey = useMemo(() => {
    const params = analysisScopeToSearchParams(scope)
    params.set('page', String(page))
    params.set('pageSize', String(pageSize))
    return params.toString()
  }, [scope, page, pageSize])

  const [state, setState] = useState<State>({
    status: 'loading',
    data: null,
    error: null,
  })

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      setState({ status: 'loading', data: null, error: null })

      try {
        const data = await getJson<InventoryResponse>(
          `/api/me/project-inventory?${paramsKey}`,
          {
            signal: controller.signal,
            errorMessage: 'Matching projects request failed',
          },
        )
        setState({ status: 'ready', data, error: null })
      } catch (error) {
        if (controller.signal.aborted) return
        setState({
          status: 'error',
          data: null,
          error: error instanceof Error ? error.message : 'Unable to load matching projects',
        })
      }
    }

    void load()
    return () => controller.abort()
  }, [paramsKey])

  return state
}
