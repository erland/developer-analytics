import { useCallback, useEffect, useState } from 'react'

export type UserAiInsight = {
  status: string
  aiGenerated: true
  likelyRoles: Array<{
    role: string
    confidence: number
    rationale: string
  }>
  technicalFocus: string
  breadthDepthObservation: string
  technologyEvolutionSummary: string
  openSourceEngagementSummary: string
  analysisVersion: string
  providerId: string | null
  modelId: string | null
  privacyProvenance: string
  createdAt: string | null
}

export function useUserAiInsights() {
  const [insight, setInsight] = useState<UserAiInsight | null>(null)
  const [status, setStatus] =
    useState<'loading' | 'ready' | 'generating' | 'error'>('loading')
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      const response = await fetch('/api/me/ai/insights', {
        credentials: 'include',
        headers: { Accept: 'application/json' },
      })

      if (!response.ok) {
        throw new Error(`AI insights request failed with HTTP ${response.status}`)
      }

      if (response.status === 204) {
        setInsight(null)
      } else {
        const text = await response.text()
        setInsight(text ? (JSON.parse(text) as UserAiInsight) : null)
      }
      setStatus('ready')
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to load AI insights',
      )
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function generate() {
    setStatus('generating')
    setError(null)
    try {
      const response = await fetch('/api/me/ai/insights', {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json' },
      })

      if (!response.ok) {
        throw new Error(`AI insight generation failed with HTTP ${response.status}`)
      }

      const data = (await response.json()) as UserAiInsight
      setInsight(data)
      setStatus('ready')
    } catch (value) {
      setStatus('error')
      setError(
        value instanceof Error
          ? value.message
          : 'Unable to generate AI insights',
      )
    }
  }

  return { insight, status, error, generate }
}
