import { useState, useMemo, useEffect } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { cn } from '@/lib/utils'

/**
 * Hook phân trang client-side cho một mảng dữ liệu.
 * Trả về dữ liệu trang hiện tại + các điều khiển trang.
 */
export function usePagination(items, pageSize = 10) {
  const [page, setPage] = useState(1)
  const list = items || []
  const totalPages = Math.max(1, Math.ceil(list.length / pageSize))

  // Nếu dữ liệu thay đổi khiến trang hiện tại vượt quá tổng số trang -> quay về trang cuối hợp lệ
  useEffect(() => {
    if (page > totalPages) setPage(totalPages)
  }, [totalPages, page])

  const paged = useMemo(() => {
    const start = (page - 1) * pageSize
    return list.slice(start, start + pageSize)
  }, [list, page, pageSize])

  return { page, setPage, totalPages, paged, total: list.length }
}

/**
 * Thanh phân trang. Ẩn nếu chỉ có 1 trang.
 */
export default function Pagination({ page, totalPages, onChange, className }) {
  if (totalPages <= 1) return null

  const pages = []
  const push = (p) => pages.push(p)
  // Hiển thị gọn: 1 ... (page-1) page (page+1) ... last
  const window = 1
  const start = Math.max(1, page - window)
  const end = Math.min(totalPages, page + window)
  if (start > 1) { push(1); if (start > 2) push('...') }
  for (let p = start; p <= end; p++) push(p)
  if (end < totalPages) { if (end < totalPages - 1) push('...'); push(totalPages) }

  return (
    <div className={cn('flex items-center justify-end gap-1.5 py-3', className)}>
      <button
        disabled={page <= 1}
        onClick={() => onChange(page - 1)}
        className="grid h-8 w-8 place-items-center rounded-lg border border-ink-200 text-ink-600 hover:bg-ink-50 disabled:opacity-40 disabled:cursor-not-allowed">
        <ChevronLeft className="h-4 w-4" />
      </button>
      {pages.map((p, i) =>
        p === '...' ? (
          <span key={`e${i}`} className="px-1 text-ink-400">…</span>
        ) : (
          <button
            key={p}
            onClick={() => onChange(p)}
            className={cn('h-8 min-w-8 px-2 rounded-lg border text-sm transition',
              p === page
                ? 'border-blue-500 bg-blue-500 text-white'
                : 'border-ink-200 text-ink-700 hover:bg-ink-50')}>
            {p}
          </button>
        )
      )}
      <button
        disabled={page >= totalPages}
        onClick={() => onChange(page + 1)}
        className="grid h-8 w-8 place-items-center rounded-lg border border-ink-200 text-ink-600 hover:bg-ink-50 disabled:opacity-40 disabled:cursor-not-allowed">
        <ChevronRight className="h-4 w-4" />
      </button>
    </div>
  )
}
