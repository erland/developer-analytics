import { useState } from 'react'
import { type ProjectDetail, useProjectDetail } from '../hooks/useProjectDetail'
import { setCategoryRejected, setProjectExcludedFromAiProfile } from '../hooks/useCorrections'
import { useSyncMonitoring } from '../hooks/useSyncMonitoring'

export function ProjectDetailView({
  repositoryId,
  onBack,
}: {
  repositoryId: string
  onBack: () => void
}) {
  const detail = useProjectDetail(repositoryId)

  return (
    <>
      <button className="text-button detail-back" type="button" onClick={onBack}>
        ← Back to projects
      </button>

      {detail.status === 'loading' ? (
        <section className="dashboard-loading" aria-live="polite">
          <div className="loading-indicator" aria-hidden="true" />
          <p>Loading project details…</p>
        </section>
      ) : null}

      {detail.status === 'error' ? (
        <section className="dashboard-error" role="alert">
          <h2>Project detail could not be loaded.</h2>
          <p>{detail.error}</p>
        </section>
      ) : null}

      {detail.status === 'ready' ? <Detail data={detail.data} repositoryId={repositoryId} /> : null}
    </>
  )
}

function Detail({ data, repositoryId }: { data: ProjectDetail; repositoryId: string }) {
  const [refreshing, setRefreshing] = useState(false)
  const [refreshMessage, setRefreshMessage] = useState<string | null>(null)
  const syncMonitoring = useSyncMonitoring(repositoryId)
  const maxCommits = Math.max(1, ...data.activity.timeline.map((point) => point.commits))



async function refreshAnalysis() {
  setRefreshing(true)
  setRefreshMessage(null)
  try {
    const response = await fetch(`/api/me/sync/github/repositories/${repositoryId}/refresh-analysis`, {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) {
      throw new Error(`Refresh request failed with HTTP ${response.status}`)
    }
    setRefreshMessage('Repository analysis queued. Refresh this view after the background jobs complete.')
  } catch (error) {
    setRefreshMessage(error instanceof Error ? error.message : 'Unable to refresh repository analysis')
  } finally {
    setRefreshing(false)
  }
}

async function toggleCategory(categoryKey: string, rejected: boolean) {
  await setCategoryRejected(repositoryId, categoryKey, !rejected)
  window.location.reload()
}

async function toggleAiProfileExclusion() {
  await setProjectExcludedFromAiProfile(
    repositoryId,
    !data.metadata.excludedFromAiProfile,
  )
  window.location.reload()
}

  return (
    <>
      <section className="project-detail-hero">
        <div>
          <p className="eyebrow">Project detail</p>
          <h2>{data.metadata.name}</h2>
          <p>{data.metadata.description || 'No repository description.'}</p>
        </div>
        <div className="inventory-badges">
          <span>{data.metadata.visibility.toLowerCase()}</span>
          <span>{ownershipLabel(data.metadata.ownershipRelation)}</span>
          {data.metadata.fork ? <span>fork</span> : null}
          {data.metadata.archived ? <span>archived</span> : null}
        </div>
      </section>

      <section className="metric-grid">
        <Metric label="Commits" value={data.activity.commits} />
        <Metric label="Pull requests" value={data.activity.pullRequests} />
        <Metric label="Reviews" value={data.activity.reviews} />
        <Metric label="Issues" value={data.activity.issues} />
        <Metric label="Additions" value={`+${formatNumber(data.activity.additions)}`} />
        <Metric label="Deletions" value={`−${formatNumber(data.activity.deletions)}`} />
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Metadata</span>
        <h2>Repository</h2>
        <dl className="detail-grid">
          <DetailItem label="Provider" value={data.metadata.provider} />
          <DetailItem label="Full name" value={data.metadata.fullName ?? 'Unknown'} />
          <DetailItem label="Owner" value={data.metadata.ownerLogin ?? 'Unknown'} />
          <DetailItem label="Last activity" value={formatDate(data.metadata.lastActivityAt)} />
        </dl>
        {data.metadata.topics.length ? (
          <div className="chip-list">
            {data.metadata.topics.map((topic) => (
              <span className="inventory-tag" key={topic}>{topic}</span>
            ))}
          </div>
        ) : null}
        {data.metadata.htmlUrl ? (
          <p className="detail-link">
            <a href={data.metadata.htmlUrl} target="_blank" rel="noreferrer">
              Open repository ↗
            </a>
          </p>
        ) : null}
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Activity</span>
        <h2>Commit timeline</h2>
        <div className="bar-chart">
          {data.activity.timeline.length ? data.activity.timeline.map((point) => (
            <div className="bar-row" key={point.month}>
              <div className="bar-label"><strong>{formatMonth(point.month)}</strong></div>
              <div className="bar-track" aria-hidden="true">
                <div
                  className="bar-fill"
                  style={{ width: `${Math.max(2, Math.round((point.commits / maxCommits) * 100))}%` }}
                />
              </div>
              <span className="bar-value">{point.commits}</span>
            </div>
          )) : <p className="empty-state">No commit activity recorded.</p>}
        </div>
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Evidence</span>
        <h2>Technologies</h2>
        {data.technologies.length ? (
          <div className="detail-evidence-list">
            {data.technologies.map((item, index) => (
              <article className="detail-evidence-row" key={`${item.technologyKey}-${item.evidenceType}-${index}`}>
                <div>
                  <strong>{item.technologyName}</strong>
                  <span>{item.evidenceType} · {item.strength}</span>
                </div>
                <span>{item.sourceValue || 'Observed evidence'}</span>
              </article>
            ))}
          </div>
        ) : <p className="empty-state">No technology evidence yet.</p>}
      </section>

      <section className="dashboard-section">
        <span className="card-kicker">Classification</span>
        <h2>Project categories</h2>
        {data.categories.length ? (
          <div className="chip-list">
            {data.categories.map((category) => (
              <div
                className={`evidence-chip ${category.rejectedByUser ? 'correction-muted' : ''}`}
                key={`${category.categoryKey}-${category.source}`}
              >
                <strong>{category.categoryName}</strong>
                <span>{category.confidence} · {category.source}</span>
                <button
                  type="button"
                  className="correction-action"
                  onClick={() => void toggleCategory(
                    category.categoryKey,
                    category.rejectedByUser,
                  )}
                >
                  {category.rejectedByUser ? 'Restore category' : 'Reject category'}
                </button>
              </div>
            ))}
          </div>
        ) : <p className="empty-state">No project categories yet.</p>}
      </section>

      <section className="detail-assessment-grid">
        <article className="dashboard-section">
          <span className="card-kicker">Project significance</span>
          <h2>{data.assessment?.significanceLevel ?? 'Not calculated'}</h2>
          {data.assessment ? <span className="privacy-provenance">{data.assessment.privacyProvenance === 'PUBLIC_ONLY' ? 'Public data only' : 'Contains private data'}</span> : null}
          <div className="detail-score">{data.assessment?.significanceScore ?? '—'}</div>
          <Rationale value={data.assessment?.significanceRationale} />
        </article>

        <article className="dashboard-section">
          <span className="card-kicker">User involvement</span>
          <h2>{data.assessment?.involvementLevel ?? 'Not calculated'}</h2>
          <div className="detail-score">{data.assessment?.involvementScore ?? '—'}</div>
          <Rationale value={data.assessment?.involvementRationale} />
        </article>
      </section>


<section className="dashboard-section">
  <span className="card-kicker">AI profile correction</span>
  <h2>Project use in AI conclusions</h2>
  <p className="settings-intro">
    This does not remove repository facts or measured statistics. It only
    controls whether this project contributes to user-level AI conclusions.
  </p>
  <button
    type="button"
    className="secondary-action"
    onClick={() => void toggleAiProfileExclusion()}
  >
    {data.metadata.excludedFromAiProfile
      ? 'Include project in AI profile'
      : 'Exclude project from AI profile'}
  </button>
</section>

      <section className="dashboard-section">
        <span className="card-kicker">Synchronisation</span>
        <h2>{data.synchronisation.status}</h2>
        <dl className="detail-grid">
          <DetailItem label="Last seen" value={formatDate(data.synchronisation.lastSeenAt)} />
          <DetailItem label="Error" value={data.synchronisation.error ?? 'None'} />
          {syncMonitoring.status === 'ready' && syncMonitoring.contributionRuns[0] ? (
            <>
              <DetailItem label="Contribution sync" value={syncMonitoring.contributionRuns[0].status} />
              <DetailItem label="Contributions processed" value={String(syncMonitoring.contributionRuns[0].contributionsSeen)} />
              <DetailItem label="Pages processed" value={String(syncMonitoring.contributionRuns[0].pagesProcessed)} />
              <DetailItem label="Latest contribution error" value={syncMonitoring.contributionRuns[0].lastError ?? 'None'} />
            </>
          ) : null}
        </dl>
        <button
          className="secondary-action"
          type="button"
          disabled={refreshing}
          onClick={() => void refreshAnalysis()}
        >
          {refreshing ? 'Queueing analysis…' : 'Refresh repository analysis'}
        </button>
        {refreshMessage ? <p className="settings-intro" role="status">{refreshMessage}</p> : null}
      </section>
    </>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return <article className="metric-card"><span>{label}</span><strong>{value}</strong></article>
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>
}

function Rationale({ value }: { value?: Record<string, unknown> }) {
  if (!value) return <p className="empty-state">No assessment available.</p>
  return (
    <dl className="rationale-list">
      {Object.entries(value).slice(0, 8).map(([key, item]) => (
        <div key={key}>
          <dt>{humanize(key)}</dt>
          <dd>{String(item)}</dd>
        </div>
      ))}
    </dl>
  )
}

function humanize(value: string) {
  return value.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())
}

function ownershipLabel(value: string) {
  return value === 'OWNED_BY_USER' ? 'own' : 'external'
}

function formatNumber(value: number) {
  return new Intl.NumberFormat().format(value)
}

function formatDate(value: string | null) {
  if (!value) return 'Unknown'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(new Date(value))
}

function formatMonth(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    year: '2-digit',
  }).format(new Date(`${value}-01T00:00:00Z`))
}
