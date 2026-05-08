import { useState } from 'react'
import { SegmentedControl } from '@/components/common'
import { CountriesManager } from './CountriesManager'
import { StatesManager } from './StatesManager'
import { CitiesManager } from './CitiesManager'
import { PropertiesApprovalManager } from './PropertiesApprovalManager'

interface AdminPanelProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

type AdminTab = 'countries' | 'states' | 'cities' | 'properties'

export function AdminPanel({ onError, onStatus }: AdminPanelProps) {
  const [activeTab, setActiveTab] = useState<AdminTab>('countries')

  return (
    <div className='admin-panel'>
      <div className='admin-header'>
        <h2>Location Management</h2>
        <p>Manage countries, states, and cities for property listings</p>
      </div>

      <div className='admin-tabs'>
        <SegmentedControl
          aria-label='Admin section'
          options={[
            { value: 'countries', label: 'Countries' },
            { value: 'states', label: 'States' },
            { value: 'cities', label: 'Cities' },
            { value: 'properties', label: 'Approvals' },
          ]}
          value={activeTab}
          onChange={setActiveTab}
        />
      </div>

      <div className='admin-content'>
        {activeTab === 'countries' && <CountriesManager onError={onError} onStatus={onStatus} />}
        {activeTab === 'states' && <StatesManager onError={onError} onStatus={onStatus} />}
        {activeTab === 'cities' && <CitiesManager onError={onError} onStatus={onStatus} />}
        {activeTab === 'properties' && (
          <PropertiesApprovalManager onError={onError} onStatus={onStatus} />
        )}
      </div>
    </div>
  )
}
