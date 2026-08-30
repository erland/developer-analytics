import { useState } from 'react'
import { type ActivityData, type ActivityPeriod, useActivityView } from '../hooks/useActivityView'

const periods: Array<{ value: ActivityPeriod; label: string }> = [
  { value: '12m', label: '12 months' },
  { value: '24m', label: '24 months' },
  { value: '5y', label: '5 years' },
  { value: 'all', label: 'All time' },
]

export function ActivityView() {
  const [period, setPeriod] = useState<ActivityPeriod>('12m')
  const activity = useActivityView(period)

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Measured activity</p>
          <h2>Commit activity</h2>
        </div>
        <label className="period-filter">
          <span>Period</span>
          <select
            value={period}
            onChange={(event) => setPeriod(event.target.value as ActivityPeriod)}
          >
            {periods.map((item) => (
              <option value={item.value} key={item.value}>
                {item.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {activity.status === 'loading' ? (
        <section className="dashboard-loading" aria-live="polite">
          <div className="loading-indicator" aria-hidden="true" />
          <p>Loading activity…</p>
        </section>
      ) : null}

      {activity.status === 'error' ? (
        <section className="dashboard-error" role="alert">
          <h2>Activity data could not be loaded.</h2>
          <p>{activity.error}</p>
        </section>
      ) : null}

      {activity.status === 'ready' ? (
        <ActivityContent data={activity.data} />
      ) : null}
    </>
  )
}

function ActivityContent({ data }: { data: ActivityData }) {
  const maxYear = Math.max(1, ...data.commitsPerYear.map((point) => point.commits))
  const maxMonth = Math.max(1, ...data.commitsPerMonth.map((point) => point.commits))

  return (
    <>
      <section className="metric-grid" aria-label="Activity metrics">
        <Metric label="Commits" value={formatNumber(data.commitCount)} />
        <Metric label="Active projects" value={formatNumber(data.activeProjects)} />
        <Metric label="Average commit size" value={`${formatNumber(Math.round(data.averageCommitSize))} lines`} />
        <Metric label="Median commit size" value={`${formatNumber(Math.round(data.medianCommitSize))} lines`} />
        <Metric label="Additions" value={`+${formatNumber(data.additions)}`} />
        <Metric label="Deletions" value={`−${formatNumber(data.deletions)}`} />
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Yearly trend</span>
        <h2>Commits per year</h2>
        <div className="bar-chart" aria-label="Commits per year">
          {data.commitsPerYear.length ? (
            data.commitsPerYear.map((point) => (
              <Bar
                key={point.year}
                label={String(point.year)}
                value={point.commits}
                max={maxYear}
              />
            ))
          ) : (
            <p className="empty-state">No commits in this period.</p>
          )}
        </div>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Monthly trend</span>
        <h2>Commits per month</h2>
        <div className="bar-chart bar-chart-scroll" aria-label="Commits per month">
          {data.commitsPerMonth.length ? (
            data.commitsPerMonth.map((point) => (
              <Bar
                key={point.month}
                label={formatMonth(point.month)}
                value={point.commits}
                max={maxMonth}
                secondary={`${point.activeProjects} active project${point.activeProjects === 1 ? '' : 's'}`}
              />
            ))
          ) : (
            <p className="empty-state">No monthly activity in this period.</p>
          )}
        </div>
      </section>

      <section className="dashboard-section activity-period-note">
        <span className="card-kicker">Activity period</span>
        <p>
          {data.firstActivityAt && data.lastActivityAt
            ? `${formatDate(data.firstActivityAt)} – ${formatDate(data.lastActivityAt)}`
            : 'No activity in selected period'}
        </p>
      </section>
    </>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function Bar({
  label,
  value,
  max,
  secondary,
}: {
  label: string
  value: number
  max: number
  secondary?: string
}) {
  const width = Math.max(2, Math.round((value / max) * 100))

  return (
    <div className="bar-row">
      <div className="bar-label">
        <strong>{label}</strong>
        {secondary ? <span>{secondary}</span> : null}
      </div>
      <div className="bar-track" aria-hidden="true">
        <div className="bar-fill" style={{ width: `${width}%` }} />
      </div>
      <span className="bar-value">{formatNumber(value)}</span>
    </div>
  )
}

function formatNumber(value: number) {
  return new Intl.NumberFormat().format(value)
}

function formatMonth(month: string) {
  const date = new Date(`${month}-01T00:00:00Z`)
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    year: '2-digit',
  }).format(date)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}
