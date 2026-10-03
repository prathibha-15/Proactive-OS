import type { LifeEventType } from './eventsApi'
import { getAccessToken, handleUnauthorized } from './authApi'

export type EventTypeCount = {
  type: LifeEventType
  count: number
}

export type RepeatedActivityObservation = {
  type: 'STUDY' | 'WORKOUT'
  label: string
  distinctDays: number
  eventCount: number
}

export type RecommendationCategory = 'STUDY' | 'WORKOUT' | 'DATA_QUALITY'

export type Recommendation = {
  id: string
  category: RecommendationCategory
  title: string
  message: string
}

export type ProactiveInsights = {
  windowStart: string
  windowEnd: string
  knownTimeEventCount: number
  unknownTimeEventCount: number
  eventCounts: EventTypeCount[]
  repeatedActivities: RepeatedActivityObservation[]
  recommendations: Recommendation[]
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function getProactiveInsights(): Promise<ProactiveInsights> {
  const token = getAccessToken()
  const response = await fetch(`${apiBaseUrl}/insights`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!response.ok) {
    if (response.status === 401) handleUnauthorized()
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Could not load activity insights.')
  }
  return response.json() as Promise<ProactiveInsights>
}
