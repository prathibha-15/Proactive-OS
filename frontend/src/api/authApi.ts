export type AuthUser = {
  id: number
  email: string
  createdAt: string
  updatedAt: string
}

export type LoginResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthUser
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
const tokenStorageKey = 'proactive-os.access-token'
const userStorageKey = 'proactive-os.auth-user'

export function getAccessToken() {
  return sessionStorage.getItem(tokenStorageKey)
}

export function getStoredUser(): AuthUser | null {
  const value = sessionStorage.getItem(userStorageKey)
  if (!value) return null
  try {
    return JSON.parse(value) as AuthUser
  } catch {
    sessionStorage.removeItem(userStorageKey)
    return null
  }
}

export function storeLogin(response: LoginResponse) {
  sessionStorage.setItem(tokenStorageKey, response.accessToken)
  sessionStorage.setItem(userStorageKey, JSON.stringify(response.user))
}

export function clearLogin() {
  sessionStorage.removeItem(tokenStorageKey)
  sessionStorage.removeItem(userStorageKey)
}

export function handleUnauthorized() {
  clearLogin()
  window.location.replace('/login')
}

export async function register(email: string, password: string): Promise<AuthUser> {
  const response = await fetch(`${apiBaseUrl}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Could not create your account.')
  }
  return response.json() as Promise<AuthUser>
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${apiBaseUrl}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(problem?.detail ?? 'Email or password is incorrect.')
  }
  return response.json() as Promise<LoginResponse>
}