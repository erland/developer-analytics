import { useEffect, useMemo, useState } from 'react'
import { createAnalysisScope, type AnalysisScope } from '../analysis/AnalysisScope'
import { nonEmptyActivityPeriods } from '../analysis/AnalysisTimeline'
import { type TechnologyView, useTechnologyViews } from '../hooks/useTechnologyViews'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { setTechnologySuppressed } from '../hooks/useCorrections'
import { AnalysisEmptyState } from './AnalysisEmptyState'
import { AnalysisFilters } from './AnalysisFilters'
import { DrilldownTimeChart } from './DrilldownTimeChart'
import { MatchingProjects } from './MatchingProjects'

export function TechnologyViews() {
  const technologies = useTechnologyViews()
  const { scope, pushScope, replaceScope } = useAnalysisScope()
  const [defaultSelectionInitialized, setDefaultSelectionInitialized] = useState(false)

  const selected = useMemo(() => {
    if (technologies.status !== 'ready') return null
    const selectedKey = scope.technologies[0]
    if (!selectedKey) return null
    return technologies.data.find((item) => item.technologyKey === selectedKey) ?? technologies.data[0] ?? null
  }, [scope.technologies, technologies])

  const technologyOptions = useMemo(() => {
    if (technologies.status !== 'ready') return []
    return technologies.data.map((technology) => ({
      value: technology.technologyKey,
      label: technology.technologyName,
      count: technology.projectCount,
    }))
  }, [technologies])

  useEffect(() => {
    if (technologies.status !== 'ready' || technologies.data.length === 0) return

    const requestedKey = scope.technologies[0]
    const requestedExists = requestedKey
      ? technologies.data.some((item) => item.technologyKey === requestedKey)
      : false

    if (!defaultSelectionInitialized) {
      setDefaultSelectionInitialized(true)
      if (!requestedKey || !requestedExists) {
        replaceScope(createAnalysisScope({ ...scope, technologies: [technologies.data[0].technologyKey] }))
      }
      return
    }

    if (requestedKey && !requestedExists) {
      replaceScope(createAnalysisScope({ ...scope, technologies: [technologies.data[0].technologyKey] }))
    }
  }, [defaultSelectionInitialized, replaceScope, scope, technologies])

  if (technologies.status === 'loading') return <section className="dashboard-loading" aria-live="polite"><div className="loading-indicator" aria-hidden="true" /><p>Loading technologies…</p></section>
  if (technologies.status === 'error') return <section className="dashboard-error" role="alert"><h2>Technology data could not be loaded.</h2><p>{technologies.error}</p></section>
  return <>
    <div className="view-toolbar"><div><p className="eyebrow">Evidence-based technology history</p><h2>Technologies</h2></div><span className="inventory-count">{technologies.data.length} technologies</span></div>
    {technologies.data.length === 0 ? <section className="empty-inventory"><h3>No technology assessments yet.</h3><p>Collect repository evidence and recalculate technology assessments first.</p></section> : <>
      <AnalysisFilters
        scope={scope}
        technologies={technologyOptions}
        showTechnology
        onChange={pushScope}
      />
      {selected ? <TechnologyDetail key={selected.technologyKey} technology={selected} scope={scope} onScopeChange={pushScope} /> : null}
    </>}
  </>
}

