import { useState, useEffect } from 'react'
import { api } from './api'
import { Button, Input } from '@/components/common'
import type { CountryDto, StateDto } from './types'

interface StatesManagerProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

export function StatesManager({ onError, onStatus }: StatesManagerProps) {
  const [countries, setCountries] = useState<CountryDto[]>([])
  const [states, setStates] = useState<StateDto[]>([])
  const [selectedCountryId, setSelectedCountryId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState({ code: '', name: '' })
  const [expandedStateId, setExpandedStateId] = useState<number | null>(null)
  const nativeSelectClassName =
    'flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50'
  const fieldClassName = 'space-y-1'

  useEffect(() => {
    let cancelled = false

    void (async () => {
      try {
        const data = await api.getAllCountries()
        if (!cancelled) setCountries(data)
      } catch (error) {
        if (!cancelled) onError(error)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [onError])

  async function loadStates(countryId: number) {
    setLoading(true)
    try {
      const data = await api.getStatesByCountry(countryId)
      setStates(data)
    } catch (error) {
      onError(error)
    } finally {
      setLoading(false)
    }
  }

  function handleCountryChange(nextCountryId: number | null) {
    setSelectedCountryId(nextCountryId)
    setExpandedStateId(null)
    setStates([])

    if (nextCountryId != null) {
      void loadStates(nextCountryId)
    }
  }

  async function handleCreateState(e: React.FormEvent) {
    e.preventDefault()
    if (selectedCountryId === null) {
      onError(new Error('Please select a country'))
      return
    }
    if (!formData.code.trim() || !formData.name.trim()) {
      onError(new Error('Code and name are required'))
      return
    }

    try {
      const newState = await api.createState({
        countryId: selectedCountryId,
        code: formData.code,
        name: formData.name,
      })
      setStates([...states, newState])
      setFormData({ code: '', name: '' })
      onStatus('State created successfully')
    } catch (error) {
      onError(error)
    }
  }

  async function handleDeleteState(stateId: number) {
    if (!window.confirm('Are you sure you want to delete this state?')) {
      return
    }

    try {
      await api.deleteState(stateId)
      setStates(states.filter((s) => s.id !== stateId))
      onStatus('State deleted successfully')
    } catch (error) {
      onError(error)
    }
  }

  return (
    <div className='admin-section'>
      <h3>States</h3>

      <label>
        <span className='text-xs font-medium text-muted-foreground'>Select Country</span>
        <select
          value={selectedCountryId ?? ''}
          onChange={(e) => handleCountryChange(e.target.value ? Number(e.target.value) : null)}
          className={nativeSelectClassName}
        >
          <option value=''>Choose a country</option>
          {countries.map((country) => (
            <option key={country.id} value={country.id}>
              {country.name}
            </option>
          ))}
        </select>
      </label>

      {selectedCountryId !== null && (
        <>
          <form onSubmit={handleCreateState} className='admin-form'>
            <label className={fieldClassName}>
              <span className='text-xs font-medium text-muted-foreground'>Code</span>
              <Input
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                placeholder='CA, TX, NY'
              />
            </label>
            <label className={fieldClassName}>
              <span className='text-xs font-medium text-muted-foreground'>Name</span>
              <Input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder='California'
              />
            </label>
            <Button type='submit'>Add State</Button>
          </form>

          <div className='items-list'>
            {loading ? (
              <p>Loading states...</p>
            ) : states.length === 0 ? (
              <p>No states found</p>
            ) : (
              states.map((state) => (
                <div key={state.id} className='item-card'>
                  <div className='item-header'>
                    <div>
                      <strong>{state.name}</strong>
                      <small>{state.code}</small>
                    </div>
                    <div className='item-actions'>
                      <Button
                        type='button'
                        variant='ghost'
                        size='sm'
                        onClick={() =>
                          setExpandedStateId(expandedStateId === state.id ? null : state.id)
                        }
                      >
                        {expandedStateId === state.id ? 'Hide' : 'Details'}
                      </Button>
                      <Button
                        type='button'
                        variant='ghost'
                        size='sm'
                        onClick={() => handleDeleteState(state.id)}
                      >
                        Delete
                      </Button>
                    </div>
                  </div>
                  {expandedStateId === state.id && (
                    <div className='item-details'>
                      <p>Created: {new Date(state.createdAt).toLocaleDateString()}</p>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </>
      )}
    </div>
  )
}
