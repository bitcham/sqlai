import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { QueryInput } from '@/components/query/QueryInput'
import { DataTable } from '@/components/query/DataTable'
import { SqlViewer } from '@/components/query/SqlViewer'
import { ExecutionStatus } from '@/components/query/ExecutionStatus'
import { QueryHistory } from '@/components/history/QueryHistory'
import { useQueryGeneration } from '@/hooks/useQueryGeneration'

const queryClient = new QueryClient()

function AppContent() {
  const { mutate: generateQuery, data, isPending, error } = useQueryGeneration()

  const handleSubmit = (question: string) => {
    generateQuery({ question })
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-bold text-gray-900">
              SQL AI - Natural Language to SQL
            </h1>
            <QueryHistory />
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-8 sm:px-6 lg:px-8 space-y-6">
        {/* Query Input */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-lg font-semibold mb-4">Ask a Question</h2>
          <QueryInput onSubmit={handleSubmit} isLoading={isPending} />
        </div>

        {/* Error Display */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <p className="text-red-800 font-medium">Error</p>
            <p className="text-red-700 text-sm mt-1">{error.message}</p>
          </div>
        )}

        {/* Results */}
        {data && (
          <>
            {/* Execution Status */}
            {data.executionResult && (
              <ExecutionStatus
                status={data.executionResult.status}
                rowCount={data.executionResult.rowCount}
                executionTimeMs={data.executionResult.executionTimeMs}
                errorMessage={data.executionResult.errorMessage}
              />
            )}

            {/* Data Table */}
            {data.executionResult &&
              data.executionResult.data.length > 0 && (
                <div className="bg-white rounded-lg shadow p-6">
                  <h2 className="text-lg font-semibold mb-4">Query Results</h2>
                  <DataTable
                    columns={data.executionResult.columns}
                    data={data.executionResult.data}
                    rowCount={data.executionResult.rowCount}
                  />
                </div>
              )}

            {/* SQL Viewer */}
            {data.executionResult && (
              <SqlViewer
                sql={data.sql}
                explanation={data.explanation}
                provider={data.provider}
                executionTimeMs={data.executionResult.executionTimeMs}
              />
            )}
          </>
        )}
      </main>
    </div>
  )
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
    </QueryClientProvider>
  )
}

export default App
