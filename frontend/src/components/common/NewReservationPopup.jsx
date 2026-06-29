import { useEffect, useState, useRef, useCallback } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  User, Phone, Clock, Armchair, CreditCard, X, Check,
  Sparkles, ChevronRight, Receipt,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { useWebSocketClient } from '@/providers/WebSocketProvider'
import { reservationsApi } from '@/api/reservations'
import { formatVND, formatDateTime, initials } from '@/lib/utils'

const PAY = {
  PAYOS: { label: 'Chuyển khoản', color: '#1a8d46' },
  CASH: { label: 'Tiền mặt', color: '#374151' },
  CASH: { label: 'Tại nhà hàng', color: '#16a34a' },
  BANK_TRANSFER: { label: 'Chuyển khoản', color: '#0891b2' },
}

/**
 * Thẻ thông báo đặt bàn mới — phong cách hiện đại (glass, gradient, slide-in),
 * nổi ở góc phải, không chặn toàn màn hình. Hỗ trợ nhiều đơn xếp chồng.
 * Cho phép Xác nhận / Từ chối ngay, hoặc mở chi tiết.
 */
export default function NewReservationPopup() {
  const client = useWebSocketClient()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const subRef = useRef(null)
  const [cards, setCards] = useState([]) // hàng đợi các đơn mới

  const enrich = useCallback(async (id) => {
    try {
      const full = await reservationsApi.adminGet(id)
      setCards((cur) => cur.map((c) => (c.id === id ? { ...c, ...full, _loaded: true } : c)))
    } catch (_) {}
  }, [])

  useEffect(() => {
    if (!client) return
    subRef.current = client.subscribe('/topic/admin/orders', (message) => {
      try {
        const data = JSON.parse(message.body)
        if (data.type !== 'NEW_RESERVATION') return
        const card = {
          id: data.reservationId,
          reservationCode: data.reservationCode,
          guestName: data.guestName,
          guestPhone: data.guestPhone,
          partySize: data.partySize,
          reservationTime: data.reservationTime,
          tableNumber: data.tableNumber,
          tableZone: data.tableZone,
          paymentMethod: data.paymentMethod,
          preorder: null,
          _loaded: false,
        }
        setCards((cur) => {
          if (cur.some((c) => c.id === card.id)) return cur
          return [card, ...cur].slice(0, 4)
        })
        enrich(data.reservationId)
      } catch (e) {
        console.warn('[NewReservationPopup] parse error', e)
      }
    })
    return () => { try { subRef.current?.unsubscribe() } catch (_) {} }
  }, [client, enrich])

  const dismiss = (id) => setCards((cur) => cur.filter((c) => c.id !== id))

  const act = async (card, status) => {
    try {
      await reservationsApi.updateStatus(card.id, { status })
      toast.success(status === 'CONFIRMED' ? `Đã xác nhận đơn #${card.reservationCode}` : `Đã từ chối đơn #${card.reservationCode}`)
      qc.invalidateQueries({ queryKey: ['admin-reservations'] })
      qc.invalidateQueries({ queryKey: ['admin-orders'] })
      qc.invalidateQueries({ queryKey: ['admin-tables'] })
      dismiss(card.id)
    } catch (e) {
      toast.error(e?.response?.data?.message || 'Thao tác thất bại')
    }
  }

  if (cards.length === 0) return null

  return (
    <div className="fixed top-4 right-4 z-[100] flex flex-col gap-3 w-[380px] max-w-[calc(100vw-2rem)]">
      {cards.map((card, i) => (
        <ReservationCard key={card.id} card={card} index={i}
          onConfirm={() => act(card, 'CONFIRMED')}
          onReject={() => act(card, 'CANCELLED')}
          onClose={() => dismiss(card.id)}
          onOpen={() => { navigate('/admin/reservations'); dismiss(card.id) }} />
      ))}
    </div>
  )
}