function TechnologyDetail({
  technology,
  scope,
  onScopeChange,
}: {
  technology: TechnologyView
  scope: AnalysisScope
  onScopeChange: (scope: AnalysisScope) => void
}) {
  const activeTimeline = nonEmptyActivityPeriods(technology.timeline)
  const hasPeriodFilter = Boolean(scope.from || scope.to || scope.year !== undefined || scope.month || scope.week)

  return <div className="technology-detail">
    <section className="project-detail-hero technology-summary">
      <div>
        <p className="eyebrow">Technology evidence</p>
        <h2>{technology.technologyName}</h2>
        <p>Evidence level {technology.evidenceLevel.toLowerCase()} based on observed repository signals. This is not a formal proficiency rating.</p>
        <p className="technology-summary-facts" aria-label="Technology summary statistics">
          <span>{technology.projectCount} project{technology.projectCount === 1 ? '' : 's'}</span>
          <span>{formatObservationRange(technology.firstObservedAt, technology.lastObservedAt)}</span>
          <span>{technology.evidenceCount} evidence item{technology.evidenceCount === 1 ? '' : 's'}</span>
          <span>{technology.independentEvidenceTypes} evidence type{technology.independentEvidenceTypes === 1 ? '' : 's'}</span>
          <span>{technology.recentProjectCount} recent project{technology.recentProjectCount === 1 ? '' : 's'}</span>
        </p>
        <span className="privacy-provenance">{privacyLabel(technology.privacyProvenance)}</span>
      </div>
      <div className="technology-score"><strong>{technology.evidenceScore}</strong><span>evidence score</span></div>
    </section>
    <section className="dashboard-section technology-over-time">
      <span className="card-kicker">Over time</span>
      <h2>Activity in projects using {technology.technologyName}</h2>
      <p className="settings-intro technology-timeline-semantics">
        Shows your activity in projects where {technology.technologyName} has been observed. It does not imply that every commit or changed line in the period used {technology.technologyName}.
      </p>
      {activeTimeline.length ? (
        <DrilldownTimeChart
          points={activeTimeline.map(point => ({ month: point.month.slice(0, 7), commits: point.commits, changedLines: point.changedLines, lineStatisticsCommitCount: point.lineStatisticsCommitCount, secondary: `${point.projectCount} project${point.projectCount === 1 ? '' : 's'}` }))}
          emptyText="No activity yet in projects where this technology has been observed."
          year={scope.year}
          month={scope.month}
          onYearChange={(year) => onScopeChange(createAnalysisScope({
            ...scope,
            from: undefined,
            to: undefined,
            year,
            month: undefined,
            week: undefined,
          }))}
          onMonthChange={(month) => onScopeChange(createAnalysisScope({
            ...scope,
            from: undefined,
            to: undefined,
            year: month ? Number(month.slice(0, 4)) : scope.year,
            month,
            week: undefined,
          }))}
        />
      ) : (
        <AnalysisEmptyState
          title="No activity over time for this selection."
          description={`No recorded activity was found in projects where ${technology.technologyName} has been observed${hasPeriodFilter ? ' during the selected period' : ''}.`}
          scope={scope}
          onScopeChange={hasPeriodFilter ? onScopeChange : undefined}
          className="analysis-empty-state-inline"
        />
      )}
    </section>
    <MatchingProjects scope={scope} onScopeChange={onScopeChange} />
    <details className="dashboard-section technology-evidence-details">
      <summary>Evidence and statistics</summary>
      <div className="technology-evidence-content">
        <p className="settings-intro">Supporting assessment details are kept secondary so the timeline and matching projects remain the primary analysis.</p>
        <dl className="technology-evidence-grid">
          <div><dt>Evidence level</dt><dd>{technology.evidenceLevel}</dd></div>
          <div><dt>Evidence score</dt><dd>{technology.evidenceScore}</dd></div>
          <div><dt>Projects</dt><dd>{technology.projectCount}</dd></div>
          <div><dt>Evidence items</dt><dd>{technology.evidenceCount}</dd></div>
          <div><dt>Evidence types</dt><dd>{technology.independentEvidenceTypes}</dd></div>
          <div><dt>Recent projects</dt><dd>{technology.recentProjectCount}</dd></div>
          <div><dt>First observed</dt><dd>{formatDate(technology.firstObservedAt)}</dd></div>
          <div><dt>Latest observed</dt><dd>{formatDate(technology.lastObservedAt)}</dd></div>
          <div><dt>Data provenance</dt><dd>{privacyLabel(technology.privacyProvenance)}</dd></div>
        </dl>
      </div>
    </details>
    <details className="dashboard-section correction-panel technology-advanced-details">
      <summary>Advanced</summary>
      <div className="technology-advanced-content">
        <span className="card-kicker">Correction</span>
        <h2>Technology inference</h2>
        <p className="settings-intro">Suppress this inference only when the technology assessment is misleading. Suppression removes it from analysis views and AI profile conclusions while retaining the underlying repository evidence.</p>
        <button className="secondary-action" type="button" onClick={async () => { await setTechnologySuppressed(technology.technologyKey, true); window.location.reload() }}>Suppress technology inference</button>
      </div>
    </details>
  </div>
}
function formatDate(value: string | null) { if (!value) return 'Unknown'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short' }).format(new Date(value)) }
function formatObservationRange(first: string | null, last: string | null) { return `Observed ${formatDate(first)} – ${formatDate(last)}` }
function privacyLabel(value: TechnologyView['privacyProvenance']) { if (value === 'PRIVATE_AGGREGATE') return 'Private aggregate'; if (value === 'INCLUDES_PRIVATE') return 'Includes private data'; return 'Public data only' }
