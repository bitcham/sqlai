import { useQuery } from '@tanstack/react-query'
import { api } from '@/services/api'

export function useQueryHistory(limit: number = 20) {
  return useQuery({
    queryKey: ['queryHistory', limit],
    queryFn: () => api.getHistory(limit),
    staleTime: 1000 * 60, // 1 minute
  })
}
