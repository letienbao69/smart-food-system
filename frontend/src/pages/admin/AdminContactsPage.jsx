import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { MessageSquare, Check, Trash2, Mail, Phone } from 'lucide-react'
import toast from 'react-hot-toast'
import { contactsApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import Pagination, { usePagination } from '@/components/ui/Pagination'
import { formatDateTime } from '@/lib/utils'

export default function AdminContactsPage() {
  const qc = useQueryClient()
  const [viewing, setViewing] = useState(null)
  const [deleteId, setDeleteId] = useState(null)
  const [reply, setReply] = useState('')

  const { data: list, isLoading } = useQuery({
    queryKey: ['admin-contacts'],
    queryFn: contactsApi.list,
    refetchInterval: 30000,
  })

  const setStatus = useMutation({
    mutationFn: ({ id, status, reply }) => contactsApi.updateStatus(id, status, reply),
    onSuccess: () => {
      toast.success('Đã cập nhật')
      qc.invalidateQueries({ queryKey: ['admin-contacts'] })
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const del = useMutation({
    mutationFn: (id) => contactsApi.delete(id),
    onSuccess: () => {
      toast.success('Đã xóa phản ánh')
      qc.invalidateQueries({ queryKey: ['admin-contacts'] })
      setDeleteId(null)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const { page, setPage, totalPages, paged } = usePagination(list, 10)

  if (isLoading) return <Loader />

  return (
    <div className="space-y-5">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
          <h1 className="font-display text-3xl font-bold text-ink-900">Liên hệ & Phản ánh</h1>
        </div>
        <span className="text-sm text-ink-500">
          Chưa xử lý: <b className="text-ink-900">{(list || []).filter((c) => c.status === 'NEW').length}</b>
        </span>
      </div>

      {!list || list.length === 0 ? (
        <Empty icon={MessageSquare} title="Chưa có phản ánh nào" />
      ) : (
        <div className="card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-ink-50 text-xs uppercase tracking-wider text-ink-500">
              <tr>
                <th className="px-5 py-3 text-left font-medium">Người gửi</th>
                <th className="px-5 py-3 text-left font-medium">Nội dung</th>
                <th className="px-5 py-3 text-center font-medium">Trạng thái</th>
                <th className="px-5 py-3 text-left font-medium">Thời gian</th>
                <th className="px-5 py-3 text-right font-medium">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ink-100">
              {paged.map((c) => (
                <tr key={c.id} className="hover:bg-ink-50 cursor-pointer" onClick={() => setViewing(c)}>
                  <td className="px-5 py-3">
                    <p className="font-medium text-ink-900">{c.name}</p>
                    <p className="text-xs text-ink-500">{c.phone || c.email || ''}</p>
                  </td>
                  <td className="px-5 py-3 max-w-[320px]">
                    {c.subject && <p className="font-medium text-ink-800 truncate">{c.subject}</p>}
                    <p className="text-ink-600 truncate">{c.message}</p>
                  </td>
                  <td className="px-5 py-3 text-center">
                    <span className={`chip border text-xs ${c.status === 'NEW' ? 'bg-amber-50 text-amber-700 border-amber-200' : 'bg-green-50 text-green-700 border-green-200'}`}>
                      {c.status === 'NEW' ? 'Chưa xử lý' : 'Đã xử lý'}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-xs text-ink-500">{formatDateTime(c.createdAt)}</td>
                  <td className="px-5 py-3" onClick={(e) => e.stopPropagation()}>
                    <div className="flex justify-end gap-2">
                      {c.status === 'NEW' && (
                        <button onClick={() => setStatus.mutate({ id: c.id, status: 'RESOLVED' })}
                          className="grid h-8 w-8 place-items-center rounded-md bg-green-50 text-green-600 hover:bg-green-100" title="Đánh dấu đã xử lý">
                          <Check className="h-4 w-4" />
                        </button>
                      )}
                      <button onClick={() => setDeleteId(c.id)}
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

      {/* Chi tiết */}
      <Modal open={!!viewing} onClose={() => setViewing(null)} title="Chi tiết phản ánh" size="md">
        {viewing && (
          <div className="space-y-3 text-sm">
            <p className="font-medium text-ink-900 text-base">{viewing.subject || '(Không có tiêu đề)'}</p>
            <div className="flex flex-wrap gap-4 text-ink-600">
              <span>{viewing.name}</span>
              {viewing.phone && <span className="inline-flex items-center gap-1"><Phone className="h-3.5 w-3.5" />{viewing.phone}</span>}
              {viewing.email && <span className="inline-flex items-center gap-1"><Mail className="h-3.5 w-3.5" />{viewing.email}</span>}
            </div>
            <div className="rounded-lg bg-ink-50 border border-ink-200 p-3 text-ink-800 whitespace-pre-line">
              {viewing.message}
            </div>
            <p className="text-xs text-ink-400">{formatDateTime(viewing.createdAt)}</p>
            {viewing.status === 'NEW' && (
              <div className="space-y-2 pt-1">
                {viewing.userId && (
                  <div>
                    <label className="text-xs font-medium text-ink-600">Lời phản hồi gửi tới khách (tùy chọn)</label>
                    <textarea
                      value={reply}
                      onChange={(e) => setReply(e.target.value)}
                      rows={3}
                      placeholder="VD: Cảm ơn bạn đã phản ánh. Chúng tôi đã xử lý và cải thiện vấn đề bạn nêu."
                      className="input w-full mt-1 resize-none"
                    />
                    <p className="mt-1 text-[11px] text-ink-400">Khách đăng nhập sẽ nhận được thông báo này trên trang của họ.</p>
                  </div>
                )}
                <Button onClick={() => { setStatus.mutate({ id: viewing.id, status: 'RESOLVED', reply: reply || undefined }); setReply(''); setViewing(null) }}>
                  <Check className="h-4 w-4" /> Đánh dấu đã xử lý {viewing.userId ? '& gửi phản hồi' : ''}
                </Button>
              </div>
            )}
          </div>
        )}
      </Modal>

      <Modal open={!!deleteId} onClose={() => setDeleteId(null)} title="Xóa phản ánh" size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setDeleteId(null)}>Hủy</Button>
            <Button variant="danger" loading={del.isPending} onClick={() => del.mutate(deleteId)}>Xác nhận xóa</Button>
          </>
        }>
        <p className="text-sm text-ink-700">Bạn có chắc muốn xóa phản ánh này?</p>
      </Modal>
    </div>
  )
}
