type Point = {
  period: string
  additions: number
  deletions: number
}

type Props = {
  points: Point[]
  title?: string
  description?: string
  emptyText?: string
}

export function CumulativeContributionChart({
  points,
  title = 'Cumulative code contribution',
  description = 'Approximate net code built up by your observed commits (additions minus deletions).',
  emptyText = 'No line statistics are available for this period.',
}: Props) {
  const series = buildCumulativeSeries(points)
  if (!series.length) {
    return <section className="dashboard-section cumulative-contribution"><span className="card-kicker">Code footprint</span><h2>{title}</h2><p className="empty-state">{emptyText}</p></section>
  }

  const values = series.map(point => point.value)
  const min = Math.min(0, ...values)
  const max = Math.max(0, ...values)
  const range = Math.max(1, max - min)
  const width = 1000
  const height = 260
  const top = 18
  const bottom = 34
  const chartHeight = height - top - bottom
  const x = (index: number) => series.length === 1 ? width / 2 : (index / (series.length - 1)) * width
  const y = (value: number) => top + ((max - value) / range) * chartHeight
  const path = series.map((point, index) => `${x(index)},${y(point.value)}`).join(' ')
  const zeroY = y(0)
  const labelIndexes = Array.from(new Set([0, Math.floor((series.length - 1) / 2), series.length - 1]))

  return (
    <section className="dashboard-section cumulative-contribution">
      <span className="card-kicker">Code footprint</span>
      <h2>{title}</h2>
      <p className="settings-intro">{description}</p>
      <div className="cumulative-chart-summary">
        <strong>{formatSigned(series.at(-1)?.value ?? 0)} net lines</strong>
        <span>{formatSigned(series.at(-1)?.additionsTotal ?? 0)} added · {formatNumber(series.at(-1)?.deletionsTotal ?? 0)} deleted</span>
      </div>
      <div className="cumulative-chart-scroll" role="img" aria-label={`${title}. Latest value ${formatSigned(series.at(-1)?.value ?? 0)} net lines.`}>
        <svg viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" className="cumulative-chart-svg" aria-hidden="true">
          <line x1="0" x2={width} y1={zeroY} y2={zeroY} className="cumulative-chart-zero" />
          <polyline points={path} fill="none" className="cumulative-chart-line" vectorEffect="non-scaling-stroke" />
          {series.map((point, index) => (
            <circle key={point.period} cx={x(index)} cy={y(point.value)} r="4" className="cumulative-chart-point">
              <title>{`${formatPeriod(point.period)}: ${formatSigned(point.value)} net lines`}</title>
            </circle>
          ))}
        </svg>
        <div className="cumulative-chart-labels">
          {labelIndexes.map(index => <span key={series[index].period}>{formatPeriod(series[index].period)}</span>)}
        </div>
      </div>
      <p className="timeline-coverage-note">This is a contribution estimate, not current repository LOC. It only reflects commits for which additions and deletions were collected.</p>
    </section>
  )
}

export function buildCumulativeSeries(points: Point[]) {
  let value = 0
  let additionsTotal = 0
  let deletionsTotal = 0
  return points
    .slice()
    .sort((a, b) => a.period.localeCompare(b.period))
    .map(point => {
      additionsTotal += Number(point.additions ?? 0)
      deletionsTotal += Number(point.deletions ?? 0)
      value = additionsTotal - deletionsTotal
      return { period: point.period, value, additionsTotal, deletionsTotal }
    })
}

const formatNumber = (value: number) => new Intl.NumberFormat().format(value)
const formatSigned = (value: number) => `${value >= 0 ? '+' : '−'}${formatNumber(Math.abs(value))}`
function formatPeriod(value: string) {
  const month = value.slice(0, 7)
  if (!/^\d{4}-\d{2}$/.test(month)) return value
  return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short' }).format(new Date(`${month}-01T00:00:00Z`))
}
