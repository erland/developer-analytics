import { useState } from 'react'
import { type ProjectDetail, useProjectDetail } from '../hooks/useProjectDetail'
import { setProjectExcludedFromAiProfile } from '../hooks/useCorrections'
import { useSyncMonitoring } from '../hooks/useSyncMonitoring'
import { DrilldownTimeChart } from './DrilldownTimeChart'
import { SummaryFacts } from './SummaryFacts'

export function ProjectDetailView({ repositoryId, onBack }: { repositoryId: string; onBack: () => void }) {
  const detail = useProjectDetail(repositoryId)
  return <>
    <button className="text-button detail-back" type="button" onClick={onBack}>← Back to projects</button>
    {detail.status === 'loading' ? <section className="dashboard-loading" aria-live="polite"><div className="loading-indicator" aria-hidden="true" /><p>Loading project details…</p></section> : null}
    {detail.status === 'error' ? <section className="dashboard-error" role="alert"><h2>Project detail could not be loaded.</h2><p>{detail.error}</p></section> : null}
    {detail.status === 'ready' ? <Detail data={detail.data} repositoryId={repositoryId} /> : null}
  </>
}

function Detail({ data, repositoryId }: { data: ProjectDetail; repositoryId: string }) {
  const [refreshing, setRefreshing] = useState(false)
  const [refreshMessage, setRefreshMessage] = useState<string | null>(null)
  const syncMonitoring = useSyncMonitoring(repositoryId)

  async function refreshAnalysis() {
    setRefreshing(true); setRefreshMessage(null)
    try {
      const response = await fetch(`/api/me/sync/github/repositories/${repositoryId}/refresh-analysis`, { method: 'POST', credentials: 'include', headers: { Accept: 'application/json' } })
      if (!response.ok) throw new Error(`Refresh request failed with HTTP ${response.status}`)
      setRefreshMessage('Repository analysis queued. Refresh this view after the background jobs complete.')
    } catch (error) {
      setRefreshMessage(error instanceof Error ? error.message : 'Unable to refresh repository analysis')
    } finally { setRefreshing(false) }
  }

  async function toggleAiProfileExclusion() {
    await setProjectExcludedFromAiProfile(repositoryId, !data.metadata.excludedFromAiProfile)
    window.location.reload()
  }

  return <>
    <section className="project-detail-hero"><div><p className="eyebrow">Project detail</p><h2>{data.metadata.name}</h2><p>{data.metadata.description || 'No repository description.'}</p></div><div className="inventory-badges"><span>{data.metadata.visibility.toLowerCase()}</span><span>{ownershipLabel(data.metadata.ownershipRelation)}</span>{data.metadata.fork ? <span>fork</span> : null}{data.metadata.archived ? <span>archived</span> : null}</div></section>
    <SummaryFacts
      ariaLabel="Project activity summary"
      items={[
        { label: 'Commits', value: formatNumber(data.activity.commits) },
        { label: 'Pull requests', value: formatNumber(data.activity.pullRequests) },
        { label: 'Reviews', value: formatNumber(data.activity.reviews) },
        { label: 'Issues', value: formatNumber(data.activity.issues) },
        { label: 'Additions', value: `+${formatNumber(data.activity.additions)}` },
        { label: 'Deletions', value: `−${formatNumber(data.activity.deletions)}` },
      ]}
    />

    <section className="dashboard-section"><span className="card-kicker">Metadata</span><h2>Repository</h2><dl className="detail-grid"><DetailItem label="Provider" value={data.metadata.provider} /><DetailItem label="Full name" value={data.metadata.fullName ?? 'Unknown'} /><DetailItem label="Owner" value={data.metadata.ownerLogin ?? 'Unknown'} /><DetailItem label="Last activity" value={formatDate(data.metadata.lastActivityAt)} /></dl>{data.metadata.topics.length ? <div className="chip-list">{data.metadata.topics.map(topic => <span className="inventory-tag" key={topic}>{topic}</span>)}</div> : null}{data.metadata.htmlUrl ? <p className="detail-link"><a href={data.metadata.htmlUrl} target="_blank" rel="noreferrer">Open repository ↗</a></p> : null}</section>

    <section className="dashboard-section"><span className="card-kicker">Activity</span><h2>Commit activity over time</h2><DrilldownTimeChart points={data.activity.timeline.map(point => ({ month: point.month, commits: point.commits, changedLines: point.changedLines, lineStatisticsCommitCount: point.lineStatisticsCommitCount }))} emptyText="No commit activity recorded." /></section>

    <section className="dashboard-section"><span className="card-kicker">Technologies</span><h2>Technologies</h2>{data.technologies.length ? <div className="chip-list">{data.technologies.map(item => <div className="evidence-chip" key={item.technologyKey}><strong>{item.technologyName}</strong><span>{item.strength.toLowerCase()}</span></div>)}</div> : <p className="empty-state">No technologies identified yet.</p>}</section>
    <section className="dashboard-section"><span className="card-kicker">Classification</span><h2>Project categories</h2>{data.categories.length ? <div className="chip-list">{data.categories.map(category => <div className="evidence-chip" key={`${category.categoryKey}-${category.source}`}><strong>{category.categoryName}</strong><span>{category.confidence.toLowerCase()} · automatic</span></div>)}</div> : <p className="empty-state">No project categories yet.</p>}</section>
    <section className="dashboard-section"><span className="card-kicker">Contributors</span><h2>Repository contributors</h2><SummaryFacts ariaLabel="Contributor summary" className="summary-facts-embedded" items={[{ label: 'Total contributors', value: data.contributors.total ?? 'Not collected' }, { label: 'People', value: data.contributors.humans ?? 'Not collected' }, { label: 'Bots', value: data.contributors.bots ?? 'Not collected' }]} /></section>

    <section className="detail-assessment-grid"><article className="dashboard-section"><span className="card-kicker">Project significance</span><h2>{data.assessment?.significanceLevel ?? 'Not calculated'}</h2>{data.assessment ? <span className="privacy-provenance">{data.assessment.privacyProvenance === 'PUBLIC_ONLY' ? 'Public data only' : 'Contains private data'}</span> : null}<div className="detail-score">{data.assessment?.significanceScore ?? '—'}</div><AssessmentRationale value={data.assessment?.significanceRationale} /></article><article className="dashboard-section"><span className="card-kicker">User involvement</span><h2>{data.assessment?.involvementLevel ?? 'Not calculated'}</h2><div className="detail-score">{data.assessment?.involvementScore ?? '—'}</div><AssessmentRationale value={data.assessment?.involvementRationale} /></article></section>

    <details className="dashboard-section secondary-details project-advanced-details"><summary>Advanced: AI profile correction</summary><div className="secondary-details-content"><h2>Project use in AI conclusions</h2><p className="settings-intro">This does not remove repository facts or measured statistics. It only controls whether this project contributes to user-level AI conclusions.</p><button type="button" className="secondary-action" onClick={() => void toggleAiProfileExclusion()}>{data.metadata.excludedFromAiProfile ? 'Include project in AI profile' : 'Exclude project from AI profile'}</button></div></details>

    <details className="dashboard-section secondary-details project-sync-details"><summary>Synchronisation · {data.synchronisation.status}</summary><div className="secondary-details-content"><dl className="detail-grid"><DetailItem label="Last seen" value={formatDate(data.synchronisation.lastSeenAt)} /><DetailItem label="Error" value={data.synchronisation.error ?? 'None'} />{syncMonitoring.status === 'ready' && syncMonitoring.contributionRuns[0] ? <><DetailItem label="Contribution sync" value={syncMonitoring.contributionRuns[0].status} /><DetailItem label="Contributions processed" value={String(syncMonitoring.contributionRuns[0].contributionsSeen)} /><DetailItem label="Pages processed" value={String(syncMonitoring.contributionRuns[0].pagesProcessed)} /><DetailItem label="Latest contribution error" value={syncMonitoring.contributionRuns[0].lastError ?? 'None'} /></> : null}</dl><button className="secondary-action" type="button" disabled={refreshing} onClick={() => void refreshAnalysis()}>{refreshing ? 'Queueing analysis…' : 'Refresh repository analysis'}</button>{refreshMessage ? <p className="settings-intro" role="status">{refreshMessage}</p> : null}</div></details>
  </>
}

function DetailItem({ label, value }: { label: string; value: string }) { return <div><dt>{label}</dt><dd>{value}</dd></div> }
function AssessmentRationale({ value }: { value?: Record<string, unknown> }) { if (!value) return <p className="empty-state">No assessment available.</p>; return <details className="assessment-rationale-details"><summary>Assessment rationale</summary><dl className="rationale-list">{Object.entries(value).slice(0, 8).map(([key, item]) => <div key={key}><dt>{humanize(key)}</dt><dd>{String(item)}</dd></div>)}</dl></details> }
function humanize(value: string) { return value.replace(/([A-Z])/g, ' $1').replace(/^./, c => c.toUpperCase()) }
function ownershipLabel(value: string) { return value === 'OWNED_BY_USER' ? 'own' : 'external' }
function formatNumber(value: number) { return new Intl.NumberFormat().format(value) }
function formatDate(value: string | null) { if (!value) return 'Unknown'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value)) }