function ReservationCard({ card, index, onConfirm, onReject, onClose, onOpen }) {
  const [busy, setBusy] = useState(false)
  const pay = PAY[card.paymentMethod] || { label: card.paymentMethod || '—', color: '#64748b' }
  const preorder = card.preorder
  const total = preorder ? Number(preorder.finalAmount || 0) : 0
  const tableLabel = card.tableNumber
    ? `${card.tableZone ? card.tableZone + ' · ' : ''}Bàn ${card.tableNumber}`
    : 'Chưa gán bàn'

  const run = async (fn) => { setBusy(true); try { await fn() } finally { setBusy(false) } }

  return (
    <div
      className="animate-slide-up overflow-hidden rounded-2xl border border-white/10 shadow-2xl backdrop-blur-xl"
      style={{
        background: 'linear-gradient(160deg, rgba(28,25,23,0.97), rgba(15,14,13,0.97))',
        boxShadow: '0 20px 50px -12px rgba(0,0,0,0.6)',
      }}>
      {/* Thanh gradient trên cùng */}
      <div className="h-1 w-full" style={{ background: 'linear-gradient(90deg,#f97316,#ef4444,#ec4899)' }} />

      <div className="p-4 text-stone-100">
        {/* Header */}
        <div className="flex items-start gap-3">
          <div className="relative shrink-0">
            <div className="grid h-11 w-11 place-items-center rounded-xl text-white font-bold"
              style={{ background: 'linear-gradient(135deg,#f97316,#ef4444)' }}>
              {initials(card.guestName) || <Sparkles className="h-5 w-5" />}
            </div>
            <span className="absolute -right-1 -top-1 flex h-3.5 w-3.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-orange-400 opacity-75" />
              <span className="relative inline-flex h-3.5 w-3.5 rounded-full bg-orange-500" />
            </span>
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <p className="font-semibold leading-tight">Đơn đặt bàn mới</p>
              <span className="rounded-full bg-amber-400/15 px-2 py-0.5 text-[10px] font-medium text-amber-300 ring-1 ring-amber-400/20">
                Chờ xử lý
              </span>
            </div>
            <p className="mt-0.5 font-mono text-xs text-stone-400">#{card.reservationCode}</p>
          </div>
          <button onClick={onClose} className="text-stone-500 hover:text-stone-200 transition">
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Thông tin */}
        <div className="mt-3 space-y-2 rounded-xl bg-white/5 p-3 text-sm">
          <Line icon={User} value={card.guestName} />
          <Line icon={Phone} value={card.guestPhone} />
          <Line icon={Clock} value={card.reservationTime ? formatDateTime(card.reservationTime) : '—'} />
          <div className="flex items-center justify-between">
            <Line icon={Armchair} value={tableLabel} />
            <span className="inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[11px] font-medium text-white"
              style={{ background: pay.color }}>
              <CreditCard className="h-3 w-3" /> {pay.label}
            </span>
          </div>
        </div>

        {/* Món đặt trước */}
        {preorder?.items?.length > 0 && (
          <div className="mt-3 rounded-xl bg-white/5 p-3">
            <div className="flex items-center gap-1.5 text-xs text-stone-400 mb-2">
              <Receipt className="h-3.5 w-3.5" /> {preorder.items.length} món đặt trước
            </div>
            <div className="space-y-1 max-h-24 overflow-y-auto">
              {preorder.items.slice(0, 4).map((it) => (
                <div key={it.id} className="flex justify-between text-xs">
                  <span className="text-stone-300 truncate pr-2">{it.foodName} ×{it.quantity}</span>
                  <span className="tabular text-stone-400 shrink-0">{formatVND(it.subtotal)}</span>
                </div>
              ))}
            </div>
            <div className="mt-2 flex justify-between border-t border-white/10 pt-2 text-sm">
              <span className="text-stone-400">Tổng cộng</span>
              <span className="font-bold tabular text-orange-400">{formatVND(total)}</span>
            </div>
          </div>
        )}

        {/* Hành động */}
        <div className="mt-3 flex gap-2">
          <button disabled={busy} onClick={() => run(onReject)}
            className="flex-1 rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm font-medium text-stone-300 hover:bg-white/10 transition disabled:opacity-50">
            Từ chối
          </button>
          <button disabled={busy} onClick={() => run(onConfirm)}
            className="flex-[1.4] inline-flex items-center justify-center gap-1.5 rounded-xl px-3 py-2 text-sm font-semibold text-white transition disabled:opacity-50 hover:brightness-110"
            style={{ background: 'linear-gradient(135deg,#16a34a,#15803d)' }}>
            <Check className="h-4 w-4" /> Xác nhận đơn
          </button>
        </div>
        <button onClick={onOpen}
          className="mt-2 flex w-full items-center justify-center gap-1 text-xs text-stone-500 hover:text-stone-300 transition">
          Xem trong quản lý đặt bàn <ChevronRight className="h-3 w-3" />
        </button>
      </div>
    </div>
  )
}

function Line({ icon: Icon, value }) {
  return (
    <div className="flex items-center gap-2 text-stone-300">
      <Icon className="h-3.5 w-3.5 text-stone-500 shrink-0" />
      <span className="truncate">{value}</span>
    </div>
  )
}
