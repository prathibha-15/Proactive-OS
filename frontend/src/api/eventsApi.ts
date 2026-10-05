import { getAccessToken, handleUnauthorized } from './authApi'

export type EventSource = 'JOURNAL' | 'MANUAL' | 'DEVICE'
export type LifeEventType = 'SLEEP' | 'WATER' | 'FOOD' | 'STUDY' | 'WORKOUT' | 'STEPS' | 'JOB_APPLICATION' | 'MOOD'
export type WaterUnit = 'GLASS' | 'ML' | 'LITER'

export const LIFE_EVENT_TYPES: LifeEventType[] = ['SLEEP', 'WATER', 'FOOD', 'STUDY', 'WORKOUT', 'STEPS', 'JOB_APPLICATION', 'MOOD']

export type LifeEvent = {
  id: number
  type: LifeEventType
  source: EventSource
  externalProvider: string | null
  journalEntryId: number | null
  eventTime: string | null
  confidence: number | null
  subject: string | null
  durationMinutes: number | null
  quantity: number | null
  unit: WaterUnit | null
  description: string | null
  calories: number | null
  activityType: string | null
  count: number | null
  company: string | null
  role: string | null
  status: string | null
  mood: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
  applicationCount: number | null
}

export type LifeEventRequest = {
  type: LifeEventType
  source: EventSource
  journalEntryId?: number | null
  eventTime?: string | null
  confidence?: number | null
  subject?: string | null
  durationMinutes?: number | null
  quantity?: number | null
  unit?: WaterUnit | null
  description?: string | null
  calories?: number | null
  activityType?: string | null
  count?: number | null
  company?: string | null
  role?: string | null
  status?: string | null
  mood?: string | null
  notes?: string | null
  applicationCount?: number | null
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getAccessToken()
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options?.headers },
    ...options,
  })

  if (!response.ok) {
    if (response.status === 401) handleUnauthorized()
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Something went wrong. Please try again.')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export const eventsApi = {
  createEvent: (event: LifeEventRequest) => request<LifeEvent>('/events', {
    method: 'POST',
    body: JSON.stringify(event),
  }),
  getEvents: (type?: LifeEventType) => request<LifeEvent[]>(`/events${type ? `?type=${type}` : ''}`),
  getEvent: (id: number) => request<LifeEvent>(`/events/${id}`),
  updateEvent: (id: number, event: LifeEventRequest) => request<LifeEvent>(`/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(event),
  }),
  deleteEvent: (id: number) => request<void>(`/events/${id}`, { method: 'DELETE' }),
  getJournalEvents: (journalId: number) => request<LifeEvent[]>(`/journals/${journalId}/events`),
  extractJournalEvents: (journalId: number) => request<LifeEvent[]>(`/journals/${journalId}/extract`, { method: 'POST' }),
}
