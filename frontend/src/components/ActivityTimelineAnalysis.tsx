import { useState } from 'react'
import { type ActivityData, type ActivityMetric, type ProjectLifecycle } from '../hooks/useActivityView'

export type ActivityColourBy = 'projectType' | 'technology' | 'none'
type Level = 'year' | 'month' | 'week'
type PeriodProject = { project: ProjectLifecycle; value: number; commits: number; changedLines: number }
type PeriodRow = { key: string; label: string; value: number; projects: PeriodProject[]; segments: Array<{ label: string; value: number }> }

type Props = {
  data: ActivityData
  metric: ActivityMetric
  colourBy: ActivityColourBy
  onOpenProject?: (repositoryId: string) => void
}

export function ActivityTimelineAnalysis({ data, metric, colourBy, onOpenProject }: Props) {
  const [selectedYear, setSelectedYear] = useState<number | null>(null)
  const [selectedMonth, setSelectedMonth] = useState<string | null>(null)
  const [selectedWeek, setSelectedWeek] = useState<string | null>(null)

  const level: Level = selectedMonth ? 'week' : selectedYear ? 'month' : 'year'
  const yearPeriods = buildPeriods(data.projectsOverTime, 'year', metric, colourBy, null, null)
  const monthPeriods = selectedYear == null
    ? []
    : buildPeriods(data.projectsOverTime, 'month', metric, colourBy, selectedYear, null)
  const weekPeriods = selectedMonth == null
    ? []
    : buildPeriods(data.projectsOverTime, 'week', metric, colourBy, selectedYear, selectedMonth)
  const periods = level === 'year' ? yearPeriods : level === 'month' ? monthPeriods : weekPeriods
  const detail = selectedWeek
    ? weekPeriods.find(period => period.key === selectedWeek) ?? null
    : selectedMonth
      ? monthPeriods.find(period => period.key === selectedMonth) ?? null
      : selectedYear != null
        ? yearPeriods.find(period => period.key === String(selectedYear)) ?? null
        : null

  function selectPeriod(period: PeriodRow) {
    if (level === 'year') {
      setSelectedYear(Number(period.key))
      setSelectedMonth(null)
      setSelectedWeek(null)
      return
    }
    if (level === 'month') {
      setSelectedMonth(period.key)
      setSelectedWeek(null)
      return
    }
    setSelectedWeek(period.key === selectedWeek ? null : period.key)
  }

  function goBack() {
    if (selectedMonth) {
      setSelectedMonth(null)
      setSelectedWeek(null)
    } else {
      setSelectedYear(null)
      setSelectedWeek(null)
    }
  }

  return <>
    {selectedYear != null ? <button className="text-button timeline-back" type="button" onClick={goBack}>← {selectedMonth ? `Back to ${selectedYear}` : 'Back to years'}</button> : null}
    <TimelineBars
      periods={periods}
      metric={metric}
      colourBy={colourBy}
      level={level}
      selectedPeriod={selectedWeek}
      onSelect={selectPeriod}
    />
    {detail ? <PeriodDetail period={detail} metric={metric} level={selectedWeek ? 'week' : selectedMonth ? 'month' : 'year'} onOpenProject={onOpenProject} /> : null}
  </>
}

function TimelineBars({ periods, metric, colourBy, level, selectedPeriod, onSelect }: { periods: PeriodRow[]; metric: ActivityMetric; colourBy: ActivityColourBy; level: Level; selectedPeriod: string | null; onSelect: (period: PeriodRow) => void }) {
  if (!periods.length) return <section className="dashboard-section"><p className="empty-state">No activity has been collected for this period.</p></section>
  const max = Math.max(1, ...periods.map(period => period.value))
  const legend = Array.from(new Set(periods.flatMap(period => period.segments.map(segment => segment.label)))).slice(0, 12)
  const next = level === 'year' ? 'months' : level === 'month' ? 'weeks' : 'projects'

  return <section className="dashboard-section timeline-period-section">
    <div className="timeline-section-heading"><div><span className="card-kicker">{level === 'year' ? 'Years' : level === 'month' ? 'Months' : 'Weeks'}</span><h2>{metric === 'lines' ? 'Changed lines' : 'Commits'} over time</h2></div><span className="timeline-hint">Select a {level} to see {next}.</span></div>
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

function PeriodDetail({ period, metric, level, onOpenProject }: { period: PeriodRow; metric: ActivityMetric; level: Level; onOpenProject?: (repositoryId: string) => void }) {
  return <section className="dashboard-section timeline-detail">
    <span className="card-kicker">Projects during this {level}</span>
    <h2>{period.label}</h2>
    <p className="settings-intro">{fmt(period.value)} {metric === 'lines' ? 'changed lines' : 'commits'} across {period.projects.length} active project{period.projects.length === 1 ? '' : 's'}.</p>
    <div className="project-list">{period.projects.slice().sort((a,b) => b.value - a.value).map(item => <div className="project-row" key={item.project.repositoryId}><div>{onOpenProject ? <button type="button" className="project-link-button" onClick={() => onOpenProject(item.project.repositoryId)}>{item.project.repositoryName}</button> : <h3>{item.project.repositoryName}</h3>}<p>{projectLabels(item.project.projectTypes, item.project.projectType)} · {projectLabels(item.project.technologies, item.project.technology)}</p></div><div className="project-scores"><strong>{compact(item.value)} {metric === 'lines' ? 'lines' : 'commits'}</strong><span>{fmt(item.commits)} commits</span>{item.changedLines > 0 ? <span>{fmt(item.changedLines)} changed lines</span> : null}</div></div>)}</div>
  </section>
}

function buildPeriods(projects: ProjectLifecycle[], level: Level, metric: ActivityMetric, colourBy: ActivityColourBy, selectedYear: number | null, selectedMonth: string | null): PeriodRow[] {
  const periods = new Map<string, Map<string, PeriodProject>>()

  for (const project of projects) {
    const source = level === 'week' ? project.weeklyActivity : project.monthlyActivity
    for (const activity of source) {
      const activityMonth = activity.parentMonth ?? activity.period.slice(0, 7)
      const year = Number(activityMonth.slice(0, 4))
      if (selectedYear != null && year !== selectedYear) continue
      if (selectedMonth != null && activityMonth !== selectedMonth) continue
      const key = level === 'year' ? String(year) : activity.period
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
    return { key, label: formatPeriod(key, level), value, projects: projectValues, segments: Array.from(segmentMap.entries()).map(([label, segmentValue]) => ({ label, value: segmentValue })).sort((a,b) => b.value - a.value) }
  }).filter(period => period.value > 0 || metric === 'commits')
}

function projectLabels(labels: string[], fallback: string) {
  return labels.length ? labels.join(', ') : fallback
}

function colour(label: string) {
  let hash = 0
  for (let index = 0; index < label.length; index++) hash = ((hash << 5) - hash + label.charCodeAt(index)) | 0
  return `hsl(${Math.abs(hash) % 360} 58% 48%)`
}

function formatPeriod(value: string, level: Level) {
  if (level === 'year') return value
  if (level === 'week') return `Week of ${new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(new Date(`${value}T00:00:00Z`))}`
  return formatMonth(value)
}

function formatMonth(value: string) {
  return new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' }).format(new Date(`${value}-01T00:00:00Z`))
}

const fmt = (value: number) => new Intl.NumberFormat().format(value)
const compact = (value: number) => new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 }).format(value)
