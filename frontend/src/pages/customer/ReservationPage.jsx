import { useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate, Link } from 'react-router-dom'
import { Trash2, Tag } from 'lucide-react'
import toast from 'react-hot-toast'
import { cartApi } from '@/api/cart'
import { tablesApi } from '@/api/tables'
import { reservationsApi } from '@/api/reservations'
import { payosApi } from '@/api/payos'
import { vouchersApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { useCartStore } from '@/store/cart'
import { useAuth } from '@/store/auth'
import { Loader, FoodImage } from '@/components/ui/Atoms'
import { formatVND, cn, isPercentDiscount } from '@/lib/utils'

function defaultTime() {
  // Mặc định đúng thời điểm hiện tại để đồng nhất với giờ thực; khách có thể chỉnh sang giờ muốn đặt.
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export default function ReservationPage() {
  const qc = useQueryClient()
  const nav = useNavigate()
  const user = useAuth((s) => s.user)
  const isStaffOrAdmin = useAuth((s) => s.isAdminOrStaff())
  const refreshCart = useCartStore((s) => s.refresh)

  const [guestName, setGuestName] = useState('')
  const [email, setEmail] = useState('')
  const [guestPhone, setGuestPhone] = useState('')
  const [reservationTime, setReservationTime] = useState(defaultTime())
  const [tableId, setTableId] = useState('')
  const [note, setNote] = useState('')
  const [voucherCode, setVoucherCode] = useState('')
  const [voucher, setVoucher] = useState(null)
  useEffect(() => {
    if (user) {
      setGuestName((v) => v || user.fullName || '')
      setEmail((v) => v || user.email || '')
      setGuestPhone((v) => v || user.phone || '')
    }
  }, [user])

  const { data: cart } = useQuery({ queryKey: ['cart'], queryFn: cartApi.get })
  const { data: tables, isLoading: tablesLoading } = useQuery({ queryKey: ['tables'], queryFn: tablesApi.list })
  const { data: activeVouchers } = useQuery({ queryKey: ['vouchers', 'active'], queryFn: vouchersApi.active, retry: false })

  const cartItems = cart?.items || []
  const hasCart = cartItems.length > 0

  const updateQty = useMutation({
    mutationFn: ({ itemId, quantity }) => cartApi.updateItem(itemId, quantity),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cart'] }); refreshCart() },
  })
  const removeItem = useMutation({
    mutationFn: (itemId) => cartApi.removeItem(itemId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['cart'] }); refreshCart() },
  })

  // Hiển thị bàn còn trống. Bàn đang chờ xác nhận (pendingReservation) vẫn hiện
  // nhưng tô đỏ và không cho chọn. Bàn đã xác nhận/đang dùng (status != AVAILABLE) sẽ ẩn.
  const usableTables = useMemo(
    () => (tables || []).filter((t) => t.status === 'AVAILABLE'),
    [tables]
  )

  const validateVoucher = async () => {
    if (!voucherCode) { setVoucher(null); return }
    if (!hasCart) {
      toast.error('Giỏ hàng trống, chưa thể áp mã giảm giá')
      return
    }
    try {
      const v = await vouchersApi.validate(voucherCode)
      // Kiểm tra điều kiện đơn tối thiểu ngay khi áp
      const min = Number(v.minOrderValue || 0)
      if (min > 0 && subtotal < min) {
        setVoucher(null)
        toast.error(`Đơn tối thiểu ${formatVND(min)} mới dùng được mã này (hiện ${formatVND(subtotal)})`)
        return
      }
      setVoucher(v)
      toast.success(`Áp dụng voucher ${v.code}`)
    } catch (e) {
      setVoucher(null)
      toast.error(errMsg(e, 'Mã giảm giá không hợp lệ'))
    }
  }

  const subtotal = useMemo(
    () => cartItems.reduce((s, it) => s + Number(it.unitPrice) * it.quantity, 0),
    [cartItems]
  )
  const discount = useMemo(() => {
    if (!voucher) return 0
    let d = 0
    if (isPercentDiscount(voucher.discountType)) {
      d = (subtotal * Number(voucher.discountValue)) / 100
      if (voucher.maxDiscount) d = Math.min(d, Number(voucher.maxDiscount))
    } else d = Number(voucher.discountValue)
    return Math.min(d, subtotal)
  }, [voucher, subtotal])
  const total = subtotal - discount

  // Gợi ý voucher tốt nhất đủ điều kiện theo tổng tiền giỏ hiện tại
  const bestVoucher = useMemo(() => {
    if (!hasCart || !activeVouchers?.length) return null
    const calcSaving = (v) => {
      const min = Number(v.minOrderValue || 0)
      if (subtotal < min) return 0
      let d = 0
      if (isPercentDiscount(v.discountType)) {
        d = (subtotal * Number(v.discountValue)) / 100
        if (v.maxDiscount) d = Math.min(d, Number(v.maxDiscount))
      } else {
        d = Number(v.discountValue)
      }
      return Math.min(d, subtotal)
    }
    let best = null
    let bestSaving = 0
    for (const v of activeVouchers) {
      const s = calcSaving(v)
      if (s > bestSaving) { bestSaving = s; best = v }
    }
    if (!best || bestSaving <= 0) return null
    // Không gợi ý lại mã đang áp
    if (voucher && voucher.code === best.code) return null
    return { ...best, saving: bestSaving }
  }, [activeVouchers, subtotal, hasCart, voucher])

  const submit = useMutation({
    mutationFn: (method) =>
      reservationsApi.create({
        guestName, guestPhone,
        partySize: 2,
        reservationTime,
        tableId: tableId ? Number(tableId) : null,
        note: note || null,
        paymentMethod: method,
        preorder: hasCart,
        voucherCode: voucher ? voucher.code : null,
      }),
    onSuccess: async (rsv, method) => {
      qc.invalidateQueries({ queryKey: ['my-reservations'] })
      qc.invalidateQueries({ queryKey: ['cart'] })
      refreshCart()
      toast.success('Đặt bàn thành công!')
      if (method === 'CASH') {
        // Thanh toán tại nhà hàng: không cọc, vào thẳng trang xác nhận thành công
        nav(`/payment-success/${rsv.id}`)
        return
      }
      // PAYOS: sang trang thanh toán cọc của hệ thống (hiện QR + đếm ngược 3 phút + tự xác nhận)
      nav(`/deposit/${rsv.id}`)
    },
    onError: (e) => toast.error(errMsg(e, 'Đặt bàn thất bại')),
  })

  const canSubmit = guestName.trim() && guestPhone.trim() && reservationTime && tableId

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[1fr_400px]">
        {/* LEFT: tables + cart */}
        <div className="space-y-6">
          <div className="card p-5">
            <h2 className="font-display text-xl font-bold text-ink-900 mb-4">Danh sách bàn</h2>
            {tablesLoading ? <Loader /> : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-left text-ink-500 border-b border-ink-200">
                    <tr>
                      <th className="px-3 py-2 w-10"></th>
                      <th className="px-3 py-2 font-medium">Số bàn</th>
                      <th className="px-3 py-2 font-medium">Tên bàn</th>
                      <th className="px-3 py-2 font-medium">Vị trí</th>
                      <th className="px-3 py-2 font-medium">Số ghế</th>
                      <th className="px-3 py-2 font-medium">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody>
                    {usableTables.map((t) => {
                      const pending = !!t.pendingReservation
                      const selected = String(tableId) === String(t.id)
                      return (
                      <tr key={t.id}
                        className={cn('border-b border-ink-100 transition',
                          pending ? 'bg-red-50 cursor-not-allowed'
                            : selected ? 'bg-blue-50 cursor-pointer' : 'hover:bg-ink-50 cursor-pointer')}
                        onClick={() => { if (!pending) setTableId(t.id) }}>
                        <td className="px-3 py-3">
                          <input type="radio" checked={selected} disabled={pending}
                            onChange={() => setTableId(t.id)} className="accent-blue-500" />
                        </td>
                        <td className={cn('px-3 py-3', pending ? 'text-red-700' : 'text-ink-900')}>{t.tableNumber}</td>
                        <td className={cn('px-3 py-3', pending ? 'text-red-600' : 'text-ink-700')}>Bàn {t.tableNumber}</td>
                        <td className={cn('px-3 py-3', pending ? 'text-red-600' : 'text-ink-700')}>{t.zone || '—'}</td>
                        <td className={cn('px-3 py-3', pending ? 'text-red-600' : 'text-ink-700')}>{t.capacity} người</td>
                        <td className="px-3 py-3">
                          {pending ? (
                            <span className="rounded-md bg-red-100 px-2 py-0.5 text-xs text-red-700 border border-red-200">Đang chờ xác nhận</span>
                          ) : (
                            <span className="rounded-md bg-green-50 px-2 py-0.5 text-xs text-green-700 border border-green-200">Trống</span>
                          )}
                        </td>
                      </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="card p-5">
            <h2 className="font-display text-xl font-bold text-ink-900 mb-4">Giỏ hàng</h2>
            {hasCart ? (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-left text-ink-500 border-b border-ink-200">
                    <tr>
                      <th className="px-3 py-2 font-medium">Sản phẩm</th>
                      <th className="px-3 py-2 font-medium">Đơn giá</th>
                      <th className="px-3 py-2 font-medium">Số lượng</th>
                      <th className="px-3 py-2 font-medium">Thành tiền</th>
                      <th className="px-3 py-2 font-medium">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cartItems.map((it) => (
                      <tr key={it.id} className="border-b border-ink-100">
                        <td className="px-3 py-3">
                          <div className="flex items-center gap-3">
                            <FoodImage src={it.imageUrl} name={it.foodName} size="sm" />
                            <span className="text-ink-900">{it.foodName}</span>
                          </div>
                        </td>
                        <td className="px-3 py-3 tabular">{formatVND(it.unitPrice)}</td>
                        <td className="px-3 py-3">
                          <input type="number" min={1} value={it.quantity}
                            onChange={(e) => updateQty.mutate({ itemId: it.id, quantity: Math.max(1, Number(e.target.value)) })}
                            className="input w-16 py-1" />
                        </td>
                        <td className="px-3 py-3 tabular font-medium">{formatVND(Number(it.unitPrice) * it.quantity)}</td>
                        <td className="px-3 py-3">
                          <button onClick={() => removeItem.mutate(it.id)}
                            className="rounded-md border border-red-200 p-1.5 text-red-500 hover:bg-red-50">
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-sm text-ink-500">
                Giỏ hàng trống. <Link to="/foods" className="text-accent-600 underline">Chọn món</Link> để đặt trước,
                hoặc đặt bàn rồi gọi món tại quán.
              </p>
            )}
          </div>
        </div>

        {/* RIGHT: summary + form + payment */}
        <div className="card p-5 h-fit space-y-4">
          <div className="flex justify-between text-sm">
            <span className="text-ink-500">Tạm tính:</span>
            <span className="tabular">{formatVND(subtotal)}</span>
          </div>
          {discount > 0 && (
            <div className="flex justify-between text-sm text-accent-600">
              <span>Giảm giá:</span>
              <span className="tabular">- {formatVND(discount)}</span>
            </div>
          )}
          <div className="flex justify-between items-baseline border-t border-ink-100 pt-3">
            <span className="font-medium text-ink-700">Tổng cộng:</span>
            <span className="font-display text-2xl font-bold tabular text-ink-900">{formatVND(total)}</span>
          </div>

          <div>
            <label className="block text-xs text-ink-500 mb-1"><span className="text-red-500">* </span>Thời gian đặt bàn</label>
            <input type="datetime-local" className="input" value={reservationTime}
              onChange={(e) => setReservationTime(e.target.value)} />
          </div>
          <div>
            <label className="block text-xs text-ink-500 mb-1">Họ và tên:</label>
            <input className="input" value={guestName} onChange={(e) => setGuestName(e.target.value)} placeholder="Nhập họ và tên" />
          </div>
          <div>
            <label className="block text-xs text-ink-500 mb-1">Email:</label>
            <input className="input" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="Nhập email" />
          </div>
          <div>
            <label className="block text-xs text-ink-500 mb-1">Số điện thoại:</label>
            <input className="input" value={guestPhone} onChange={(e) => setGuestPhone(e.target.value)} placeholder="Nhập số điện thoại" />
          </div>

          <div>
            <label className="block text-xs text-red-500 font-medium mb-1">Mã giảm giá</label>
            <div className="flex gap-2">
              <input className="input flex-1" value={voucherCode}
                onChange={(e) => setVoucherCode(e.target.value.toUpperCase())} placeholder="Nhập mã giảm giá" />
              <button onClick={validateVoucher}
                className="rounded-lg border border-ink-300 px-4 text-sm font-medium hover:bg-ink-50 inline-flex items-center gap-1">
                <Tag className="h-3.5 w-3.5" /> Áp dụng
              </button>
            </div>
            {/* Gợi ý voucher tốt nhất đủ điều kiện */}
            {bestVoucher && (
              <button
                type="button"
                onClick={() => {
                  setVoucherCode(bestVoucher.code)
                  navigator.clipboard?.writeText(bestVoucher.code)
                  toast.success(`Đã điền mã ${bestVoucher.code}, bấm "Áp dụng" để dùng`)
                }}
                className="mt-1.5 flex w-full items-center gap-1.5 text-left text-xs font-medium text-accent-700 hover:text-accent-800">
                <Tag className="h-3.5 w-3.5 shrink-0" />
                <span>
                  Dùng mã <b>{bestVoucher.code}</b> để tiết kiệm {formatVND(bestVoucher.saving)} — bấm để điền nhanh
                </span>
              </button>
            )}
          </div>

          <div>
            <label className="block text-xs text-ink-500 mb-1">Ghi chú:</label>
            <textarea className="input resize-none" rows={2} value={note}
              onChange={(e) => setNote(e.target.value)} placeholder="Nhập ghi chú..." />
          </div>

          {!tableId && (
            <p className="text-xs text-amber-600 bg-amber-50 border border-amber-200 rounded-md px-3 py-2">
              Vui lòng chọn bàn trước khi thanh toán. Bạn có thể đặt bàn trước mà chưa cần chọn món.
            </p>
          )}
          <button disabled={!canSubmit || submit.isPending} onClick={() => submit.mutate('PAYOS')}
            className="w-full inline-flex items-center justify-center gap-2 rounded-lg px-4 py-3 text-sm font-semibold text-white hover:opacity-90 disabled:opacity-60"
            style={{ background: '#1a8d46' }}>
            Thanh toán chuyển khoản
          </button>
          {isStaffOrAdmin && (
            <button disabled={!canSubmit || submit.isPending} onClick={() => submit.mutate('CASH')}
              className="w-full inline-flex items-center justify-center gap-2 rounded-lg border border-ink-300 px-4 py-3 text-sm font-semibold text-ink-800 hover:bg-ink-50 disabled:opacity-60">
              Thanh toán tại nhà hàng
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
