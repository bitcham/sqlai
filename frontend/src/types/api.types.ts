// Enums
export enum AIProviderType {
  CLAUDE = 'CLAUDE',
  OPENAI = 'OPENAI',
  GEMINI = 'GEMINI',
}

export enum ExecutionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  TIMEOUT = 'TIMEOUT',
}

// DTOs
export interface GenerateQueryRequest {
  question: string
}

export interface ColumnInfo {
  name: string
  type: string
}

export interface ExecutionResult {
  status: ExecutionStatus
  data: Record<string, any>[]
  columns: ColumnInfo[]
  rowCount: number
  executionTimeMs: number
  errorMessage?: string
}

export interface GenerateQueryResponse {
  sql: string
  explanation?: string
  provider: AIProviderType
  executionResult?: ExecutionResult
}

export interface QueryHistoryResponse {
  naturalLanguageQueryId: number
  question: string
  sql: string
  provider: AIProviderType
  status: ExecutionStatus
  rowCount: number
  executedAt: string // ISO 8601 format
}

export interface HealthCheckResponse {
  ready: boolean
  message: string
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  timestamp: string
}
