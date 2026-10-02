import type { LifeEventType } from './eventsApi'

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

export type ProactiveInsights = {
  windowStart: string
  windowEnd: string
  knownTimeEventCount: number
  unknownTimeEventCount: number
  eventCounts: EventTypeCount[]
  repeatedActivities: RepeatedActivityObservation[]
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'

export async function getProactiveInsights(): Promise<ProactiveInsights> {
  const response = await fetch(`${apiBaseUrl}/insights`)
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Could not load activity insights.')
  }
  return response.json() as Promise<ProactiveInsights>
}
