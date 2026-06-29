import { useEffect } from 'react'
import { useParams, useSearchParams, Link } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ArrowRight } from 'lucide-react'
import { reservationsApi } from '@/api/reservations'
import { payosApi } from '@/api/payos'
import { useAuth } from '@/store/auth'
import { Loader, FoodImage } from '@/components/ui/Atoms'
import {
  formatVND, formatDateTime,
  reservationStatusLabel, reservationStatusTone,
} from '@/lib/utils'

const PAY_LABEL = { PAYOS: 'Chuyển khoản', CASH: 'Tiền mặt' }

export default function PaymentSuccessPage() {
  const { id: idFromPath } = useParams()
  const [search] = useSearchParams()
  const user = useAuth((s) => s.user)
  // PayOS gọi returnUrl với ?orderCode=... ; id đặt bàn nằm ở 6 chữ số cuối của orderCode
  const orderCode = search.get('orderCode')
  const id = idFromPath || (orderCode ? String(Number(orderCode) % 1000000) : null)
  const qc = useQueryClient()

  // Dự phòng: nếu quay về từ cổng PayOS kèm orderCode, hỏi trạng thái để tự xác nhận cọc.
  useEffect(() => {
    if (!orderCode) return
    payosApi.status(orderCode)
      .then(() => qc.invalidateQueries({ queryKey: ['my-reservation', id] }))
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderCode])

  const { data: r, isLoading } = useQuery({
    queryKey: ['my-reservation', id],
    queryFn: () => reservationsApi.myDetail(id),
    enabled: !!id,
  })

  if (isLoading) return <Loader className="min-h-[50vh]" />
  if (!r) return <div className="p-12 text-center">Không tìm thấy đơn đặt bàn</div>

  const preorder = r.preorder
  const total = preorder ? Number(preorder.finalAmount || 0) : 0

  return (
    <div>
      {/* Banner */}
      <div className="relative overflow-hidden" style={{ background: 'linear-gradient(120deg,#1f2937,#111827)' }}>
        <div className="mx-auto max-w-5xl px-6 py-16 text-center">
          <div className="mx-auto mb-4 grid h-16 w-16 place-items-center rounded-full bg-green-500/20">
            <CheckCircle2 className="h-9 w-9 text-green-400" />
          </div>
          <h1 className="font-display text-4xl font-bold text-white">Thanh toán thành công</h1>
          <p className="mt-3 text-white/70 inline-flex items-center gap-2 text-sm">
            <Link to="/" className="hover:text-white">Trang chủ</Link>
            <ArrowRight className="h-3.5 w-3.5" />
            <span>Thanh toán thành công</span>
          </p>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-6 py-10">
        <div className="card p-8">
          <h2 className="text-center font-display text-2xl font-bold text-ink-900 mb-8">Thông tin đơn hàng</h2>

          <div className="grid gap-8 md:grid-cols-2">
            {/* Khách hàng */}
            <div>
              <h3 className="font-display font-semibold text-ink-900 mb-4">Thông tin khách hàng</h3>
              <dl className="space-y-3 text-sm">
                <Row label="Tên" value={r.guestName} />
                <Row label="Email" value={user?.email || '—'} />
                <Row label="SĐT" value={r.guestPhone} />
              </dl>
            </div>

            {/* Đặt bàn */}
            <div>
              <h3 className="font-display font-semibold text-ink-900 mb-4">Thông tin đặt bàn</h3>
              <dl className="space-y-3 text-sm">
                <Row label="Bàn" value={r.table ? `Bàn ${r.table.tableNumber} (${r.table.zone || 'khu chung'})` : 'Sẽ được sắp xếp'} />
                <Row label="Sức chứa" value={r.table ? `${r.table.capacity} người` : `${r.partySize} người`} />
                <Row label="Thời gian" value={formatDateTime(r.reservationTime)} />
                <Row label="Phương thức" value={PAY_LABEL[r.paymentMethod] || r.paymentMethod} />
                <div className="flex items-center gap-2">
                  <dt className="text-ink-500 w-28 shrink-0">Trạng thái:</dt>
                  <dd><span className={`chip border ${reservationStatusTone(r.status)}`}>{reservationStatusLabel(r.status)}</span></dd>
                </div>
              </dl>
            </div>
          </div>

          {/* Danh sách món */}
          {preorder?.items?.length > 0 && (
            <div className="mt-8 border-t border-ink-100 pt-6">
              <h3 className="font-display font-semibold text-ink-900 mb-4">Danh sách món ăn</h3>
              <div className="rounded-xl border border-ink-200 divide-y divide-ink-100">
                {preorder.items.map((it) => (
                  <div key={it.id} className="flex items-center gap-3 px-4 py-3">
                    <FoodImage src={it.imageUrl} name={it.foodName} size="sm" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-ink-900">{it.foodName}</p>
                      <p className="text-xs text-ink-500">{formatVND(it.unitPrice)} × {it.quantity}</p>
                    </div>
                    <span className="tabular font-medium text-sm">{formatVND(it.subtotal)}</span>
                  </div>
                ))}
                <div className="flex justify-between px-4 py-3 font-semibold">
                  <span>Tổng cộng</span>
                  <span className="font-display text-lg text-danger-600 tabular">{formatVND(total)}</span>
                </div>
              </div>
            </div>
          )}

          <div className="mt-8 flex justify-center gap-3">
            <Link to="/reservations" className="btn btn-secondary">Xem lượt đặt bàn của tôi</Link>
            <Link to="/foods" className="btn btn-primary">Tiếp tục đặt món <ArrowRight className="h-4 w-4" /></Link>
          </div>
        </div>
      </div>
    </div>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex items-start gap-2">
      <dt className="text-ink-500 w-28 shrink-0">{label}:</dt>
      <dd className="font-medium text-ink-900">{value}</dd>
    </div>
  )
}
