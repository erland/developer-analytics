import { useState } from 'react'
import { type ActivityData, type ActivityMetric, type ActivityPeriod, useActivityView } from '../hooks/useActivityView'

const periods: Array<{ value: ActivityPeriod; label: string }> = [
  { value: '12m', label: '12 months' },
  { value: '24m', label: '24 months' },
  { value: '5y', label: '5 years' },
  { value: 'all', label: 'All time' },
]

export function ActivityView() {
  const [period, setPeriod] = useState<ActivityPeriod>('12m')
  const [metric, setMetric] = useState<ActivityMetric>('lines')
  const activity = useActivityView(period)

  return <>
    <div className="view-toolbar">
      <div><p className="eyebrow">Measured activity</p><h2>Development activity</h2></div>
      <div className="timeline-controls activity-controls">
        <label><span>Period</span><select value={period} onChange={event => setPeriod(event.target.value as ActivityPeriod)}>{periods.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        <label><span>Trend measure</span><select value={metric} onChange={event => setMetric(event.target.value as ActivityMetric)}><option value="lines">Changed lines</option><option value="commits">Commits</option></select></label>
      </div>
    </div>
    {activity.status === 'loading' ? <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading activity…</p></section> : null}
    {activity.status === 'error' ? <section className="dashboard-error"><h2>Activity data could not be loaded.</h2><p>{activity.error}</p></section> : null}
    {activity.status === 'ready' ? <ActivityContent key={period} data={activity.data} period={period} metric={metric}/> : null}
  </>
}

function ActivityContent({ data, period, metric }: { data: ActivityData; period: ActivityPeriod; metric: ActivityMetric }) {
  const [drillYear, setDrillYear] = useState<number | null>(null)
  const [selected, setSelected] = useState<{ title: string; value: number; commits: number; changedLines: number; projects: number; projectNames: string[] } | null>(null)
  const longInterval = period === '5y' || period === 'all'
  const showYears = longInterval && drillYear === null
  const months = drillYear === null ? data.commitsPerMonth : data.commitsPerMonth.filter(point => Number(point.month.slice(0, 4)) === drillYear)
  const points = showYears ? data.commitsPerYear : months
  const max = Math.max(1, ...points.map(point => metric === 'lines' ? point.changedLines : point.commits))
  const lineCoverage = data.commitCount > 0 ? Math.round((data.lineStatisticsCommitCount / data.commitCount) * 100) : 0

  return <>
    <section className="metric-grid">
      <Metric label="Commits" value={fmt(data.commitCount)}/>
      <Metric label="Active projects" value={fmt(data.activeProjects)}/>
      <Metric label="Average commit size" value={data.commitSizeStatisticsAvailable ? `${fmt(Math.round(data.averageCommitSize))} lines` : 'Not available'}/>
      <Metric label="Additions" value={data.commitSizeStatisticsAvailable ? `+${fmt(data.additions)}` : 'Not available'}/>
      <Metric label="Deletions" value={data.commitSizeStatisticsAvailable ? `−${fmt(data.deletions)}` : 'Not available'}/>
    </section>

    {metric === 'lines' && lineCoverage < 100 ? <p className="timeline-coverage-note">The trend uses additions + deletions only where commit-level line statistics are available ({data.lineStatisticsCommitCount.toLocaleString()} of {data.commitCount.toLocaleString()} commits, {lineCoverage}%).</p> : null}

    <section className="dashboard-section">
      <div className="timeline-section-heading">
        <div><span className="card-kicker">{showYears ? 'Yearly trend' : 'Monthly trend'}</span><h2>{metric === 'lines' ? 'Changed lines' : 'Commits'} per {showYears ? 'year' : 'month'}</h2></div>
        {drillYear !== null ? <button className="text-button" type="button" onClick={() => { setDrillYear(null); setSelected(null) }}>← Back to years</button> : null}
      </div>
      <div className="bar-chart bar-chart-scroll">
        {points.map(point => {
          const isYear = 'year' in point
          const label = isYear ? String(point.year) : formatMonth(point.month)
          const value = metric === 'lines' ? point.changedLines : point.commits
          return <Bar key={isYear ? point.year : point.month} label={label} value={value} max={max} secondary={`${point.activeProjects} active project${point.activeProjects === 1 ? '' : 's'}`} onClick={() => {
            if (isYear) {
              setDrillYear(point.year)
              setSelected(null)
            } else {
              setSelected({ title: label, value, commits: point.commits, changedLines: point.changedLines, projects: point.activeProjects, projectNames: point.projects })
            }
          }}/>
        })}
      </div>
      {selected ? <div className="activity-drilldown"><strong>{selected.title}</strong><span>{fmt(selected.value)} {metric === 'lines' ? 'changed lines' : 'commits'} across {selected.projects} active projects</span><span>{fmt(selected.commits)} commits · {fmt(selected.changedLines)} changed lines</span>{selected.projectNames.length ? <span>{selected.projectNames.slice(0, 30).join(' · ')}{selected.projectNames.length > 30 ? ' …' : ''}</span> : null}</div> : null}
    </section>

    <section className="dashboard-section activity-period-note"><span className="card-kicker">Activity period</span><p>{data.firstActivityAt && data.lastActivityAt ? `${formatDate(data.firstActivityAt)} – ${formatDate(data.lastActivityAt)}` : 'No activity in selected period'}</p></section>
  </>
}

function Metric({ label, value }: { label: string; value: string }) {
  return <article className="metric-card"><span>{label}</span><strong>{value}</strong></article>
}

function Bar({ label, value, max, secondary, onClick }: { label: string; value: number; max: number; secondary?: string; onClick?: () => void }) {
  const width = Math.max(value ? 2 : 0, Math.round((value / max) * 100))
  return <button type="button" className="bar-row bar-row-button" onClick={onClick}><div className="bar-label"><strong>{label}</strong>{secondary ? <span>{secondary}</span> : null}</div><div className="bar-track"><div className="bar-fill" style={{ width: `${width}%` }}/></div><span className="bar-value">{compact(value)}</span></button>
}

const fmt = (value: number) => new Intl.NumberFormat().format(value)
const compact = (value: number) => new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value)
const formatMonth = (month: string) => new Intl.DateTimeFormat(undefined, { month: 'short', year: 'numeric' }).format(new Date(`${month}-01T00:00:00Z`))
const formatDate = (value: string) => new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
