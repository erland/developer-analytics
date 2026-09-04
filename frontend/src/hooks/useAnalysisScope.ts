import { useCallback, useEffect, useState } from 'react'
import { createAnalysisScope, type AnalysisScope } from '../analysis/AnalysisScope'
import { analysisScopeFromSearchParams, analysisScopeToSearchParams } from '../analysis/AnalysisScopeUrl'

type HistoryMode = 'push' | 'replace'

export const ANALYSIS_SCOPE_CHANGE_EVENT = 'developer-analytics:analysis-scope-change'

export type AnalysisScopeController = {
  scope: AnalysisScope
  pushScope: (scope: AnalysisScope) => void
  replaceScope: (scope: AnalysisScope) => void
}

/**
 * Keeps Explore analysis selection synchronized with the browser URL.
 *
 * The URL is the source of truth: initial state and browser back/forward are
 * always parsed from location.search, while writes serialize the same
 * AnalysisScope contract before updating local React state.
 */
export function useAnalysisScope(): AnalysisScopeController {
  const [scope, setScope] = useState<AnalysisScope>(() => analysisScopeFromSearchParams(window.location.search))

  useEffect(() => {
    const handlePopState = () => {
      setScope(analysisScopeFromSearchParams(window.location.search))
    }

    window.addEventListener('popstate', handlePopState)
    window.addEventListener(ANALYSIS_SCOPE_CHANGE_EVENT, handlePopState)
    return () => {
      window.removeEventListener('popstate', handlePopState)
      window.removeEventListener(ANALYSIS_SCOPE_CHANGE_EVENT, handlePopState)
    }
  }, [])

  const writeScope = useCallback((nextScope: AnalysisScope, mode: HistoryMode) => {
    const normalizedScope = createAnalysisScope(nextScope)
    const params = analysisScopeToSearchParams(normalizedScope)
    const query = params.toString()
    const nextUrl = `${window.location.pathname}${query ? `?${query}` : ''}${window.location.hash}`

    if (mode === 'replace') {
      window.history.replaceState(null, '', nextUrl)
    } else {
      window.history.pushState(null, '', nextUrl)
    }

    setScope(normalizedScope)
    window.dispatchEvent(new Event(ANALYSIS_SCOPE_CHANGE_EVENT))
  }, [])

  const pushScope = useCallback((nextScope: AnalysisScope) => writeScope(nextScope, 'push'), [writeScope])
  const replaceScope = useCallback((nextScope: AnalysisScope) => writeScope(nextScope, 'replace'), [writeScope])

  return { scope, pushScope, replaceScope }
}
