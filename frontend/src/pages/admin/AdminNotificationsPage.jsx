import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  Bell, Check, CheckCheck, Trash2, ShoppingCart,
  CreditCard, Package, CalendarClock, CheckCircle2, XCircle, Clock,
  ExternalLink, X,
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
  if (type === 'ORDER_CREATED')          return { icon: ShoppingCart,  color: 'text-blue-500',    bg: 'bg-blue-50',    label: 'Đơn mới' }
  if (type === 'RESERVATION_CREATED')    return { icon: CalendarClock,  color: 'text-blue-500',    bg: 'bg-blue-50',    label: 'Đặt bàn mới' }
  if (type === 'RESERVATION_STATUS_UPDATED' || type === 'DEPOSIT_CLAIMED')
                                         return { icon: CalendarClock,  color: 'text-violet-600',  bg: 'bg-violet-50',  label: 'Đặt bàn' }
  if (type === 'ORDER_STATUS_UPDATED') {
    if (msg.includes('Hoàn thành'))      return { icon: CheckCircle2,  color: 'text-emerald-600', bg: 'bg-emerald-50', label: 'Hoàn thành' }
    if (msg.includes('Đã phục vụ'))      return { icon: Package,        color: 'text-cyan-600',    bg: 'bg-cyan-50',    label: 'Đã phục vụ' }
    if (msg.includes('hủy'))             return { icon: XCircle,       color: 'text-red-500',     bg: 'bg-red-50',     label: 'Đã hủy' }
    return                                { icon: Clock,               color: 'text-amber-600',   bg: 'bg-amber-50',   label: 'Trạng thái' }
  }
  if (type === 'PAYMENT_UPDATED' || type === 'PAYMENT_CONFIRMED' || type === 'PAYMENT_CLAIMED')
                                         return { icon: CreditCard,    color: 'text-violet-600',  bg: 'bg-violet-50',  label: 'Thanh toán' }
  return                                  { icon: Bell,                color: 'text-ink-500',     bg: 'bg-ink-100',    label: 'Thông báo' }
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

