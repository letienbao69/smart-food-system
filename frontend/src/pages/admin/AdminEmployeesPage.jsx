import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, UserCog, Briefcase } from 'lucide-react'
import toast from 'react-hot-toast'
import { employeesApi, positionsApi } from '@/api/admin'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import { Input, Select, Textarea } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { initials, formatVND, formatDate, cn } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

/** Auto-generate employee code like NV-20260520-XXXX */
function genEmployeeCode() {
  const today = new Date()
  const ymd = today.toISOString().slice(0, 10).replace(/-/g, '')
  const rand = Math.random().toString(36).slice(2, 6).toUpperCase()
  return `NV-${ymd}-${rand}`
}

export default function AdminEmployeesPage() {
  const [tab, setTab] = useState('employees')

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
        <h1 className="font-display text-3xl font-bold text-ink-900">Nhân viên</h1>
      </div>

      <div className="flex gap-1 border-b border-ink-200">
        <TabButton active={tab === 'employees'} onClick={() => setTab('employees')}>
          <UserCog className="h-3.5 w-3.5" /> Nhân viên
        </TabButton>
        <TabButton active={tab === 'positions'} onClick={() => setTab('positions')}>
          <Briefcase className="h-3.5 w-3.5" /> Vị trí
        </TabButton>
      </div>

      {tab === 'employees' ? <EmployeesTab /> : <PositionsTab />}
    </div>
  )
}

function TabButton({ active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'inline-flex items-center gap-1.5 px-3 py-2 text-sm font-medium border-b-2 transition -mb-px',
        active
          ? 'border-ink-900 text-ink-900'
          : 'border-transparent text-ink-500 hover:text-ink-700'
      )}
    >
      {children}
    </button>
  )
}

