import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  Bell, Check, CheckCheck, Trash2, ShoppingCart,
  Package, CalendarClock, CreditCard, CheckCircle2, XCircle, Clock, X,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { notificationsApi } from '@/api/admin'
import { errMsg } from '@/api/client'
import { Loader, Empty } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import Modal from '@/components/ui/Modal'
import { cn } from '@/lib/utils'

function notifMeta(n) {
  const type = n.type || ''
  const msg  = n.message || ''
  if (type === 'ORDER_CREATED')          return { icon: ShoppingCart,  color: 'text-blue-500',    bg: 'bg-blue-50' }
  if (type === 'RESERVATION_CREATED')    return { icon: CalendarClock,  color: 'text-blue-500',    bg: 'bg-blue-50' }
  if (type === 'RESERVATION_STATUS_UPDATED' || type === 'DEPOSIT_CLAIMED')
                                         return { icon: CalendarClock,  color: 'text-violet-600',  bg: 'bg-violet-50' }
  if (type === 'ORDER_STATUS_UPDATED') {
    if (msg.includes('Hoàn thành'))      return { icon: CheckCircle2,  color: 'text-emerald-600', bg: 'bg-emerald-50' }
    if (msg.includes('Đã phục vụ'))      return { icon: Package,        color: 'text-cyan-600',    bg: 'bg-cyan-50' }
    if (msg.includes('hủy'))             return { icon: XCircle,       color: 'text-red-500',     bg: 'bg-red-50' }
    return                                { icon: Clock,               color: 'text-amber-600',   bg: 'bg-amber-50' }
  }
  if (type === 'PAYMENT_UPDATED' || type === 'PAYMENT_CLAIMED')
                                         return { icon: CreditCard,    color: 'text-violet-600',  bg: 'bg-violet-50' }
  return                                  { icon: Bell,                color: 'text-ink-500',   bg: 'bg-ink-100' }
}

