import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Tags } from 'lucide-react'
import toast from 'react-hot-toast'
import { categoriesApi } from '@/api/foods'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { cn } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

function Toggle({ checked, onChange }) {
  return (
    <button type="button" onClick={() => onChange(!checked)}
      className={cn('relative inline-flex h-6 w-11 items-center rounded-full transition',
        checked ? 'bg-blue-500' : 'bg-ink-300')}>
      <span className={cn('inline-block h-4 w-4 transform rounded-full bg-white transition',
        checked ? 'translate-x-6' : 'translate-x-1')} />
    </button>
  )
}

export default function AdminCategoriesPage() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: cats, isLoading } = useQuery({
    queryKey: ['admin-categories'],
    queryFn: categoriesApi.list,
  })

  const mutate = useMutation({
    mutationFn: (data) => (data.id ? categoriesApi.update(data.id, data) : categoriesApi.create(data)),
    onSuccess: (_, vars) => {
      toast.success(vars.id ? 'Cập nhật danh mục thành công' : 'Thêm danh mục thành công')
      qc.invalidateQueries({ queryKey: ['admin-categories'] })
      setEditing(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => categoriesApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa danh mục')
      qc.invalidateQueries({ queryKey: ['admin-categories'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const toggleFeatured = (c) =>
    mutate.mutate({ ...c, featured: !c.featured })

  const { page, setPage, totalPages, paged } = usePagination(cats, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-end flex-wrap gap-3">
        <h1 className="font-display text-3xl font-bold text-ink-900">Quản lý danh mục</h1>
        <Button onClick={() => setEditing({ status: 'ACTIVE', featured: false })}>
          <Plus className="h-4 w-4" /> Thêm danh mục
        </Button>
      </div>

      {!cats || cats.length === 0 ? (
        <Empty icon={Tags} title="Chưa có danh mục" />
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-ink-200 text-left text-ink-500">
                <th className="px-5 py-3 font-medium">Tên danh mục</th>
                <th className="px-5 py-3 font-medium">Trạng thái</th>
                <th className="px-5 py-3 font-medium">Nổi bật</th>
                <th className="px-5 py-3 font-medium text-right">Hành động</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((c) => (
                <tr key={c.id} className="border-b border-ink-100 last:border-0">
                  <td className="px-5 py-4 font-medium text-ink-900">{c.name}</td>
                  <td className="px-5 py-4 text-ink-600">
                    {c.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ẩn'}
                  </td>
                  <td className="px-5 py-4">
                    <Toggle checked={!!c.featured} onChange={() => toggleFeatured(c)} />
                  </td>
                  <td className="px-5 py-4">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => setEditing(c)}
                        className="rounded-md bg-blue-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-blue-600">
                        Sửa
                      </button>
                      <button onClick={() => setDeleteId(c.id)}
                        className="rounded-md border border-red-300 px-4 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">
                        Xóa
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

      {/* Modal thêm/sửa */}
      <Modal open={!!editing} onClose={() => setEditing(null)}
        title={editing?.id ? 'Sửa danh mục' : 'Thêm danh mục'} size="sm">
        {editing && (
          <div className="space-y-4">
            <Input label="Tên danh mục" value={editing.name || ''}
              onChange={(e) => setEditing({ ...editing, name: e.target.value })} placeholder="Tên danh mục" />
            <div>
              <p className="text-sm text-ink-700 mb-1.5">Trạng thái</p>
              <button type="button"
                onClick={() => setEditing({ ...editing, status: editing.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' })}
                className={cn('inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-sm font-medium transition',
                  editing.status === 'ACTIVE' ? 'bg-blue-500 text-white' : 'bg-ink-200 text-ink-600')}>
                {editing.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ẩn'}
                <span className={cn('inline-block h-4 w-4 rounded-full bg-white')} />
              </button>
            </div>
            <div>
              <p className="text-sm text-ink-700 mb-1.5">Nổi bật</p>
              <Toggle checked={!!editing.featured} onChange={(v) => setEditing({ ...editing, featured: v })} />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="secondary" onClick={() => setEditing(null)}>Hủy</Button>
              <Button loading={mutate.isPending} disabled={!editing.name}
                onClick={() => mutate.mutate(editing)}>
                {editing.id ? 'Cập nhật' : 'Thêm mới'}
              </Button>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal xóa */}
      <Modal open={!!deleteId} onClose={() => setDeleteId(null)} title="Xóa danh mục" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Hủy</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>Xác nhận xóa</Button>
          </>
        }>
        <p className="text-sm text-ink-700">Bạn có chắc muốn xóa danh mục này?</p>
      </Modal>
    </div>
  )
}
