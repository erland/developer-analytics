import { useMemo, useState } from 'react'

export type DrilldownMetric = 'lines' | 'commits'

export type DrilldownMonthPoint = {
  month: string
  commits: number
  changedLines: number
  lineStatisticsCommitCount?: number
  secondary?: string
}

type Props = {
  points: DrilldownMonthPoint[]
  emptyText: string
  initialLevel?: 'year' | 'month'
}

export function DrilldownTimeChart({ points, emptyText, initialLevel = 'year' }: Props) {
  const [selectedYear, setSelectedYear] = useState<number | null>(null)
  const [metric, setMetric] = useState<DrilldownMetric>('lines')

  const normalizedPoints = useMemo(() => points.map(point => ({
    ...point,
    commits: finite(point.commits),
    changedLines: finite(point.changedLines),
    lineStatisticsCommitCount: finite(point.lineStatisticsCommitCount ?? 0),
  })), [points])

  const years = useMemo(() => {
    const grouped = new Map<number, { commits: number; changedLines: number; lineStatisticsCommitCount: number; months: number }>()
    for (const point of normalizedPoints) {
      const year = Number(point.month.slice(0, 4))
      if (!Number.isFinite(year)) continue
      const current = grouped.get(year) ?? { commits: 0, changedLines: 0, lineStatisticsCommitCount: 0, months: 0 }
      current.commits += point.commits
      current.changedLines += point.changedLines
      current.lineStatisticsCommitCount += point.lineStatisticsCommitCount ?? 0
      current.months += 1
      grouped.set(year, current)
    }
    return Array.from(grouped.entries()).sort(([a], [b]) => a - b).map(([year, value]) => ({ year, ...value }))
  }, [normalizedPoints])

  const monthlyPoints = selectedYear == null
    ? normalizedPoints
    : normalizedPoints.filter(point => Number(point.month.slice(0, 4)) === selectedYear)

  const showYears = initialLevel === 'year' && selectedYear == null
  const selectedPoints = showYears ? years : monthlyPoints
  const valueOf = (point: { commits: number; changedLines: number }) => metric === 'lines' ? point.changedLines : point.commits
  const max = Math.max(1, ...selectedPoints.map(valueOf))
  const commitCount = normalizedPoints.reduce((sum, point) => sum + point.commits, 0)
  const lineStatisticsCommitCount = normalizedPoints.reduce((sum, point) => sum + (point.lineStatisticsCommitCount ?? 0), 0)
  const lineCoverage = commitCount > 0 ? Math.round((lineStatisticsCommitCount / commitCount) * 100) : 0

  if (!normalizedPoints.length) return <p className="empty-state">{emptyText}</p>

  return <>
    <div className="timeline-controls time-chart-controls">
      <label>
        <span>Measure</span>
        <select value={metric} onChange={event => setMetric(event.target.value as DrilldownMetric)}>
          <option value="lines">Changed lines</option>
          <option value="commits">Commits</option>
        </select>
      </label>
    </div>

    {metric === 'lines' && lineCoverage < 100 ? (
      <p className="timeline-coverage-note">
        Changed-line activity uses additions + deletions where line statistics are available
        ({lineStatisticsCommitCount.toLocaleString()} of {commitCount.toLocaleString()} commits, {lineCoverage}%).
      </p>
    ) : null}

    {selectedYear != null ? (
      <button className="text-button timeline-back" type="button" onClick={() => setSelectedYear(null)}>
        ← Back to years
      </button>
    ) : null}

    <div className="bar-chart bar-chart-scroll">
      {showYears
        ? years.map(point => (
          <TimeBar
            key={point.year}
            label={String(point.year)}
            secondary={`${point.months} active month${point.months === 1 ? '' : 's'}`}
            value={valueOf(point)}
            max={max}
            onClick={() => setSelectedYear(point.year)}
          />
        ))
        : monthlyPoints.map(point => (
          <TimeBar
            key={point.month}
            label={formatMonth(point.month)}
            secondary={point.secondary}
            value={valueOf(point)}
            max={max}
          />
        ))}
    </div>

    <p className="time-chart-caption">
      {showYears
        ? `Select a year to see ${metric === 'lines' ? 'changed lines' : 'commits'} by month.`
        : selectedYear != null
          ? `${selectedYear} by month · ${metric === 'lines' ? 'changed lines' : 'commits'}.`
          : `${metric === 'lines' ? 'Changed lines' : 'Commits'} by month.`}
    </p>
  </>
}

function TimeBar({ label, secondary, value, max, onClick }: {
  label: string
  secondary?: string
  value: number
  max: number
  onClick?: () => void
}) {
  const content = <>
    <div className="bar-label"><strong>{label}</strong>{secondary ? <span>{secondary}</span> : null}</div>
    <div className="bar-track" aria-hidden="true"><div className="bar-fill" style={{ width: `${Math.max(value ? 2 : 0, Math.round((value / max) * 100))}%` }}/></div>
    <span className="bar-value">{compact(value)}</span>
  </>

  return onClick
    ? <button type="button" className="bar-row bar-row-button" onClick={onClick}>{content}</button>
    : <div className="bar-row">{content}</div>
}

function formatMonth(value: string) {
  const normalized = value.length === 7 ? `${value}-01` : value
  return new Intl.DateTimeFormat(undefined, { month: 'short', year: 'numeric' })
    .format(new Date(`${normalized}T00:00:00Z`))
}

function finite(value: number) {
  return Number.isFinite(value) ? value : 0
}

const compact = (value: number) => new Intl.NumberFormat(undefined, {
  notation: 'compact',
  maximumFractionDigits: 1,
}).format(value)
