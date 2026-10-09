import { getAccessToken, handleUnauthorized } from './authApi'
import type { LifeEventType } from './eventsApi'

export type ExternalProviderId = 'MOCK' | 'HEALTH_CONNECT'

export type IntegrationProviderStatus = {
  provider: ExternalProviderId
  displayName: string
  developmentOnly: boolean
  clientUploadRequired: boolean
  supportedEventTypes: LifeEventType[]
  lastSyncedAt: string | null
}

export type IntegrationSyncResponse = {
  provider: ExternalProviderId
  eventsFetched: number
  eventsCreated: number
  eventsUpdated: number
  eventsSkipped: number
  lastSyncedAt: string
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
    throw new Error(problem?.detail ?? 'Could not load integrations.')
  }
  return response.json() as Promise<T>
}

export const integrationsApi = {
  getProviders: () => request<IntegrationProviderStatus[]>('/integrations'),
  sync: (provider: ExternalProviderId) => request<IntegrationSyncResponse>(`/integrations/${provider}/sync`, {
    method: 'POST',
  }),
}
