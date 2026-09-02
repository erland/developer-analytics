import { useMemo, useState } from 'react'

export type DrilldownMonthPoint = {
  month: string
  value: number
  secondary?: string
}

type Props = {
  points: DrilldownMonthPoint[]
  valueLabel: string
  emptyText: string
  initialLevel?: 'year' | 'month'
}

export function DrilldownTimeChart({
  points,
  valueLabel,
  emptyText,
  initialLevel = 'year',
}: Props) {
  const [selectedYear, setSelectedYear] = useState<number | null>(null)

  const years = useMemo(() => {
    const grouped = new Map<number, { value: number; months: number }>()
    for (const point of points) {
      const year = Number(point.month.slice(0, 4))
      if (!Number.isFinite(year)) continue
      const current = grouped.get(year) ?? { value: 0, months: 0 }
      current.value += point.value
      current.months += 1
      grouped.set(year, current)
    }
    return Array.from(grouped.entries())
      .sort(([a], [b]) => a - b)
      .map(([year, value]) => ({ year, ...value }))
  }, [points])

  const monthlyPoints = selectedYear == null
    ? points
    : points.filter(point => Number(point.month.slice(0, 4)) === selectedYear)

  const showYears = initialLevel === 'year' && selectedYear == null
  const values = showYears ? years.map(point => point.value) : monthlyPoints.map(point => point.value)
  const max = Math.max(1, ...values)

  if (!points.length) return <p className="empty-state">{emptyText}</p>

  return <>
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
            value={point.value}
            max={max}
            onClick={() => setSelectedYear(point.year)}
          />
        ))
        : monthlyPoints.map(point => (
          <TimeBar
            key={point.month}
            label={formatMonth(point.month)}
            secondary={point.secondary}
            value={point.value}
            max={max}
          />
        ))}
    </div>

    <p className="time-chart-caption">
      {showYears
        ? `Select a year to see ${valueLabel.toLowerCase()} by month.`
        : selectedYear != null
          ? `${selectedYear} by month.`
          : `${valueLabel} by month.`}
    </p>
  </>
}

function TimeBar({
  label,
  secondary,
  value,
  max,
  onClick,
}: {
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

const compact = (value: number) => new Intl.NumberFormat(undefined, {
  notation: 'compact',
  maximumFractionDigits: 1,
}).format(value)
