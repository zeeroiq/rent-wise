import { useState, useEffect } from 'react'
import { api } from './api'
import type { CountryDto, StateDto, CityDto } from './types'

interface CitiesManagerProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

export function CitiesManager({ onError, onStatus }: CitiesManagerProps) {
  const [countries, setCountries] = useState<CountryDto[]>([])
  const [states, setStates] = useState<StateDto[]>([])
  const [cities, setCities] = useState<CityDto[]>([])
  const [selectedCountryId, setSelectedCountryId] = useState<number | null>(null)
  const [selectedStateId, setSelectedStateId] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState({ code: '', name: '' })
  const [expandedCityId, setExpandedCityId] = useState<number | null>(null)

  useEffect(() => {
    loadCountries()
  }, [])

  useEffect(() => {
    if (selectedCountryId !== null) {
      loadStates(selectedCountryId)
      setSelectedStateId(null)
      setCities([])
    }
  }, [selectedCountryId])

  useEffect(() => {
    if (selectedStateId !== null) {
      loadCities(selectedStateId)
    } else {
      setCities([])
    }
  }, [selectedStateId])

  async function loadCountries() {
    try {
      const data = await api.getAllCountries()
      setCountries(data)
    } catch (error) {
      onError(error)
    }
  }

  async function loadStates(countryId: number) {
    try {
      const data = await api.getStatesByCountry(countryId)
      setStates(data)
    } catch (error) {
      onError(error)
    }
  }

  async function loadCities(stateId: number) {
    setLoading(true)
    try {
      const data = await api.getCitiesByState(stateId)
      setCities(data)
    } catch (error) {
      onError(error)
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateCity(e: React.FormEvent) {
    e.preventDefault()
    if (selectedStateId === null) {
      onError(new Error('Please select a state'))
      return
    }
    if (!formData.code.trim() || !formData.name.trim()) {
      onError(new Error('Code and name are required'))
      return
    }

    try {
      const newCity = await api.createCity({
        stateId: selectedStateId,
        code: formData.code,
        name: formData.name,
      })
      setCities([...cities, newCity])
      setFormData({ code: '', name: '' })
      onStatus('City created successfully')
    } catch (error) {
      onError(error)
    }
  }

  async function handleDeleteCity(cityId: number) {
    if (!window.confirm('Are you sure you want to delete this city?')) {
      return
    }

    try {
      await api.deleteCity(cityId)
      setCities(cities.filter((c) => c.id !== cityId))
      onStatus('City deleted successfully')
    } catch (error) {
      onError(error)
    }
  }

  return (
    <div className='admin-section'>
      <h3>Cities</h3>

      <label>
        <span>Select Country</span>
        <select
          value={selectedCountryId ?? ''}
          onChange={(e) => setSelectedCountryId(e.target.value ? Number(e.target.value) : null)}
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
        <label>
          <span>Select State</span>
          <select
            value={selectedStateId ?? ''}
            onChange={(e) => setSelectedStateId(e.target.value ? Number(e.target.value) : null)}
          >
            <option value=''>Choose a state</option>
            {states.map((state) => (
              <option key={state.id} value={state.id}>
                {state.name}
              </option>
            ))}
          </select>
        </label>
      )}

      {selectedStateId !== null && (
        <>
          <form onSubmit={handleCreateCity} className='admin-form'>
            <label>
              <span>Code</span>
              <input
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                placeholder='SF, NYC, LA'
              />
            </label>
            <label>
              <span>Name</span>
              <input
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder='San Francisco'
              />
            </label>
            <button type='submit'>Add City</button>
          </form>

          <div className='items-list'>
            {loading ? (
              <p>Loading cities...</p>
            ) : cities.length === 0 ? (
              <p>No cities found</p>
            ) : (
              cities.map((city) => (
                <div key={city.id} className='item-card'>
                  <div className='item-header'>
                    <div>
                      <strong>{city.name}</strong>
                      <small>{city.code}</small>
                    </div>
                    <div className='item-actions'>
                      <button
                        type='button'
                        className='ghost-button'
                        onClick={() =>
                          setExpandedCityId(expandedCityId === city.id ? null : city.id)
                        }
                      >
                        {expandedCityId === city.id ? 'Hide' : 'Details'}
                      </button>
                      <button
                        type='button'
                        className='ghost-button danger'
                        onClick={() => handleDeleteCity(city.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  {expandedCityId === city.id && (
                    <div className='item-details'>
                      <p>Created: {new Date(city.createdAt).toLocaleDateString()}</p>
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
