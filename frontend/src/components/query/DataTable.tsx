import { useState, useEffect } from 'react'
import { Button } from '@/components/ui/Button'
import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, AlertCircle } from 'lucide-react'
import type { ColumnInfo } from '@/types/api.types'

interface DataTableProps {
  columns: ColumnInfo[]
  data: Record<string, any>[]
  rowCount: number
  itemsPerPage?: number
}

const MAX_BACKEND_ROWS = 1000 // Backend row limit from ExecutionPolicy

// Format cell value based on column name or type
function formatCellValue(value: any, columnName: string): string {
  // Check if column name contains 'percentage' or 'percent'
  const isPercentageColumn = columnName.toLowerCase().includes('percentage') ||
                             columnName.toLowerCase().includes('percent')

  if (isPercentageColumn && typeof value === 'number') {
    // If value is between 0-1, multiply by 100
    const percentValue = value < 1 ? value * 100 : value
    return `${percentValue.toFixed(2)}%`
  }

  return String(value)
}

export function DataTable({
  columns,
  data,
  rowCount,
  itemsPerPage = 10,
}: DataTableProps) {
  const [currentPage, setCurrentPage] = useState(1)

  // Reset to page 1 when data changes
  useEffect(() => {
    setCurrentPage(1)
  }, [data])

  const totalPages = Math.ceil(data.length / itemsPerPage)
  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const currentData = data.slice(startIndex, endIndex)

  // Check if backend hit row limit
  const isLimitedByBackend = rowCount === MAX_BACKEND_ROWS

  const goToNextPage = () => setCurrentPage((prev) => Math.min(prev + 1, totalPages))
  const goToPrevPage = () => setCurrentPage((prev) => Math.max(prev - 1, 1))
  const goToFirstPage = () => setCurrentPage(1)
  const goToLastPage = () => setCurrentPage(totalPages)

  if (data.length === 0) {
    return (
      <div className="text-center py-8 text-gray-500">
        No data to display
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Warning if backend hit row limit */}
      {isLimitedByBackend && (
        <div className="flex items-start gap-2 p-3 bg-yellow-50 border border-yellow-200 rounded-md text-sm text-yellow-800">
          <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
          <div>
            <p className="font-medium">Result set limited to {MAX_BACKEND_ROWS} rows</p>
            <p className="text-yellow-700 mt-1">
              The query may have returned more data, but only the first {MAX_BACKEND_ROWS} rows are displayed.
              Consider refining your query with additional filters.
            </p>
          </div>
        </div>
      )}

      {/* Table */}
      <div className="rounded-md border">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                {columns.map((column) => (
                  <th
                    key={column.name}
                    className="px-4 py-3 text-left font-medium text-gray-900"
                  >
                    {column.name}
                    <span className="ml-1 text-xs text-gray-500">
                      ({column.type})
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {currentData.map((row, rowIndex) => (
                <tr
                  key={rowIndex}
                  className="border-b last:border-b-0 hover:bg-gray-50"
                >
                  {columns.map((column) => (
                    <td key={column.name} className="px-4 py-3 text-gray-700">
                      {row[column.name] !== null && row[column.name] !== undefined
                        ? formatCellValue(row[column.name], column.name)
                        : <span className="text-gray-400">-</span>}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination Controls */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        {/* Page Info */}
        <div className="text-sm text-gray-700">
          Showing{' '}
          <span className="font-medium">{startIndex + 1}</span>
          {' '}-{' '}
          <span className="font-medium">{Math.min(endIndex, data.length)}</span>
          {' '}of{' '}
          <span className="font-medium">{rowCount}</span>
          {' '}results
          {isLimitedByBackend && (
            <span className="text-yellow-600"> (limited to {MAX_BACKEND_ROWS})</span>
          )}
        </div>

        {/* Pagination Buttons */}
        <div className="flex items-center gap-2">
          <Button
            variant="secondary"
            size="sm"
            onClick={goToFirstPage}
            disabled={currentPage === 1}
            title="First page"
          >
            <ChevronsLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={goToPrevPage}
            disabled={currentPage === 1}
            title="Previous page"
          >
            <ChevronLeft className="h-4 w-4" />
            Previous
          </Button>

          {/* Page indicator */}
          <span className="text-sm text-gray-700 px-2">
            Page <span className="font-medium">{currentPage}</span> of{' '}
            <span className="font-medium">{totalPages}</span>
          </span>

          <Button
            variant="secondary"
            size="sm"
            onClick={goToNextPage}
            disabled={currentPage === totalPages}
            title="Next page"
          >
            Next
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Button
            variant="secondary"
            size="sm"
            onClick={goToLastPage}
            disabled={currentPage === totalPages}
            title="Last page"
          >
            <ChevronsRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}
