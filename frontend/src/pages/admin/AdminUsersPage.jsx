import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Search, Users as UsersIcon, Shield, ShieldOff } from 'lucide-react'
import toast from 'react-hot-toast'
import { adminUsersApi, rolesApi } from '@/api/admin'
import { errMsg } from '@/api/client'
import { Loader, Badge, Empty } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { initials, formatDate } from '@/lib/utils'
import Pagination, { usePagination } from '@/components/ui/Pagination'

export default function AdminUsersPage() {
  const qc = useQueryClient()
  const [search, setSearch] = useState('')
  const [editingRoles, setEditingRoles] = useState(null)

  const { data: users, isLoading } = useQuery({
    queryKey: ['admin-users'],
    queryFn: adminUsersApi.list,
  })

  const { data: roles } = useQuery({
    queryKey: ['admin-roles'],
    queryFn: rolesApi.list,
  })

  const toggleStatus = useMutation({
    mutationFn: ({ id, status }) => adminUsersApi.updateStatus(id, status),
    onSuccess: () => {
      toast.success('Đã cập nhật trạng thái')
      qc.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const updateRoles = useMutation({
    mutationFn: ({ id, roles }) => adminUsersApi.updateRoles(id, roles),
    onSuccess: () => {
      toast.success('Đã cập nhật quyền')
      qc.invalidateQueries({ queryKey: ['admin-users'] })
      setEditingRoles(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const filteredAll = (users || []).filter(
    (u) =>
      u.fullName?.toLowerCase().includes(search.toLowerCase()) ||
      u.email?.toLowerCase().includes(search.toLowerCase())
  )

  const { page, setPage, totalPages, paged: filtered } = usePagination(filteredAll, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
        <h1 className="font-display text-3xl font-bold text-ink-900">Người dùng</h1>
      </div>

      <div className="card p-3">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Tìm theo tên hoặc email..."
            className="input pl-10"
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <Empty icon={UsersIcon} title="Không có người dùng" />
      ) : (
        <div className="card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
                <tr>
                  <th className="px-4 py-2.5 text-left font-medium">Người dùng</th>
                  <th className="px-4 py-2.5 text-left font-medium">Quyền</th>
                  <th className="px-4 py-2.5 text-left font-medium">Ngày tham gia</th>
                  <th className="px-4 py-2.5 text-center font-medium">Trạng thái</th>
                  <th className="px-4 py-2.5 w-48"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {filtered.map((u) => {
                  const userRoles = (u.roles || []).map((r) =>
                    typeof r === 'string' ? r : r.name
                  )
                  return (
                    <tr key={u.id} className="hover:bg-ink-50">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="grid h-9 w-9 place-items-center rounded-full bg-ink-900 text-xs font-semibold text-white">
                            {initials(u.fullName || u.email)}
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-ink-900 truncate">
                              {u.fullName || '—'}
                            </p>
                            <p className="text-xs text-ink-500 truncate">{u.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1">
                          {userRoles.map((r) => (
                            <Badge key={r} tone={r === 'ADMIN' ? 'danger' : 'ink'}>
                              {r}
                            </Badge>
                          ))}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-xs text-ink-500">
                        {formatDate(u.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge tone={u.status === 'ACTIVE' ? 'success' : 'danger'}>
                          {u.status || 'ACTIVE'}
                        </Badge>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-1">
                          <button
                            onClick={() => setEditingRoles({ user: u, roles: userRoles })}
                            className="text-xs text-ink-700 hover:text-ink-900 px-2 py-1 rounded hover:bg-ink-100"
                          >
                            Quyền
                          </button>
                          <button
                            onClick={() =>
                              toggleStatus.mutate({
                                id: u.id,
                                status: u.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE',
                              })
                            }
                            className="text-xs text-ink-700 hover:text-ink-900 px-2 py-1 rounded hover:bg-ink-100 inline-flex items-center gap-1"
                          >
                            {u.status === 'ACTIVE' ? (
                              <>
                                <ShieldOff className="h-3 w-3" />
                                Khoá
                              </>
                            ) : (
                              <>
                                <Shield className="h-3 w-3" />
                                Mở khoá
                              </>
                            )}
                          </button>
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

      <Modal
        open={!!editingRoles}
        onClose={() => setEditingRoles(null)}
        title="Phân quyền"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setEditingRoles(null)}>Huỷ</Button>
            <Button
              loading={updateRoles.isPending}
              onClick={() =>
                updateRoles.mutate({
                  id: editingRoles.user.id,
                  roles: editingRoles.roles,
                })
              }
            >
              Lưu
            </Button>
          </>
        }
      >
        {editingRoles && (
          <div className="space-y-2">
            <p className="text-sm text-ink-700">
              Người dùng: <span className="font-medium">{editingRoles.user.fullName || editingRoles.user.email}</span>
            </p>
            <div className="space-y-1.5 pt-2">
              {(roles || []).map((r) => {
                const name = r.name || r
                return (
                  <label
                    key={name}
                    className="flex items-center gap-2.5 rounded-lg border border-ink-200 p-2.5 cursor-pointer hover:bg-ink-50"
                  >
                    <input
                      type="checkbox"
                      checked={editingRoles.roles.includes(name)}
                      onChange={(e) => {
                        setEditingRoles((s) => ({
                          ...s,
                          roles: e.target.checked
                            ? [...s.roles, name]
                            : s.roles.filter((x) => x !== name),
                        }))
                      }}
                    />
                    <div>
                      <p className="text-sm font-medium text-ink-900">{name}</p>
                      {r.description && (
                        <p className="text-xs text-ink-500">{r.description}</p>
                      )}
                    </div>
                  </label>
                )
              })}
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