export default function AdminNotificationsPage() {
  const qc  = useQueryClient()
  const nav = useNavigate()
  const [confirmDeleteAll, setConfirmDeleteAll] = useState(false)
  const [filter, setFilter] = useState('ALL') // ALL | UNREAD

  const { data: items, isLoading } = useQuery({
    queryKey: ['admin-notifications'],
    queryFn: notificationsApi.adminList,
  })

  const invalidateAll = () => {
    qc.invalidateQueries({ queryKey: ['admin-notifications'] })
    qc.invalidateQueries({ queryKey: ['admin-notifications-poll'] })
  }

  const markRead = useMutation({
    mutationFn: (id) => notificationsApi.adminMarkRead(id),
    onSuccess: invalidateAll,
    onError: (e) => toast.error(errMsg(e)),
  })

  const markAllRead = useMutation({
    mutationFn: notificationsApi.adminMarkAllRead,
    onSuccess: () => { toast.success('Đã đánh dấu tất cả đã đọc'); invalidateAll() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const deleteOne = useMutation({
    mutationFn: (id) => notificationsApi.adminDeleteOne(id),
    onSuccess: () => { toast.success('Đã xóa thông báo'); invalidateAll() },
    onError: (e) => toast.error(errMsg(e)),
  })

  const deleteAll = useMutation({
    mutationFn: notificationsApi.adminDeleteAll,
    onSuccess: () => {
      toast.success('Đã xóa tất cả thông báo')
      invalidateAll()
      setConfirmDeleteAll(false)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (isLoading) return <Loader />

  const allItems    = items || []
  const unreadCount = allItems.filter((n) => !n.readStatus).length
  const displayed   = filter === 'UNREAD' ? allItems.filter((n) => !n.readStatus) : allItems

  const handleClick = (n) => {
    if (!n.readStatus) markRead.mutate(n.id)
    // Navigate đến trang đơn hàng với orderId cụ thể → auto mở modal
    if (n.referenceType === 'ORDER' && n.referenceId) {
      nav(`/admin/orders?viewOrder=${n.referenceId}`)
    }
  }

  return (
    <div className="space-y-5">

      {/* Header */}
      <div className="flex justify-between items-start flex-wrap gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Quản lý</p>
          <h1 className="font-display text-3xl font-bold text-ink-900">Thông báo</h1>
          <p className="mt-1 text-sm text-ink-500">
            {allItems.length} thông báo{unreadCount > 0 ? ` · ${unreadCount} chưa đọc` : ''}
          </p>
        </div>
        <div className="flex gap-2 flex-wrap">
          {unreadCount > 0 && (
            <Button variant="secondary" size="sm" onClick={() => markAllRead.mutate()} loading={markAllRead.isPending}>
              <CheckCheck className="h-3.5 w-3.5" /> Đọc tất cả
            </Button>
          )}
          {allItems.length > 0 && (
            <Button variant="secondary" size="sm" onClick={() => setConfirmDeleteAll(true)}
              className="text-danger-600 hover:bg-danger-50 hover:border-red-200">
              <Trash2 className="h-3.5 w-3.5" /> Xóa tất cả
            </Button>
          )}
        </div>
      </div>

      {/* Filter tabs */}
      <div className="flex gap-1 border-b border-ink-200">
        {[
          { key: 'ALL',    label: `Tất cả (${allItems.length})` },
          { key: 'UNREAD', label: `Chưa đọc (${unreadCount})` },
        ].map((t) => (
          <button
            key={t.key}
            onClick={() => setFilter(t.key)}
            className={cn(
              'px-4 py-2 text-sm font-medium border-b-2 -mb-px transition',
              filter === t.key
                ? 'border-ink-900 text-ink-900'
                : 'border-transparent text-ink-500 hover:text-ink-700'
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* List */}
      {displayed.length === 0 ? (
        <Empty
          icon={Bell}
          title={filter === 'UNREAD' ? 'Không có thông báo chưa đọc' : 'Chưa có thông báo'}
        />
      ) : (
        <div className="space-y-2">
          {displayed.map((n) => {
            const { icon: Icon, color, bg, label } = notifMeta(n)
            const isOrder = n.referenceType === 'ORDER' && n.referenceId

            return (
              <div
                key={n.id}
                className={cn(
                  'group card p-4 flex items-start gap-3 transition-all',
                  !n.readStatus && 'border-accent-200 bg-accent-50/30',
                  isOrder && 'cursor-pointer hover:shadow-card'
                )}
                onClick={() => isOrder && handleClick(n)}
              >
                {/* Icon */}
                <div className={cn('grid h-10 w-10 shrink-0 place-items-center rounded-xl', bg)}>
                  <Icon className={cn('h-4.5 w-4.5', color)} />
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className={cn(
                      'text-sm leading-snug',
                      n.readStatus ? 'text-ink-700' : 'text-ink-900 font-semibold'
                    )}>
                      {n.title || 'Thông báo'}
                    </p>
                    <span className={cn('chip border text-[10px]', bg, color)}>
                      {label}
                    </span>
                    {!n.readStatus && (
                      <span className="h-2 w-2 rounded-full bg-accent-500 shrink-0" />
                    )}
                  </div>
                  <p className="mt-0.5 text-sm text-ink-600 line-clamp-2">{n.message}</p>
                  <div className="mt-1.5 flex items-center gap-3">
                    <span className="text-[10px] text-ink-400">{timeAgo(n.createdAt)}</span>
                    {isOrder && (
                      <span className="text-[10px] text-accent-600 font-medium inline-flex items-center gap-0.5">
                        <ExternalLink className="h-2.5 w-2.5" /> Xem đơn hàng
                      </span>
                    )}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1 shrink-0 opacity-0 group-hover:opacity-100 transition-opacity"
                  onClick={(e) => e.stopPropagation()}>
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
                    className="grid h-8 w-8 place-items-center rounded-lg text-ink-400 hover:bg-danger-50 hover:text-danger-600 transition"
                    title="Xóa thông báo"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
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
          Xóa toàn bộ {allItems.length} thông báo? Hành động không thể hoàn tác.
        </p>
      </Modal>
    </div>
  )
}
