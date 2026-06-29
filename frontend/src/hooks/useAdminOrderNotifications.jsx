import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useWebSocketClient } from '@/providers/WebSocketProvider'

const TOAST_STYLE = {
  background: '#1c1917',
  color: '#fafafa',
  fontSize: '13px',
  lineHeight: '1.45',
  borderRadius: '14px',
  padding: '14px 16px',
  maxWidth: '380px',
  whiteSpace: 'pre-line',
  boxShadow: '0 10px 30px rgba(0,0,0,0.25)',
}

/**
 * Lắng nghe /topic/admin/orders qua WebSocket và hiện TOAST nổi góc phải
 * cho: đặt bàn mới, đơn hàng mới, và xác nhận thanh toán/cọc.
 * Tự biến mất sau vài giây, không chặn màn hình.
 */
export function useAdminOrderNotifications() {
  const client = useWebSocketClient()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const subRef = useRef(null)
  if (!window.__adminSeenNotifIds) window.__adminSeenNotifIds = new Set()

  useEffect(() => {
    if (!client) return

    subRef.current = client.subscribe('/topic/admin/orders', (message) => {
      try {
        const data = JSON.parse(message.body)
        const type = data.type || ''

        // Chọn icon + tiêu đề + nội dung theo loại thông báo
        let icon = '🔔'
        let title = data.title || 'Thông báo mới'
        let body = data.message || ''

        // Hàm refresh mọi dữ liệu admin liên quan (gọi ở MỌI event để tự cập nhật realtime)
        const refreshAll = () => {
          qc.invalidateQueries({ queryKey: ['admin-orders'] })
          qc.invalidateQueries({ queryKey: ['admin-order'] })
          qc.invalidateQueries({ queryKey: ['admin-reservations'] })
          qc.invalidateQueries({ queryKey: ['admin-tables'] })
          qc.invalidateQueries({ queryKey: ['admin-notifications'] })
          qc.invalidateQueries({ queryKey: ['admin-notifications-poll'] })
          qc.invalidateQueries({ queryKey: ['report-summary'] })
          qc.invalidateQueries({ queryKey: ['report-revenue'] })
          qc.invalidateQueries({ queryKey: ['report-best'] })
        }

        // Đường dẫn điều hướng khi bấm vào toast
        let goTo = null
        if (type === 'NEW_RESERVATION') {
          // Đặt bàn mới đã có popup riêng (NewReservationPopup) — chỉ refresh, không toast trùng
          refreshAll()
          return
        } else if (type === 'NEW_ORDER' || type === 'ORDER_CREATED') {
          icon = '🔔'
          if (!data.title) title = 'Có đơn hàng mới'
          goTo = '/admin/orders'
        } else if (type === 'PAYMENT_CONFIRMED' || type === 'PAYMENT_CLAIMED' || type === 'DEPOSIT_CLAIMED' || type === 'DEPOSIT_PAID') {
          icon = type === 'DEPOSIT_PAID' ? '💰' : '🔔'
          if (!data.title) title = type === 'DEPOSIT_PAID' ? 'Đã nhận tiền cọc' : 'Xác nhận thanh toán'
          goTo = '/admin/reservations'
        } else if (type === 'RESERVATION_CANCELLED') {
          // Khách tự hủy hoặc hệ thống tự hủy do quá hạn thanh toán cọc
          icon = '❌'
          if (!data.title) title = 'Đặt bàn đã bị hủy'
          goTo = '/admin/reservations'
        } else {
          // Khách hủy đặt bàn -> báo cho admin/staff biết (toast); các đổi trạng thái khác chỉ refresh.
          if (type === 'RESERVATION_STATUS_CHANGED' && data.newStatus === 'CANCELLED') {
            icon = '❌'
            if (!data.title) title = 'Khách đã hủy đặt bàn'
            goTo = '/admin/reservations'
          } else if (type === 'ORDER_STATUS_CHANGED' || type === 'PAYMENT_UPDATED' || type === 'RESERVATION_STATUS_CHANGED') {
            // Các thay đổi trạng thái nội bộ (do chính admin/staff thao tác): chỉ refresh, không toast
            refreshAll()
            return
          }
        }

        toast((t) => (
          <div
            onClick={() => { if (goTo) navigate(goTo); toast.dismiss(t.id) }}
            style={{ cursor: goTo ? 'pointer' : 'default' }}>
            <div style={{ fontWeight: 600, marginBottom: 2 }}>{icon} {title}</div>
            <div style={{ opacity: 0.85 }}>{body}</div>
            {goTo && <div style={{ fontSize: 11, opacity: 0.6, marginTop: 4 }}>Bấm để xem chi tiết →</div>}
          </div>
        ), {
          duration: 6000,
          position: 'top-right',
          style: TOAST_STYLE,
        })

        if (data.orderId || data.reservationId) {
          window.__adminSeenNotifIds.add(`n-${data.orderId || data.reservationId}-${type}`)
        }

        refreshAll()
      } catch (e) {
        console.warn('[WS admin] parse error', e)
      }
    })

    return () => {
      try { subRef.current?.unsubscribe() } catch (_) {}
    }
  }, [client, qc])
}

function formatVN(iso) {
  try {
    const d = new Date(iso)
    const pad = (n) => String(n).padStart(2, '0')
    return `${pad(d.getHours())}:${pad(d.getMinutes())} ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
  } catch {
    return iso
  }
}
