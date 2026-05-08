import type {
  AuthChannel,
  AuthSession,
  CityDto,
  CountryDto,
  CreateCityCommand,
  CreateCountryCommand,
  CreateStateCommand,
  OtpChallenge,
  PropertyCard,
  PropertyDetail,
  CreatePropertyPayload,
  Review,
  ReviewComment,
  ReviewDraft,
  ReviewVoteType,
  TotpEnrollment,
  StateDto,
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

  verifyTotp(payload: { identifier: string; code: string }) {
    return request<AuthSession>('/auth/totp/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  startTotpEnrollment() {
    return request<TotpEnrollment>('/auth/totp/enrollment', {
      method: 'POST',
    })
  },

  activateTotp(payload: { code: string }) {
    return request<AuthSession>('/auth/totp/enrollment/activate', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  disableTotp() {
    return request<void>('/auth/totp/enrollment/disable', { method: 'POST' })
  },

  logout() {
    return request<void>('/auth/logout', { method: 'POST' })
  },

  fetchStates() {
    return request<string[]>('/catalog/states?country=India')
  },

  fetchCountries() {
    return request<string[]>('/catalog/countries')
  },

  fetchStatesByCountry(country: string) {
    return request<string[]>(`/catalog/states?country=${encodeURIComponent(country)}`)
  },

  fetchCities(country: string, state: string) {
    return request<string[]>(
      `/catalog/cities?country=${encodeURIComponent(country)}&state=${encodeURIComponent(state)}`,
    )
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
    return request<PropertyCard[]>(`/properties/filter${query ? `?${query}` : ''}`)
  },

  createProperty(payload: CreatePropertyPayload) {
    return request<PropertyDetail>('/properties', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
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

  // Location Management Endpoints
  getAllCountries() {
    return request<CountryDto[]>('/admin/locations/countries')
  },

  getCountry(countryId: number) {
    return request<CountryDto>(`/admin/locations/countries/${countryId}`)
  },

  createCountry(payload: CreateCountryCommand) {
    return request<CountryDto>('/admin/locations/countries', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  deleteCountry(countryId: number) {
    return request<void>(`/admin/locations/countries/${countryId}`, {
      method: 'DELETE',
    })
  },

  getStatesByCountry(countryId: number) {
    return request<StateDto[]>(`/admin/locations/countries/${countryId}/states`)
  },

  getState(stateId: number) {
    return request<StateDto>(`/admin/locations/states/${stateId}`)
  },

  createState(payload: CreateStateCommand) {
    return request<StateDto>('/admin/locations/states', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  deleteState(stateId: number) {
    return request<void>(`/admin/locations/states/${stateId}`, {
      method: 'DELETE',
    })
  },

  getCitiesByState(stateId: number) {
    return request<CityDto[]>(`/admin/locations/states/${stateId}/cities`)
  },

  getCity(cityId: number) {
    return request<CityDto>(`/admin/locations/cities/${cityId}`)
  },

  createCity(payload: CreateCityCommand) {
    return request<CityDto>('/admin/locations/cities', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  deleteCity(cityId: number) {
    return request<void>(`/admin/locations/cities/${cityId}`, {
      method: 'DELETE',
    })
  },

  getPendingProperties(page = 0, size = 50) {
    return request<{ content: PropertyCard[] }>(
      `/properties/pending/all?page=${page}&size=${size}`,
    )
  },

  approveProperty(propertyId: number) {
    return request<PropertyDetail>(`/properties/${propertyId}/verify`, {
      method: 'POST',
    })
  },
}
