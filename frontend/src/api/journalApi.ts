import { getAccessToken, handleUnauthorized } from './authApi'

export type JournalRequest = {
  content: string
}

export type Journal = {
  id: number
  content: string
  entryDate: string
  createdAt: string
  updatedAt: string
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

export const journalApi = {
  createJournal: (journal: JournalRequest) => request<Journal>('/journals', {
    method: 'POST',
    body: JSON.stringify(journal),
  }),
  getJournals: () => request<Journal[]>('/journals'),
  getJournal: (id: string) => request<Journal>(`/journals/${id}`),
  updateJournal: (id: number, journal: JournalRequest) => request<Journal>(`/journals/${id}`, {
    method: 'PUT',
    body: JSON.stringify(journal),
  }),
  deleteJournal: (id: number) => request<void>(`/journals/${id}`, { method: 'DELETE' }),
}