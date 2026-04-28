export type AuthChannel = 'EMAIL' | 'MOBILE'
export type ReviewVoteType = 'HELPFUL' | 'NOT_HELPFUL' | 'SAME_ISSUE'

export interface SessionUser {
  id: number
  displayName: string
  email: string | null
  mobileNumber: string | null
  avatarUrl: string | null
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
}

export interface OtpChallenge {
  challengeId: number
  destination: string
  expiresAt: string
  devCode: string | null
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
