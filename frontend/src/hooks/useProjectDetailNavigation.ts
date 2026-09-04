import { useCallback, useEffect, useState } from 'react'

const PROJECT_PARAM = 'project'
const DETAIL_STATE_KEY = 'developerAnalyticsProjectDetail'

export function useProjectDetailNavigation() {
  const [selectedProjectId, setSelectedProjectId] = useState<string | null>(() => readProjectId())

  useEffect(() => {
    const handlePopState = () => setSelectedProjectId(readProjectId())
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  const openProject = useCallback((repositoryId: string) => {
    const params = new URLSearchParams(window.location.search)
    params.set(PROJECT_PARAM, repositoryId)
    const nextUrl = buildUrl(params)
    window.history.pushState({ ...(window.history.state ?? {}), [DETAIL_STATE_KEY]: true }, '', nextUrl)
    setSelectedProjectId(repositoryId)
  }, [])

  const closeProject = useCallback(() => {
    if (window.history.state?.[DETAIL_STATE_KEY]) {
      // Hide the detail synchronously; popstate then restores the URL-backed state.
      setSelectedProjectId(null)
      window.history.back()
      return
    }

    const params = new URLSearchParams(window.location.search)
    params.delete(PROJECT_PARAM)
    window.history.replaceState(window.history.state, '', buildUrl(params))
    setSelectedProjectId(null)
  }, [])

  const clearProject = useCallback(() => {
    const params = new URLSearchParams(window.location.search)
    params.delete(PROJECT_PARAM)
    window.history.replaceState(window.history.state, '', buildUrl(params))
    setSelectedProjectId(null)
  }, [])

  return { selectedProjectId, openProject, closeProject, clearProject }
}

function readProjectId(): string | null {
  const value = new URLSearchParams(window.location.search).get(PROJECT_PARAM)?.trim()
  return value || null
}

function buildUrl(params: URLSearchParams): string {
  const query = params.toString()
  return `${window.location.pathname}${query ? `?${query}` : ''}${window.location.hash}`
}
