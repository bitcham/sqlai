import { useState } from 'react'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/Collapsible'
import { Button } from '@/components/ui/Button'
import { ChevronDown, Copy, Check } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { AIProviderType } from '@/types/api.types'

interface SqlViewerProps {
  sql: string
  explanation?: string
  provider: AIProviderType
  executionTimeMs: number
}

export function SqlViewer({
  sql,
  explanation,
  provider,
  executionTimeMs,
}: SqlViewerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(sql)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <div className="rounded-lg border bg-gray-50 p-4">
        <div className="flex items-center justify-between">
          <CollapsibleTrigger asChild>
            <Button variant="ghost" size="sm" className="gap-2">
              <ChevronDown
                className={cn(
                  'h-4 w-4 transition-transform',
                  isOpen && 'rotate-180'
                )}
              />
              Show SQL Query
            </Button>
          </CollapsibleTrigger>
          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span>Provider: {provider}</span>
            <span>Execution: {executionTimeMs}ms</span>
          </div>
        </div>

        <CollapsibleContent className="mt-4 space-y-4">
          {explanation && (
            <div className="rounded-md bg-blue-50 p-3 text-sm text-blue-900">
              <strong>Explanation:</strong> {explanation}
            </div>
          )}

          <div className="relative">
            <pre className="rounded-md bg-gray-900 p-4 text-sm text-gray-100 overflow-x-auto">
              <code>{sql}</code>
            </pre>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleCopy}
              className="absolute top-2 right-2 bg-gray-800 hover:bg-gray-700"
            >
              {copied ? (
                <Check className="h-4 w-4 text-green-500" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </Button>
          </div>
        </CollapsibleContent>
      </div>
    </Collapsible>
  )
}
