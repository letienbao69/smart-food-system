import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, Users, Armchair, ArrowLeft, Wallet, Utensils, XCircle, Banknote, Clock } from 'lucide-react'
import toast from 'react-hot-toast'
import { reservationsApi } from '@/api/reservations'
import { payosApi } from '@/api/payos'
import { errMsg } from '@/api/client'
import { Loader } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import {
  formatDateTime, formatVND,
  reservationStatusLabel, reservationStatusTone, depositStatusLabel,
  orderStatusLabel, orderStatusTone,
} from '@/lib/utils'

export default function ReservationDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const qc = useQueryClient()

  const { data: r, isLoading } = useQuery({
    queryKey: ['my-reservation', id],
    queryFn: () => reservationsApi.myDetail(id),
  })

  // Thanh toán cọc qua PayOS: tạo link rồi chuyển sang trang thanh toán.
  // PayOS gọi webhook về backend -> tự xác nhận cọc, không cần admin/staff bấm.
  const payDeposit = useMutation({
    mutationFn: () => payosApi.createForReservation(id),
    onSuccess: (link) => {
      if (link?.checkoutUrl) {
        window.location.href = link.checkoutUrl
      } else {
        toast.error('Không tạo được link thanh toán cọc')
        qc.invalidateQueries({ queryKey: ['my-reservation', id] })
      }
    },
    onError: (e) => toast.error(errMsg(e, 'Không tạo được link thanh toán cọc')),
  })

  const cancel = useMutation({
    mutationFn: () => reservationsApi.cancel(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-reservation', id] })
      qc.invalidateQueries({ queryKey: ['my-reservations'] })
      toast.success('Đã hủy lượt đặt bàn')
    },
    onError: (e) => toast.error(errMsg(e, 'Không hủy được')),
  })

  if (isLoading) return <Loader className="min-h-[40vh]" />
  if (!r) return null

  const canCancel = ['PENDING', 'CONFIRMED'].includes(r.status)
  const canPayDeposit = r.depositStatus === 'NONE' && ['PENDING', 'CONFIRMED'].includes(r.status)
  // Đã tạo link cọc PayOS, đang chờ khách thanh toán — có hạn tự hủy (depositExpiresAt)
  const awaitingDeposit = r.depositStatus === 'PENDING' && r.status === 'PENDING' && !!r.depositExpiresAt
  const preorder = r.preorder

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/reservations" className="inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900 mb-4">
        <ArrowLeft className="h-4 w-4" />Tất cả lượt đặt bàn
      </Link>

      <div className="card p-6">
        <div className="flex justify-between items-start flex-wrap gap-3">
          <div>
            <p className="text-xs text-ink-500">Mã đặt bàn</p>
            <p className="font-mono text-lg font-bold text-ink-900">#{r.reservationCode}</p>
          </div>
          <span className={`chip border ${reservationStatusTone(r.status)}`}>
            {reservationStatusLabel(r.status)}
          </span>
        </div>

        <div className="mt-5 grid sm:grid-cols-2 gap-4">
          <Info icon={CalendarClock} label="Thời gian" value={formatDateTime(r.reservationTime)} />
          <Info icon={Users} label="Số khách" value={`${r.partySize} người`} />
          <Info icon={Armchair} label="Bàn" value={r.table ? `Bàn ${r.table.tableNumber} (${r.table.zone || 'khu chung'})` : 'Nhà hàng sẽ sắp xếp'} />
          <Info icon={r.paymentMethod === 'CASH' ? Banknote : Wallet} label="Thanh toán tại quán"
            value={r.paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản'} />
        </div>

        <div className="mt-4 rounded-lg bg-ink-50 border border-ink-200 p-3">
          <div className="flex justify-between text-sm">
            <span className="text-ink-600">Tiền cọc giữ bàn</span>
            <span className="font-semibold tabular">{formatVND(r.depositAmount)}</span>
          </div>
          <div className="flex justify-between text-sm mt-1">
            <span className="text-ink-600">Trạng thái cọc</span>
            <span className="font-medium">{depositStatusLabel(r.depositStatus)}</span>
          </div>
        </div>

        {awaitingDeposit && (
          <DepositCountdown
            expiresAt={r.depositExpiresAt}
            onExpire={() => qc.invalidateQueries({ queryKey: ['my-reservation', id] })}
          />
        )}

        {r.guestName && (
          <p className="mt-3 text-sm text-ink-500">
            Người đặt: <span className="text-ink-800 font-medium">{r.guestName}</span> · {r.guestPhone}
          </p>
        )}
        {r.note && <p className="mt-1 text-sm text-ink-500">Ghi chú: {r.note}</p>}

        {/* Đơn món đặt trước */}
        {preorder && (
          <div className="mt-5 border-t border-ink-100 pt-4">
            <div className="flex items-center justify-between mb-2">
              <h3 className="flex items-center gap-1.5 font-medium text-ink-900">
                <Utensils className="h-4 w-4 text-ink-500" />Món đặt trước
              </h3>
              <span className={`chip border ${orderStatusTone(preorder.orderStatus)}`}>
                {orderStatusLabel(preorder.orderStatus)}
              </span>
            </div>
            <div className="rounded-lg border border-ink-200 divide-y divide-ink-100">
              {preorder.items?.map((it) => (
                <div key={it.id} className="flex justify-between px-3 py-2 text-sm">
                  <span className="text-ink-700">{it.foodName} × {it.quantity}</span>
                  <span className="font-medium tabular">{formatVND(it.subtotal)}</span>
                </div>
              ))}
              {Number(r.depositAmount) > 0 && r.depositStatus === 'PAID' ? (
                <>
                  <div className="flex justify-between px-3 py-2 text-sm">
                    <span className="text-ink-600">Tổng tiền món</span>
                    <span className="tabular">{formatVND(preorder.finalAmount)}</span>
                  </div>
                  <div className="flex justify-between px-3 py-2 text-sm text-green-600">
                    <span>Đã đặt cọc (trừ vào hóa đơn)</span>
                    <span className="tabular">−{formatVND(r.depositAmount)}</span>
                  </div>
                  <div className="flex justify-between px-3 py-2 text-sm font-semibold">
                    <span>Còn phải trả tại quán</span>
                    <span className="tabular">{formatVND(Math.max(0, Number(preorder.finalAmount) - Number(r.depositAmount)))}</span>
                  </div>
                </>
              ) : (
                <div className="flex justify-between px-3 py-2 text-sm font-semibold">
                  <span>Tổng tiền món (trả tại quán)</span>
                  <span className="tabular">{formatVND(preorder.finalAmount)}</span>
                </div>
              )}
            </div>
          </div>
        )}

        {(canPayDeposit || awaitingDeposit || canCancel) && (
          <div className="mt-6 flex gap-2 flex-wrap">
            {canPayDeposit && (
              <Button onClick={() => payDeposit.mutate()} loading={payDeposit.isPending}>
                <Wallet className="h-4 w-4" />Đặt cọc giữ bàn
              </Button>
            )}
            {awaitingDeposit && (
              <Button onClick={() => payDeposit.mutate()} loading={payDeposit.isPending}>
                <Wallet className="h-4 w-4" />Thanh toán cọc ngay
              </Button>
            )}
            {canCancel && (
              <Button variant="danger"
                onClick={() => { if (confirm('Bạn chắc chắn muốn hủy đặt bàn?')) cancel.mutate() }}
                loading={cancel.isPending}>
                <XCircle className="h-4 w-4" />Hủy đặt bàn
              </Button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

/** Đồng hồ đếm ngược thời gian còn lại để thanh toán cọc trước khi đặt bàn tự hủy. */
function DepositCountdown({ expiresAt, onExpire }) {
  const [remaining, setRemaining] = useState(() => Math.max(0, new Date(expiresAt).getTime() - Date.now()))
  useEffect(() => {
    let polls = 0
    const timer = setInterval(() => {
      const ms = Math.max(0, new Date(expiresAt).getTime() - Date.now())
      setRemaining(ms)
      if (ms <= 0) {
        polls += 1
        if (polls % 3 === 0) onExpire?.()   // sau khi hết hạn, làm mới trạng thái mỗi 3 giây
        if (polls >= 30) clearInterval(timer)
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [expiresAt])

  const totalSec = Math.ceil(remaining / 1000)
  const mm = String(Math.floor(totalSec / 60)).padStart(2, '0')
  const ss = String(totalSec % 60).padStart(2, '0')
  const expired = remaining <= 0

  return (
    <div className={`mt-3 rounded-lg border p-3 flex items-center gap-2 text-sm ${expired ? 'bg-red-50 border-red-200 text-red-700' : 'bg-amber-50 border-amber-200 text-amber-800'}`}>
      <Clock className="h-4 w-4 shrink-0" />
      {expired ? (
        <span>Đã hết thời gian thanh toán. Lượt đặt bàn sẽ được tự động hủy.</span>
      ) : (
        <span>Vui lòng thanh toán cọc trong <strong className="tabular font-bold">{mm}:{ss}</strong>, nếu không lượt đặt bàn sẽ tự động bị hủy.</span>
      )}
    </div>
  )
}

function Info({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-2.5">
      <Icon className="h-4 w-4 text-ink-400 mt-0.5 shrink-0" />
      <div>
        <p className="text-xs text-ink-500">{label}</p>
        <p className="text-sm font-medium text-ink-900">{value}</p>
      </div>
    </div>
  )
}
