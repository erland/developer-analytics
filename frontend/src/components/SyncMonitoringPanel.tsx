import { useState } from 'react'
import { type SyncJob, type SyncJobOverview, useSyncMonitoring } from '../hooks/useSyncMonitoring'

export type SyncHeadline = {
  kind: 'working' | 'complete' | 'attention' | 'idle'
  title: string
  detail: string
}

export function syncHeadline(jobs: SyncJobOverview): SyncHeadline {
  if (jobs.failed > 0) {
    return {
      kind: 'attention',
      title: 'Needs attention',
      detail: `${jobs.failed} ${jobs.failed === 1 ? 'job has' : 'jobs have'} stopped after all retry attempts.`,
    }
  }
  if (jobs.running > 0 || jobs.queued > 0 || jobs.waiting > 0) {
    const parts: string[] = []
    if (jobs.running) parts.push(`${jobs.running} running`)
    if (jobs.queued) parts.push(`${jobs.queued} queued`)
    if (jobs.waiting) parts.push(`${jobs.waiting} waiting to retry`)
    return { kind: 'working', title: 'Analysis in progress', detail: parts.join(' · ') }
  }
  if (jobs.totalRepositories > 0) {
    return {
      kind: 'complete',
      title: 'Analysis complete',
      detail: 'No jobs are queued, running, waiting to retry, or failed.',
    }
  }
  return { kind: 'idle', title: 'No analysis yet', detail: 'No repository analysis jobs have been scheduled.' }
}

export function partitionSyncIssues(errors: SyncJob[]) {
  return {
    activeIssues: errors.filter((job) => job.status !== 'COMPLETED'),
    recoveredIssues: errors.filter((job) => job.status === 'COMPLETED'),
  }
}

