import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Armchair } from 'lucide-react'
import toast from 'react-hot-toast'
import { tablesApi } from '@/api/tables'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import { Input, Textarea, Select } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { cn } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

export default function AdminTablesPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: tables, isLoading } = useQuery({
    queryKey: ['admin-tables'],
    queryFn: tablesApi.list,
  })

  const mutate = useMutation({
    mutationFn: (data) => (data.id ? tablesApi.update(data.id, data) : tablesApi.create(data)),
    onSuccess: () => {
      toast.success('Đã lưu bàn')
      qc.invalidateQueries({ queryKey: ['admin-tables'] })
      setEditing(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => tablesApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa bàn')
      qc.invalidateQueries({ queryKey: ['admin-tables'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const { page, setPage, totalPages, paged } = usePagination(tables, 12)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-end flex-wrap gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
          <h1 className="font-display text-3xl font-bold text-ink-900">Bàn ăn</h1>
        </div>
        <Button onClick={() => setEditing({ capacity: 4, status: 'AVAILABLE' })}>
          <Plus className="h-4 w-4" /> Thêm bàn
        </Button>
      </div>

      {!tables || tables.length === 0 ? (
        <Empty icon={Armchair} title="Chưa có bàn nào" />
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {paged.map((t) => (
            <div key={t.id} className="card p-4">
              <div className="flex justify-between items-start gap-2">
                <div className="min-w-0">
                  <p className="font-display text-lg font-semibold text-ink-900">Bàn {t.tableNumber}</p>
                  <p className="text-sm text-ink-600">{t.capacity} chỗ · {t.zone || 'Khu chung'}</p>
                </div>
                <span className={cn('chip border text-xs',
                  t.status === 'AVAILABLE'
                    ? 'bg-success-50 text-success-700 border-green-200'
                    : t.status === 'OCCUPIED'
                    ? 'bg-blue-50 text-blue-700 border-blue-200'
                    : t.status === 'RESERVED'
                    ? 'bg-purple-50 text-purple-700 border-purple-200'
                    : 'bg-amber-50 text-amber-700 border-amber-200')}>
                  {t.status === 'AVAILABLE' ? 'Trống'
                    : t.status === 'OCCUPIED' ? 'Đang phục vụ'
                    : t.status === 'RESERVED' ? 'Đã đặt'
                    : 'Bảo trì'}
                </span>
              </div>
              {t.description && <p className="mt-2 text-xs text-ink-500 line-clamp-2">{t.description}</p>}
              <div className="mt-3 flex flex-wrap gap-1.5">
                {t.status !== 'AVAILABLE' && (
                  <button onClick={() => mutate.mutate({ ...t, status: 'AVAILABLE' })}
                    className="rounded-md bg-green-50 border border-green-200 px-2 py-1 text-xs text-green-700 hover:bg-green-100">
                    Đặt Trống
                  </button>
                )}
                {t.status !== 'OCCUPIED' && (
                  <button onClick={() => mutate.mutate({ ...t, status: 'OCCUPIED' })}
                    className="rounded-md bg-blue-50 border border-blue-200 px-2 py-1 text-xs text-blue-700 hover:bg-blue-100">
                    Khách đang dùng
                  </button>
                )}
                {t.status !== 'MAINTENANCE' && (
                  <button onClick={() => mutate.mutate({ ...t, status: 'MAINTENANCE' })}
                    className="rounded-md bg-amber-50 border border-amber-200 px-2 py-1 text-xs text-amber-700 hover:bg-amber-100">
                    Bảo trì
                  </button>
                )}
              </div>
              <div className="mt-2 flex gap-1 justify-end">
                <button onClick={() => setEditing(t)} className="rounded-md p-1.5 text-ink-500 hover:bg-ink-100 hover:text-ink-900">
                  <Pencil className="h-4 w-4" />
                </button>
                <button onClick={() => setDeleteId(t.id)} className="rounded-md p-1.5 text-danger-500 hover:bg-danger-50">
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
          <div className="col-span-full"><Pagination page={page} totalPages={totalPages} onChange={setPage} /></div>
        </div>
      )}

      {/* Modal tạo/sửa */}
      <Modal open={!!editing} onClose={() => setEditing(null)}
        title={editing?.id ? 'Sửa bàn' : 'Thêm bàn'}>
        {editing && (
          <div className="space-y-3">
            <Input label="Số bàn" value={editing.tableNumber || ''}
              onChange={(e) => setEditing({ ...editing, tableNumber: e.target.value })} placeholder="B11" />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Sức chứa" type="number" min={1} value={editing.capacity || ''}
                onChange={(e) => setEditing({ ...editing, capacity: Number(e.target.value) })} />
              <Select label="Trạng thái" value={editing.status || 'AVAILABLE'}
                onChange={(e) => setEditing({ ...editing, status: e.target.value })}>
                <option value="AVAILABLE">Trống (sẵn sàng)</option>
                <option value="OCCUPIED">Đang phục vụ</option>
                <option value="MAINTENANCE">Bảo trì</option>
              </Select>
            </div>
            <Input label="Khu vực" value={editing.zone || ''}
              onChange={(e) => setEditing({ ...editing, zone: e.target.value })} placeholder="Tầng 1 / VIP / Sân vườn" />
            <Textarea label="Mô tả" rows={2} value={editing.description || ''}
              onChange={(e) => setEditing({ ...editing, description: e.target.value })} />
            <div className="flex gap-2 pt-1">
              <Button variant="secondary" className="flex-1" onClick={() => setEditing(null)}>Hủy</Button>
              <Button className="flex-1" loading={mutate.isPending}
                disabled={!editing.tableNumber || !editing.capacity}
                onClick={() => mutate.mutate(editing)}>Lưu</Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal xóa */}
      <Modal open={!!deleteId} onClose={() => setDeleteId(null)} title="Xóa bàn?">
        <p className="text-sm text-ink-600">Bạn chắc chắn muốn xóa bàn này? Hành động không thể hoàn tác.</p>
        <div className="mt-4 flex gap-2">
          <Button variant="secondary" className="flex-1" onClick={() => setDeleteId(null)}>Hủy</Button>
          <Button variant="danger" className="flex-1" loading={del.isPending}
            onClick={() => del.mutate(deleteId)}>Xóa</Button>
        </div>
      </Modal>
    </div>
  )
}
