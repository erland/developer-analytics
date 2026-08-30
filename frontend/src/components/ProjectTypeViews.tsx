import { useMemo, useState } from 'react'
import { type ProjectTypeView, useProjectTypes } from '../hooks/useProjectTypes'

export function ProjectTypeViews() {
  const projectTypes = useProjectTypes()
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

  const selected = useMemo(() => {
    if (projectTypes.status !== 'ready') return null
    if (!selectedKey) return projectTypes.data[0] ?? null
    return projectTypes.data.find((item) => item.categoryKey === selectedKey) ?? null
  }, [projectTypes, selectedKey])

  if (projectTypes.status === 'loading') {
    return (
      <section className="dashboard-loading" aria-live="polite">
        <div className="loading-indicator" aria-hidden="true" />
        <p>Loading project types…</p>
      </section>
    )
  }

  if (projectTypes.status === 'error') {
    return (
      <section className="dashboard-error" role="alert">
        <h2>Project type data could not be loaded.</h2>
        <p>{projectTypes.error}</p>
      </section>
    )
  }

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Project classification</p>
          <h2>Project types</h2>
        </div>
        <span className="inventory-count">{projectTypes.data.length} categories</span>
      </div>

      {projectTypes.data.length === 0 ? (
        <section className="empty-inventory">
          <h3>No project categories yet.</h3>
          <p>Run project classification before viewing project-type trends.</p>
        </section>
      ) : (
        <div className="technology-layout">
          <section className="technology-list" aria-label="Project type list">
            {projectTypes.data.map((item) => (
              <button
                type="button"
                key={item.categoryKey}
                className={`technology-list-item ${
                  selected?.categoryKey === item.categoryKey
                    ? 'technology-list-item-active'
                    : ''
                }`}
                onClick={() => setSelectedKey(item.categoryKey)}
              >
                <div>
                  <strong>{item.categoryName}</strong>
                  <span>{item.projectCount} projects</span>
                </div>
                <div className="technology-list-meta">
                  <strong>{item.activityCount}</strong>
                  <span>activity</span>
                </div>
              </button>
            ))}
          </section>

          {selected ? <ProjectTypeDetail item={selected} /> : null}
        </div>
      )}
    </>
  )
}

function ProjectTypeDetail({ item }: { item: ProjectTypeView }) {
  const maxActivity = Math.max(
    1,
    ...item.timeline.map((point) => point.activityCount),
  )

  return (
    <div className="technology-detail">
      <section className="project-detail-hero">
        <div>
          <p className="eyebrow">Project category</p>
          <h2>{item.categoryName}</h2>
          <p>
            Evolution and activity for projects classified in this category.
            A repository may belong to more than one category.
          </p>
        </div>
      </section>

      <section className="metric-grid">
        <Metric label="Projects" value={item.projectCount} />
        <Metric label="Observed activity" value={item.activityCount} />
        <Metric
          label="Timeline months"
          value={item.timeline.length}
        />
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Evolution</span>
        <h2>Category activity over time</h2>
        <div className="bar-chart">
          {item.timeline.length ? (
            item.timeline.map((point) => (
              <div className="bar-row" key={point.month}>
                <div className="bar-label">
                  <strong>{formatMonth(point.month)}</strong>
                  <span>
                    {point.activeProjectCount} active project
                    {point.activeProjectCount === 1 ? '' : 's'}
                  </span>
                </div>
                <div className="bar-track" aria-hidden="true">
                  <div
                    className="bar-fill"
                    style={{
                      width: `${Math.max(
                        2,
                        Math.round((point.activityCount / maxActivity) * 100),
                      )}%`,
                    }}
                  />
                </div>
                <span className="bar-value">{point.activityCount}</span>
              </div>
            ))
          ) : (
            <p className="empty-state">No activity timeline available.</p>
          )}
        </div>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Representative work</span>
        <h2>Representative projects</h2>
        {item.representativeProjects.length ? (
          <div className="project-list">
            {item.representativeProjects.map((project) => (
              <article className="project-row" key={project.repositoryId}>
                <div>
                  <h3>
                    {project.htmlUrl ? (
                      <a href={project.htmlUrl} target="_blank" rel="noreferrer">
                        {project.repositoryName}
                      </a>
                    ) : (
                      project.repositoryName
                    )}
                  </h3>
                  <p>
                    {ownershipLabel(project.ownershipRelation)} ·{' '}
                    {project.visibility.toLowerCase()} ·{' '}
                    {project.contributionCount} contribution
                    {project.contributionCount === 1 ? '' : 's'}
                  </p>
                </div>
                <span className="representative-date">
                  {formatDate(project.lastActivityAt)}
                </span>
              </article>
            ))}
          </div>
        ) : (
          <p className="empty-state">No representative projects available.</p>
        )}
      </section>
    </div>
  )
}

function Metric({
  label,
  value,
}: {
  label: string
  value: string | number
}) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  )
}

function ownershipLabel(value: string) {
  return value === 'OWNED_BY_USER' ? 'own' : 'external'
}

function formatMonth(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    year: '2-digit',
  }).format(new Date(`${value}-01T00:00:00Z`))
}

function formatDate(value: string | null) {
  if (!value) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
  }).format(new Date(value))
}
