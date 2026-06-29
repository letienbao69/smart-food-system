import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Search, Eye, Receipt, CreditCard, Plus, Minus, Trash2, X } from 'lucide-react'
import toast from 'react-hot-toast'
import { ordersApi } from '@/api/cart'
import { foodsApi } from '@/api/foods'
import { errMsg } from '@/api/client'
import { Loader, Empty, FoodImage } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import {
  formatVND, formatDateTime,
  orderStatusLabel, orderStatusTone, paymentStatusLabel,
} from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

const STATUS_OPTIONS = ['PENDING', 'CONFIRMED', 'PREPARING', 'SERVED', 'COMPLETED', 'CANCELLED']
const PAY_LABEL = { PAYOS: 'Chuyển khoản', CASH: 'Tiền mặt' }

export default function AdminOrdersPage() {
  const qc = useQueryClient()
  const nav = useNavigate()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [viewing, setViewing] = useState(null)

  const { data: orders, isLoading } = useQuery({
    queryKey: ['admin-orders'],
    queryFn: ordersApi.adminList,
    refetchInterval: 15000,
  })

  const updateStatus = useMutation({
    mutationFn: ({ id, payload }) => ordersApi.updateStatus(id, payload),
    onSuccess: () => {
      toast.success('Đã cập nhật trạng thái')
      qc.invalidateQueries({ queryKey: ['admin-orders'] })
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const filtered = useMemo(() => {
    let o = orders || []
    if (filter !== 'ALL') o = o.filter((x) => x.orderStatus === filter)
    if (search.trim()) {
      const q = search.toLowerCase()
      o = o.filter((x) =>
        x.customerName?.toLowerCase().includes(q) ||
        x.guestPhone?.includes(q) ||
        x.orderCode?.toLowerCase().includes(q))
    }
    return o
  }, [orders, filter, search])

  const { page, setPage, totalPages, paged } = usePagination(filtered, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
        <h1 className="font-display text-3xl font-bold text-ink-900">Quản lý đơn hàng</h1>
      </div>

      <div className="card p-4 flex gap-3 flex-wrap items-center">
        <div className="relative flex-1 min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
          <input className="input pl-9" placeholder="Tìm theo tên, số điện thoại hoặc mã đơn..."
            value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <select className="input max-w-[200px]" value={filter} onChange={(e) => setFilter(e.target.value)}>
          <option value="ALL">Tất cả trạng thái</option>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{orderStatusLabel(s)}</option>)}
        </select>
        <span className="text-sm text-ink-500">Tìm thấy <b className="text-ink-900">{filtered.length}</b> đơn hàng</span>
      </div>

      {filtered.length === 0 ? (
        <Empty icon={Receipt} title="Chưa có đơn hàng" />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Mã đơn</th>
                  <th className="px-4 py-3 text-left font-medium">Thông tin khách hàng</th>
                  <th className="px-4 py-3 text-left font-medium">Bàn</th>
                  <th className="px-4 py-3 text-right font-medium">Tổng tiền</th>
                  <th className="px-4 py-3 text-center font-medium">Thanh toán</th>
                  <th className="px-4 py-3 text-left font-medium">Trạng thái</th>
                  <th className="px-4 py-3 text-left font-medium">Thời gian</th>
                  <th className="px-4 py-3 text-right font-medium">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {paged.map((o) => {
                  const served = ['SERVED', 'COMPLETED'].includes(o.orderStatus)
                  return (
                    <tr key={o.id} className="hover:bg-ink-50">
                      <td className="px-4 py-3">
                        <span className="rounded-md bg-teal-50 px-2 py-1 font-mono text-xs text-teal-700">
                          {o.orderCode?.slice(0, 8)}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-ink-900">{o.customerName || '—'}</p>
                        {o.guestPhone && <p className="text-xs text-ink-500">{o.guestPhone}</p>}
                      </td>
                      <td className="px-4 py-3">
                        {o.tableNumber ? (
                          <span className="rounded-md bg-blue-50 px-2 py-0.5 text-xs text-blue-700">Bàn {o.tableNumber}</span>
                        ) : '—'}
                      </td>
                      <td className="px-4 py-3 text-right tabular font-semibold text-green-600">{formatVND(o.finalAmount)}</td>
                      <td className="px-4 py-3 text-center">
                        <span className="inline-flex items-center gap-1 text-xs text-ink-700">
                          <CreditCard className="h-3.5 w-3.5" />{PAY_LABEL[o.paymentMethod] || o.paymentMethod}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <select className={`rounded-md border px-2 py-1 text-xs ${orderStatusTone(o.orderStatus)}`}
                          value={o.orderStatus}
                          onChange={(e) => updateStatus.mutate({ id: o.id, payload: { orderStatus: e.target.value } })}>
                          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{orderStatusLabel(s)}</option>)}
                        </select>
                      </td>
                      <td className="px-4 py-3 text-xs text-ink-500">{formatDateTime(o.createdAt)}</td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <button onClick={() => setViewing(o)}
                            className="grid h-8 w-8 place-items-center rounded-md bg-blue-500 text-white hover:bg-blue-600">
                            <Eye className="h-4 w-4" />
                          </button>
                          {served && (
                            <button onClick={() => nav(`/admin/orders/${o.id}/invoice`)}
                              className="rounded-md bg-red-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-red-600">
                              Xuất hoá đơn
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="px-4"><Pagination page={page} totalPages={totalPages} onChange={setPage} /></div>
        </div>
      )}

      <OrderDetailModal
        order={viewing}
        onClose={() => setViewing(null)}
        onChanged={(fresh) => {
          setViewing(fresh)
          qc.invalidateQueries({ queryKey: ['admin-orders'] })
        }}
        onInvoice={(id) => nav(`/admin/orders/${id}/invoice`)}
      />
    </div>
  )
}

function OrderDetailModal({ order, onClose, onChanged, onInvoice }) {
  const [adding, setAdding] = useState(false)
  const [foodSearch, setFoodSearch] = useState('')
  const [busy, setBusy] = useState(false)

  const { data: foods } = useQuery({
    queryKey: ['foods', 'order-add'],
    queryFn: () => foodsApi.list(),
    enabled: adding,
  })

  if (!order) return null

  const editable = !['COMPLETED', 'CANCELLED'].includes(order.orderStatus)

  const run = async (fn, okMsg) => {
    setBusy(true)
    try {
      const fresh = await fn()
      if (okMsg) toast.success(okMsg)
      onChanged(fresh)
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  const changeQty = (it, delta) =>
    run(() => ordersApi.updateItem(order.id, it.id, Math.max(0, (it.quantity || 0) + delta)))
  const removeItem = (it) => run(() => ordersApi.removeItem(order.id, it.id), 'Đã xóa món')
  const addFood = (f) => run(() => ordersApi.addItem(order.id, f.id, 1), 'Đã thêm món')

  const foodList = (foods || []).filter((f) =>
    f.name?.toLowerCase().includes(foodSearch.toLowerCase())
  ).slice(0, 20)

  return (
    <Modal open={!!order} onClose={onClose}
      title={`Đơn #${order.orderCode?.slice(0, 8)}`} size="lg">
      <div className="space-y-4">
        <div className="grid sm:grid-cols-2 gap-3 text-sm">
          <Info label="Khách hàng" value={`${order.customerName || '—'}${order.guestPhone ? ' · ' + order.guestPhone : ''}`} />
          <Info label="Bàn" value={order.tableNumber ? `Bàn ${order.tableNumber}` : '—'} />
          <Info label="Thanh toán" value={`${PAY_LABEL[order.paymentMethod] || order.paymentMethod} · ${paymentStatusLabel(order.paymentStatus)}`} />
          <Info label="Thời gian đặt" value={order.reservationTime ? formatDateTime(order.reservationTime) : formatDateTime(order.createdAt)} />
        </div>

        {/* Danh sách món — có sửa/xóa khi đơn chưa hoàn tất */}
        <div className="rounded-lg border border-ink-200 divide-y divide-ink-100">
          {order.items?.length ? order.items.map((it) => (
            <div key={it.id} className="flex items-center gap-3 px-3 py-2">
              <FoodImage src={it.imageUrl} name={it.foodName} size="sm" />
              <span className="flex-1 text-sm">{it.foodName}</span>
              {editable ? (
                <div className="flex items-center gap-1.5">
                  <button disabled={busy} onClick={() => changeQty(it, -1)}
                    className="grid h-7 w-7 place-items-center rounded-md border border-ink-200 hover:bg-ink-50 disabled:opacity-50">
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="w-7 text-center text-sm font-medium tabular">{it.quantity}</span>
                  <button disabled={busy} onClick={() => changeQty(it, 1)}
                    className="grid h-7 w-7 place-items-center rounded-md border border-ink-200 hover:bg-ink-50 disabled:opacity-50">
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                  <button disabled={busy} onClick={() => removeItem(it)}
                    className="grid h-7 w-7 place-items-center rounded-md text-danger-500 hover:bg-danger-50 disabled:opacity-50 ml-1">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              ) : (
                <span className="text-sm text-ink-500">× {it.quantity}</span>
              )}
              <span className="tabular font-medium text-sm w-24 text-right">{formatVND(it.subtotal)}</span>
            </div>
          )) : (
            <div className="px-3 py-4 text-center text-sm text-ink-400">Chưa có món nào</div>
          )}
          <div className="flex justify-between px-3 py-2 font-semibold">
            <span>Tổng cộng</span><span className="tabular text-accent-700">{formatVND(order.finalAmount)}</span>
          </div>
        </div>

        {/* Thêm món (khi khách order tiếp) */}
        {editable && (
          <div>
            {!adding ? (
              <Button variant="secondary" onClick={() => setAdding(true)} className="w-full">
                <Plus className="h-4 w-4" /> Thêm món vào đơn
              </Button>
            ) : (
              <div className="rounded-lg border border-ink-200 p-3 space-y-2">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-medium">Chọn món thêm</p>
                  <button onClick={() => { setAdding(false); setFoodSearch('') }} className="text-ink-400 hover:text-ink-700">
                    <X className="h-4 w-4" />
                  </button>
                </div>
                <input value={foodSearch} onChange={(e) => setFoodSearch(e.target.value)}
                  placeholder="Tìm món..." className="input w-full" />
                <div className="max-h-52 overflow-y-auto divide-y divide-ink-100">
                  {foodList.map((f) => {
                    const dpct = Number(f.discountPercent) || 0
                    const price = dpct > 0 ? Math.round(Number(f.price) * (100 - dpct) / 100) : Number(f.price)
                    return (
                      <div key={f.id} className="flex items-center gap-3 py-2">
                        <FoodImage src={f.imageUrl} name={f.name} size="sm" />
                        <span className="flex-1 text-sm">{f.name}</span>
                        <span className="text-sm tabular text-ink-600">{formatVND(price)}</span>
                        <button disabled={busy} onClick={() => addFood(f)}
                          className="rounded-md bg-accent-600 px-2.5 py-1 text-xs font-medium text-white hover:bg-accent-700 disabled:opacity-50">
                          Thêm
                        </button>
                      </div>
                    )
                  })}
                  {foodList.length === 0 && <p className="py-3 text-center text-sm text-ink-400">Không tìm thấy món</p>}
                </div>
              </div>
            )}
          </div>
        )}

        {['SERVED', 'COMPLETED'].includes(order.orderStatus) && (
          <Button onClick={() => onInvoice(order.id)} className="w-full">
            <Receipt className="h-4 w-4" /> Xuất hoá đơn
          </Button>
        )}
      </div>
    </Modal>
  )
}

function Info({ label, value }) {
  return (
    <div>
      <p className="text-xs text-ink-500">{label}</p>
      <p className="font-medium text-ink-900">{value}</p>
    </div>
  )
}