export function SyncMonitoringPanel() {
  const monitoring = useSyncMonitoring()
  const [retryMessage, setRetryMessage] = useState<string | null>(null)
  if (monitoring.status === 'loading') return <section className="dashboard-section"><span className="card-kicker">Synchronisation</span><p>Loading progress…</p></section>
  if (monitoring.status === 'error') return <section className="dashboard-section"><span className="card-kicker">Synchronisation</span><p className="sync-error-text">{monitoring.error}</p></section>

  const { jobs, errors, contributionRuns } = monitoring
  const headline = syncHeadline(jobs)
  const current = jobs.activeJobs.find((job) => job.status === 'RUNNING')
  const currentRun = current?.repositoryId ? contributionRuns.find((run) => run.repositoryId === current.repositoryId && run.status === 'RUNNING') : undefined
  const { activeIssues, recoveredIssues } = partitionSyncIssues(errors)

  async function retry(job: SyncJob) {
    if (!job.repositoryId) return
    setRetryMessage(null)
    try {
      const response = await fetch(`/api/me/sync/github/repositories/${job.repositoryId}/refresh-analysis`, { method: 'POST', credentials: 'include', headers: { Accept: 'application/json' } })
      if (!response.ok) throw new Error(`Retry failed with HTTP ${response.status}`)
      setRetryMessage(`Analysis queued for ${job.repositoryName ?? 'repository'}.`)
    } catch (error) { setRetryMessage(error instanceof Error ? error.message : 'Unable to retry repository analysis') }
  }

  const summary = [headline.title]
  if (headline.kind === 'working') summary.push(headline.detail)
  else if (headline.kind === 'attention') summary.push(headline.detail)
  else if (jobs.analysisStepsTotal > 0) summary.push(`${jobs.analysisStepsCompleted}/${jobs.analysisStepsTotal} steps completed`)

  return <details className={`dashboard-section secondary-details sync-monitoring-details sync-state-${headline.kind}`} aria-labelledby="sync-progress-heading">
    <summary><span id="sync-progress-heading">Analysis status</span><span className="secondary-details-summary-meta">{summary.join(' · ')}</span></summary>
    <div className="secondary-details-content">
      <div className="sync-current" role="status"><strong>{headline.title}</strong><span>{headline.detail}</span></div>
      {jobs.analysisStepsTotal > 0 ? <div className="sync-current"><strong>Pipeline progress</strong><span>{jobs.analysisStepsCompleted} of {jobs.analysisStepsTotal} steps completed</span><progress aria-label="Analysis pipeline progress" max={jobs.analysisStepsTotal} value={jobs.analysisStepsCompleted} /></div> : null}
      <div className="sync-status-grid">
        <Status label="Queued" value={jobs.queued} />
        <Status label="Waiting to retry" value={jobs.waiting} />
        <Status label="Running" value={jobs.running} />
        <Status label="Completed" value={jobs.completed} />
        <Status label="Failed" value={jobs.failed} />
      </div>
      {current ? <div className="sync-current"><strong>Current: {current.repositoryName ?? humanizeJob(current.jobType)}</strong><span>{current.analysisStep && current.analysisStepsTotal ? `Step ${current.analysisStep}/${current.analysisStepsTotal} · ` : ''}{humanizeJob(current.jobType)} · attempt {current.attemptCount}/{current.maxAttempts}</span>{currentRun ? <span>{currentRun.contributionsSeen} contributions · {currentRun.pagesProcessed} pages processed</span> : null}</div> : headline.kind === 'complete' ? <p className="empty-state">All scheduled analysis work is finished.</p> : <p className="empty-state">No background analysis job is running right now.</p>}
      <div className="sync-errors-heading"><h3>Recent synchronisation issues</h3><span>{activeIssues.length} current</span></div>
      {activeIssues.length > 0 ? <div className="sync-error-list">{activeIssues.map((job) => <IssueRow key={job.id} job={job} retry={retry} />)}</div> : <p className="empty-state">No current synchronisation issues.</p>}
      {recoveredIssues.length > 0 ? <details className="sync-resolved-issues"><summary>Show resolved issues ({recoveredIssues.length})</summary><p className="settings-intro">These historical issues completed successfully later and do not require action.</p><div className="sync-error-list">{recoveredIssues.map((job) => <IssueRow key={job.id} job={job} retry={retry} />)}</div></details> : null}
      {retryMessage ? <p role="status" className="settings-intro">{retryMessage}</p> : null}
    </div>
  </details>
}

function IssueRow({ job, retry }: { job: SyncJob; retry: (job: SyncJob) => Promise<void> }) {
  const recovered = job.status === 'COMPLETED'
  const waiting = job.status === 'WAITING' || job.status === 'PAUSED_RATE_LIMIT'
  return <article className={`sync-error-row${recovered ? ' recovered' : ''}`}><div><strong>{job.repositoryName ?? humanizeJob(job.jobType)}</strong><span>{humanizeJob(job.jobType)} · {issueStatus(job)} · attempt {job.attemptCount}/{job.maxAttempts}</span>{recovered ? <span>Recovered automatically — no action needed.</span> : waiting ? <span>The system will retry automatically.</span> : null}{job.lastError ? <code>{job.lastError}</code> : null}<span>{formatDateTime(job.completedAt ?? job.startedAt ?? job.createdAt)}</span></div>{job.status === 'FAILED' && job.repositoryId ? <button type="button" className="secondary-action" onClick={() => void retry(job)}>Retry repository</button> : null}</article>
}

function Status({ label, value }: { label: string; value: number }) { return <div><span>{label}</span><strong>{value}</strong></div> }
function issueStatus(job: SyncJob) { if (job.status === 'WAITING') return 'RETRY SCHEDULED'; if (job.status === 'PAUSED_RATE_LIMIT') return 'PAUSED · RATE LIMIT'; if (job.status === 'COMPLETED') return 'RECOVERED'; if (job.status === 'FAILED') return 'NEEDS ATTENTION'; return job.status }
function humanizeJob(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/^./, (c) => c.toUpperCase()) }
function formatDateTime(value: string | null) { if (!value) return 'Unknown time'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