function EmployeesTab() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: employees, isLoading } = useQuery({
    queryKey: ['admin-employees'],
    queryFn: employeesApi.list,
  })

  const { data: positions } = useQuery({
    queryKey: ['admin-positions'],
    queryFn: positionsApi.list,
  })

  const mutate = useMutation({
    mutationFn: (data) =>
      data.id ? employeesApi.update(data.id, data) : employeesApi.create(data),
    onSuccess: () => {
      toast.success('Đã lưu nhân viên')
      qc.invalidateQueries({ queryKey: ['admin-employees'] })
      setEditing(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => employeesApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa nhân viên')
      qc.invalidateQueries({ queryKey: ['admin-employees'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const { page, setPage, totalPages, paged } = usePagination(employees, 9)

  if (isLoading) return <Loader />

  return (
    <div>
      <div className="flex justify-end mb-3">
        <Button onClick={() => setEditing({ employeeCode: genEmployeeCode() })}>
          <Plus className="h-4 w-4" /> Thêm nhân viên
        </Button>
      </div>

      {!employees || employees.length === 0 ? (
        <Empty icon={UserCog} title="Chưa có nhân viên" />
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {paged.map((e) => (
            <div key={e.id} className="card p-4">
              <div className="flex items-start gap-3">
                <div className="grid h-10 w-10 place-items-center rounded-full bg-ink-900 text-xs font-semibold text-white shrink-0">
                  {initials(e.fullName)}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-ink-900 truncate">{e.fullName}</p>
                  <p className="text-xs text-ink-500">{e.positionName || '—'}</p>
                  <p className="text-[10px] font-mono text-ink-400">{e.employeeCode}</p>
                </div>
                <div className="flex gap-1 shrink-0">
                  <button
                    onClick={() => setEditing(e)}
                    className="grid h-7 w-7 place-items-center rounded-md hover:bg-ink-100 text-ink-600"
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </button>
                  <button
                    onClick={() => setDeleteId(e.id)}
                    className="grid h-7 w-7 place-items-center rounded-md hover:bg-danger-50 text-danger-600"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-ink-600">
                <p>📞 {e.phone || '—'}</p>
                <p>📅 {formatDate(e.hireDate)}</p>
              </div>
              {e.salary > 0 && (
                <p className="mt-1 text-xs text-ink-500">
                  Lương: <span className="tabular font-medium text-ink-900">{formatVND(e.salary)}</span>
                </p>
              )}
            </div>
          ))}
          <div className="col-span-full"><Pagination page={page} totalPages={totalPages} onChange={setPage} /></div>
        </div>
      )}

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title={editing?.id ? 'Sửa nhân viên' : 'Thêm nhân viên'}
        size="lg"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault()
            const raw = Object.fromEntries(new FormData(e.currentTarget))
            mutate.mutate({
              ...editing,
              ...raw,
              positionId: raw.positionId ? Number(raw.positionId) : null,
              salary: raw.salary ? Number(raw.salary) : 0,
            })
          }}
          className="space-y-3"
        >
          {/* Mã nhân viên — bắt buộc theo backend @NotBlank */}
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Mã nhân viên *"
              name="employeeCode"
              required
              defaultValue={editing?.employeeCode || ''}
              placeholder="VD: NV-20260520-AB12"
            />
            <Input
              label="Họ và tên *"
              name="fullName"
              required
              defaultValue={editing?.fullName || ''}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Số điện thoại"
              name="phone"
              defaultValue={editing?.phone || ''}
            />
            <Input
              label="Email"
              name="email"
              type="email"
              defaultValue={editing?.email || ''}
            />
          </div>

          <Input
            label="Địa chỉ"
            name="address"
            defaultValue={editing?.address || ''}
          />

          <div className="grid grid-cols-3 gap-3">
            <Select
              label="Giới tính"
              name="gender"
              defaultValue={editing?.gender || ''}
            >
              <option value="">—</option>
              <option value="MALE">Nam</option>
              <option value="FEMALE">Nữ</option>
              <option value="OTHER">Khác</option>
            </Select>
            <Input
              type="date"
              label="Ngày sinh"
              name="dateOfBirth"
              defaultValue={editing?.dateOfBirth?.slice(0, 10) || ''}
            />
            <Select
              label="Ca làm việc"
              name="shiftName"
              defaultValue={editing?.shiftName || ''}
            >
              <option value="">—</option>
              <option value="MORNING">Ca sáng</option>
              <option value="AFTERNOON">Ca chiều</option>
              <option value="EVENING">Ca tối</option>
              <option value="FULL_DAY">Cả ngày</option>
            </Select>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <Select
              label="Vị trí *"
              name="positionId"
              required
              defaultValue={editing?.positionId || ''}
            >
              <option value="">— Chọn vị trí —</option>
              {(positions || []).map((p) => (
                <option key={p.id} value={p.id}>{p.positionName}</option>
              ))}
            </Select>
            <Input
              type="date"
              label="Ngày vào làm *"
              name="hireDate"
              required
              defaultValue={editing?.hireDate?.slice(0, 10) || ''}
            />
            <Input
              type="number"
              label="Lương (VND)"
              name="salary"
              defaultValue={editing?.salary || ''}
            />
          </div>

          <Select
            label="Trạng thái"
            name="status"
            defaultValue={editing?.status || 'WORKING'}
          >
            <option value="WORKING">Đang làm việc</option>
            <option value="ON_LEAVE">Nghỉ phép</option>
            <option value="RESIGNED">Đã nghỉ việc</option>
          </Select>

          <Textarea
            label="Ghi chú"
            name="note"
            rows={2}
            defaultValue={editing?.note || ''}
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" type="button" onClick={() => setEditing(null)}>Huỷ</Button>
            <Button type="submit" loading={mutate.isPending}>Lưu</Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Xóa nhân viên"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Huỷ</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>
              Xác nhận xóa
            </Button>
          </>
        }
      >
        <p className="text-sm text-ink-700">Xóa nhân viên này khỏi hệ thống?</p>
      </Modal>
    </div>
  )
}

function PositionsTab() {
  const qc = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)

  const { data: positions, isLoading } = useQuery({
    queryKey: ['admin-positions'],
    queryFn: positionsApi.list,
  })

  const mutate = useMutation({
    mutationFn: (data) =>
      data.id ? positionsApi.update(data.id, data) : positionsApi.create(data),
    onSuccess: () => {
      toast.success('Đã lưu vị trí')
      qc.invalidateQueries({ queryKey: ['admin-positions'] })
      setEditing(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => positionsApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa vị trí')
      qc.invalidateQueries({ queryKey: ['admin-positions'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (isLoading) return <Loader />

  return (
    <div>
      <div className="flex justify-end mb-3">
        <Button onClick={() => setEditing({})}>
          <Plus className="h-4 w-4" /> Thêm vị trí
        </Button>
      </div>

      {!positions || positions.length === 0 ? (
        <Empty icon={Briefcase} title="Chưa có vị trí" />
      ) : (
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {positions.map((p) => (
            <div key={p.id} className="card p-4 flex justify-between items-start gap-2">
              <div className="min-w-0 flex-1">
                <p className="font-display font-semibold text-ink-900">{p.positionName}</p>
                {p.description && (
                  <p className="mt-1 text-sm text-ink-600 line-clamp-2">{p.description}</p>
                )}
                {p.baseSalary > 0 && (
                  <p className="mt-2 text-xs text-ink-500">
                    Lương cơ bản: <span className="tabular font-medium text-ink-900">{formatVND(p.baseSalary)}</span>
                  </p>
                )}
              </div>
              <div className="flex gap-1">
                <button
                  onClick={() => setEditing(p)}
                  className="grid h-7 w-7 place-items-center rounded-md hover:bg-ink-100 text-ink-600"
                >
                  <Pencil className="h-3.5 w-3.5" />
                </button>
                <button
                  onClick={() => setDeleteId(p.id)}
                  className="grid h-7 w-7 place-items-center rounded-md hover:bg-danger-50 text-danger-600"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        open={!!editing}
        onClose={() => setEditing(null)}
        title={editing?.id ? 'Sửa vị trí' : 'Thêm vị trí'}
        size="sm"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault()
            const data = Object.fromEntries(new FormData(e.currentTarget))
            mutate.mutate({
              ...editing,
              ...data,
              baseSalary: data.baseSalary ? Number(data.baseSalary) : 0,
            })
          }}
          className="space-y-3"
        >
          <Input label="Tên vị trí *" name="positionName" required defaultValue={editing?.positionName || ''} />
          <Textarea label="Mô tả" name="description" rows={3} defaultValue={editing?.description || ''} />
          <Input type="number" label="Lương cơ bản (VND)" name="baseSalary" defaultValue={editing?.baseSalary || ''} />
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="secondary" type="button" onClick={() => setEditing(null)}>Huỷ</Button>
            <Button type="submit" loading={mutate.isPending}>Lưu</Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Xóa vị trí"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Huỷ</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>
              Xác nhận
            </Button>
          </>
        }
      >
        <p className="text-sm text-ink-700">Xóa vị trí này? Chỉ xóa được khi không có nhân viên thuộc vị trí.</p>
      </Modal>
    </div>
  )
}
