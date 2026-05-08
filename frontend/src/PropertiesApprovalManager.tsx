import { useCallback, useEffect, useState } from 'react'
import { api } from './api'
import type { PropertyCard } from './types'
import { Button } from '@/components/common'

interface PropertiesApprovalManagerProps {
  onError: (error: unknown) => void
  onStatus: (status: string) => void
}

export function PropertiesApprovalManager({ onError, onStatus }: PropertiesApprovalManagerProps) {
  const [pendingProperties, setPendingProperties] = useState<PropertyCard[]>([])
  const [loading, setLoading] = useState(true)
  const [approvingId, setApprovingId] = useState<number | null>(null)

  const loadPendingProperties = useCallback(async () => {
    setLoading(true)
    try {
      const response = await api.getPendingProperties()
      setPendingProperties(response.content)
    } catch (error) {
      onError(error)
    } finally {
      setLoading(false)
    }
  }, [onError])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadPendingProperties()
  }, [loadPendingProperties])

  async function handleApprove(propertyId: number) {
    setApprovingId(propertyId)
    try {
      await api.approveProperty(propertyId)
      setPendingProperties((current) => current.filter((property) => property.id !== propertyId))
      onStatus('Property approved and now visible to users')
    } catch (error) {
      onError(error)
    } finally {
      setApprovingId(null)
    }
  }

  return (
    <div className='admin-section'>
      <h3>Property Approvals</h3>
      {loading ? (
        <p>Loading pending properties...</p>
      ) : pendingProperties.length === 0 ? (
        <p>No pending properties to approve</p>
      ) : (
        <div className='items-grid'>
          {pendingProperties.map((property) => (
            <div key={property.id} className='item-card'>
              <div className='item-header'>
                <div>
                  <strong>{property.title}</strong>
                  <small>
                    {property.locality}, {property.city}, {property.state}
                  </small>
                </div>
                <Button
                  type='button'
                  size='sm'
                  onClick={() => handleApprove(property.id)}
                  disabled={approvingId === property.id}
                >
                  {approvingId === property.id ? 'Approving...' : 'Approve'}
                </Button>
              </div>
              <p>
                <strong>Type:</strong> {property.propertyType}
              </p>
              <p>
                <strong>Landlord:</strong> {property.landlordName}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
