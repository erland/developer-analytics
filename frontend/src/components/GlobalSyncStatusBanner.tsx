import type { GitHubSyncStatusState } from '../hooks/useGitHubSyncStatus'
import './GlobalSyncStatusBanner.css'

type Props = {
  sync: GitHubSyncStatusState
}

export function GlobalSyncStatusBanner({ sync }: Props) {
  const overview = sync.overview

  if (!overview || sync.status === 'loading' || sync.status === 'idle' || sync.status === 'error') {
    return null
  }

  const progress = overview.analysisStepsTotal > 0
    ? Math.max(0, Math.min(100, Math.round((overview.analysisStepsCompleted / overview.analysisStepsTotal) * 100)))
    : null

  if (sync.status === 'paused') {
    return (
      <section className="global-sync-status global-sync-status-paused" role="status" aria-live="polite">
        <div className="global-sync-status-copy">
          <strong>GitHub sync paused to protect API capacity.</strong>
          <span>
            {overview.pausedRateLimit} {overview.pausedRateLimit === 1 ? 'job is' : 'jobs are'} paused
            {sync.resumeAt ? ` and will resume automatically ${formatResumeAt(sync.resumeAt)}` : ''}.
          </span>
        </div>
        <SyncProgress completed={overview.analysisStepsCompleted} total={overview.analysisStepsTotal} percent={progress} />
      </section>
    )
  }

  return (
    <section className="global-sync-status" role="status" aria-live="polite">
      <div className="global-sync-status-copy">
        <strong>GitHub sync is running.</strong>
        <span>{activeJobSummary(overview.running, overview.queued, overview.waiting)}</span>
      </div>
      <SyncProgress completed={overview.analysisStepsCompleted} total={overview.analysisStepsTotal} percent={progress} />
    </section>
  )
}

function SyncProgress({ completed, total, percent }: { completed: number; total: number; percent: number | null }) {
  if (percent === null || total <= 0) return null
  return (
    <div className="global-sync-progress" aria-label={`Analysis progress ${percent}%`}>
      <div className="global-sync-progress-track" aria-hidden="true">
        <span style={{ width: `${percent}%` }} />
      </div>
      <span>{completed}/{total} analysis steps · {percent}%</span>
    </div>
  )
}

function activeJobSummary(running: number, queued: number, waiting: number) {
  const parts: string[] = []
  if (running > 0) parts.push(`${running} running`)
  if (queued > 0) parts.push(`${queued} queued`)
  if (waiting > 0) parts.push(`${waiting} waiting`)
  return parts.length > 0 ? parts.join(' · ') : 'Work is being scheduled.'
}

function formatResumeAt(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'when GitHub capacity is available'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
