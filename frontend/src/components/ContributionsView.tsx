import { useContributions } from '../hooks/useContributions'

export function ContributionsView({ onOpenProject }: { onOpenProject: (id: string) => void }) {
  const state = useContributions()
  if (state.status === 'loading') return <section className="dashboard-loading"><div className="loading-indicator"/><p>Loading contributions…</p></section>
  if (state.status === 'error') return <section className="dashboard-error"><h2>Contributions could not be loaded.</h2><p>{state.error}</p></section>
  const data = state.data
  return <>
    <section className="metric-grid">
      <Metric label="Commits" value={data.commits}/>
      <Metric label="Pull requests" value={data.pullRequests}/>
      <Metric label="Reviews" value={data.reviews}/>
      <Metric label="Issues" value={data.issues}/>
    </section>
    <section className="dashboard-section">
      <span className="card-kicker">Recent work</span>
      <h2>Recently active projects</h2>
      {data.recentProjects.length ? <div className="project-list">
        {data.recentProjects.map(project => <article className="project-row" key={project.repositoryId}>
          <div>
            <h3><button className="project-detail-link" type="button" onClick={() => onOpenProject(project.repositoryId)}>{project.repositoryName}</button></h3>
            <p>{number(project.commitCount)} commits · {number(project.contributionCount)} total contributions</p>
          </div>
          <span className="representative-date">{date(project.lastActivityAt)}</span>
        </article>)}
      </div> : <p className="empty-state">No contributions have been collected yet.</p>}
    </section>
  </>
}

function Metric({ label, value }: { label: string; value: number }) {
  return <article className="metric-card"><span>{label}</span><strong>{number(value)}</strong></article>
}
const number = (value: number) => new Intl.NumberFormat().format(value)
const date = (value: string) => new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value))
