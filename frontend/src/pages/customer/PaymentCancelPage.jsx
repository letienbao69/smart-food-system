import { useEffect, useRef } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { XCircle, ArrowRight, RotateCcw } from 'lucide-react'
import { reservationsApi } from '@/api/reservations'

/**
 * Trang khách hàng được PayOS chuyển về khi huỷ thanh toán.
 * URL: /payment-cancel?orderCode=...&cancel=true&status=CANCELLED
 * Khi huỷ thanh toán, lượt đặt bàn được tự động huỷ và admin/staff nhận thông báo.
 */
export default function PaymentCancelPage() {
  const [search] = useSearchParams()
  const qc = useQueryClient()
  const orderCode = search.get('orderCode')
  const reservationId = orderCode ? String(Number(orderCode) % 1000000) : null
  const doneRef = useRef(false)

  // Huỷ thanh toán -> huỷ luôn lượt đặt bàn (báo admin + chuyển trạng thái CANCELLED)
  useEffect(() => {
    if (!reservationId || doneRef.current) return
    doneRef.current = true
    reservationsApi.cancel(reservationId)
      .then(() => qc.invalidateQueries({ queryKey: ['my-reservations'] }))
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reservationId])

  return (
    <div>
      <div className="relative overflow-hidden" style={{ background: 'linear-gradient(120deg,#1f2937,#111827)' }}>
        <div className="mx-auto max-w-5xl px-6 py-16 text-center">
          <div className="mx-auto mb-4 grid h-16 w-16 place-items-center rounded-full bg-amber-500/20">
            <XCircle className="h-9 w-9 text-amber-400" />
          </div>
          <h1 className="font-display text-4xl font-bold text-white">Đã huỷ thanh toán</h1>
          <p className="mt-3 text-white/70 inline-flex items-center gap-2 text-sm">
            <Link to="/" className="hover:text-white">Trang chủ</Link>
            <ArrowRight className="h-3.5 w-3.5" />
            <span>Huỷ thanh toán</span>
          </p>
        </div>
      </div>

      <div className="mx-auto max-w-2xl px-6 py-10">
        <div className="card p-8 text-center">
          <p className="text-ink-700">
            Bạn đã huỷ thanh toán trên cổng PayOS. Lượt đặt bàn tương ứng đã được huỷ.
          </p>
          <p className="mt-2 text-sm text-ink-500">
            Bạn có thể đặt lại bàn mới bất cứ lúc nào trong mục <span className="font-semibold">Đặt bàn</span>.
          </p>

          <div className="mt-6 flex justify-center gap-3">
            <Link to="/reservations" className="btn btn-secondary">
              <RotateCcw className="h-4 w-4" /> Xem đặt bàn của tôi
            </Link>
            {reservationId && (
              <Link to={`/reservations/${reservationId}`} className="btn btn-primary">
                Mở lượt đặt <ArrowRight className="h-4 w-4" />
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
