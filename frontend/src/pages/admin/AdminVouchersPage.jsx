import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Gift, Pencil, Trash2 } from 'lucide-react'
import toast from 'react-hot-toast'
import { vouchersApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { formatVND, formatDate, isPercentDiscount } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

function toStartDateTime(d) {
  if (!d) return null
  return d.length === 10 ? `${d}T00:00:00` : d
}
function toEndDateTime(d) {
  if (!d) return null
  return d.length === 10 ? `${d}T23:59:59` : d
}

export default function AdminVouchersPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: vouchers, isLoading } = useQuery({
    queryKey: ['admin-vouchers'],
    queryFn: vouchersApi.list,
  })

  const del = useMutation({
    mutationFn: (id) => vouchersApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa mã giảm giá')
      qc.invalidateQueries({ queryKey: ['admin-vouchers'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const { page, setPage, totalPages, paged } = usePagination(vouchers, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-end flex-wrap gap-3">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink-900">Quản lý mã giảm giá</h1>
          <p className="text-sm text-ink-500">Quản lý các mã giảm giá trong cửa hàng</p>
        </div>
        <Button onClick={() => setEditing({})}>
          <Plus className="h-4 w-4" /> Thêm mã giảm giá
        </Button>
      </div>

      {!vouchers || vouchers.length === 0 ? (
        <Empty icon={Gift} title="Chưa có mã giảm giá" />
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
              <tr>
                <th className="px-5 py-3 text-left font-medium">Mã giảm giá</th>
                <th className="px-5 py-3 text-left font-medium">Giảm giá</th>
                <th className="px-5 py-3 text-center font-medium">Số lượng</th>
                <th className="px-5 py-3 text-left font-medium">Hiệu lực</th>
                <th className="px-5 py-3 text-right font-medium">Đơn tối thiểu</th>
                <th className="px-5 py-3 text-right font-medium">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-100">
              {paged.map((v) => (
                <tr key={v.id} className="hover:bg-ink-50">
                  <td className="px-5 py-3 font-mono font-bold text-accent-600">{v.code}</td>
                  <td className="px-5 py-3 text-ink-900">
                    {isPercentDiscount(v.discountType) ? `${v.discountValue}%` : formatVND(v.discountValue)}
                  </td>
                  <td className="px-5 py-3 text-center">{v.quantity ?? '∞'}</td>
                  <td className="px-5 py-3 text-xs text-ink-600">
                    {v.startDate ? formatDate(v.startDate) : '—'} → {v.endDate ? formatDate(v.endDate) : '—'}
                  </td>
                  <td className="px-5 py-3 text-right tabular">{formatVND(v.minOrderValue || 0)}</td>
                  <td className="px-5 py-3">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => setEditing(v)}
                        className="grid h-8 w-8 place-items-center rounded-md hover:bg-ink-100 text-ink-600">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button onClick={() => setDeleteId(v.id)}
                        className="grid h-8 w-8 place-items-center rounded-md hover:bg-danger-50 text-danger-600">
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="px-5"><Pagination page={page} totalPages={totalPages} onChange={setPage} /></div>
        </div>
      )}

      <VoucherFormModal
        voucher={editing}
        open={!!editing}
        onClose={() => setEditing(null)}
        onSaved={() => { qc.invalidateQueries({ queryKey: ['admin-vouchers'] }); setEditing(null) }}
      />

      <Modal open={!!deleteId} onClose={() => setDeleteId(null)} title="Xóa mã giảm giá" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Hủy</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>Xác nhận xóa</Button>
          </>
        }>
        <p className="text-sm text-ink-700">Bạn có chắc muốn xóa mã giảm giá này?</p>
      </Modal>
    </div>
  )
}

function VoucherFormModal({ voucher, open, onClose, onSaved }) {
  const isEdit = !!voucher?.id
  const [form, setForm] = useState(blank())

  function blank() {
    return { code: '', discountValue: '', quantity: '', startDate: '', endDate: '', minOrderValue: '' }
  }

  useEffect(() => {
    if (open) {
      setForm(voucher?.id ? {
        code: voucher.code || '',
        discountValue: voucher.discountValue || '',
        quantity: voucher.quantity ?? '',
        startDate: voucher.startDate ? String(voucher.startDate).slice(0, 10) : '',
        endDate: voucher.endDate ? String(voucher.endDate).slice(0, 10) : '',
        minOrderValue: voucher.minOrderValue || '',
      } : blank())
    }
  }, [open, voucher])

  const mutate = useMutation({
    mutationFn: (data) => (isEdit ? vouchersApi.update(voucher.id, data) : vouchersApi.create(data)),
    onSuccess: () => { toast.success(isEdit ? 'Đã cập nhật mã' : 'Thêm mã giảm giá thành công'); onSaved() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const submit = () => {
    mutate.mutate({
      code: form.code,
      name: form.code,            // dùng mã làm tên
      discountType: '%',
      discountValue: Number(form.discountValue),
      quantity: form.quantity ? Number(form.quantity) : 0,
      minOrderValue: form.minOrderValue ? Number(form.minOrderValue) : 0,
      startDate: toStartDateTime(form.startDate),
      endDate: toEndDateTime(form.endDate),
      status: 'ACTIVE',
    })
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <Modal open={open} onClose={onClose}
      title={<span className="text-red-500 inline-flex items-center gap-2"><Gift className="h-5 w-5" /> {isEdit ? 'Sửa mã giảm giá' : 'Thêm mã giảm giá mới'}</span>}
      size="md">
      <div className="space-y-4">
        <Input label="* Mã giảm giá" value={form.code}
          onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} placeholder="TEST2025" />
        <div className="grid grid-cols-2 gap-3">
          <Input type="number" label="* Giảm giá (%)" value={form.discountValue} onChange={set('discountValue')} placeholder="Nhập giá..." />
          <Input type="number" label="* Số lượng" value={form.quantity} onChange={set('quantity')} placeholder="Nhập số..." />
        </div>
        <div>
          <label className="block text-xs text-ink-500 mb-1"><span className="text-red-500">* </span>Thời gian hiệu lực</label>
          <div className="flex items-center gap-2">
            <input type="date" className="input" value={form.startDate} onChange={set('startDate')} />
            <span className="text-ink-400">→</span>
            <input type="date" className="input" value={form.endDate} onChange={set('endDate')} />
          </div>
        </div>
        <Input type="number" label="* Đơn hàng tối thiểu" value={form.minOrderValue} onChange={set('minOrderValue')} placeholder="đ" />

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button loading={mutate.isPending} disabled={!form.code || !form.discountValue}
            onClick={submit}>{isEdit ? 'Cập nhật' : 'Thêm mới'}</Button>
        </div>
      </div>
    </Modal>
  )
}
