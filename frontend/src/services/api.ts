import type {
  GenerateQueryRequest,
  GenerateQueryResponse,
  QueryHistoryResponse,
  HealthCheckResponse,
} from '@/types/api.types'

const API_BASE_URL = '/api'

class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public data?: any
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function fetchApi<T>(
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  })

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}))
    throw new ApiError(
      response.status,
      errorData.message || 'API request failed',
      errorData
    )
  }

  return response.json()
}

export const api = {
  // Generate SQL query and execute
  generateQuery: (request: GenerateQueryRequest) =>
    fetchApi<GenerateQueryResponse>('/query/generate', {
      method: 'POST',
      body: JSON.stringify(request),
    }),

  // Get query history
  getHistory: (limit: number = 20) =>
    fetchApi<QueryHistoryResponse[]>(`/query/history?limit=${limit}`),

  // Health check
  healthCheck: () => fetchApi<HealthCheckResponse>('/query/health'),
}

export { ApiError }
