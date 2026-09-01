import { useState } from 'react'
import { type ActivityMetric, type ProjectLifecycle, useActivityView } from '../hooks/useActivityView'

type Granularity = 'year' | 'month' | 'week'
type ColourBy = 'projectType' | 'technology' | 'none'
type Props = { onOpenProject?: (repositoryId: string) => void }
type PeriodProject = { project: ProjectLifecycle; value: number; commits: number; changedLines: number }
type PeriodRow = { key: string; label: string; value: number; projects: PeriodProject[]; segments: Array<{ label: string; value: number }> }

export function TimelineView({ onOpenProject }: Props) {
  const activity = useActivityView('all')
  const [granularity, setGranularity] = useState<Granularity>('year')
  const [metric, setMetric] = useState<ActivityMetric>('lines')
  const [colourBy, setColourBy] = useState<ColourBy>('projectType')
  const [selectedYear, setSelectedYear] = useState<number | null>(null)
  const [selectedPeriod, setSelectedPeriod] = useState<string | null>(null)

  if (activity.status === 'loading') {
    return <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading timeline…</p></section>
  }
  if (activity.status === 'error') {
    return <section className="dashboard-error"><h2>Timeline could not be loaded.</h2><p>{activity.error}</p></section>
  }

  const data = activity.data
  const periods = buildPeriods(data.projectsOverTime, granularity, metric, colourBy, selectedYear)
  const selected = selectedPeriod ? periods.find(period => period.key === selectedPeriod) ?? null : null
  const lineCoverage = data.commitCount > 0 ? Math.round((data.lineStatisticsCommitCount / data.commitCount) * 100) : 0

  function chooseGranularity(next: Granularity) {
    setGranularity(next)
    setSelectedPeriod(null)
    if (next === 'year') setSelectedYear(null)
  }

  function choosePeriod(period: PeriodRow) {
    if (granularity === 'year') {
      setSelectedYear(Number(period.key))
      setSelectedPeriod(null)
      setGranularity('month')
      return
    }
    setSelectedPeriod(period.key === selectedPeriod ? null : period.key)
  }

  return <>
    <div className="view-toolbar timeline-toolbar">
      <div>
        <p className="eyebrow">Engagement over time</p>
        <h2>{selectedYear && granularity !== 'year' ? `${selectedYear} timeline` : 'Timeline'}</h2>
      </div>
      <div className="timeline-controls" aria-label="Timeline controls">
        <label><span>Interval</span><select value={granularity} onChange={event => chooseGranularity(event.target.value as Granularity)}><option value="year">Year</option><option value="month">Month</option><option value="week">Week</option></select></label>
        <label><span>Measure</span><select value={metric} onChange={event => setMetric(event.target.value as ActivityMetric)}><option value="lines">Changed lines</option><option value="commits">Commits</option></select></label>
        <label><span>Colour by</span><select value={colourBy} onChange={event => setColourBy(event.target.value as ColourBy)}><option value="projectType">Project type</option><option value="technology">Technology</option><option value="none">Single colour</option></select></label>
      </div>
    </div>

    {selectedYear && granularity !== 'year' ? <button className="text-button timeline-back" type="button" onClick={() => { setSelectedYear(null); setSelectedPeriod(null); setGranularity('year') }}>← Back to years</button> : null}

    {metric === 'lines' && lineCoverage < 100 ? <p className="timeline-coverage-note">Changed-line activity is based on commits where additions/deletions are available ({data.lineStatisticsCommitCount.toLocaleString()} of {data.commitCount.toLocaleString()} commits, {lineCoverage}%). Switch to commits for complete activity coverage.</p> : null}

    <TimelineBars periods={periods} metric={metric} colourBy={colourBy} selectedPeriod={selectedPeriod} onSelect={choosePeriod} />

    {selected ? <PeriodDetail period={selected} metric={metric} onOpenProject={onOpenProject} /> : null}
  </>
}

