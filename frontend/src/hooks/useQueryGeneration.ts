import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '@/services/api'
import type { GenerateQueryRequest } from '@/types/api.types'

export function useQueryGeneration() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: GenerateQueryRequest) => api.generateQuery(request),
    onSuccess: () => {
      // Invalidate history to refetch
      queryClient.invalidateQueries({ queryKey: ['queryHistory'] })
    },
  })
}
