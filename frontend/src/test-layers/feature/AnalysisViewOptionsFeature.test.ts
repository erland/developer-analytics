import { describe, expect, it } from 'vitest'
import {
  createAnalysisViewOptions,
  emptyAnalysisViewOptions,
  type AnalysisViewOptions,
} from '../../analysis/AnalysisViewOptions'

describe('AnalysisViewOptions foundation', () => {
  it('starts without presentation preferences', () => {
    expect(emptyAnalysisViewOptions).toEqual({})
  })

  it('represents presentation choices without analysis filters', () => {
    const options: AnalysisViewOptions = createAnalysisViewOptions({
      metric: 'changed-lines',
      colourBy: 'project-type',
      sort: { key: 'activity', direction: 'desc' },
      groupBy: 'year',
    })

    expect(options).toEqual({
      metric: 'changed-lines',
      colourBy: 'project-type',
      sort: { key: 'activity', direction: 'desc' },
      groupBy: 'year',
    })

    expect('technologies' in options).toBe(false)
    expect('projectTypes' in options).toBe(false)
    expect('year' in options).toBe(false)
    expect('search' in options).toBe(false)
  })

  it('creates an independent sort object', () => {
    const sort = { key: 'name', direction: 'asc' as const }
    const options = createAnalysisViewOptions({ sort })

    expect(options.sort).toEqual(sort)
    expect(options.sort).not.toBe(sort)
  })
})
