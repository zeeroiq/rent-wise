import { useState, useEffect } from 'react'
import { api } from './api'
import type { CountryDto, CreateCountryCommand } from './types'

interface CountriesManagerProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

export function CountriesManager({ onError, onStatus }: CountriesManagerProps) {
  const [countries, setCountries] = useState<CountryDto[]>([])
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState({ code: '', name: '' })
  const [expandedCountryId, setExpandedCountryId] = useState<number | null>(null)

  useEffect(() => {
    loadCountries()
  }, [])

  async function loadCountries() {
    setLoading(true)
    try {
      const data = await api.getAllCountries()
      setCountries(data)
    } catch (error) {
      onError(error)
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateCountry(e: React.FormEvent) {
    e.preventDefault()
    if (!formData.code.trim() || !formData.name.trim()) {
      onError(new Error('Code and name are required'))
      return
    }

    try {
      const newCountry = await api.createCountry(formData as CreateCountryCommand)
      setCountries([...countries, newCountry])
      setFormData({ code: '', name: '' })
      onStatus('Country created successfully')
    } catch (error) {
      onError(error)
    }
  }

  async function handleDeleteCountry(countryId: number) {
    if (!window.confirm('Are you sure you want to delete this country?')) {
      return
    }

    try {
      await api.deleteCountry(countryId)
      setCountries(countries.filter((c) => c.id !== countryId))
      onStatus('Country deleted successfully')
    } catch (error) {
      onError(error)
    }
  }

  return (
    <div className='admin-section'>
      <h3>Countries</h3>

      <form onSubmit={handleCreateCountry} className='admin-form'>
        <label>
          <span>Code</span>
          <input
            value={formData.code}
            onChange={(e) => setFormData({ ...formData, code: e.target.value })}
            placeholder='US, UK, CA'
          />
        </label>
        <label>
          <span>Name</span>
          <input
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder='United States'
          />
        </label>
        <button type='submit'>Add Country</button>
      </form>

      <div className='items-list'>
        {loading ? (
          <p>Loading countries...</p>
        ) : countries.length === 0 ? (
          <p>No countries found</p>
        ) : (
          countries.map((country) => (
            <div key={country.id} className='item-card'>
              <div className='item-header'>
                <div>
                  <strong>{country.name}</strong>
                  <small>{country.code}</small>
                </div>
                <div className='item-actions'>
                  <button
                    type='button'
                    className='ghost-button'
                    onClick={() =>
                      setExpandedCountryId(
                        expandedCountryId === country.id ? null : country.id,
                      )
                    }
                  >
                    {expandedCountryId === country.id ? 'Hide' : 'View states'}
                  </button>
                  <button
                    type='button'
                    className='ghost-button danger'
                    onClick={() => handleDeleteCountry(country.id)}
                  >
                    Delete
                  </button>
                </div>
              </div>
              {expandedCountryId === country.id && (
                <div className='item-details'>
                  <p>Created: {new Date(country.createdAt).toLocaleDateString()}</p>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  )
}
