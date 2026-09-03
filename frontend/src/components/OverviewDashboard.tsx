import { useOverviewDashboard } from '../hooks/useOverviewDashboard'
import { SyncMonitoringPanel } from './SyncMonitoringPanel'
import { SummaryFacts } from './SummaryFacts'

type Props = {
  displayName: string
}

export function OverviewDashboard({ displayName }: Props) {
  const overview = useOverviewDashboard(true)

  if (overview.status === 'loading') {
    return (
      <section className="dashboard-loading" aria-live="polite">
        <div className="loading-indicator" aria-hidden="true" />
        <p>Loading overview…</p>
      </section>
    )
  }

  if (overview.status === 'error') {
    return (
      <section className="dashboard-error" role="alert">
        <h2>Overview data could not be loaded.</h2>
        <p>{overview.error}</p>
      </section>
    )
  }

  const data = overview.data

  return (
    <>
      <section className="welcome-panel">
        <p>Welcome back, {displayName}.</p>
        <h2>Your development history at a glance.</h2>
        <p>
          This overview separates measured repository activity from inferred technologies,
          project types and significance.
        </p>
      </section>

      <SummaryFacts
        ariaLabel="Repository overview"
        className="overview-summary-facts"
        items={[
          { label: 'Repositories analysed', value: data.repositoriesAnalysed },
          { label: 'Own / external', value: `${data.ownRepositories} / ${data.externalRepositories}` },
          { label: 'Public / private', value: `${data.publicRepositories} / ${data.privateRepositories}` },
          { label: 'Commits observed', value: data.commits },
          { label: 'Active projects', value: data.activeProjects },
          { label: 'Activity period', value: formatPeriod(data.firstActivityAt, data.lastActivityAt) },
        ]}
      />

      <SyncMonitoringPanel />

      <section className="dashboard-section" aria-labelledby="technology-heading">
        <div className="section-heading-row">
          <div>
            <span className="card-kicker">Evidence</span>
            <h2 id="technology-heading">Key technologies</h2>
          </div>
        </div>
        {data.keyTechnologies.length > 0 ? (
          <div className="chip-list">
            {data.keyTechnologies.map((technology) => (
              <div className="evidence-chip" key={technology.technologyKey}>
                <strong>{technology.technologyName}</strong>
                <span>{technology.evidenceLevel}</span>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState text="No technology assessments yet." />
        )}
      </section>

      <section className="dashboard-section" aria-labelledby="categories-heading">
        <div className="section-heading-row">
          <div>
            <span className="card-kicker">Classification</span>
            <h2 id="categories-heading">Project categories</h2>
          </div>
        </div>
        {data.projectCategories.length > 0 ? (
          <div className="chip-list">
            {data.projectCategories.map((category) => (
              <div className="evidence-chip" key={category.categoryKey}>
                <strong>{category.categoryName}</strong>
                <span>{category.confidence}</span>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState text="No project classifications yet." />
        )}
      </section>

      <section className="dashboard-section" aria-labelledby="significant-heading">
        <div className="section-heading-row">
          <div>
            <span className="card-kicker">Projects</span>
            <h2 id="significant-heading">Significant external projects</h2>
          </div>
        </div>
        {data.significantProjects.length > 0 ? (
          <div className="project-list">
            {data.significantProjects.map((project) => (
              <article className="project-row" key={project.repositoryId}>
                <div>
                  <h3>{project.repositoryName}</h3>
                  <p>{labelReason(project.matchReason)}</p>
                </div>
                <div className="project-scores" aria-label="Project scores">
                  <span>Significance {project.significanceScore}</span>
                  <span>Involvement {project.involvementScore}</span>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState text="No significant external projects identified yet." />
        )}
      </section>
    </>
  )
}

function EmptyState({ text }: { text: string }) {
  return <p className="empty-state">{text}</p>
}

function formatPeriod(first: string | null, last: string | null) {
  if (!first || !last) return 'No activity yet'

  const formatter = new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
  })

  return `${formatter.format(new Date(first))} – ${formatter.format(new Date(last))}`
}

function labelReason(reason: string) {
  switch (reason) {
    case 'BOTH':
      return 'High project significance and high user involvement'
    case 'PROJECT_SIGNIFICANCE':
      return 'High project significance'
    case 'USER_INVOLVEMENT':
      return 'High user involvement'
    default:
      return reason
  }
}
