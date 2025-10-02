import { CheckCircle, XCircle, Clock } from 'lucide-react'
import { cn } from '@/lib/utils'
import { ExecutionStatus as Status } from '@/types/api.types'

interface ExecutionStatusProps {
  status: Status
  rowCount: number
  executionTimeMs: number
  errorMessage?: string
}

export function ExecutionStatus({
  status,
  rowCount,
  executionTimeMs,
  errorMessage,
}: ExecutionStatusProps) {
  const statusConfig = {
    [Status.SUCCESS]: {
      icon: CheckCircle,
      color: 'text-green-600',
      bgColor: 'bg-green-50',
      label: 'Success',
    },
    [Status.FAILED]: {
      icon: XCircle,
      color: 'text-red-600',
      bgColor: 'bg-red-50',
      label: 'Failed',
    },
    [Status.TIMEOUT]: {
      icon: Clock,
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-50',
      label: 'Timeout',
    },
  }

  const config = statusConfig[status]
  const Icon = config.icon

  return (
    <div className={cn('rounded-lg p-4', config.bgColor)}>
      <div className="flex items-center gap-2">
        <Icon className={cn('h-5 w-5', config.color)} />
        <span className={cn('font-medium', config.color)}>
          {config.label}
        </span>
        {status === Status.SUCCESS && (
          <span className="text-sm text-gray-600">
            " {rowCount} rows " {executionTimeMs}ms
          </span>
        )}
      </div>
      {errorMessage && (
        <p className="mt-2 text-sm text-red-700">{errorMessage}</p>
      )}
    </div>
  )
}
