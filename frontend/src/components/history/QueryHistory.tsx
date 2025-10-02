import { Dialog, DialogContent, DialogTitle, DialogTrigger } from '@/components/ui/Dialog'
import { Button } from '@/components/ui/Button'
import { History } from 'lucide-react'
import { useQueryHistory } from '@/hooks/useQueryHistory'
import { HistoryItem } from './HistoryItem'

export function QueryHistory() {
  const { data: history, isLoading } = useQueryHistory()

  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="secondary">
          <History className="mr-2 h-4 w-4" />
          History
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-3xl max-h-[80vh] overflow-y-auto">
        <DialogTitle>Query History</DialogTitle>
        {isLoading ? (
          <div className="py-8 text-center text-gray-500">Loading...</div>
        ) : history && history.length > 0 ? (
          <div className="space-y-2 mt-4">
            {history.map((item) => (
              <HistoryItem key={item.naturalLanguageQueryId} item={item} />
            ))}
          </div>
        ) : (
          <div className="py-8 text-center text-gray-500">
            No query history yet
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
