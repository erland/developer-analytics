import { useState } from 'react'

export function useSyncRecovery() {
  const [status, setStatus] =
    useState<'idle' | 'recovering' | 'retrying' | 'error'>('idle')
  const [message, setMessage] = useState<string | null>(null)

  async function recoverInterrupted() {
    setStatus('recovering')
    setMessage(null)
    const response = await fetch('/api/me/sync-recovery/recover', {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) {
      setStatus('error')
      setMessage(`Recovery failed with HTTP ${response.status}`)
      return
    }
    const data = (await response.json()) as { recoveredJobs: number }
    setStatus('idle')
    setMessage(`${data.recoveredJobs} interrupted jobs recovered.`)
  }

  async function retryGitHub() {
    setStatus('retrying')
    setMessage(null)
    const response = await fetch('/api/me/sync-recovery/github/retry', {
      method: 'POST',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) {
      setStatus('error')
      setMessage(`GitHub retry failed with HTTP ${response.status}`)
      return
    }
    setStatus('idle')
    setMessage('GitHub synchronisation queued.')
  }

  return { status, message, recoverInterrupted, retryGitHub }
}
