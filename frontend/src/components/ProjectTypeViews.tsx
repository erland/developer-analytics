import { useEffect, useMemo } from 'react'
import { createAnalysisScope, type AnalysisScope } from '../analysis/AnalysisScope'
import { nonEmptyActivityPeriods } from '../analysis/AnalysisTimeline'
import { type ProjectTypeView, useProjectTypes } from '../hooks/useProjectTypes'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { AnalysisEmptyState } from './AnalysisEmptyState'
import { AnalysisFilters } from './AnalysisFilters'
import { DrilldownTimeChart } from './DrilldownTimeChart'
import { MatchingProjects } from './MatchingProjects'

export function ProjectTypeViews() {
  const projectTypes = useProjectTypes()
  const { scope, pushScope, replaceScope } = useAnalysisScope()

  const selected = useMemo(() => {
    if (projectTypes.status !== 'ready') return null
    const selectedKey = scope.projectTypes[0]
    if (!selectedKey) return projectTypes.data[0] ?? null
    return projectTypes.data.find((item) => item.categoryKey === selectedKey) ?? projectTypes.data[0] ?? null
  }, [projectTypes, scope.projectTypes])

  const projectTypeOptions = useMemo(() => {
    if (projectTypes.status !== 'ready') return []
    return projectTypes.data.map((projectType) => ({
      value: projectType.categoryKey,
      label: projectType.categoryName,
      count: projectType.projectCount,
    }))
  }, [projectTypes])

  useEffect(() => {
    if (projectTypes.status !== 'ready' || projectTypes.data.length === 0) return

    const requestedKey = scope.projectTypes[0]
    if (!requestedKey) return

    const requestedExists = projectTypes.data.some((item) => item.categoryKey === requestedKey)
    if (!requestedExists) {
      replaceScope(createAnalysisScope({ ...scope, projectTypes: [projectTypes.data[0].categoryKey] }))
    }
  }, [projectTypes, replaceScope, scope])

  if (projectTypes.status === 'loading') return <section className="dashboard-loading" aria-live="polite"><div className="loading-indicator" aria-hidden="true" /><p>Loading project types…</p></section>
  if (projectTypes.status === 'error') return <section className="dashboard-error" role="alert"><h2>Project type data could not be loaded.</h2><p>{projectTypes.error}</p></section>

  return <>
    <div className="view-toolbar"><div><p className="eyebrow">Project classification</p><h2>Project types</h2></div><span className="inventory-count">{projectTypes.data.length} categories</span></div>
    {projectTypes.data.length === 0 ? <section className="empty-inventory"><h3>No project categories yet.</h3><p>Run project classification before viewing project-type trends.</p></section> : <>
      <AnalysisFilters
        scope={scope}
        projectTypes={projectTypeOptions}
        showProjectType
        onChange={pushScope}
      />
      {selected ? <ProjectTypeDetail key={selected.categoryKey} item={selected} scope={scope} onScopeChange={pushScope} /> : null}
    </>}
  </>
}

function ProjectTypeDetail({
  item,
  scope,
  onScopeChange,
}: {
  item: ProjectTypeView
  scope: AnalysisScope
  onScopeChange: (scope: AnalysisScope) => void
}) {
  const activeTimeline = nonEmptyActivityPeriods(item.timeline)
  const hasPeriodFilter = Boolean(scope.from || scope.to || scope.year !== undefined || scope.month || scope.week)

  return <div className="technology-detail project-type-detail">
    <section className="project-detail-hero technology-summary project-type-summary">
      <div>
        <p className="eyebrow">Project category</p>
        <h2>{item.categoryName}</h2>
        <p>Evolution and activity for projects classified in this category. A repository may belong to more than one category.</p>
        <p className="technology-summary-facts" aria-label="Project type summary statistics">
          <span>{item.projectCount} project{item.projectCount === 1 ? '' : 's'}</span>
          <span>{item.activityCount} observed commit{item.activityCount === 1 ? '' : 's'}</span>
          <span>{activeTimeline.length} timeline month{activeTimeline.length === 1 ? '' : 's'}</span>
        </p>
      </div>
    </section>
    <section className="dashboard-section project-type-over-time">
      <span className="card-kicker">Over time</span>
      <h2>Activity in {item.categoryName} projects</h2>
      {activeTimeline.length ? (
        <DrilldownTimeChart
          points={activeTimeline.map(point => ({
              month: point.month.slice(0, 7),
              commits: point.commits,
              changedLines: point.changedLines,
              lineStatisticsCommitCount: point.lineStatisticsCommitCount,
              secondary: `${point.activeProjectCount} active project${point.activeProjectCount === 1 ? '' : 's'}`,
            }))}
          emptyText="No activity yet in projects matching this project type."
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
          description={`No recorded activity was found in ${item.categoryName} projects${hasPeriodFilter ? ' during the selected period' : ''}.`}
          scope={scope}
          onScopeChange={hasPeriodFilter ? onScopeChange : undefined}
          className="analysis-empty-state-inline"
        />
      )}
    </section>
    <MatchingProjects scope={scope} onScopeChange={onScopeChange} />
    <details className="dashboard-section technology-evidence-details project-type-details">
      <summary>Classification statistics</summary>
      <div className="technology-evidence-content">
        <p className="settings-intro">Classification statistics are kept secondary so activity over time and the projects behind it remain the primary analysis.</p>
        <dl className="technology-evidence-grid">
          <div><dt>Projects</dt><dd>{item.projectCount}</dd></div>
          <div><dt>Observed commits</dt><dd>{item.activityCount}</dd></div>
          <div><dt>Timeline months</dt><dd>{activeTimeline.length}</dd></div>
        </dl>
      </div>
    </details>
  </div>
}
