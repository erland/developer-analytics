import { useState } from 'react'
import type { AnalysisScope } from '../analysis/AnalysisScope'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { type ActivityData, type ActivityMetric, type ActivityPeriod, useActivityView } from '../hooks/useActivityView'
import { ActivityTimelineAnalysis, type ActivityColourBy } from './ActivityTimelineAnalysis'
import { AnalysisFilters } from './AnalysisFilters'
import { AnalysisEmptyState } from './AnalysisEmptyState'
import { SummaryFacts } from './SummaryFacts'

const periods: Array<{ value: ActivityPeriod; label: string }> = [
  { value: '12m', label: '12 months' },
  { value: '24m', label: '24 months' },
  { value: '5y', label: '5 years' },
  { value: 'all', label: 'All time' },
]

type Props = { onOpenProject?: (repositoryId: string) => void }

export function ActivityView({ onOpenProject }: Props) {
  const [period, setPeriod] = useState<ActivityPeriod>('12m')
  const [metric, setMetric] = useState<ActivityMetric>('lines')
  const [colourBy, setColourBy] = useState<ActivityColourBy>('projectType')
  const { scope, pushScope } = useAnalysisScope()
  const activity = useActivityView(period, scope)

  return <>
    {hasActiveScope(scope) ? <>
      <p className="eyebrow activity-scope-label">Analysis scope carried from Explore</p>
      <AnalysisFilters scope={scope} onChange={pushScope} />
    </> : null}
    <div className="view-toolbar timeline-toolbar">
      <div><p className="eyebrow">Measured activity</p><h2>Development activity</h2></div>
      <div className="timeline-controls activity-controls" aria-label="Activity controls">
        <label><span>Activity window</span><select value={period} onChange={event => setPeriod(event.target.value as ActivityPeriod)}>{periods.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        <label><span>Measure</span><select value={metric} onChange={event => setMetric(event.target.value as ActivityMetric)}><option value="lines">Changed lines</option><option value="commits">Commits</option></select></label>
        <label><span>Colour by</span><select value={colourBy} onChange={event => setColourBy(event.target.value as ActivityColourBy)}><option value="projectType">Project type</option><option value="technology">Technology</option><option value="none">Single colour</option></select></label>
      </div>
    </div>
    {activity.status === 'loading' ? <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading activity…</p></section> : null}
    {activity.status === 'error' ? <section className="dashboard-error"><h2>Activity data could not be loaded.</h2><p>{activity.error}</p></section> : null}
    {activity.status === 'ready' ? (hasActiveScope(scope) && activity.data.commitCount === 0 ? <AnalysisEmptyState title="No activity matches the current selection." description="Broaden the analysis selection to see activity again." scope={scope} onScopeChange={pushScope} /> : <ActivityContent key={period} data={activity.data} metric={metric} colourBy={colourBy} onOpenProject={onOpenProject}/>) : null}
  </>
}

function ActivityContent({ data, metric, colourBy, onOpenProject }: { data: ActivityData; metric: ActivityMetric; colourBy: ActivityColourBy; onOpenProject?: (repositoryId: string) => void }) {
  const lineCoverage = data.commitCount > 0 ? Math.round((data.lineStatisticsCommitCount / data.commitCount) * 100) : 0

  return <>
    {metric === 'lines' && lineCoverage < 100 ? <p className="timeline-coverage-note">Changed-line activity is based on commits where additions/deletions are available ({data.lineStatisticsCommitCount.toLocaleString()} of {data.commitCount.toLocaleString()} commits, {lineCoverage}%). Switch to commits for complete activity coverage.</p> : null}

    <ActivityTimelineAnalysis data={data} metric={metric} colourBy={colourBy} onOpenProject={onOpenProject}/>

    <section className="secondary-summary-section" aria-labelledby="activity-summary-heading">
      <span className="card-kicker">Summary</span>
      <h2 id="activity-summary-heading">Activity statistics</h2>
      <SummaryFacts
        ariaLabel="Activity summary"
        items={[
          { label: 'Commits', value: fmt(data.commitCount) },
          { label: 'Active projects', value: fmt(data.activeProjects) },
          { label: 'Average commit size', value: data.commitSizeStatisticsAvailable ? `${fmt(Math.round(data.averageCommitSize))} lines` : 'Not available' },
          { label: 'Additions', value: data.commitSizeStatisticsAvailable ? `+${fmt(data.additions)}` : 'Not available' },
          { label: 'Deletions', value: data.commitSizeStatisticsAvailable ? `−${fmt(data.deletions)}` : 'Not available' },
          { label: 'Activity period', value: data.firstActivityAt && data.lastActivityAt ? `${formatDate(data.firstActivityAt)} – ${formatDate(data.lastActivityAt)}` : 'No activity in selected period' },
        ]}
      />
    </section>

  </>
}


const fmt = (value: number) => new Intl.NumberFormat().format(value)
const formatDate = (value: string) => new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))

function hasActiveScope(scope: AnalysisScope): boolean {
  return scope.technologies.length > 0
    || scope.projectTypes.length > 0
    || Boolean(scope.ownership)
    || Boolean(scope.visibility)
    || Boolean(scope.from)
    || Boolean(scope.to)
    || scope.year !== undefined
    || Boolean(scope.month)
    || Boolean(scope.week)
    || Boolean(scope.search)
}
