import { useState, useEffect } from 'react'
import { api } from './api'
import { Button, Input } from '@/components/common'
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
  const selectlessInputClassName = 'space-y-1'

  useEffect(() => {
    let cancelled = false

    void (async () => {
      setLoading(true)
      try {
        const data = await api.getAllCountries()
        if (!cancelled) setCountries(data)
      } catch (error) {
        if (!cancelled) onError(error)
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [onError])

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
        <label className={selectlessInputClassName}>
          <span className='text-xs font-medium text-muted-foreground'>Code</span>
          <Input
            value={formData.code}
            onChange={(e) => setFormData({ ...formData, code: e.target.value })}
            placeholder='US, UK, CA'
          />
        </label>
        <label className={selectlessInputClassName}>
          <span className='text-xs font-medium text-muted-foreground'>Name</span>
          <Input
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder='United States'
          />
        </label>
        <Button type='submit'>Add Country</Button>
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
                  <Button
                    type='button'
                    variant='ghost'
                    size='sm'
                    onClick={() =>
                      setExpandedCountryId(
                        expandedCountryId === country.id ? null : country.id,
                      )
                    }
                  >
                    {expandedCountryId === country.id ? 'Hide' : 'View states'}
                  </Button>
                  <Button
                    type='button'
                    variant='ghost'
                    size='sm'
                    onClick={() => handleDeleteCountry(country.id)}
                  >
                    Delete
                  </Button>
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
