import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, Users, Armchair, Trash2, Search, Wallet, Utensils, Check, Ticket } from 'lucide-react'
import toast from 'react-hot-toast'
import { reservationsApi } from '@/api/reservations'
import { tablesApi } from '@/api/tables'
import { foodsApi } from '@/api/foods'
import { ordersApi } from '@/api/cart'
import { vouchersApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import { Select } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import {
  formatDateTime, formatVND,
  reservationStatusLabel, reservationStatusTone, depositStatusLabel,
  orderStatusLabel, orderStatusTone, paymentStatusLabel, cn,
} from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

const STATUSES = ['PENDING', 'CONFIRMED', 'SEATED', 'COMPLETED', 'CANCELLED', 'NO_SHOW']

export default function AdminReservationsPage() {
  const qc = useQueryClient()
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [viewing, setViewing] = useState(null)

  const { data: list, isLoading } = useQuery({
    queryKey: ['admin-reservations'],
    queryFn: reservationsApi.adminList,
    refetchInterval: 15000,
  })

  const { data: tables } = useQuery({ queryKey: ['admin-tables-all'], queryFn: tablesApi.list })

  const update = useMutation({
    mutationFn: ({ id, payload }) => reservationsApi.updateStatus(id, payload),
    onSuccess: (updated) => {
      toast.success('Đã cập nhật đặt bàn')
      qc.invalidateQueries({ queryKey: ['admin-reservations'] })
      if (viewing) setViewing(updated)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => reservationsApi.adminDelete(id),
    onSuccess: () => {
      toast.success('Đã xóa lượt đặt bàn')
      qc.invalidateQueries({ queryKey: ['admin-reservations'] })
      setViewing(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const filtered = useMemo(() => {
    let r = list || []
    if (filter !== 'ALL') r = r.filter((x) => x.status === filter)
    if (search.trim()) {
      const q = search.toLowerCase()
      r = r.filter((x) =>
        x.reservationCode?.toLowerCase().includes(q) ||
        x.guestName?.toLowerCase().includes(q) ||
        x.guestPhone?.includes(q))
    }
    return r
  }, [list, filter, search])

  const { page, setPage, totalPages, paged } = usePagination(filtered, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-end flex-wrap gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
          <h1 className="font-display text-3xl font-bold text-ink-900">Đặt bàn</h1>
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
          <input className="input pl-9" placeholder="Tìm mã, tên, SĐT..."
            value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="input max-w-[200px]" value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          {STATUSES.map((s) => <option key={s} value={s}>{reservationStatusLabel(s)}</option>)}
        </select>
      </div>

      {filtered.length === 0 ? (
        <Empty icon={CalendarClock} title="Không có lượt đặt bàn" />
      ) : (
        <div className="space-y-2.5">
          {paged.map((r) => (
            <button key={r.id} onClick={() => setViewing(r)}
              className="card w-full text-left p-4 hover:border-ink-300 transition">
              <div className="flex justify-between items-start flex-wrap gap-2">
                <div>
                  <p className="font-mono text-sm font-semibold text-ink-900">#{r.reservationCode}</p>
                  <p className="text-sm text-ink-600">{r.guestName} · {r.guestPhone}</p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className={`chip border ${reservationStatusTone(r.status)}`}>
                    {reservationStatusLabel(r.status)}
                  </span>
                  {r.depositStatus === 'PENDING' && !['CANCELLED', 'NO_SHOW', 'COMPLETED'].includes(r.status) && (
                    <span className="text-[11px] text-amber-600 font-medium">⚠ Chờ xác nhận cọc</span>
                  )}
                </div>
              </div>
              <div className="mt-2 flex flex-wrap gap-x-5 gap-y-1 text-xs text-ink-500">
                <span className="flex items-center gap-1"><CalendarClock className="h-3.5 w-3.5" />{formatDateTime(r.reservationTime)}</span>
                <span className="flex items-center gap-1"><Users className="h-3.5 w-3.5" />{r.partySize} khách</span>
                <span className="flex items-center gap-1"><Armchair className="h-3.5 w-3.5" />{r.table ? `Bàn ${r.table.tableNumber}` : 'Chưa gán'}</span>
                {r.hasPreorder && <span className="flex items-center gap-1"><Utensils className="h-3.5 w-3.5" />Có món đặt trước</span>}
              </div>
            </button>
          ))}
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </div>
      )}

      <ReservationDetailModal
        reservation={viewing}
        tables={tables || []}
        onClose={() => setViewing(null)}
        onUpdate={(payload) => update.mutate({ id: viewing.id, payload })}
        onDelete={() => { if (confirm('Xóa lượt đặt bàn này?')) del.mutate(viewing.id) }}
        onAdded={async () => {
          try {
            const fresh = await reservationsApi.adminGet(viewing.id)
            setViewing(fresh)
          } catch {}
          qc.invalidateQueries({ queryKey: ['admin-reservations'] })
          qc.invalidateQueries({ queryKey: ['admin-orders'] })
        }}
        loading={update.isPending}
        deleting={del.isPending}
      />
    </div>
  )
}

function ReservationDetailModal({ reservation, tables, onClose, onUpdate, onDelete, onAdded, loading, deleting }) {
  const [assignTable, setAssignTable] = useState('')

  // Tự chọn sẵn bàn khách đã đặt (nếu có) khi mở modal
  useEffect(() => {
    if (reservation?.table?.id) setAssignTable(String(reservation.table.id))
    else setAssignTable('')
  }, [reservation?.id, reservation?.table?.id])
  if (!reservation) return null
  const r = reservation
  const canDelete = ['CANCELLED', 'COMPLETED', 'NO_SHOW'].includes(r.status)
  const usable = tables.filter((t) => t.status === 'AVAILABLE' || t.id === reservation?.table?.id)

  // Hành động theo vòng đời
  const actions = []
  if (r.status === 'PENDING') actions.push({ label: 'Xác nhận', status: 'CONFIRMED', variant: 'primary' })
  if (r.status === 'CONFIRMED') actions.push({ label: 'Khách đã đến (nhận bàn)', status: 'SEATED', variant: 'primary' })
  if (r.status === 'SEATED') actions.push({ label: 'Hoàn tất', status: 'COMPLETED', variant: 'primary' })
  if (['PENDING', 'CONFIRMED'].includes(r.status)) actions.push({ label: 'Hủy', status: 'CANCELLED', variant: 'danger' })
  if (['CONFIRMED', 'SEATED'].includes(r.status)) actions.push({ label: 'Khách không đến', status: 'NO_SHOW', variant: 'secondary' })

  return (
    <Modal open={!!reservation} onClose={onClose} title={`Đặt bàn #${r.reservationCode}`} size="lg">
      <div className="space-y-4">
        <div className="flex justify-between items-center">
          <span className={`chip border ${reservationStatusTone(r.status)}`}>{reservationStatusLabel(r.status)}</span>
          <span className="text-sm text-ink-500">{formatDateTime(r.reservationTime)}</span>
        </div>

        <div className="grid sm:grid-cols-2 gap-3 text-sm">
          <Field label="Khách" value={`${r.guestName} · ${r.guestPhone}`} />
          <Field label="Số khách" value={`${r.partySize} người`} />
          <Field label="Bàn" value={r.table ? `Bàn ${r.table.tableNumber} (${r.table.zone || 'khu chung'})` : 'Chưa gán'} />
          <Field label="Thanh toán tại quán" value={r.paymentMethod === 'CASH' ? 'Tiền mặt' : 'Chuyển khoản'} />
        </div>
        {r.note && <p className="text-sm text-ink-500">Ghi chú: {r.note}</p>}

        {/* Cọc */}
        <div className="rounded-lg bg-ink-50 border border-ink-200 p-3">
          <div className="flex justify-between items-center">
            <div className="text-sm">
              <span className="text-ink-600">Tiền cọc: </span>
              <span className="font-semibold">{formatVND(r.depositAmount)}</span>
              <span className="ml-2 text-ink-500">({depositStatusLabel(r.depositStatus)})</span>
            </div>
            {r.depositStatus !== 'PAID' && !['CANCELLED', 'NO_SHOW', 'COMPLETED'].includes(r.status) && (
              <Button size="sm" variant="secondary" loading={loading}
                onClick={() => onUpdate({ depositStatus: 'PAID' })}>
                <Check className="h-4 w-4" />Xác nhận đã nhận cọc
              </Button>
            )}
          </div>
        </div>

        {/* Gán bàn */}
        {!['COMPLETED', 'CANCELLED', 'NO_SHOW'].includes(r.status) && (
          <div className="rounded-lg border border-ink-200 p-3">
            <p className="text-sm font-medium text-ink-900 mb-2 flex items-center gap-1.5">
              <Armchair className="h-4 w-4 text-ink-500" />Gán / đổi bàn
            </p>
            <div className="flex gap-2">
              <select className="input flex-1" value={assignTable} onChange={(e) => setAssignTable(e.target.value)}>
                <option value="">-- Chọn bàn --</option>
                {usable.map((t) => (
                  <option key={t.id} value={t.id}>
                    Bàn {t.tableNumber} · {t.capacity} chỗ · {t.zone || 'khu chung'}
                  </option>
                ))}
              </select>
              <Button variant="secondary" disabled={!assignTable} loading={loading}
                onClick={() => onUpdate({ tableId: Number(assignTable) })}>Gán bàn</Button>
            </div>
          </div>
        )}

        {/* Đơn món đặt trước */}
        {r.preorder && (
          <div className="rounded-lg border border-ink-200 p-3">
            <div className="flex items-center justify-between mb-2">
              <p className="text-sm font-medium text-ink-900 flex items-center gap-1.5">
                <Utensils className="h-4 w-4 text-ink-500" />Món đặt trước (#{r.preorder.orderCode})
              </p>
              <span className={`chip border text-xs ${orderStatusTone(r.preorder.orderStatus)}`}>
                {orderStatusLabel(r.preorder.orderStatus)}
              </span>
            </div>
            <div className="divide-y divide-ink-100 text-sm">
              {r.preorder.items?.map((it) => (
                <div key={it.id} className="flex justify-between py-1.5">
                  <span className="text-ink-700">{it.foodName} × {it.quantity}</span>
                  <span className="tabular">{formatVND(it.subtotal)}</span>
                </div>
              ))}
              {Number(r.preorder.discountAmount) > 0 && (
                <>
                  <div className="flex justify-between py-1.5 text-ink-500">
                    <span>Tạm tính</span>
                    <span className="tabular">{formatVND(r.preorder.totalAmount)}</span>
                  </div>
                  <div className="flex justify-between py-1.5 text-success-700">
                    <span>Giảm giá (voucher)</span>
                    <span className="tabular">− {formatVND(r.preorder.discountAmount)}</span>
                  </div>
                </>
              )}
              <div className="flex justify-between py-1.5 font-semibold">
                <span>Tổng món · {paymentStatusLabel(r.preorder.paymentStatus)}</span>
                <span className="tabular">{formatVND(r.preorder.finalAmount)}</span>
              </div>
            </div>
          </div>
        )}

        {/* Áp dụng / gợi ý voucher cho đơn của khách (hiện ngay khi đã có món) */}
        {r.preorder && (r.preorder.items?.length || 0) > 0 && ['CONFIRMED', 'SEATED'].includes(r.status) && (
          <VoucherForReservation reservationId={r.id} preorder={r.preorder} onApplied={onAdded} />
        )}

        {/* Gọi món cho khách tại bàn (khi khách đã đến) */}
        {['CONFIRMED', 'SEATED'].includes(r.status) && (
          <AddFoodToReservation reservationId={r.id} onAdded={onAdded} />
        )}

        {/* Hành động trạng thái */}
        <div className="flex gap-2 flex-wrap pt-1">
          {actions.map((a) => (
            <Button key={a.status} variant={a.variant} loading={loading}
              disabled={(a.status === 'CONFIRMED' || a.status === 'SEATED') && !r.table}
              onClick={() => onUpdate({ status: a.status })}>
              {a.label}
            </Button>
          ))}
          {canDelete && (
            <Button variant="danger" loading={deleting} onClick={onDelete}>
              <Trash2 className="h-4 w-4" />Xóa
            </Button>
          )}
        </div>
        {(actions.some((a) => a.status === 'CONFIRMED' || a.status === 'SEATED')) && !r.table && (
          <p className="text-xs text-amber-600">Cần gán bàn trước khi xác nhận / cho khách nhận bàn.</p>
        )}
      </div>
    </Modal>
  )
}

function Field({ label, value }) {
  return (
    <div>
      <p className="text-xs text-ink-500">{label}</p>
      <p className="font-medium text-ink-900">{value}</p>
    </div>
  )
}

// Khu gọi món cho khách tại bàn — staff/admin thêm món vào đơn của lượt đặt bàn
function AddFoodToReservation({ reservationId, onAdded }) {
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState('')
  const [busy, setBusy] = useState(false)

  const { data: foods } = useQuery({
    queryKey: ['foods', 'reservation-add'],
    queryFn: () => foodsApi.list(),
    enabled: open,
  })

  const add = async (f) => {
    setBusy(true)
    try {
      await ordersApi.addItemByReservation(reservationId, f.id, 1)
      toast.success(`Đã thêm ${f.name}`)
      onAdded?.()
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  const list = (foods || []).filter((f) => f.name?.toLowerCase().includes(search.toLowerCase())).slice(0, 20)

  return (
    <div className="rounded-lg border border-accent-200 bg-accent-50/40 p-3">
      {!open ? (
        <Button variant="secondary" className="w-full" onClick={() => setOpen(true)}>
          <Utensils className="h-4 w-4" /> Gọi món cho khách tại bàn
        </Button>
      ) : (
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-ink-900">Chọn món gọi thêm</p>
            <button onClick={() => { setOpen(false); setSearch('') }} className="text-xs text-ink-500 hover:text-ink-800">Đóng</button>
          </div>
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Tìm món..." className="input w-full" />
          <div className="max-h-56 overflow-y-auto divide-y divide-ink-100">
            {list.map((f) => {
              const dpct = Number(f.discountPercent) || 0
              const price = dpct > 0 ? Math.round(Number(f.price) * (100 - dpct) / 100) : Number(f.price)
              return (
                <div key={f.id} className="flex items-center gap-3 py-2">
                  <span className="flex-1 text-sm">{f.name}</span>
                  <span className="text-sm tabular text-ink-600">{formatVND(price)}</span>
                  <button disabled={busy} onClick={() => add(f)}
                    className="rounded-md bg-accent-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-accent-700 disabled:opacity-50">
                    Thêm
                  </button>
                </div>
              )
            })}
            {list.length === 0 && <p className="py-3 text-center text-sm text-ink-400">Không tìm thấy món</p>}
          </div>
          <p className="text-xs text-ink-500">Món sẽ được thêm vào đơn món của khách. Xem/sửa chi tiết ở mục "Đơn món".</p>
        </div>
      )}
    </div>
  )
}

// Áp dụng & gợi ý voucher cho đơn món của lượt đặt bàn
function VoucherForReservation({ reservationId, preorder, onApplied }) {
  const [voucherCode, setVoucherCode] = useState('')
  const [busy, setBusy] = useState(false)

  const { data: vouchers } = useQuery({
    queryKey: ['vouchers', 'active'],
    queryFn: () => vouchersApi.active(),
  })

  const orderTotal = Number(preorder?.totalAmount) || 0
  const hasDiscount = Number(preorder?.discountAmount) > 0

  const bestVoucher = useMemo(() => {
    if (!vouchers || !orderTotal) return null
    let best = null
    for (const v of vouchers) {
      const min = Number(v.minOrderValue) || 0
      if (orderTotal < min) continue
      let d = v.discountType === 'PERCENT'
        ? Math.round(orderTotal * Number(v.discountValue) / 100)
        : Number(v.discountValue)
      if (v.maxDiscount && d > Number(v.maxDiscount)) d = Number(v.maxDiscount)
      if (d > 0 && (!best || d > best.discount)) best = { ...v, discount: d }
    }
    return best
  }, [vouchers, orderTotal])

  const apply = async (code) => {
    const c = (code ?? voucherCode).trim()
    setBusy(true)
    try {
      await ordersApi.applyVoucherByReservation(reservationId, c)
      toast.success(c ? 'Đã áp dụng mã giảm giá' : 'Đã bỏ mã giảm giá')
      if (code) setVoucherCode(code)
      onApplied?.()
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="rounded-lg border border-amber-200 bg-amber-50/50 p-3 space-y-2">
      <p className="text-sm font-medium text-ink-900 flex items-center gap-1.5">
        <Ticket className="h-4 w-4 text-amber-600" /> Mã giảm giá cho đơn này
      </p>
      <div className="flex gap-2">
        <input
          value={voucherCode}
          onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
          placeholder="Nhập mã giảm giá..."
          className="input flex-1 text-sm"
        />
        <button
          disabled={busy || !voucherCode.trim()}
          onClick={() => apply()}
          className="rounded-md bg-ink-900 px-3 py-1.5 text-sm font-medium text-white hover:bg-ink-800 disabled:opacity-50">
          Áp dụng
        </button>
        {hasDiscount && (
          <button
            disabled={busy}
            onClick={() => { setVoucherCode(''); apply('') }}
            className="rounded-md border border-ink-200 px-2.5 py-1.5 text-sm text-ink-500 hover:bg-white disabled:opacity-50">
            Bỏ mã
          </button>
        )}
      </div>
      {bestVoucher && !hasDiscount && (
        <button
          onClick={() => apply(bestVoucher.code)}
          className="w-full text-left text-xs text-accent-700 bg-white border border-accent-200 rounded-md px-2.5 py-2 hover:bg-accent-50 transition">
          💡 Gợi ý: dùng mã <b>{bestVoucher.code}</b> để giảm <b>{formatVND(bestVoucher.discount)}</b> — bấm để áp dụng nhanh
        </button>
      )}
      {hasDiscount && (
        <p className="text-xs text-success-700">
          ✓ Đã giảm {formatVND(preorder.discountAmount)} · Còn lại {formatVND(preorder.finalAmount)}
        </p>
      )}
      {!bestVoucher && !hasDiscount && (
        <p className="text-xs text-ink-400">Không có mã phù hợp với đơn hiện tại.</p>
      )}
    </div>
  )
}
