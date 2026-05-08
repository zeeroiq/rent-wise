export type AuthChannel = 'EMAIL' | 'MOBILE' | 'TELEGRAM' | 'SIGNAL' | 'TOTP'
export type ReviewVoteType = 'HELPFUL' | 'NOT_HELPFUL' | 'SAME_ISSUE'
export type PropertyStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'ARCHIVED'
export type PropertyCondition = 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR'
export type FurnishingType = 'UNFURNISHED' | 'SEMI_FURNISHED' | 'FULLY_FURNISHED'
export type OccupancyType = 'SOLO' | 'SHARED' | 'FAMILY'

export interface SessionUser {
  id: number
  displayName: string
  email: string | null
  mobileNumber: string | null
  avatarUrl: string | null
  isAdmin?: boolean
  totpEnabled?: boolean
}

export interface AuthSession {
  user: SessionUser | null
  oauthProviders: string[]
  devOtpVisible: boolean
}

export interface Scorecard {
  overallScore: number
  landlordScore: number
  propertyScore: number
  recommendation: string
  reviewCount: number
  unresolvedIssueCount: number
  depositDisputeCount: number
}

export interface PropertyCard {
  id: number
  title: string
  propertyType: string
  addressLine1: string
  locality: string
  city: string
  state: string
  postalCode: string | null
  highlights: string | null
  landlordName: string
  status?: PropertyStatus
  onboardingDate?: string
  scorecard: Scorecard
}

export interface Landlord {
  id: number
  name: string
  email: string | null
  phoneNumber: string | null
  managementStyle: string | null
}

export interface VoteSummary {
  helpful: number
  notHelpful: number
  sameIssue: number
  currentUserVote: ReviewVoteType | null
}

export interface ReviewComment {
  id: number
  author: SessionUser
  body: string
  createdAt: string
  replies: ReviewComment[]
}

export interface Review {
  id: number
  author: SessionUser
  headline: string
  experienceSummary: string
  problemsFaced: string
  landlordSupport: string
  leaseClosure: string
  securityDepositOutcome: string
  overallRating: number
  landlordRating: number
  propertyRating: number
  maintenanceRating: number
  moveOutRating: number
  depositRating: number
  recommended: boolean
  issuesResolved: boolean
  wouldRentAgain: boolean
  createdAt: string
  votes: VoteSummary
  thread: ReviewComment[]
}

export interface PropertyDetail {
  id: number
  title: string
  propertyType: string
  addressLine1: string
  locality: string
  city: string
  state: string
  postalCode: string | null
  highlights: string | null
  landlord: Landlord
  scorecard: Scorecard
  reviews: Review[]
  status?: PropertyStatus
  condition?: PropertyCondition
  furnishingType?: FurnishingType
  occupancyType?: OccupancyType
  yearBuilt?: number
  totalArea?: number
  bedroomCount?: number
  bathroomCount?: number
}

export interface OtpChallenge {
  challengeId: number
  destination: string
  expiresAt: string
  devCode: string | null
}

export interface TotpEnrollment {
  issuer: string
  accountName: string
  secret: string
  otpauthUri: string
  enabled: boolean
}

export interface ReviewDraft {
  headline: string
  experienceSummary: string
  problemsFaced: string
  landlordSupport: string
  leaseClosure: string
  securityDepositOutcome: string
  overallRating: number
  landlordRating: number
  propertyRating: number
  maintenanceRating: number
  moveOutRating: number
  depositRating: number
  recommended: boolean
  issuesResolved: boolean
  wouldRentAgain: boolean
}

// Location Management DTOs
export interface CountryDto {
  id: number
  code: string
  name: string
  createdAt: string
}

export interface StateDto {
  id: number
  countryId: number
  code: string
  name: string
  createdAt: string
}

export interface CityDto {
  id: number
  stateId: number
  code: string
  name: string
  createdAt: string
}

export interface CreateCountryCommand {
  code: string
  name: string
}

export interface CreateStateCommand {
  countryId: number
  code: string
  name: string
}

export interface CreateCityCommand {
  stateId: number
  code: string
  name: string
}

export interface PropertyOnboarding {
  title: string
  propertyType: string
  addressLine1: string
  addressLine2?: string
  locality: string
  city: string
  state: string
  postalCode?: string
  highlights?: string
  status?: PropertyStatus
  condition?: PropertyCondition
  furnishingType?: FurnishingType
  occupancyType?: OccupancyType
  yearBuilt?: number
  totalArea?: number
  bedroomCount?: number
  bathroomCount?: number
}

export interface CreatePropertyPayload {
  title: string
  propertyType: string
  addressLine1: string
  locality: string
  city: string
  state: string
  postalCode?: string
  highlights?: string
  landlordName: string
  landlordEmail?: string
  landlordPhoneNumber?: string
  landlordManagementStyle?: string
  onboardingDate: string
  exitDate?: string
  monthlyRent?: number
  depositAmount?: number
  propertyConditionOnEntry?: PropertyCondition
  propertyConditionOnExit?: PropertyCondition
  amenities?: string
  furnishingType?: FurnishingType
  occupancyType?: OccupancyType
}
