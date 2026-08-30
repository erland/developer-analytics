import { useState } from 'react'

export function useUserDataDeletion() {
  const [status, setStatus] =
    useState<'idle' | 'deleting' | 'error'>('idle')
  const [error, setError] = useState<string | null>(null)

  async function deleteAllData() {
    setStatus('deleting')
    setError(null)

    try {
      const response = await fetch('/api/me/data', {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ confirmation: 'DELETE_MY_DATA' }),
      })

      if (!response.ok) {
        throw new Error(`Data deletion failed with HTTP ${response.status}`)
      }

      window.location.assign('/')
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to delete account data',
      )
    }
  }

  return { status, error, deleteAllData }
}
