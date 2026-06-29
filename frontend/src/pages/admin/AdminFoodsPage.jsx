import { useState, useEffect, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Search, Utensils, Upload } from 'lucide-react'
import toast from 'react-hot-toast'
import { foodsApi, categoriesApi } from '@/api/foods'
import { uploadApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { Loader, FoodImage, Badge, Empty } from '@/components/ui/Atoms'
import { Input, Select, Textarea } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { formatVND } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

export default function AdminFoodsPage() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: foods, isLoading } = useQuery({
    queryKey: ['admin-foods'],
    queryFn: () => foodsApi.list(),
  })
  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.list,
  })

  const del = useMutation({
    mutationFn: (id) => foodsApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa món')
      qc.invalidateQueries({ queryKey: ['admin-foods'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const filtered = (foods || []).filter((f) => f.name?.toLowerCase().includes(search.toLowerCase()))

  const { page, setPage, totalPages, paged } = usePagination(filtered, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex justify-between items-end flex-wrap gap-3">
        <h1 className="font-display text-3xl font-bold text-ink-900">Quản lý món ăn</h1>
        <Button onClick={() => setEditing('new')}>
          <Plus className="h-4 w-4" /> Thêm món ăn
        </Button>
      </div>

      <div className="card p-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
          <input value={search} onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm món..." className="input pl-10" />
        </div>
      </div>

      {filtered.length === 0 ? (
        <Empty icon={Utensils} title="Chưa có món nào" />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
                <tr>
                  <th className="px-4 py-2.5 text-left font-medium">Hình ảnh</th>
                  <th className="px-4 py-2.5 text-left font-medium">Tên món</th>
                  <th className="px-4 py-2.5 text-right font-medium">Giá</th>
                  <th className="px-4 py-2.5 text-left font-medium">Danh mục</th>
                  <th className="px-4 py-2.5 text-center font-medium">Trạng thái</th>
                  <th className="px-4 py-2.5 text-center font-medium">Giảm giá</th>
                  <th className="px-4 py-2.5 text-right font-medium">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {paged.map((f) => {
                  const dpct = Number(f.discountPercent) || 0
                  const finalPrice = dpct > 0 ? Math.round(Number(f.price) * (100 - dpct) / 100) : Number(f.price)
                  return (
                    <tr key={f.id} className="hover:bg-ink-50">
                      <td className="px-4 py-3"><FoodImage src={f.imageUrl} name={f.name} size="md" /></td>
                      <td className="px-4 py-3">
                        <p className="font-medium text-accent-600">{f.name}</p>
                      </td>
                      <td className="px-4 py-3 text-right tabular font-medium text-green-600">{formatVND(finalPrice)}</td>
                      <td className="px-4 py-3">
                        {f.categoryName && (
                          <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs text-blue-700">{f.categoryName}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge tone={f.status === 'AVAILABLE' ? 'success' : 'danger'}>
                          {f.status === 'AVAILABLE' ? 'Có sẵn' : 'Hết hàng'}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {dpct > 0 ? <Badge tone="danger">{dpct}%</Badge> : <span className="text-ink-400">—</span>}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          <button onClick={() => setEditing(f)}
                            className="rounded-md bg-blue-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-blue-600">Sửa</button>
                          <button onClick={() => setDeleteId(f.id)}
                            className="rounded-md border border-red-300 px-4 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">Xóa</button>
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

      <FoodFormModal
        food={editing === 'new' ? null : editing}
        open={!!editing}
        onClose={() => setEditing(null)}
        categories={categories || []}
        onSaved={() => { qc.invalidateQueries({ queryKey: ['admin-foods'] }); setEditing(null) }}
      />

      <Modal open={!!deleteId} onClose={() => setDeleteId(null)} title="Xóa món ăn" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Hủy</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>Xác nhận xóa</Button>
          </>
        }>
        <p className="text-sm text-ink-700">Bạn có chắc muốn xóa món này? Hành động không thể hoàn tác.</p>
      </Modal>
    </div>
  )
}

function FoodFormModal({ food, open, onClose, categories, onSaved }) {
  const isEdit = !!food
  const fileRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [form, setForm] = useState(blank())

  function blank() {
    return {
      name: '', price: '', stock: 0, categoryId: '', status: 'AVAILABLE',
      discountPercent: '', prepTimeMinutes: '', ingredients: '', imageUrl: '',
      description: '', calories: '', proteinG: '', fatG: '', carbsG: '',
    }
  }

  useEffect(() => {
    if (open) {
      setForm(food ? {
        name: food.name || '', price: food.price || '', stock: food.stock ?? 0,
        categoryId: food.categoryId || '', status: food.status || 'AVAILABLE',
        discountPercent: food.discountPercent || '', prepTimeMinutes: food.prepTimeMinutes || '',
        ingredients: food.ingredients || '', imageUrl: food.imageUrl || '',
        description: food.description || '', calories: food.calories || '',
        proteinG: food.proteinG || '', fatG: food.fatG || '', carbsG: food.carbsG || '',
      } : blank())
    }
  }, [open, food])

  const mutate = useMutation({
    mutationFn: (data) => (isEdit ? foodsApi.update(food.id, data) : foodsApi.create(data)),
    onSuccess: () => { toast.success(isEdit ? 'Đã cập nhật món' : 'Thêm món ăn thành công'); onSaved() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const pickImage = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const res = await uploadApi.image(file)
      setForm((f) => ({ ...f, imageUrl: res.url }))
      toast.success('Đã tải ảnh lên')
    } catch (err) {
      toast.error(errMsg(err, 'Tải ảnh thất bại'))
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const submit = () => {
    mutate.mutate({
      name: form.name,
      description: form.description || null,
      price: Number(form.price),
      stock: Number(form.stock || 0),
      imageUrl: form.imageUrl || null,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      status: form.status,
      discountPercent: form.discountPercent ? Number(form.discountPercent) : 0,
      prepTimeMinutes: form.prepTimeMinutes ? Number(form.prepTimeMinutes) : null,
      ingredients: form.ingredients || null,
      calories: form.calories ? Number(form.calories) : null,
      proteinG: form.proteinG ? Number(form.proteinG) : null,
      fatG: form.fatG ? Number(form.fatG) : null,
      carbsG: form.carbsG ? Number(form.carbsG) : null,
    })
  }

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  return (
    <Modal open={open} onClose={onClose} title={isEdit ? 'Sửa món ăn' : 'Thêm món ăn'} size="lg">
      <div className="space-y-4">
        <div className="grid sm:grid-cols-2 gap-3">
          <Input label="Tên món" required value={form.name} onChange={set('name')} placeholder="Tên món" />
          <Input type="number" label="Giá (đ)" required value={form.price} onChange={set('price')} placeholder="Nhập giá" />
        </div>
        <div className="grid sm:grid-cols-2 gap-3">
          <Select label="Danh mục" value={form.categoryId} onChange={set('categoryId')}>
            <option value="">Chọn danh mục</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </Select>
          <Select label="Trạng thái" value={form.status} onChange={set('status')}>
            <option value="AVAILABLE">Có sẵn</option>
            <option value="OUT_OF_STOCK">Hết hàng</option>
          </Select>
        </div>
        <div className="grid sm:grid-cols-2 gap-3">
          <Input type="number" label="Giảm giá (%)" value={form.discountPercent} onChange={set('discountPercent')} placeholder="Nhập % giảm giá" />
          <Input type="number" label="Số lượng kho" value={form.stock} onChange={set('stock')} placeholder="0" />
        </div>
        <div className="grid sm:grid-cols-2 gap-3">
          <Input type="number" label="Thời gian chuẩn bị (phút)" value={form.prepTimeMinutes} onChange={set('prepTimeMinutes')} placeholder="Nhập thời gian" />
          <Input label="Nguyên liệu (cách nhau bằng dấu phẩy)" value={form.ingredients} onChange={set('ingredients')} placeholder="Ví dụ: thịt bò, rau cải, hành tây" />
        </div>

        {/* Upload ảnh */}
        <div>
          <p className="text-sm text-ink-700 mb-1.5">Hình ảnh</p>
          <div className="flex items-center gap-4">
            {form.imageUrl && <FoodImage src={form.imageUrl} name={form.name} size="lg" />}
            <button type="button" onClick={() => fileRef.current?.click()}
              className="rounded-xl border-2 border-dashed border-ink-300 px-6 py-5 text-center hover:border-ink-400 transition">
              {uploading ? <Loader className="h-5 w-5" /> : <Upload className="h-5 w-5 mx-auto text-ink-500" />}
              <span className="block text-xs text-ink-600 mt-1">Tải lên</span>
            </button>
            <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={pickImage} />
          </div>
        </div>

        <Textarea label="Mô tả" rows={2} value={form.description} onChange={set('description')} placeholder="Nhập mô tả món ăn" />

        <details className="rounded-lg border border-ink-200 p-3">
          <summary className="cursor-pointer text-sm font-medium text-ink-700">Thông tin dinh dưỡng (tùy chọn)</summary>
          <div className="grid grid-cols-4 gap-3 mt-3">
            <Input type="number" label="Calo" value={form.calories} onChange={set('calories')} />
            <Input type="number" step="0.1" label="Đạm (g)" value={form.proteinG} onChange={set('proteinG')} />
            <Input type="number" step="0.1" label="Béo (g)" value={form.fatG} onChange={set('fatG')} />
            <Input type="number" step="0.1" label="Carb (g)" value={form.carbsG} onChange={set('carbsG')} />
          </div>
        </details>

        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onClose}>Hủy</Button>
          <Button loading={mutate.isPending} disabled={!form.name || !form.price || !form.categoryId} onClick={submit}>
            {isEdit ? 'Cập nhật' : 'Thêm'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
