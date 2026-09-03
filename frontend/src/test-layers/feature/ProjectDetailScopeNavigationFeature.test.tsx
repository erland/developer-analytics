import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { useProjectDetailNavigation } from '../../hooks/useProjectDetailNavigation'

describe('project detail scope navigation', () => {
  beforeEach(() => {
    window.history.replaceState(null, '', '/?technology=java&projectType=backend&year=2025')
  })

  it('adds project detail without changing AnalysisScope parameters and browser back restores the scoped URL', async () => {
    const { result } = renderHook(() => useProjectDetailNavigation())

    act(() => result.current.openProject('repo-1'))

    let params = new URLSearchParams(window.location.search)
    expect(params.get('project')).toBe('repo-1')
    expect(params.get('technology')).toBe('java')
    expect(params.get('projectType')).toBe('backend')
    expect(params.get('year')).toBe('2025')
    expect(result.current.selectedProjectId).toBe('repo-1')

    await act(async () => {
      window.history.back()
      await new Promise((resolve) => setTimeout(resolve, 0))
    })

    params = new URLSearchParams(window.location.search)
    expect(params.get('project')).toBeNull()
    expect(params.get('technology')).toBe('java')
    expect(params.get('projectType')).toBe('backend')
    expect(params.get('year')).toBe('2025')
    expect(result.current.selectedProjectId).toBeNull()
  })

  it('can open a project deep link and clear only the project parameter', () => {
    window.history.replaceState(null, '', '/?technology=java&year=2025&project=repo-2')
    const { result } = renderHook(() => useProjectDetailNavigation())

    expect(result.current.selectedProjectId).toBe('repo-2')

    act(() => result.current.clearProject())

    const params = new URLSearchParams(window.location.search)
    expect(params.get('project')).toBeNull()
    expect(params.get('technology')).toBe('java')
    expect(params.get('year')).toBe('2025')
  })
})
