import { useState, useEffect } from 'react'
import { api } from './api'
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

  useEffect(() => {
    loadCountries()
  }, [])

  useEffect(() => {
    if (selectedCountryId !== null) {
      loadStates(selectedCountryId)
    } else {
      setStates([])
    }
  }, [selectedCountryId])

  async function loadCountries() {
    try {
      const data = await api.getAllCountries()
      setCountries(data)
    } catch (error) {
      onError(error)
    }
  }

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
    <div className="admin-section">
      <h3>States</h3>

      <label>
        <span>Select Country</span>
        <select
          value={selectedCountryId ?? ''}
          onChange={(e) => setSelectedCountryId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">Choose a country</option>
          {countries.map((country) => (
            <option key={country.id} value={country.id}>
              {country.name}
            </option>
          ))}
        </select>
      </label>

      {selectedCountryId !== null && (
        <>
          <form onSubmit={handleCreateState} className="admin-form">
            <label>
              <span>Code</span>
              <input
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                placeholder="CA, TX, NY"
              />
            </label>
            <label>
              <span>Name</span>
              <input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="California"
              />
            </label>
            <button type="submit">Add State</button>
          </form>

          <div className="items-list">
            {loading ? (
              <p>Loading states...</p>
            ) : states.length === 0 ? (
              <p>No states found</p>
            ) : (
              states.map((state) => (
                <div key={state.id} className="item-card">
                  <div className="item-header">
                    <div>
                      <strong>{state.name}</strong>
                      <small>{state.code}</small>
                    </div>
                    <div className="item-actions">
                      <button
                        type="button"
                        className="ghost-button"
                        onClick={() =>
                          setExpandedStateId(expandedStateId === state.id ? null : state.id)
                        }
                      >
                        {expandedStateId === state.id ? 'Hide' : 'Details'}
                      </button>
                      <button
                        type="button"
                        className="ghost-button danger"
                        onClick={() => handleDeleteState(state.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  {expandedStateId === state.id && (
                    <div className="item-details">
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
