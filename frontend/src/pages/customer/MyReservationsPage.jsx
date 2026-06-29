import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { CalendarClock, ArrowRight, Users, Armchair } from 'lucide-react'
import { reservationsApi } from '@/api/reservations'
import { Loader, Empty } from '@/components/ui/Atoms'
import Pagination, { usePagination } from '@/components/ui/Pagination'
import {
  formatDateTime,
  formatVND,
  reservationStatusLabel,
  reservationStatusTone,
  depositStatusLabel,
} from '@/lib/utils'

export default function MyReservationsPage() {
  const { data: list, isLoading } = useQuery({
    queryKey: ['my-reservations'],
    queryFn: reservationsApi.myList,
  })

  const { page, setPage, totalPages, paged } = usePagination(list, 8)

  if (isLoading) return <Loader className="min-h-[40vh]" />

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between flex-wrap gap-2">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Đặt bàn</p>
          <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">Lượt đặt bàn của tôi</h1>
        </div>
        <Link to="/reserve" className="btn-primary btn">
          <CalendarClock className="h-4 w-4" />Đặt bàn mới
        </Link>
      </div>

      {list?.length === 0 ? (
        <Empty
          icon={CalendarClock}
          title="Chưa có lượt đặt bàn"
          description="Đặt bàn để giữ chỗ tại nhà hàng."
          action={<Link to="/reserve" className="btn-primary btn">Đặt bàn ngay<ArrowRight className="h-4 w-4" /></Link>}
        />
      ) : (
        <div className="space-y-3">
          {paged.map((r) => (
            <Link key={r.id} to={`/reservations/${r.id}`}
              className="card block p-5 hover:border-ink-300 hover:shadow-card transition">
              <div className="flex justify-between items-start flex-wrap gap-2">
                <div>
                  <p className="text-xs text-ink-500">Mã đặt bàn</p>
                  <p className="font-mono text-sm font-semibold text-ink-900">#{r.reservationCode}</p>
                </div>
                <span className={`chip border ${reservationStatusTone(r.status)}`}>
                  {reservationStatusLabel(r.status)}
                </span>
              </div>

              <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1.5 text-sm text-ink-600">
                <span className="flex items-center gap-1.5">
                  <CalendarClock className="h-4 w-4 text-ink-400" />{formatDateTime(r.reservationTime)}
                </span>
                <span className="flex items-center gap-1.5">
                  <Users className="h-4 w-4 text-ink-400" />{r.partySize} khách
                </span>
                <span className="flex items-center gap-1.5">
                  <Armchair className="h-4 w-4 text-ink-400" />
                  {r.table ? `Bàn ${r.table.tableNumber}` : 'Chưa gán bàn'}
                </span>
              </div>

              <div className="mt-2 flex items-center justify-between flex-wrap gap-2">
                <span className="text-xs text-ink-500">
                  Cọc: {formatVND(r.depositAmount)} · {depositStatusLabel(r.depositStatus)}
                  {r.hasPreorder && ' · Có đặt món trước'}
                </span>
                <span className="text-ink-400 flex items-center gap-1 text-xs">
                  Xem chi tiết <ArrowRight className="h-3 w-3" />
                </span>
              </div>
            </Link>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} className="justify-center" />
        </div>
      )}
    </div>
  )
}
