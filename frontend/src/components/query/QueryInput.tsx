import React, { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Loader2 } from 'lucide-react'

interface QueryInputProps {
  onSubmit: (question: string) => void
  isLoading: boolean
}

export function QueryInput({ onSubmit, isLoading }: QueryInputProps) {
  const [question, setQuestion] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (question.trim()) {
      onSubmit(question.trim())
    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full space-y-4">
      <div className="flex gap-2">
        <Input
          type="text"
          placeholder="Ask a question in natural language..."
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          disabled={isLoading}
          className="flex-1"
        />
        <Button type="submit" disabled={isLoading || !question.trim()}>
          {isLoading ? (
            <>
              <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              Processing...
            </>
          ) : (
            'Generate Query'
          )}
        </Button>
      </div>
    </form>
  )
}