function TimelineBars({ periods, metric, colourBy, selectedPeriod, onSelect }: { periods: PeriodRow[]; metric: ActivityMetric; colourBy: ColourBy; selectedPeriod: string | null; onSelect: (period: PeriodRow) => void }) {
  if (!periods.length) return <section className="dashboard-section"><p className="empty-state">No activity has been collected for this interval.</p></section>
  const max = Math.max(1, ...periods.map(period => period.value))
  const legend = Array.from(new Set(periods.flatMap(period => period.segments.map(segment => segment.label)))).slice(0, 12)

  return <section className="dashboard-section timeline-period-section">
    <div className="timeline-section-heading"><div><span className="card-kicker">Activity</span><h2>{metric === 'lines' ? 'Changed lines' : 'Commits'} by period</h2></div><span className="timeline-hint">Select a period for projects{periods[0]?.key.length === 4 ? ' and month drill-down' : ''}</span></div>
    {colourBy !== 'none' && legend.length ? <div className="timeline-legend">{legend.map(label => <span key={label}><i style={{ background: colour(label) }} />{label}</span>)}</div> : null}
    <div className="timeline-period-list">
      {periods.map(period => {
        const width = Math.max(period.value ? 3 : 0, Math.round((period.value / max) * 100))
        return <button key={period.key} type="button" className={`timeline-period-row ${selectedPeriod === period.key ? 'timeline-period-selected' : ''}`} onClick={() => onSelect(period)}>
          <span className="timeline-period-label">{period.label}</span>
          <span className="timeline-bar-track"><span className="timeline-bar-size" style={{ width: `${width}%` }}>{period.segments.filter(segment => segment.value > 0).map(segment => <span key={segment.label} title={`${segment.label}: ${fmt(segment.value)} ${metric === 'lines' ? 'changed lines' : 'commits'}`} style={{ width: `${period.value ? (segment.value / period.value) * 100 : 0}%`, background: colourBy === 'none' ? '#334155' : colour(segment.label) }} />)}</span></span>
          <span className="timeline-period-value">{compact(period.value)}</span>
        </button>
      })}
    </div>
  </section>
}

function PeriodDetail({ period, metric, onOpenProject }: { period: PeriodRow; metric: ActivityMetric; onOpenProject?: (repositoryId: string) => void }) {
  return <section className="dashboard-section timeline-detail">
    <span className="card-kicker">Selected period</span>
    <h2>{period.label}</h2>
    <p className="settings-intro">{fmt(period.value)} {metric === 'lines' ? 'changed lines' : 'commits'} across {period.projects.length} active project{period.projects.length === 1 ? '' : 's'}.</p>
    <div className="project-list">{period.projects.slice().sort((a,b) => b.value - a.value).map(item => <div className="project-row" key={item.project.repositoryId}><div>{onOpenProject ? <button type="button" className="project-link-button" onClick={() => onOpenProject(item.project.repositoryId)}>{item.project.repositoryName}</button> : <h3>{item.project.repositoryName}</h3>}<p>{item.project.projectType} · {item.project.technology}</p></div><div className="project-scores"><strong>{compact(item.value)} {metric === 'lines' ? 'lines' : 'commits'}</strong><span>{fmt(item.commits)} commits</span>{item.changedLines > 0 ? <span>{fmt(item.changedLines)} changed lines</span> : null}</div></div>)}</div>
  </section>
}

function buildPeriods(projects: ProjectLifecycle[], granularity: Granularity, metric: ActivityMetric, colourBy: ColourBy, selectedYear: number | null): PeriodRow[] {
  const periods = new Map<string, Map<string, PeriodProject>>()

  for (const project of projects) {
    const source = granularity === 'week' ? project.weeklyActivity : project.monthlyActivity
    for (const activity of source) {
      const year = Number(activity.period.slice(0, 4))
      if (selectedYear && year !== selectedYear) continue
      const key = granularity === 'year' ? String(year) : activity.period
      const value = metric === 'lines' ? activity.changedLines : activity.commits
      const projectMap = periods.get(key) ?? new Map<string, PeriodProject>()
      const existing = projectMap.get(project.repositoryId)
      if (existing) {
        existing.value += value
        existing.commits += activity.commits
        existing.changedLines += activity.changedLines
      } else {
        projectMap.set(project.repositoryId, { project, value, commits: activity.commits, changedLines: activity.changedLines })
      }
      periods.set(key, projectMap)
    }
  }

  return Array.from(periods.entries()).sort(([a], [b]) => a.localeCompare(b)).map(([key, projectMap]) => {
    const projectValues = Array.from(projectMap.values()).filter(item => item.value > 0 || metric === 'commits')
    const segmentMap = new Map<string, number>()
    for (const item of projectValues) {
      const label = colourBy === 'projectType' ? item.project.projectType : colourBy === 'technology' ? item.project.technology : 'Activity'
      segmentMap.set(label, (segmentMap.get(label) ?? 0) + item.value)
    }
    const value = projectValues.reduce((sum, item) => sum + item.value, 0)
    return { key, label: formatPeriod(key, granularity), value, projects: projectValues, segments: Array.from(segmentMap.entries()).map(([label, segmentValue]) => ({ label, value: segmentValue })).sort((a,b) => b.value - a.value) }
  })
}

function colour(label: string) {
  let hash = 0
  for (let index = 0; index < label.length; index++) hash = ((hash << 5) - hash + label.charCodeAt(index)) | 0
  return `hsl(${Math.abs(hash) % 360} 58% 48%)`
}

function formatPeriod(value: string, granularity: Granularity) {
  if (granularity === 'year') return value
  const date = new Date(`${value}T00:00:00Z`)
  if (granularity === 'week') return `Week of ${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' }).format(date)}`
  return new Intl.DateTimeFormat(undefined, { month: 'short', year: 'numeric' }).format(new Date(`${value}-01T00:00:00Z`))
}

const fmt = (value: number) => new Intl.NumberFormat().format(value)
const compact = (value: number) => new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value)
