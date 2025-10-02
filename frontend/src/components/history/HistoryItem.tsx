import { formatDistanceToNow } from 'date-fns'
import { cn } from '@/lib/utils'
import { ExecutionStatus } from '@/types/api.types'
import type { QueryHistoryResponse } from '@/types/api.types'

interface HistoryItemProps {
  item: QueryHistoryResponse
}

export function HistoryItem({ item }: HistoryItemProps) {
  const statusColor = {
    [ExecutionStatus.SUCCESS]: 'bg-green-100 text-green-800',
    [ExecutionStatus.FAILED]: 'bg-red-100 text-red-800',
    [ExecutionStatus.TIMEOUT]: 'bg-yellow-100 text-yellow-800',
  }

  return (
    <div className="rounded-lg border p-4 hover:bg-gray-50 transition-colors">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <p className="font-medium text-gray-900 truncate">{item.question}</p>
          <p className="text-sm text-gray-500 mt-1 truncate">{item.sql}</p>
        </div>
        <div className="flex flex-col items-end gap-2 shrink-0">
          <span
            className={cn(
              'px-2 py-1 rounded-full text-xs font-medium',
              statusColor[item.status]
            )}
          >
            {item.status}
          </span>
          <span className="text-xs text-gray-500">
            {formatDistanceToNow(new Date(item.executedAt), { addSuffix: true })}
          </span>
        </div>
      </div>
      <div className="mt-2 flex items-center gap-4 text-xs text-gray-600">
        <span>Provider: {item.provider}</span>
        <span>Rows: {item.rowCount}</span>
      </div>
    </div>
  )
}
