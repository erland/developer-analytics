import { useCallback, useEffect, useState } from 'react'

export const externalClientScopes = [
  'PROFILE_READ',
  'PROJECTS_READ',
  'ACTIVITY_READ',
  'TECHNOLOGIES_READ',
  'PROJECT_TYPES_READ',
  'CONTRIBUTIONS_READ',
  'EVIDENCE_READ',
  'AI_ASSESSMENTS_WRITE',
] as const

export type ExternalClientScope = (typeof externalClientScopes)[number]

export const externalPrivacyScopes = [
  'PUBLIC_ONLY',
  'PUBLIC_PLUS_PRIVATE_AGGREGATES',
  'FULL_AUTHORISED_ANALYSIS',
] as const

export type ExternalPrivacyScope = (typeof externalPrivacyScopes)[number]

export type ExternalClient = {
  id: string
  name: string
  scopes: ExternalClientScope[]
  privacyScope: ExternalPrivacyScope
  createdAt: string
  lastUsedAt: string | null
  revokedAt: string | null
}

export function useExternalClients() {
  const [clients, setClients] = useState<ExternalClient[]>([])
  const [createdToken, setCreatedToken] = useState<string | null>(null)
  const [status, setStatus] =
    useState<'loading' | 'ready' | 'saving' | 'error'>('loading')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const response = await fetch('/api/me/external-clients', {
        credentials: 'include',
        headers: { Accept: 'application/json' },
      })
      if (!response.ok) {
        throw new Error(`External clients request failed with HTTP ${response.status}`)
      }
      setClients((await response.json()) as ExternalClient[])
      setStatus('ready')
    } catch (value) {
      setStatus('error')
      setError(value instanceof Error ? value.message : 'Unable to load external clients')
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function create(
    name: string,
    scopes: ExternalClientScope[],
    privacyScope: ExternalPrivacyScope,
  ) {
    setStatus('saving')
    setCreatedToken(null)
    const response = await fetch('/api/me/external-clients', {
      method: 'POST',
      credentials: 'include',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ name, scopes, privacyScope }),
    })
    if (!response.ok) {
      setStatus('error')
      setError(`External client creation failed with HTTP ${response.status}`)
      return
    }
    const data = (await response.json()) as ExternalClient & { token: string }
    setCreatedToken(data.token)
    await load()
  }

  async function revoke(id: string) {
    const response = await fetch(`/api/me/external-clients/${id}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: { Accept: 'application/json' },
    })
    if (!response.ok) {
      setStatus('error')
      setError(`External client revocation failed with HTTP ${response.status}`)
      return
    }
    await load()
  }

  return { clients, createdToken, status, error, create, revoke }
}
