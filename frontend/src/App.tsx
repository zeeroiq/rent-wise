import { startTransition, type FormEvent, useEffect, useMemo, useRef, useState } from 'react'
import { api } from './api'
import { AdminPanel } from './AdminPanel'
import {
  Button,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  Input,
  Textarea,
  SegmentedControl,
} from '@/components/common'
import { ThemeMenu } from '@/components/layout'
import type {
  AuthChannel,
  AuthSession,
  OtpChallenge,
  PropertyCard,
  PropertyDetail,
  ReviewComment,
  ReviewDraft,
  ReviewVoteType,
} from './types'

const initialReviewDraft: ReviewDraft = {
  headline: '',
  experienceSummary: '',
  problemsFaced: '',
  landlordSupport: '',
  leaseClosure: '',
  securityDepositOutcome: '',
  overallRating: 4,
  landlordRating: 4,
  propertyRating: 4,
  maintenanceRating: 4,
  moveOutRating: 4,
  depositRating: 4,
  recommended: true,
  issuesResolved: true,
  wouldRentAgain: true,
}

type ReplyState = {
  body: string
  parentCommentId: number | null
  parentLabel: string | null
}

function App() {
  const [session, setSession] = useState<AuthSession | null>(null)
  const [sessionLoaded, setSessionLoaded] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const initialLoginDecision = useRef(false)
  const [authChannel, setAuthChannel] = useState<AuthChannel>('EMAIL')
  const [displayName, setDisplayName] = useState('')
  const [destination, setDestination] = useState('')
  const [otpChallenge, setOtpChallenge] = useState<OtpChallenge | null>(null)
  const [otpCode, setOtpCode] = useState('')
  const [states, setStates] = useState<string[]>([])
  const [cities, setCities] = useState<string[]>([])
  const [localities, setLocalities] = useState<string[]>([])
  const [selectedState, setSelectedState] = useState('')
  const [selectedCity, setSelectedCity] = useState('')
  const [selectedLocality, setSelectedLocality] = useState('')
  const [properties, setProperties] = useState<PropertyCard[]>([])
  const [selectedPropertyId, setSelectedPropertyId] = useState<number | null>(null)
  const [propertyDetail, setPropertyDetail] = useState<PropertyDetail | null>(null)
  const [reviewDraft, setReviewDraft] = useState<ReviewDraft>(initialReviewDraft)
  const [replyDrafts, setReplyDrafts] = useState<Record<number, ReplyState>>({})
  const [error, setError] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)
  const [loadingSearch, setLoadingSearch] = useState(false)
  const [loadingDetail, setLoadingDetail] = useState(false)
  const [showAdminPanel, setShowAdminPanel] = useState(false)

  const oauthProviders = session?.oauthProviders ?? []
  const selectedProperty = useMemo(
    () => properties.find((property) => property.id === selectedPropertyId) ?? null,
    [properties, selectedPropertyId],
  )

  const reportError = (caughtError: unknown) => {
    const message =
      caughtError instanceof Error ? caughtError.message : 'Unexpected error'
    setError(message)
    setStatus(null)
  }

  useEffect(() => {
    let cancelled = false

    void (async () => {
      try {
        const [sessionData, statesData, propertiesData] = await Promise.all([
          api.getSession(),
          api.fetchStates(),
          api.searchProperties({}),
        ])

        if (cancelled) return

        if (window.location.search.includes('auth=success')) {
          window.history.replaceState({}, '', window.location.pathname)
        }

        setSession(sessionData)
        setSessionLoaded(true)
        if (!initialLoginDecision.current) {
          setLoginOpen(!sessionData.user)
          initialLoginDecision.current = true
        }
        setStates(statesData)
        startTransition(() => {
          setProperties(propertiesData)
          setSelectedPropertyId(propertiesData[0]?.id ?? null)
          if (propertiesData.length === 0) {
            setPropertyDetail(null)
          }
        })
      } catch (caughtError) {
        if (!cancelled) {
          reportError(caughtError)
          setSessionLoaded(true)
          if (!initialLoginDecision.current) {
            setLoginOpen(true)
            initialLoginDecision.current = true
          }
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!selectedState) {
      return
    }

    void api
      .fetchCities(selectedState)
      .then((items) => {
        setCities(items)
        setSelectedCity((current) => (items.includes(current) ? current : ''))
      })
      .catch(reportError)
  }, [selectedState])

  useEffect(() => {
    if (!selectedState || !selectedCity) {
      return
    }

    void api
      .fetchLocalities(selectedState, selectedCity)
      .then((items) => {
        setLocalities(items)
        setSelectedLocality((current) => (items.includes(current) ? current : ''))
      })
      .catch(reportError)
  }, [selectedState, selectedCity])

  useEffect(() => {
    if (selectedPropertyId == null) {
      return
    }

    let cancelled = false

    void (async () => {
      setLoadingDetail(true)
      try {
        const detail = await api.fetchPropertyDetail(selectedPropertyId)
        if (!cancelled) {
          startTransition(() => setPropertyDetail(detail))
        }
      } catch (caughtError) {
        if (!cancelled) {
          reportError(caughtError)
        }
      } finally {
        if (!cancelled) {
          setLoadingDetail(false)
        }
      }
    })()

    return () => {
      cancelled = true
    }
  }, [selectedPropertyId])

  async function handleSearch() {
    setLoadingSearch(true)
    setStatus(null)
    try {
      const preservedPropertyId = selectedPropertyId
      const results = await api.searchProperties({
        state: selectedState || undefined,
        city: selectedCity || undefined,
        locality: selectedLocality || undefined,
      })
      startTransition(() => {
        const nextSelection = results.some(
          (property) => property.id === preservedPropertyId,
        )
          ? preservedPropertyId
          : (results[0]?.id ?? null)
        setProperties(results)
        setSelectedPropertyId(nextSelection)
        if (nextSelection == null) {
          setPropertyDetail(null)
        }
      })
    } catch (caughtError) {
      reportError(caughtError)
    } finally {
      setLoadingSearch(false)
    }
  }

  async function handleRequestOtp() {
    setError(null)
    setStatus(null)
    try {
      const challenge = await api.requestOtp({
        channel: authChannel,
        destination,
        displayName,
      })
      setOtpChallenge(challenge)
      setOtpCode(challenge.devCode ?? '')
      setStatus(`OTP requested for ${challenge.destination}`)
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  async function handleVerifyOtp() {
    if (!otpChallenge) return
    setError(null)
    try {
      const sessionData = await api.verifyOtp({
        challengeId: otpChallenge.challengeId,
        code: otpCode,
        displayName,
      })
      setSession(sessionData)
      setOtpChallenge(null)
      setOtpCode('')
      setDestination('')
      setStatus('Signed in')
      setLoginOpen(false)
      if (selectedPropertyId != null) {
        const detail = await api.fetchPropertyDetail(selectedPropertyId)
        startTransition(() => setPropertyDetail(detail))
      }
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  async function handleLogout() {
    try {
      await api.logout()
      setSession((current) => (current ? { ...current, user: null } : current))
      setOtpChallenge(null)
      setStatus('Signed out')
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  async function handleCreateReview(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedPropertyId) return
    try {
      await api.createReview(selectedPropertyId, reviewDraft)
      setReviewDraft(initialReviewDraft)
      setStatus('Review submitted')
      const detail = await api.fetchPropertyDetail(selectedPropertyId)
      startTransition(() => setPropertyDetail(detail))
      await handleSearch()
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  async function handleVote(reviewId: number, type: ReviewVoteType) {
    try {
      await api.vote(reviewId, type)
      if (selectedPropertyId != null) {
        const detail = await api.fetchPropertyDetail(selectedPropertyId)
        startTransition(() => setPropertyDetail(detail))
      }
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  async function handleReply(reviewId: number) {
    const draft = replyDrafts[reviewId]
    if (!draft || !draft.body.trim()) return
    try {
      await api.addReply(reviewId, {
        body: draft.body,
        parentCommentId: draft.parentCommentId,
      })
      setReplyDrafts((current) => ({
        ...current,
        [reviewId]: { body: '', parentCommentId: null, parentLabel: null },
      }))
      setStatus('Reply added')
      if (selectedPropertyId != null) {
        const detail = await api.fetchPropertyDetail(selectedPropertyId)
        startTransition(() => setPropertyDetail(detail))
      }
    } catch (caughtError) {
      reportError(caughtError)
    }
  }

  function beginReply(reviewId: number, comment: ReviewComment) {
    setReplyDrafts((current) => ({
      ...current,
      [reviewId]: {
        body: current[reviewId]?.body ?? '',
        parentCommentId: comment.id,
        parentLabel: comment.author.displayName,
      },
    }))
  }

  function clearReplyTarget(reviewId: number) {
    setReplyDrafts((current) => ({
      ...current,
      [reviewId]: {
        body: current[reviewId]?.body ?? '',
        parentCommentId: null,
        parentLabel: null,
      },
    }))
  }

  function handleStateChange(value: string) {
    setSelectedState(value)
    setSelectedCity('')
    setSelectedLocality('')
    setCities([])
    setLocalities([])
  }

  function handleCityChange(value: string) {
    setSelectedCity(value)
    setSelectedLocality('')
    setLocalities([])
  }

  function updateReviewDraft<K extends keyof ReviewDraft>(
    key: K,
    value: ReviewDraft[K],
  ) {
    setReviewDraft((current) => ({ ...current, [key]: value }))
  }

  const nativeSelectClassName =
    'flex h-10 w-full appearance-none rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50'

  return (
    <div className='min-h-screen bg-background text-foreground'>
      <div className='mx-auto flex min-h-screen max-w-7xl flex-col gap-6 px-4 py-6 sm:px-6 lg:px-8'>
      <header className='flex flex-col gap-4 rounded-xl border border-border bg-card/80 p-5 shadow-sm backdrop-blur supports-[backdrop-filter]:bg-card/60 sm:flex-row sm:items-end sm:justify-between'>
        <div className='space-y-1'>
          <p className='text-xs font-semibold uppercase tracking-wider text-muted-foreground'>
            Tenant intelligence platform
          </p>
          <h1 className='text-3xl font-semibold tracking-tight'>RentWise</h1>
        </div>
        <div className='flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-stretch'>
          {import.meta.env.DEV ? (
            <div className='min-w-0 rounded-lg border border-input bg-background/60 p-3'>
              <p className='text-xs font-medium text-muted-foreground'>Backend</p>
              <code className='block max-w-full truncate text-xs'>{api.backendBaseUrl}</code>
            </div>
          ) : null}
          {session?.user ? (
            <div className='flex flex-wrap items-center justify-between gap-3 rounded-lg border border-input bg-background/60 p-3 sm:min-w-[320px] sm:flex-nowrap sm:justify-start'>
              <div className='min-w-0'>
                <p className='truncate text-sm font-semibold'>{session.user.displayName}</p>
                <p className='truncate text-xs text-muted-foreground'>
                  {session.user.email ?? session.user.mobileNumber}
                </p>
              </div>
              <ThemeMenu />
              {session.user.isAdmin && (
                <Button
                  type='button'
                  variant={showAdminPanel ? 'default' : 'outline'}
                  size='sm'
                  onClick={() => setShowAdminPanel(!showAdminPanel)}
                >
                  Admin
                </Button>
              )}
              <Button type='button' variant='ghost' size='sm' onClick={handleLogout}>
                Sign out
              </Button>
            </div>
          ) : (
            <div className='flex flex-wrap items-center justify-between gap-3 rounded-lg border border-input bg-background/60 p-3 sm:min-w-[320px] sm:flex-nowrap'>
              <div className='min-w-0'>
                <p className='truncate text-sm font-semibold'>Read mode</p>
                <p className='truncate text-xs text-muted-foreground'>
                  Sign in to submit reviews and vote
                </p>
              </div>
              <ThemeMenu />
              <Button
                type='button'
                variant='outline'
                size='sm'
                onClick={() => setLoginOpen(true)}
                disabled={!sessionLoaded}
              >
                Sign in
              </Button>
            </div>
          )}
        </div>
      </header>

      <Dialog open={loginOpen} onOpenChange={setLoginOpen}>
        <DialogContent className='sm:max-w-xl'>
          <DialogHeader>
            <DialogTitle>Sign in</DialogTitle>
            <DialogDescription>
              Use OTP or OAuth. You can close this dialog to continue in read mode.
            </DialogDescription>
          </DialogHeader>

          <div className='space-y-4'>
            <SegmentedControl
              aria-label='Auth channel'
              options={[
                { value: 'EMAIL', label: 'Email OTP' },
                { value: 'MOBILE', label: 'Mobile OTP' },
              ]}
              value={authChannel}
              onChange={setAuthChannel}
            />

            <div className='grid gap-3 sm:grid-cols-2'>
              <label className='space-y-1'>
                <span className='text-xs font-medium text-muted-foreground'>Name</span>
                <Input
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                  placeholder='Your display name'
                />
              </label>

              <label className='space-y-1'>
                <span className='text-xs font-medium text-muted-foreground'>
                  {authChannel === 'EMAIL' ? 'Email' : 'Mobile'}
                </span>
                <Input
                  value={destination}
                  onChange={(event) => setDestination(event.target.value)}
                  placeholder={
                    authChannel === 'EMAIL' ? 'tenant@example.com' : '+1 555 000 1001'
                  }
                />
              </label>
            </div>

            <div className='grid grid-cols-2 gap-3'>
              <Button type='button' onClick={handleRequestOtp}>
                Request OTP
              </Button>
              <Button
                type='button'
                variant='outline'
                onClick={() => {
                  setDestination('')
                  setOtpChallenge(null)
                  setOtpCode('')
                }}
              >
                Reset
              </Button>
            </div>

            {otpChallenge ? (
              <div className='space-y-3 rounded-lg border border-input bg-card p-4'>
                <label className='space-y-1'>
                  <span className='text-xs font-medium text-muted-foreground'>Enter OTP</span>
                  <Input
                    value={otpCode}
                    onChange={(event) => setOtpCode(event.target.value)}
                    placeholder='6-digit code'
                  />
                </label>
                {otpChallenge.devCode && session?.devOtpVisible !== false ? (
                  <p className='text-xs text-muted-foreground'>
                    Dev code: <strong>{otpChallenge.devCode}</strong>
                  </p>
                ) : null}
                <Button type='button' onClick={handleVerifyOtp}>
                  Verify and sign in
                </Button>
              </div>
            ) : null}

            <div className='space-y-2'>
              {(['google', 'facebook'] as const).map((provider) => {
                const enabled = oauthProviders.includes(provider)
                const label = `Continue with ${provider === 'google' ? 'Google' : 'Facebook'}`
                return enabled ? (
                  <Button key={provider} asChild variant='outline' className='w-full'>
                    <a href={api.oauthUrl(provider)}>{label}</a>
                  </Button>
                ) : (
                  <Button key={provider} variant='outline' className='w-full' disabled>
                    {label}
                  </Button>
                )
              })}
              <p className='text-xs text-muted-foreground'>
                Configure OAuth credentials on the backend to enable social sign-in.
              </p>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {showAdminPanel ? (
        <main className='min-h-0 flex-1'>
          <AdminPanel onError={reportError} onStatus={(msg) => setStatus(msg)} />
        </main>
      ) : (
        <main className='grid min-h-0 flex-1 gap-6 lg:grid-cols-[380px_1fr]'>
          <aside className='space-y-6'>
          <section className='rounded-xl border border-border bg-card p-5 shadow-sm'>
            <div className='mb-4'>
              <h2 className='text-base font-semibold'>Search</h2>
              <p className='text-xs text-muted-foreground'>State → city → locality</p>
            </div>

            <label>
              <span className='text-xs font-medium text-muted-foreground'>State</span>
              <select
                value={selectedState}
                onChange={(event) => handleStateChange(event.target.value)}
                className={nativeSelectClassName}
              >
                <option value=''>All states</option>
                {states.map((state) => (
                  <option key={state} value={state}>
                    {state}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span className='text-xs font-medium text-muted-foreground'>City</span>
              <select
                value={selectedCity}
                onChange={(event) => handleCityChange(event.target.value)}
                disabled={!selectedState}
                className={nativeSelectClassName}
              >
                <option value=''>All cities</option>
                {cities.map((city) => (
                  <option key={city} value={city}>
                    {city}
                  </option>
                ))}
              </select>
            </label>

            <label>
              <span className='text-xs font-medium text-muted-foreground'>Locality</span>
              <select
                value={selectedLocality}
                onChange={(event) => setSelectedLocality(event.target.value)}
                disabled={!selectedCity}
                className={nativeSelectClassName}
              >
                <option value=''>All localities</option>
                {localities.map((locality) => (
                  <option key={locality} value={locality}>
                    {locality}
                  </option>
                ))}
              </select>
            </label>

            <Button type='button' onClick={handleSearch} disabled={loadingSearch}>
              {loadingSearch ? 'Searching...' : 'Search properties'}
            </Button>
          </section>

          <section className='results-band'>
            <div className='section-head'>
              <h2>Properties</h2>
              <span>{properties.length} listed</span>
            </div>

            <div className='results-list'>
              {properties.map((property) => (
                <button
                  key={property.id}
                  type='button'
                  className={
                    selectedPropertyId === property.id
                      ? 'w-full rounded-xl border border-ring bg-accent/50 p-4 text-left shadow-sm transition-colors hover:bg-accent/60'
                      : 'w-full rounded-xl border border-input bg-background p-4 text-left shadow-sm transition-colors hover:bg-accent/30'
                  }
                  onClick={() => setSelectedPropertyId(property.id)}
                >
                  <div className='tile-topline'>
                    <strong>{property.title}</strong>
                    <span>{property.scorecard.overallScore}/100</span>
                  </div>
                  <p>
                    {property.locality}, {property.city}
                  </p>
                  <p>{property.landlordName}</p>
                  <small>{property.scorecard.recommendation}</small>
                </button>
              ))}

              {properties.length === 0 ? (
                <div className='empty-state'>No properties matched the selected area.</div>
              ) : null}
            </div>
          </section>
        </aside>

        <section className='detail-band'>
          {error ? <div className='banner error'>{error}</div> : null}
          {status ? <div className='banner success'>{status}</div> : null}

          {loadingDetail ? (
            <div className='empty-state'>Loading property detail...</div>
          ) : propertyDetail ? (
            <>
              <section className='summary-strip'>
                <div>
                  <p className='eyebrow'>{propertyDetail.propertyType}</p>
                  <h2>{propertyDetail.title}</h2>
                  <p className='address-line'>
                    {propertyDetail.addressLine1}, {propertyDetail.locality},{' '}
                    {propertyDetail.city}, {propertyDetail.state}
                  </p>
                  <p className='summary-copy'>{propertyDetail.highlights}</p>
                </div>

                <div className='score-row'>
                  <div className='score-chip'>
                    <span>Overall</span>
                    <strong>{propertyDetail.scorecard.overallScore}</strong>
                  </div>
                  <div className='score-chip'>
                    <span>Landlord</span>
                    <strong>{propertyDetail.scorecard.landlordScore}</strong>
                  </div>
                  <div className='score-chip'>
                    <span>Property</span>
                    <strong>{propertyDetail.scorecard.propertyScore}</strong>
                  </div>
                  <div className='score-chip accent'>
                    <span>Recommendation</span>
                    <strong>{propertyDetail.scorecard.recommendation}</strong>
                  </div>
                </div>
              </section>

              <section className='landlord-band'>
                <div className='section-head'>
                  <h2>Landlord profile</h2>
                  <span>{propertyDetail.landlord.name}</span>
                </div>
                <div className='landlord-grid'>
                  <p>
                    <strong>Email:</strong> {propertyDetail.landlord.email ?? 'Not shared'}
                  </p>
                  <p>
                    <strong>Phone:</strong>{' '}
                    {propertyDetail.landlord.phoneNumber ?? 'Not shared'}
                  </p>
                  <p className='landlord-note'>
                    {propertyDetail.landlord.managementStyle}
                  </p>
                </div>
              </section>

              <section className='review-band'>
                <div className='section-head'>
                  <h2>Tenant reviews</h2>
                  <span>
                    {propertyDetail.scorecard.reviewCount} reviews,{' '}
                    {propertyDetail.scorecard.unresolvedIssueCount} unresolved issue
                    reports
                  </span>
                </div>

                <div className='review-list'>
                  {propertyDetail.reviews.map((review) => (
                    <article key={review.id} className='review-card'>
                      <div className='review-header'>
                        <div>
                          <h3>{review.headline}</h3>
                          <p>
                            {review.author.displayName} ·{' '}
                            {new Date(review.createdAt).toLocaleDateString()}
                          </p>
                        </div>
                        <div className='rating-badges'>
                          <span>{review.overallRating}/5 overall</span>
                          <span>{review.depositRating}/5 deposit</span>
                          <span>{review.maintenanceRating}/5 maintenance</span>
                        </div>
                      </div>

                      <div className='review-copy-grid'>
                        <section>
                          <h4>Experience</h4>
                          <p>{review.experienceSummary}</p>
                        </section>
                        <section>
                          <h4>Problems faced</h4>
                          <p>{review.problemsFaced}</p>
                        </section>
                        <section>
                          <h4>Landlord response</h4>
                          <p>{review.landlordSupport}</p>
                        </section>
                        <section>
                          <h4>Lease exit or renewal</h4>
                          <p>{review.leaseClosure}</p>
                        </section>
                        <section>
                          <h4>Security deposit</h4>
                          <p>{review.securityDepositOutcome}</p>
                        </section>
                        <section>
                          <h4>Signals</h4>
                          <div className='signal-list'>
                            <span>{review.recommended ? 'Recommended' : 'Not recommended'}</span>
                            <span>
                              {review.issuesResolved ? 'Issues resolved' : 'Issues stayed open'}
                            </span>
                            <span>
                              {review.wouldRentAgain ? 'Would rent again' : 'Would not rent again'}
                            </span>
                          </div>
                        </section>
                      </div>

                      <div className='vote-row'>
                        {([
                          ['HELPFUL', 'Helpful', review.votes.helpful],
                          ['SAME_ISSUE', 'Same issue', review.votes.sameIssue],
                          ['NOT_HELPFUL', 'Not helpful', review.votes.notHelpful],
                        ] as const).map(([type, label, count]) => (
                          <Button
                            key={type}
                            type='button'
                            variant={
                              review.votes.currentUserVote === type ? 'default' : 'outline'
                            }
                            size='sm'
                            disabled={!session?.user}
                            onClick={() => handleVote(review.id, type)}
                          >
                            {label} · {count}
                          </Button>
                        ))}
                      </div>

                      <div className='thread-block'>
                        <div className='thread-list'>
                          {review.thread.map((comment) => (
                            <ThreadComment
                              key={comment.id}
                              comment={comment}
                              onReply={(item) => beginReply(review.id, item)}
                            />
                          ))}
                        </div>

                        {session?.user ? (
                          <div className='reply-form'>
                            <div className='reply-meta'>
                              <strong>Join the thread</strong>
                              {replyDrafts[review.id]?.parentLabel ? (
                                <Button
                                  type='button'
                                  variant='ghost'
                                  size='sm'
                                  onClick={() => clearReplyTarget(review.id)}
                                >
                                  Replying to {replyDrafts[review.id]?.parentLabel}
                                </Button>
                              ) : null}
                            </div>
                            <Textarea
                              value={replyDrafts[review.id]?.body ?? ''}
                              onChange={(event) =>
                                setReplyDrafts((current) => ({
                                  ...current,
                                  [review.id]: {
                                    body: event.target.value,
                                    parentCommentId:
                                      current[review.id]?.parentCommentId ?? null,
                                    parentLabel: current[review.id]?.parentLabel ?? null,
                                  },
                                }))
                              }
                              placeholder='Add context, confirm a pattern, or ask a follow-up.'
                            />
                            <Button type='button' onClick={() => handleReply(review.id)}>
                              Post reply
                            </Button>
                          </div>
                        ) : null}
                      </div>
                    </article>
                  ))}
                </div>
              </section>

              <section className='composer-band'>
                <div className='section-head'>
                  <h2>Submit a review</h2>
                  <span>{session?.user ? 'Authenticated' : 'Sign in required'}</span>
                </div>

                {session?.user ? (
                  <form className='review-form' onSubmit={handleCreateReview}>
                    <div className='form-grid'>
                      <label className='wide'>
                        <span>Headline</span>
                        <Input
                          value={reviewDraft.headline}
                          onChange={(event) =>
                            updateReviewDraft('headline', event.target.value)
                          }
                          placeholder='Short summary of the tenancy outcome'
                        />
                      </label>

                      <TextAreaField
                        label='Experience summary'
                        value={reviewDraft.experienceSummary}
                        onChange={(value) =>
                          updateReviewDraft('experienceSummary', value)
                        }
                      />
                      <TextAreaField
                        label='Problems faced'
                        value={reviewDraft.problemsFaced}
                        onChange={(value) => updateReviewDraft('problemsFaced', value)}
                      />
                      <TextAreaField
                        label='How helpful was the landlord?'
                        value={reviewDraft.landlordSupport}
                        onChange={(value) => updateReviewDraft('landlordSupport', value)}
                      />
                      <TextAreaField
                        label='Termination or renewal'
                        value={reviewDraft.leaseClosure}
                        onChange={(value) => updateReviewDraft('leaseClosure', value)}
                      />
                      <TextAreaField
                        label='Security deposit outcome'
                        value={reviewDraft.securityDepositOutcome}
                        onChange={(value) =>
                          updateReviewDraft('securityDepositOutcome', value)
                        }
                      />
                    </div>

                    <div className='slider-grid'>
                      <RatingSlider
                        label='Overall'
                        value={reviewDraft.overallRating}
                        onChange={(value) => updateReviewDraft('overallRating', value)}
                      />
                      <RatingSlider
                        label='Landlord'
                        value={reviewDraft.landlordRating}
                        onChange={(value) => updateReviewDraft('landlordRating', value)}
                      />
                      <RatingSlider
                        label='Property'
                        value={reviewDraft.propertyRating}
                        onChange={(value) => updateReviewDraft('propertyRating', value)}
                      />
                      <RatingSlider
                        label='Maintenance'
                        value={reviewDraft.maintenanceRating}
                        onChange={(value) =>
                          updateReviewDraft('maintenanceRating', value)
                        }
                      />
                      <RatingSlider
                        label='Move-out'
                        value={reviewDraft.moveOutRating}
                        onChange={(value) => updateReviewDraft('moveOutRating', value)}
                      />
                      <RatingSlider
                        label='Deposit'
                        value={reviewDraft.depositRating}
                        onChange={(value) => updateReviewDraft('depositRating', value)}
                      />
                    </div>

                    <div className='checkbox-row'>
                      <label className='checkbox-field'>
                        <input
                          type='checkbox'
                          className='h-4 w-4 rounded border border-input bg-background text-primary accent-[hsl(var(--primary))]'
                          checked={reviewDraft.recommended}
                          onChange={(event) =>
                            updateReviewDraft('recommended', event.target.checked)
                          }
                        />
                        <span>Recommend this tenancy</span>
                      </label>
                      <label className='checkbox-field'>
                        <input
                          type='checkbox'
                          className='h-4 w-4 rounded border border-input bg-background text-primary accent-[hsl(var(--primary))]'
                          checked={reviewDraft.issuesResolved}
                          onChange={(event) =>
                            updateReviewDraft('issuesResolved', event.target.checked)
                          }
                        />
                        <span>Core issues were resolved</span>
                      </label>
                      <label className='checkbox-field'>
                        <input
                          type='checkbox'
                          className='h-4 w-4 rounded border border-input bg-background text-primary accent-[hsl(var(--primary))]'
                          checked={reviewDraft.wouldRentAgain}
                          onChange={(event) =>
                            updateReviewDraft('wouldRentAgain', event.target.checked)
                          }
                        />
                        <span>Would rent here again</span>
                      </label>
                    </div>

                    <Button type='submit'>Publish review</Button>
                  </form>
                ) : (
                  <div className='empty-state'>
                    Sign in with OTP or configure OAuth to add your own review.
                  </div>
                )}
              </section>
            </>
          ) : (
            <div className='empty-state'>
              {selectedProperty
                ? 'Property details are not available yet.'
                : 'Choose a property from the list to inspect the review thread.'}
            </div>
          )}
        </section>
        </main>
      )}
      </div>
    </div>
  )
}

function TextAreaField(props: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label>
      <span className='text-xs font-medium text-muted-foreground'>{props.label}</span>
      <Textarea
        value={props.value}
        onChange={(event) => props.onChange(event.target.value)}
      />
    </label>
  )
}

function RatingSlider(props: {
  label: string
  value: number
  onChange: (value: number) => void
}) {
  return (
    <label className='slider-field'>
      <div className='slider-label'>
        <span>{props.label}</span>
        <strong>{props.value}</strong>
      </div>
      <input
        type='range'
        min='1'
        max='5'
        step='1'
        value={props.value}
        onChange={(event) => props.onChange(Number(event.target.value))}
        className='h-2 w-full cursor-pointer appearance-none rounded-full bg-muted accent-[hsl(var(--primary))]'
      />
    </label>
  )
}

function ThreadComment(props: {
  comment: ReviewComment
  onReply: (comment: ReviewComment) => void
}) {
  return (
    <div className='comment-node'>
      <div className='comment-body'>
        <p>
          <strong>{props.comment.author.displayName}</strong> ·{' '}
          {new Date(props.comment.createdAt).toLocaleDateString()}
        </p>
        <p>{props.comment.body}</p>
        <Button type='button' variant='ghost' size='sm' onClick={() => props.onReply(props.comment)}>
          Reply
        </Button>
      </div>
      {props.comment.replies.length > 0 ? (
        <div className='comment-children'>
          {props.comment.replies.map((reply) => (
            <ThreadComment key={reply.id} comment={reply} onReply={props.onReply} />
          ))}
        </div>
      ) : null}
    </div>
  )
}

export default App
