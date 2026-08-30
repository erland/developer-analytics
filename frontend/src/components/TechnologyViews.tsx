import { useMemo, useState } from 'react'
import { type TechnologyView, useTechnologyViews } from '../hooks/useTechnologyViews'

export function TechnologyViews() {
  const technologies = useTechnologyViews()
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

  const selected = useMemo(() => {
    if (technologies.status !== 'ready') return null
    if (!selectedKey) return technologies.data[0] ?? null
    return technologies.data.find((item) => item.technologyKey === selectedKey) ?? null
  }, [selectedKey, technologies])

  if (technologies.status === 'loading') {
    return (
      <section className="dashboard-loading" aria-live="polite">
        <div className="loading-indicator" aria-hidden="true" />
        <p>Loading technologies…</p>
      </section>
    )
  }

  if (technologies.status === 'error') {
    return (
      <section className="dashboard-error" role="alert">
        <h2>Technology data could not be loaded.</h2>
        <p>{technologies.error}</p>
      </section>
    )
  }

  return (
    <>
      <div className="view-toolbar">
        <div>
          <p className="eyebrow">Evidence-based technology history</p>
          <h2>Technologies</h2>
        </div>
        <span className="inventory-count">{technologies.data.length} technologies</span>
      </div>

      {technologies.data.length === 0 ? (
        <section className="empty-inventory">
          <h3>No technology assessments yet.</h3>
          <p>Collect repository evidence and recalculate technology assessments first.</p>
        </section>
      ) : (
        <div className="technology-layout">
          <section className="technology-list" aria-label="Technology list">
            {technologies.data.map((technology) => (
              <button
                type="button"
                key={technology.technologyKey}
                className={`technology-list-item ${
                  selected?.technologyKey === technology.technologyKey
                    ? 'technology-list-item-active'
                    : ''
                }`}
                onClick={() => setSelectedKey(technology.technologyKey)}
              >
                <div>
                  <strong>{technology.technologyName}</strong>
                  <span>{humanize(technology.technologyCategory)}</span>
                </div>
                <div className="technology-list-meta">
                  <strong>{technology.evidenceLevel}</strong>
                  <span>{technology.projectCount} projects</span>
                </div>
              </button>
            ))}
          </section>

          {selected ? <TechnologyDetail technology={selected} /> : null}
        </div>
      )}
    </>
  )
}

function TechnologyDetail({ technology }: { technology: TechnologyView }) {
  const maxActivity = Math.max(
    1,
    ...technology.timeline.map((point) => point.activityCount),
  )

  return (
    <div className="technology-detail">
      <section className="project-detail-hero">
        <div>
          <p className="eyebrow">Technology evidence</p>
          <h2>{technology.technologyName}</h2>
          <p>
            Evidence level {technology.evidenceLevel.toLowerCase()} based on observed
            repository signals. This is not a formal proficiency rating.
          </p>
        </div>
        <div className="technology-score">
          <strong>{technology.evidenceScore}</strong>
          <span>evidence score</span>
        </div>
      </section>

      <section className="metric-grid">
        <Metric label="Projects" value={technology.projectCount} />
        <Metric label="Evidence items" value={technology.evidenceCount} />
        <Metric label="Evidence types" value={technology.independentEvidenceTypes} />
        <Metric label="Recent projects" value={technology.recentProjectCount} />
        <Metric label="First observed" value={formatDate(technology.firstObservedAt)} compact />
        <Metric label="Latest observed" value={formatDate(technology.lastObservedAt)} compact />
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Timeline</span>
        <h2>Activity over time</h2>
        <div className="bar-chart">
          {technology.timeline.length ? (
            technology.timeline.map((point) => (
              <div className="bar-row" key={point.month}>
                <div className="bar-label">
                  <strong>{formatMonth(point.month)}</strong>
                  <span>{point.projectCount} project{point.projectCount === 1 ? '' : 's'}</span>
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
            <p className="empty-state">No timeline aggregate yet.</p>
          )}
        </div>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Representative work</span>
        <h2>Representative projects</h2>
        {technology.representativeProjects.length ? (
          <div className="project-list">
            {technology.representativeProjects.map((project) => (
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
                    {project.visibility.toLowerCase()} · {project.evidenceCount} evidence item
                    {project.evidenceCount === 1 ? '' : 's'}
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
  compact = false,
}: {
  label: string
  value: string | number
  compact?: boolean
}) {
  return (
    <article className="metric-card">
      <span>{label}</span>
      <strong className={compact ? 'metric-value-compact' : undefined}>{value}</strong>
    </article>
  )
}

function ownershipLabel(value: string) {
  return value === 'OWNED_BY_USER' ? 'own' : 'external'
}

function humanize(value: string) {
  return value.toLowerCase().replaceAll('_', ' ')
}

function formatDate(value: string | null) {
  if (!value) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
  }).format(new Date(value))
}

function formatMonth(value: string) {
  const normalized = value.length === 7 ? `${value}-01` : value
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    year: '2-digit',
  }).format(new Date(`${normalized}T00:00:00Z`))
}
