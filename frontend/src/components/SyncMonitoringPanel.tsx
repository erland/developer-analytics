import { useState } from 'react'
import { type SyncJob, useSyncMonitoring } from '../hooks/useSyncMonitoring'

export function SyncMonitoringPanel() {
  const monitoring = useSyncMonitoring()
  const [retryMessage, setRetryMessage] = useState<string | null>(null)
  if (monitoring.status === 'loading') return <section className="dashboard-section"><span className="card-kicker">Synchronisation</span><p>Loading progress…</p></section>
  if (monitoring.status === 'error') return <section className="dashboard-section"><span className="card-kicker">Synchronisation</span><p className="sync-error-text">{monitoring.error}</p></section>

  const { jobs, errors, contributionRuns } = monitoring
  const current = jobs.activeJobs.find((job) => job.status === 'RUNNING')
  const currentRun = current?.repositoryId ? contributionRuns.find((run) => run.repositoryId === current.repositoryId && run.status === 'RUNNING') : undefined

  async function retry(job: SyncJob) {
    if (!job.repositoryId) return
    setRetryMessage(null)
    try {
      const response = await fetch(`/api/me/sync/github/repositories/${job.repositoryId}/refresh-analysis`, { method: 'POST', credentials: 'include', headers: { Accept: 'application/json' } })
      if (!response.ok) throw new Error(`Retry failed with HTTP ${response.status}`)
      setRetryMessage(`Analysis queued for ${job.repositoryName ?? 'repository'}.`)
    } catch (error) { setRetryMessage(error instanceof Error ? error.message : 'Unable to retry repository analysis') }
  }

  const summary = [`${jobs.completed}/${jobs.totalRepositories} repositories completed`]
  if (jobs.analysisStepsTotal > 0 && jobs.analysisStepsCompleted < jobs.analysisStepsTotal) {
    summary.push(`${jobs.analysisStepsCompleted}/${jobs.analysisStepsTotal} analysis steps`)
  }
  if (jobs.running) summary.push(`${jobs.running} running`)
  if (jobs.failed) summary.push(`${jobs.failed} failed`)

  return <details className="dashboard-section secondary-details sync-monitoring-details" aria-labelledby="sync-progress-heading">
    <summary><span id="sync-progress-heading">Analysis progress</span><span className="secondary-details-summary-meta">{summary.join(' · ')}</span></summary>
    <div className="secondary-details-content">
      {jobs.analysisStepsTotal > 0 ? <div className="sync-current"><strong>Pipeline progress</strong><span>{jobs.analysisStepsCompleted} of {jobs.analysisStepsTotal} steps completed</span><progress aria-label="Analysis pipeline progress" max={jobs.analysisStepsTotal} value={jobs.analysisStepsCompleted} /></div> : null}
      <div className="sync-status-grid">
        <Status label="Queued" value={jobs.queued} />
        <Status label="Pending" value={jobs.waiting} />
        <Status label="Running" value={jobs.running} />
        <Status label="Completed" value={jobs.completed} />
        <Status label="Failed" value={jobs.failed} />
      </div>
      {current ? <div className="sync-current"><strong>Current: {current.repositoryName ?? humanizeJob(current.jobType)}</strong><span>{current.analysisStep && current.analysisStepsTotal ? `Step ${current.analysisStep}/${current.analysisStepsTotal} · ` : ''}{humanizeJob(current.jobType)} · attempt {current.attemptCount}/{current.maxAttempts}</span>{currentRun ? <span>{currentRun.contributionsSeen} contributions · {currentRun.pagesProcessed} pages processed</span> : null}</div> : <p className="empty-state">No background analysis job is running right now.</p>}
      <div className="sync-errors-heading"><h3>Recent synchronisation errors</h3><span>{errors.length} shown</span></div>
      {errors.length ? <div className="sync-error-list">{errors.map((job) => <article className="sync-error-row" key={job.id}><div><strong>{job.repositoryName ?? humanizeJob(job.jobType)}</strong><span>{humanizeJob(job.jobType)} · {errorStatus(job)} · attempt {job.attemptCount}/{job.maxAttempts}</span><code>{job.lastError}</code><span>{formatDateTime(job.completedAt ?? job.startedAt ?? job.createdAt)}</span></div>{job.repositoryId ? <button type="button" className="secondary-action" onClick={() => void retry(job)}>Retry repository</button> : null}</article>)}</div> : <p className="empty-state">No synchronisation errors recorded.</p>}
      {retryMessage ? <p role="status" className="settings-intro">{retryMessage}</p> : null}
    </div>
  </details>
}

function Status({ label, value }: { label: string; value: number }) { return <div><span>{label}</span><strong>{value}</strong></div> }
function errorStatus(job: SyncJob) { if (job.status === 'WAITING') return 'RETRYING'; if (job.status === 'COMPLETED') return 'RECOVERED'; return job.status }
function humanizeJob(value: string) { return value.toLowerCase().replaceAll('_', ' ').replace(/^./, (c) => c.toUpperCase()) }
function formatDateTime(value: string | null) { if (!value) return 'Unknown time'; return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) }