function timeAgo(d) {
  if (!d) return ''
  const diff = Date.now() - new Date(d).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)  return 'Vừa xong'
  if (m < 60) return `${m} phút trước`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} giờ trước`
  return `${Math.floor(h / 24)} ngày trước`
}

export default function NotificationsPage() {
  const qc  = useQueryClient()
  const nav = useNavigate()
  const [confirmDeleteAll, setConfirmDeleteAll] = useState(false)

  const { data: items, isLoading } = useQuery({
    queryKey: ['my-notifications'],
    queryFn: notificationsApi.mine,
  })

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['my-notifications'] })
    qc.invalidateQueries({ queryKey: ['my-notifications-count'] })
    qc.invalidateQueries({ queryKey: ['my-notifications-recent'] })
  }

  const markRead = useMutation({
    mutationFn: (id) => notificationsApi.markRead(id),
    onSuccess: invalidateAll,
    onError: (e) => toast.error(errMsg(e)),
  })

  const markAllRead = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => { toast.success('Đã đánh dấu tất cả đã đọc'); invalidateAll() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const deleteOne = useMutation({
    mutationFn: (id) => notificationsApi.deleteOne(id),
    onSuccess: () => { toast.success('Đã xóa thông báo'); invalidateAll() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const deleteAll = useMutation({
    mutationFn: notificationsApi.deleteAll,
    onSuccess: () => {
      toast.success('Đã xóa tất cả thông báo')
      invalidateAll()
      setConfirmDeleteAll(false)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (isLoading) return <Loader className="min-h-[40vh]" />

  const unreadCount = (items || []).filter((n) => !n.readStatus).length

  const handleClick = (n) => {
    if (!n.readStatus) markRead.mutate(n.id)
    if (n.referenceType === 'ORDER' && n.referenceId) nav(`/orders/${n.referenceId}`)
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 sm:px-6 lg:px-8">

      {/* Header */}
      <div className="flex justify-between items-end flex-wrap gap-3 mb-6">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-400 font-medium">Tài khoản</p>
          <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">Thông báo</h1>
          {unreadCount > 0 && (
            <p className="mt-1 text-sm text-ink-500">{unreadCount} chưa đọc</p>
          )}
        </div>
        <div className="flex gap-2">
          {unreadCount > 0 && (
            <Button variant="secondary" size="sm" onClick={() => markAllRead.mutate()} loading={markAllRead.isPending}>
              <CheckCheck className="h-3.5 w-3.5" /> Đọc tất cả
            </Button>
          )}
          {items?.length > 0 && (
            <Button variant="secondary" size="sm" onClick={() => setConfirmDeleteAll(true)}
              className="text-red-600 hover:bg-red-50 hover:border-red-200">
              <Trash2 className="h-3.5 w-3.5" /> Xóa tất cả
            </Button>
          )}
        </div>
      </div>

      {/* List */}
      {!items || items.length === 0 ? (
        <Empty icon={Bell} title="Chưa có thông báo" description="Thông báo về đơn hàng và thanh toán sẽ hiện ở đây." />
      ) : (
        <div className="space-y-2">
          {items.map((n) => {
            const { icon: Icon, color, bg } = notifMeta(n)
            return (
              <div
                key={n.id}
                className={cn(
                  'group relative rounded-2xl border p-4 transition-all hover:shadow-md',
                  n.readStatus
                    ? 'border-ink-100 bg-white'
                    : 'border-blue-100 bg-blue-50/40'
                )}
              >
                <div className="flex items-start gap-3">
                  {/* Icon */}
                  <button onClick={() => handleClick(n)}
                    className={cn('grid h-10 w-10 shrink-0 place-items-center rounded-xl', bg)}>
                    <Icon className={cn('h-4.5 w-4.5', color)} />
                  </button>

                  {/* Content — clickable */}
                  <button onClick={() => handleClick(n)}
                    className="flex-1 min-w-0 text-left">
                    <div className="flex items-center gap-2">
                      <p className={cn(
                        'text-sm leading-snug',
                        n.readStatus ? 'text-ink-600' : 'text-ink-900 font-semibold'
                      )}>
                        {n.title}
                      </p>
                      {!n.readStatus && (
                        <span className="h-2 w-2 rounded-full bg-blue-500 shrink-0" />
                      )}
                    </div>
                    <p className="mt-0.5 text-xs text-ink-500 line-clamp-2 leading-relaxed">
                      {n.message}
                    </p>
                    <p className="mt-1.5 text-[10px] text-ink-400">{timeAgo(n.createdAt)}</p>
                  </button>

                  {/* Actions */}
                  <div className="flex items-center gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
                    {!n.readStatus && (
                      <button
                        onClick={() => markRead.mutate(n.id)}
                        className="grid h-8 w-8 place-items-center rounded-lg text-ink-400 hover:bg-ink-100 hover:text-ink-700 transition"
                        title="Đánh dấu đã đọc"
                      >
                        <Check className="h-3.5 w-3.5" />
                      </button>
                    )}
                    <button
                      onClick={() => deleteOne.mutate(n.id)}
                      className="grid h-8 w-8 place-items-center rounded-lg text-ink-400 hover:bg-red-50 hover:text-red-600 transition"
                      title="Xóa"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Delete all confirmation */}
      <Modal
        open={confirmDeleteAll}
        onClose={() => setConfirmDeleteAll(false)}
        title="Xóa tất cả thông báo"
        size="sm"
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirmDeleteAll(false)}>Huỷ</Button>
            <Button variant="danger" loading={deleteAll.isPending} onClick={() => deleteAll.mutate()}>
              <Trash2 className="h-4 w-4" /> Xác nhận xóa
            </Button>
          </>
        }
      >
        <p className="text-sm text-ink-600">
          Xóa toàn bộ thông báo? Hành động này không thể hoàn tác.
        </p>
      </Modal>
    </div>
  )
}
