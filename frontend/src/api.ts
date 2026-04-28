import type {
  AuthChannel,
  AuthSession,
  OtpChallenge,
  PropertyCard,
  PropertyDetail,
  Review,
  ReviewComment,
  ReviewDraft,
  ReviewVoteType,
  VoteSummary,
} from './types'

const defaultOrigin =
  typeof window === 'undefined' ? 'http://localhost:8080' : window.location.origin
const API_BASE =
  import.meta.env.VITE_API_BASE_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080/api' : `${defaultOrigin}/api`)
const BACKEND_BASE =
  import.meta.env.VITE_BACKEND_BASE_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080' : defaultOrigin)

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
    ...init,
  })

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as
      | { message?: string }
      | null
    throw new Error(payload?.message ?? 'Request failed')
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const api = {
  backendBaseUrl: BACKEND_BASE,

  oauthUrl(provider: string) {
    return `${BACKEND_BASE}/oauth2/authorization/${provider}`
  },

  getSession() {
    return request<AuthSession>('/auth/session')
  },

  requestOtp(payload: {
    channel: AuthChannel
    destination: string
    displayName: string
  }) {
    return request<OtpChallenge>('/auth/otp/request', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  verifyOtp(payload: {
    challengeId: number
    code: string
    displayName: string
  }) {
    return request<AuthSession>('/auth/otp/verify', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  logout() {
    return request<void>('/auth/logout', { method: 'POST' })
  },

  fetchStates() {
    return request<string[]>('/catalog/states')
  },

  fetchCities(state: string) {
    return request<string[]>(`/catalog/cities?state=${encodeURIComponent(state)}`)
  },

  fetchLocalities(state: string, city: string) {
    return request<string[]>(
      `/catalog/localities?state=${encodeURIComponent(state)}&city=${encodeURIComponent(city)}`,
    )
  },

  searchProperties(filters: {
    state?: string
    city?: string
    locality?: string
  }) {
    const params = new URLSearchParams()
    if (filters.state) params.set('state', filters.state)
    if (filters.city) params.set('city', filters.city)
    if (filters.locality) params.set('locality', filters.locality)
    const query = params.toString()
    return request<PropertyCard[]>(`/properties${query ? `?${query}` : ''}`)
  },

  fetchPropertyDetail(propertyId: number) {
    return request<PropertyDetail>(`/properties/${propertyId}`)
  },

  createReview(propertyId: number, payload: ReviewDraft) {
    return request<Review>(`/properties/${propertyId}/reviews`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  addReply(reviewId: number, payload: { body: string; parentCommentId: number | null }) {
    return request<ReviewComment>(`/reviews/${reviewId}/replies`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  vote(reviewId: number, type: ReviewVoteType) {
    return request<VoteSummary>(`/reviews/${reviewId}/votes`, {
      method: 'POST',
      body: JSON.stringify({ type }),
    })
  },
}
