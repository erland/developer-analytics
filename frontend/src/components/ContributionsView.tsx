import { useMemo } from 'react'
import { rollingAnalysisPeriodOptions } from '../analysis/AnalysisPeriodPresets'
import { useContributions } from '../hooks/useContributions'
import { useAnalysisScope } from '../hooks/useAnalysisScope'
import { useProjectTypes } from '../hooks/useProjectTypes'
import { useTechnologyViews } from '../hooks/useTechnologyViews'
import { AnalysisFilters } from './AnalysisFilters'
import { MatchingProjects } from './MatchingProjects'
import { SummaryFacts } from './SummaryFacts'

export function ContributionsView() {
  const { scope, pushScope } = useAnalysisScope()
  const state = useContributions(scope)
  const technologies = useTechnologyViews()
  const projectTypes = useProjectTypes()
  const periods = useMemo(() => rollingAnalysisPeriodOptions(new Date()), [])

  const technologyOptions = useMemo(() => technologies.status === 'ready'
    ? technologies.data.map((technology) => ({
        value: technology.technologyKey,
        label: technology.technologyName,
        count: technology.projectCount,
      }))
    : [], [technologies])

  const projectTypeOptions = useMemo(() => projectTypes.status === 'ready'
    ? projectTypes.data.map((projectType) => ({
        value: projectType.categoryKey,
        label: projectType.categoryName,
        count: projectType.projectCount,
      }))
    : [], [projectTypes])


  return <>
    <div className="view-toolbar">
      <div><p className="eyebrow">Measured contribution history</p><h2>Contributions</h2></div>
    </div>
    <AnalysisFilters
      scope={scope}
      technologies={technologyOptions}
      projectTypes={projectTypeOptions}
      periods={periods}
      showTechnology
      showProjectType
      showOwnership
      showPeriod
      onChange={pushScope}
    />
    {state.status === 'loading' ? <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading contributions…</p></section> : null}
    {state.status === 'error' ? <section className="dashboard-error"><h2>Contributions could not be loaded.</h2><p>{state.error}</p></section> : null}
    <MatchingProjects scope={scope} onScopeChange={pushScope} />
    {state.status === 'ready' ? <ContributionContent data={state.data} /> : null}
  </>
}

function ContributionContent({ data }: { data: import('../hooks/useContributions').ContributionSummary }) {
  return <section className="secondary-summary-section" aria-labelledby="contribution-summary-heading">
    <span className="card-kicker">Summary</span>
    <h2 id="contribution-summary-heading">Contribution statistics</h2>
    <SummaryFacts
      ariaLabel="Contribution summary"
      items={[
        { label: 'Commits', value: number(data.commits) },
        { label: 'Pull requests', value: number(data.pullRequests) },
        { label: 'Reviews', value: number(data.reviews) },
        { label: 'Issues', value: number(data.issues) },
      ]}
    />
  </section>
}
const number = (value: number) => new Intl.NumberFormat().format(value)
