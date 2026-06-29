import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ArrowLeft, CheckCircle2, Clock, Package, CalendarClock, Armchair, Users, Trash2,
  Star, MessageSquare,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { ordersApi } from '@/api/cart'
import { reviewsApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { Loader, FoodImage } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import {
  formatDateTime, formatVND,
  orderStatusLabel, orderStatusTone, paymentStatusLabel,
} from '@/lib/utils'

// Vòng đời món tại nhà hàng
const STATUS_FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'SERVED', 'COMPLETED']

export default function OrderDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const qc  = useQueryClient()

  const { data: order, isLoading } = useQuery({
    queryKey: ['my-order', id],
    queryFn:  () => ordersApi.myOrder(id),
    refetchInterval: 6000,
    refetchOnWindowFocus: true,
    refetchOnMount: true,
  })

  const deleteMyOrder = useMutation({
    mutationFn: () => ordersApi.deleteMyOrder(id),
    onSuccess: () => {
      toast.success('Đã xóa đơn món')
      qc.invalidateQueries({ queryKey: ['my-orders'] })
      nav('/orders')
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (isLoading) return <Loader className="min-h-[40vh]" />
  if (!order)    return <div className="p-12 text-center">Không tìm thấy đơn</div>

  const currentStep = STATUS_FLOW.indexOf(order.orderStatus)
  const isCompleted = order.orderStatus === 'COMPLETED'
  const cancelled   = order.orderStatus === 'CANCELLED'

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
      <Link to="/orders" className="inline-flex items-center gap-1 text-sm text-ink-600 hover:text-ink-900 mb-4">
        <ArrowLeft className="h-4 w-4" /> Quay lại
      </Link>

      {/* Header */}
      <div className="card p-6">
        <div className="flex justify-between items-start flex-wrap gap-3">
          <div>
            <p className="text-xs uppercase tracking-wider text-ink-500">Mã đơn món</p>
            <h1 className="font-mono mt-1 font-bold text-xl text-ink-900">#{order.orderCode}</h1>
            <p className="mt-1 text-xs text-ink-500">{formatDateTime(order.createdAt)}</p>
            <p className="mt-1 flex items-center gap-1 text-[10px] text-ink-400">
              <span className="inline-block h-1.5 w-1.5 rounded-full bg-green-400 animate-pulse" />
              Tự động cập nhật mỗi 6 giây
            </p>
          </div>
          <div className="flex flex-col items-end gap-1">
            <span className={`chip border ${orderStatusTone(order.orderStatus)}`}>
              {orderStatusLabel(order.orderStatus)}
            </span>
            <span className={`text-xs font-medium ${order.paymentStatus === 'PAID' ? 'text-success-600' : 'text-amber-600'}`}>
              {paymentStatusLabel(order.paymentStatus)}
            </span>
          </div>
        </div>

        {/* Progress */}
        {!cancelled && (
          <div className="mt-6">
            <div className="relative flex justify-between items-center">
              <div className="absolute left-0 right-0 top-4 h-0.5 bg-ink-200" />
              <div className="absolute left-0 top-4 h-0.5 bg-accent-500 transition-all"
                style={{ width: `${(Math.max(0, currentStep) / (STATUS_FLOW.length - 1)) * 100}%` }} />
              {STATUS_FLOW.map((s, i) => {
                const done = i <= currentStep
                return (
                  <div key={s} className="relative flex flex-col items-center gap-1.5">
                    <div className={`grid h-8 w-8 place-items-center rounded-full transition ${done ? 'bg-accent-500 text-white' : 'bg-white border border-ink-200 text-ink-400'}`}>
                      {done ? <CheckCircle2 className="h-4 w-4" /> : <span className="text-xs">{i + 1}</span>}
                    </div>
                    <span className="text-[10px] text-center text-ink-600 max-w-[60px]">{orderStatusLabel(s)}</span>
                  </div>
                )
              })}
            </div>
          </div>
        )}

        {/* Xóa khi CANCELLED/COMPLETED */}
        {(cancelled || isCompleted) && (
          <div className="mt-5 rounded-xl border border-ink-200 bg-white p-4 flex flex-col sm:flex-row items-start sm:items-center gap-3">
            <div className="flex items-center gap-2 flex-1">
              <Trash2 className="h-5 w-5 text-ink-400 shrink-0" />
              <div>
                <p className="text-sm font-semibold text-ink-900">Xóa đơn món</p>
                <p className="text-xs text-ink-500 mt-0.5">Xóa vĩnh viễn đơn món khỏi lịch sử của bạn.</p>
              </div>
            </div>
            <Button size="sm" variant="secondary" loading={deleteMyOrder.isPending}
              onClick={() => { if (confirm('Xóa đơn món này? Không thể hoàn tác.')) deleteMyOrder.mutate() }}
              className="shrink-0 text-red-600 hover:bg-red-50 hover:border-red-200">
              <Trash2 className="h-4 w-4" /> Xóa
            </Button>
          </div>
        )}
      </div>

      {/* Items */}
      <div className="card p-5 mt-5">
        <h3 className="font-display font-semibold text-ink-900 mb-3 inline-flex items-center gap-2">
          <Package className="h-4 w-4" /> Món ăn ({order.items?.length || 0})
        </h3>
        <div className="space-y-3">
          {order.items?.map((it) => (
            <div key={it.id} className="flex gap-3 items-center">
              <FoodImage src={it.imageUrl} name={it.foodName} size="md" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-ink-900 line-clamp-1">{it.foodName}</p>
                <p className="text-xs text-ink-500 tabular">{formatVND(it.unitPrice)} × {it.quantity}</p>
              </div>
              <p className="font-semibold tabular text-sm">{formatVND(it.subtotal)}</p>
            </div>
          ))}
        </div>
        <div className="mt-5 space-y-1.5 text-sm border-t border-ink-200 pt-4">
          <div className="flex justify-between text-ink-700">
            <span>Tạm tính</span>
            <span className="tabular">{formatVND(order.totalAmount)}</span>
          </div>
          {order.discountAmount > 0 && (
            <div className="flex justify-between text-success-700">
              <span>Giảm giá</span>
              <span className="tabular">−{formatVND(order.discountAmount)}</span>
            </div>
          )}
          <div className="flex justify-between pt-2 border-t border-ink-100 font-display">
            <span className="font-medium">Tổng</span>
            <span className="font-bold tabular text-lg">{formatVND(order.finalAmount)}</span>
          </div>
        </div>
      </div>

      {/* Đặt bàn + Thanh toán */}
      <div className="mt-5 grid gap-5 md:grid-cols-2">
        <div className="card p-5">
          <h3 className="font-display font-semibold text-ink-900 mb-2 inline-flex items-center gap-2">
            <Armchair className="h-4 w-4" /> Thông tin bàn
          </h3>
          {order.reservationCode ? (
            <div className="text-sm text-ink-700 space-y-1">
              <p className="flex items-center gap-1.5">
                <span className="text-ink-500">Mã đặt bàn:</span>
                <Link to={`/reservations/${order.reservationId}`} className="font-mono text-accent-600 hover:underline">
                  #{order.reservationCode}
                </Link>
              </p>
              {order.tableNumber && (
                <p className="flex items-center gap-1.5"><Armchair className="h-3.5 w-3.5 text-ink-400" />Bàn {order.tableNumber}</p>
              )}
              {order.reservationTime && (
                <p className="flex items-center gap-1.5"><CalendarClock className="h-3.5 w-3.5 text-ink-400" />{formatDateTime(order.reservationTime)}</p>
              )}
              {order.partySize && (
                <p className="flex items-center gap-1.5"><Users className="h-3.5 w-3.5 text-ink-400" />{order.partySize} khách</p>
              )}
            </div>
          ) : <p className="text-sm text-ink-500">Đơn gọi món tại quán</p>}
        </div>
        <div className="card p-5">
          <h3 className="font-display font-semibold text-ink-900 mb-2 inline-flex items-center gap-2">
            <Clock className="h-4 w-4" /> Thanh toán
          </h3>
          <p className="text-sm text-ink-700">
            <span className="font-medium">{order.paymentMethod === 'CASH' ? 'Tiền mặt' : order.paymentMethod === 'BANK_TRANSFER' ? 'Chuyển khoản' : order.paymentMethod}</span> —{' '}
            <span className={order.paymentStatus === 'PAID' ? 'text-success-600 font-medium' : ''}>
              {paymentStatusLabel(order.paymentStatus)}
            </span>
          </p>
          <p className="mt-1 text-xs text-ink-500">Thanh toán tại quán khi dùng bữa.</p>
          {order.note && (
            <p className="mt-2 text-sm text-ink-600"><span className="text-ink-500">Ghi chú: </span>{order.note}</p>
          )}
        </div>
      </div>

      {/* Đánh giá khi hoàn thành */}
      {isCompleted && <ReviewSection orderId={order.id} items={order.items || []} />}
    </div>
  )
}

function ReviewSection({ orderId, items }) {
  return (
    <div className="mt-5 card p-5">
      <h3 className="font-display font-semibold text-ink-900 mb-1 inline-flex items-center gap-2">
        <Star className="h-4 w-4" /> Đánh giá món ăn
      </h3>
      <p className="mb-4 text-xs text-ink-500">Bữa ăn đã hoàn tất — hãy chia sẻ cảm nhận về từng món.</p>
      <div className="space-y-5">
        {items.map((it) => (
          <ReviewItem key={it.id} orderId={orderId} foodId={it.foodId} foodName={it.foodName} imageUrl={it.imageUrl} />
        ))}
      </div>
    </div>
  )
}

function ReviewItem({ orderId, foodId, foodName, imageUrl }) {
  const qc = useQueryClient()
  const [rating, setRating]   = useState(0)
  const [hovering, setHovering] = useState(0)
  const [comment, setComment] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const submit = useMutation({
    mutationFn: () => reviewsApi.create({ foodId, orderId, rating, comment }),
    onSuccess: () => {
      setSubmitted(true)
      toast.success(`Đã gửi đánh giá cho "${foodName}"`)
      qc.invalidateQueries({ queryKey: ['reviews', String(foodId)] })
    },
    onError: (e) => toast.error(errMsg(e, 'Không thể gửi đánh giá')),
  })

  if (submitted) {
    return (
      <div className="flex items-center gap-3 rounded-lg bg-success-50 border border-green-200 p-3">
        <CheckCircle2 className="h-5 w-5 text-success-600 shrink-0" />
        <p className="text-sm text-success-700 font-medium">Đã đánh giá "{foodName}"</p>
      </div>
    )
  }

  return (
    <div className="border border-ink-200 rounded-xl p-4">
      <div className="flex items-center gap-3 mb-3">
        <FoodImage src={imageUrl} name={foodName} size="sm" />
        <p className="font-medium text-sm text-ink-900">{foodName}</p>
      </div>
      <div className="flex items-center gap-1 mb-3">
        {[1, 2, 3, 4, 5].map((star) => (
          <button key={star} type="button" onClick={() => setRating(star)}
            onMouseEnter={() => setHovering(star)} onMouseLeave={() => setHovering(0)}
            className="focus:outline-none transition-transform hover:scale-110">
            <Star className={`h-7 w-7 transition-colors ${star <= (hovering || rating) ? 'fill-amber-400 text-amber-400' : 'text-ink-300'}`} />
          </button>
        ))}
        {rating > 0 && (
          <span className="ml-2 text-xs text-ink-500">{['','Rất tệ','Tệ','Bình thường','Tốt','Tuyệt vời'][rating]}</span>
        )}
      </div>
      <textarea value={comment} onChange={(e) => setComment(e.target.value)} rows={2}
        placeholder="Chia sẻ cảm nhận của bạn (tuỳ chọn)..." className="input resize-none mb-3 text-sm" />
      <Button size="sm" onClick={() => submit.mutate()} disabled={rating === 0} loading={submit.isPending}>
        <MessageSquare className="h-3.5 w-3.5" /> Gửi đánh giá
      </Button>
      {rating === 0 && <p className="mt-1 text-xs text-ink-500">Chọn số sao trước khi gửi.</p>}
    </div>
  )
}
