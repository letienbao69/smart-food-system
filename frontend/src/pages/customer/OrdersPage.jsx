import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Package, ArrowRight } from 'lucide-react'
import { ordersApi } from '@/api/cart'
import { Loader, Empty } from '@/components/ui/Atoms'
import Pagination, { usePagination } from '@/components/ui/Pagination'
import {
  formatDateTime,
  formatVND,
  orderStatusLabel,
  orderStatusTone,
} from '@/lib/utils'

export default function OrdersPage() {
  const { data: orders, isLoading } = useQuery({
    queryKey: ['my-orders'],
    queryFn: ordersApi.myOrders,
  })

  const { page, setPage, totalPages, paged } = usePagination(orders, 8)

  if (isLoading) return <Loader className="min-h-[40vh]" />

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <p className="text-xs uppercase tracking-wider text-ink-500">Đơn hàng</p>
        <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">
          Đơn hàng của tôi
        </h1>
      </div>

      {orders?.length === 0 ? (
        <Empty
          icon={Package}
          title="Chưa có đơn hàng"
          description="Đơn hàng bạn đặt sẽ xuất hiện tại đây."
          action={
            <Link to="/foods" className="btn-primary btn">
              Đặt món đầu tiên
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      ) : (
        <div className="space-y-3">
          {paged.map((o) => (
            <Link
              key={o.id}
              to={`/orders/${o.id}`}
              className="card block p-5 hover:border-ink-300 hover:shadow-card transition"
            >
              <div className="flex justify-between items-start flex-wrap gap-2">
                <div>
                  <p className="text-xs text-ink-500">Mã đơn</p>
                  <p className="font-mono text-sm font-semibold text-ink-900">
                    #{o.orderCode}
                  </p>
                </div>
                <span
                  className={`chip border ${orderStatusTone(o.orderStatus)}`}
                >
                  {orderStatusLabel(o.orderStatus)}
                </span>
              </div>

              <div className="mt-3 flex flex-wrap justify-between items-end gap-2">
                <p className="text-xs text-ink-500">
                  {formatDateTime(o.createdAt)} · {o.items?.length || 0} món
                </p>
                <p className="font-display text-lg font-bold tabular text-ink-900">
                  {formatVND(o.finalAmount)}
                </p>
              </div>
            </Link>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} className="justify-center" />
        </div>
      )}
    </div>
  )
}
