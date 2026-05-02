import { useState } from 'react'
import { CountriesManager } from './CountriesManager'
import { StatesManager } from './StatesManager'
import { CitiesManager } from './CitiesManager'

interface AdminPanelProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

type AdminTab = 'countries' | 'states' | 'cities'

export function AdminPanel({ onError, onStatus }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>('countries')

  return (
    <div className='admin-panel'>
      <div className='admin-header'>
        <h2>Location Management</h2>
        <p>Manage countries, states, and cities for property listings</p>
      </div>

      <div className='admin-tabs'>
        {(['countries', 'states', 'cities'] as const).map((tab) => (
          <button
            key={tab}
            type='button'
            className={activeTab === tab ? 'tab-button active' : 'tab-button'}
            onClick={() => setActiveTab(tab)}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      <div className='admin-content'>
        {activeTab === 'countries' && <CountriesManager onError={onError} onStatus={onStatus} />}
        {activeTab === 'states' && <StatesManager onError={onError} onStatus={onStatus} />}
        {activeTab === 'cities' && <CitiesManager onError={onError} onStatus={onStatus} />}
      </div>
    </div>
  )
}
