import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { createAnalysisScope } from '../../analysis/AnalysisScope'
import { useAnalysisScope } from '../../hooks/useAnalysisScope'

describe('shared AnalysisScope URL controller', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/projects?technology=java&year=2026#results')
  })

  it('reads the initial scope from the URL and preserves pathname/hash when pushing changes', () => {
    const { result } = renderHook(() => useAnalysisScope())

    expect(result.current.scope).toEqual(createAnalysisScope({ technologies: ['java'], year: 2026 }))

    act(() => {
      result.current.pushScope(createAnalysisScope({ technologies: ['typescript'], projectTypes: ['backend'] }))
    })

    expect(window.location.pathname).toBe('/projects')
    expect(window.location.hash).toBe('#results')
    expect(window.location.search).toBe('?technology=typescript&projectType=backend')
    expect(result.current.scope).toEqual(createAnalysisScope({ technologies: ['typescript'], projectTypes: ['backend'] }))
  })

  it('uses replaceState without adding a new history entry for transient changes', () => {
    const { result } = renderHook(() => useAnalysisScope())
    const initialLength = window.history.length

    act(() => {
      result.current.replaceScope(createAnalysisScope({ search: 'analytics' }))
    })

    expect(window.history.length).toBe(initialLength)
    expect(window.location.search).toBe('?search=analytics')
    expect(window.location.hash).toBe('#results')
  })

  it('keeps multiple mounted scope consumers synchronized after pushState writes', () => {
    const first = renderHook(() => useAnalysisScope())
    const second = renderHook(() => useAnalysisScope())

    act(() => {
      first.result.current.pushScope(createAnalysisScope({ technologies: ['java'], year: 2025 }))
    })

    expect(second.result.current.scope).toEqual(
      createAnalysisScope({ technologies: ['java'], year: 2025 }),
    )
  })

  it('re-parses the URL on browser back/forward popstate', () => {
    const { result } = renderHook(() => useAnalysisScope())

    act(() => {
      window.history.pushState(null, '', '/projects?technology=swift&month=2026-08#results')
      window.dispatchEvent(new PopStateEvent('popstate'))
    })

    expect(result.current.scope).toEqual(createAnalysisScope({ technologies: ['swift'], month: '2026-08' }))
  })
})
