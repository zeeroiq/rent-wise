import { startTransition, type FormEvent, useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { AdminPanel } from './AdminPanel'
import { useTheme } from './hooks/useTheme'
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
  const { themePreference, resolvedTheme, setThemePreference } = useTheme()
  const [session, setSession] = useState<AuthSession | null>(null)
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

  return (
    <div className='app-shell'>
      <header className='topbar'>
        <div>
          <p className='eyebrow'>Tenant intelligence platform</p>
          <h1>RentWise</h1>
        </div>
        <div className='status-strip'>
          <div className='status-item'>
            <span className='status-label'>Backend</span>
            <code>{api.backendBaseUrl}</code>
          </div>
          <div className='session-badge'>
            <strong>Theme</strong>
            <span className='status-label'>
              {themePreference === 'system'
                ? `System (${resolvedTheme})`
                : themePreference}
            </span>
            <div className='segmented-control three' role='group' aria-label='Theme'>
              {(['system', 'light', 'dark'] as const).map((mode) => (
                <button
                  key={mode}
                  type='button'
                  className={themePreference === mode ? 'segment active' : 'segment'}
                  onClick={() => setThemePreference(mode)}
                >
                  {mode[0]!.toUpperCase()}
                  {mode.slice(1)}
                </button>
              ))}
            </div>
          </div>
          {session?.user ? (
            <div className='session-badge'>
              <strong>{session.user.displayName}</strong>
              <span>{session.user.email ?? session.user.mobileNumber}</span>
              {session.user.isAdmin && (
                <button
                  type='button'
                  className={showAdminPanel ? 'admin-badge active' : 'admin-badge'}
                  onClick={() => setShowAdminPanel(!showAdminPanel)}
                >
                  Admin
                </button>
              )}
              <button type='button' className='ghost-button' onClick={handleLogout}>
                Sign out
              </button>
            </div>
          ) : (
            <div className='session-badge'>
              <strong>Read mode</strong>
              <span>Sign in to submit reviews and vote</span>
            </div>
          )}
        </div>
      </header>

      {showAdminPanel ? (
        <main className='workspace admin-view'>
          <AdminPanel onError={reportError} onStatus={(msg) => setStatus(msg)} />
        </main>
      ) : (
        <main className='workspace'>
          <aside className='left-rail'>
            <section className='panel'>
              <div className='section-head'>
                <h2>Access</h2>
                <span>{session?.user ? 'Active' : 'Guest'}</span>
              </div>

              {!session?.user ? (
                <>
                  <div className='segmented-control' role='tablist' aria-label='Auth channel'>
                    {(['EMAIL', 'MOBILE'] as AuthChannel[]).map((channel) => (
                      <button
                        key={channel}
                        type='button'
                        className={authChannel === channel ? 'segment active' : 'segment'}
                        onClick={() => setAuthChannel(channel)}
                      >
                        {channel === 'EMAIL' ? 'Email OTP' : 'Mobile OTP'}
                      </button>
                    ))}
                  </div>

                  <label>
                    <span>Name</span>
                    <input
                      value={displayName}
                      onChange={(event) => setDisplayName(event.target.value)}
                      placeholder='Your display name'
                    />
                  </label>

                  <label>
                    <span>{authChannel === 'EMAIL' ? 'Email' : 'Mobile'}</span>
                    <input
                      value={destination}
                      onChange={(event) => setDestination(event.target.value)}
                      placeholder={
                        authChannel === 'EMAIL'
                          ? 'tenant@example.com'
                          : '+1 555 000 1001'
                      }
                    />
                  </label>

                  <div className='inline-actions'>
                    <button type='button' onClick={handleRequestOtp}>
                      Request OTP
                    </button>
                    <button
                      type='button'
                      className='ghost-button'
                      onClick={() => {
                        setDestination('')
                        setOtpChallenge(null)
                        setOtpCode('')
                      }}
                    >
                      Reset
                    </button>
                  </div>

                  {otpChallenge ? (
                    <div className='otp-box'>
                      <label>
                        <span>Enter OTP</span>
                        <input
                          value={otpCode}
                          onChange={(event) => setOtpCode(event.target.value)}
                          placeholder='6-digit code'
                        />
                      </label>
                      {otpChallenge.devCode && session?.devOtpVisible !== false ? (
                        <p className='helper-copy'>
                          Dev code: <strong>{otpChallenge.devCode}</strong>
                        </p>
                      ) : null}
                      <button type='button' onClick={handleVerifyOtp}>
                        Verify and sign in
                      </button>
                    </div>
                  ) : null}

                  <div className='oauth-stack'>
                  {['google', 'facebook'].map((provider) => {
                    const enabled = oauthProviders.includes(provider)
                    return (
                      <a
                        key={provider}
                        href={enabled ? api.oauthUrl(provider) : undefined}
                        className={enabled ? 'oauth-button' : 'oauth-button disabled'}
                        aria-disabled={!enabled}
                      >
                        Continue with {provider === 'google' ? 'Google' : 'Facebook'}
                      </a>
                    )
                  })}
                  <p className='helper-copy'>
                    Configure Google or Facebook OAuth credentials on the backend to
                    enable social sign-in.
                  </p>
                </div>
              </>
            ) : (
              <div className='signed-in-note'>
                <p>Signed in as {session.user.displayName}.</p>
                <p>Reviews, replies, and votes are now enabled.</p>
              </div>
            )}
          </section>

          <section className='panel'>
            <div className='section-head'>
              <h2>Search</h2>
              <span>State → city → locality</span>
            </div>

            <label>
              <span>State</span>
              <select
                value={selectedState}
                onChange={(event) => handleStateChange(event.target.value)}
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
              <span>City</span>
              <select
                value={selectedCity}
                onChange={(event) => handleCityChange(event.target.value)}
                disabled={!selectedState}
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
              <span>Locality</span>
              <select
                value={selectedLocality}
                onChange={(event) => setSelectedLocality(event.target.value)}
                disabled={!selectedCity}
              >
                <option value=''>All localities</option>
                {localities.map((locality) => (
                  <option key={locality} value={locality}>
                    {locality}
                  </option>
                ))}
              </select>
            </label>

            <button type='button' onClick={handleSearch}>
              {loadingSearch ? 'Searching...' : 'Search properties'}
            </button>
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
                      ? 'property-tile active'
                      : 'property-tile'
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
                          <button
                            key={type}
                            type='button'
                            className={
                              review.votes.currentUserVote === type
                                ? 'vote-button active'
                                : 'vote-button'
                            }
                            disabled={!session?.user}
                            onClick={() => handleVote(review.id, type)}
                          >
                            {label} · {count}
                          </button>
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
                                <button
                                  type='button'
                                  className='ghost-button'
                                  onClick={() => clearReplyTarget(review.id)}
                                >
                                  Replying to {replyDrafts[review.id]?.parentLabel}
                                </button>
                              ) : null}
                            </div>
                            <textarea
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
                            <button type='button' onClick={() => handleReply(review.id)}>
                              Post reply
                            </button>
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
                        <input
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
                          checked={reviewDraft.wouldRentAgain}
                          onChange={(event) =>
                            updateReviewDraft('wouldRentAgain', event.target.checked)
                          }
                        />
                        <span>Would rent here again</span>
                      </label>
                    </div>

                    <button type='submit'>Publish review</button>
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
  )
}

function TextAreaField(props: {
  label: string
  value: string
  onChange: (value: string) => void
}) {
  return (
    <label>
      <span>{props.label}</span>
      <textarea value={props.value} onChange={(event) => props.onChange(event.target.value)} />
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
        <button
          type='button'
          className='ghost-button'
          onClick={() => props.onReply(props.comment)}
        >
          Reply
        </button>
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
