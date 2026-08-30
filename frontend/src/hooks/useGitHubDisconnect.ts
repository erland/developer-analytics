import { useState } from 'react'

export type DisconnectDataDisposition =
  | 'PRESERVE_ANALYSED_DATA'
  | 'REMOVE_ANALYSED_DATA'

export function useGitHubDisconnect() {
  const [status, setStatus] =
    useState<'idle' | 'disconnecting' | 'done' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  async function disconnect(dataDisposition: DisconnectDataDisposition) {
    setStatus('disconnecting')
    setError(null)

    try {
      const response = await fetch('/api/me/connections/github/disconnect', {
        method: 'POST',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ dataDisposition }),
      })

      if (!response.ok) {
        throw new Error(`GitHub disconnect failed with HTTP ${response.status}`)
      }

      setStatus('done')
      window.location.reload()
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to disconnect GitHub',
      )
    }
  }

  return { status, error, disconnect }
}
