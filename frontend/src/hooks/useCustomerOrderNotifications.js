import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { useWebSocketClient } from '@/providers/WebSocketProvider'
import { orderStatusLabel, paymentStatusLabel } from '@/lib/utils'

const TOAST_STYLE = {
  background: '#1c1917',
  color: '#fafafa',
  fontSize: '13px',
  borderRadius: '12px',
  padding: '14px 16px',
  maxWidth: '360px',
}

/**
 * Subscribes to the customer's personal order queue via WebSocket.
 * Shows toast + invalidates React Query cache so pages refresh instantly.
 * Wired in CustomerLayout — active on all customer pages.
 */
export function useCustomerOrderNotifications() {
  const client = useWebSocketClient()
  const qc = useQueryClient()

  useEffect(() => {
    if (!client) return

    const sub = client.subscribe('/user/queue/orders', (message) => {
      try {
        const data = JSON.parse(message.body)
        const orderId = data.orderId ? String(data.orderId) : null

        // ── Hiển thị toast ──────────────────────────────────────
        let icon = '📦'
        let msg  = ''

        switch (data.type) {
          case 'ORDER_CREATED':
            icon = '🎉'
            msg  = `Đặt hàng thành công!\nĐơn #${data.orderCode} đang chờ xác nhận.`
            break

          case 'ORDER_STATUS_CHANGED': {
            const label = data.newStatus ? orderStatusLabel(data.newStatus) : ''
            if (data.newStatus === 'COMPLETED') {
              icon = '✅'
              msg  = `Đơn #${data.orderCode} đã hoàn thành!\nCảm ơn bạn đã đặt hàng 🙏`
            } else if (data.newStatus === 'SERVED') {
              icon = '🍽️'
              msg  = `Món của bạn (đơn #${data.orderCode}) đã được phục vụ!`
            } else if (data.newStatus === 'CANCELLED') {
              icon = '❌'
              msg  = `Đơn #${data.orderCode} đã bị hủy.`
            } else {
              icon = '🔄'
              msg  = `Đơn #${data.orderCode} → ${label}`
            }
            break
          }

          case 'PAYMENT_UPDATED':
          case 'PAYMENT_CLAIMED': {
            const payLabel = data.newPaymentStatus
              ? paymentStatusLabel(data.newPaymentStatus)
              : ''
            if (data.newPaymentStatus === 'PAID') {
              icon = '💰'
              msg  = `Thanh toán đơn #${data.orderCode} đã được xác nhận!`
            } else if (data.type === 'PAYMENT_CLAIMED') {
              icon = '📤'
              msg  = `Đã gửi thông báo chuyển khoản.\nAdmin đang kiểm tra đơn #${data.orderCode}.`
            } else {
              msg  = `Thanh toán đơn #${data.orderCode} → ${payLabel}`
            }
            break
          }

          case 'CONTACT_RESOLVED':
            icon = '💬'
            msg  = data.message || 'Phản ánh của bạn đã được xử lý. Cảm ơn bạn!'
            break

          default:
            msg = data.message || (data.orderCode ? `Cập nhật đơn #${data.orderCode}` : '')
        }

        if (msg) {
          toast(msg, {
            icon,
            duration: 5000,
            style: { ...TOAST_STYLE, whiteSpace: 'pre-line' },
          })
        }

        // ── Invalidate React Query cache ─────────────────────────
        // Luôn invalidate ALL 'my-order' queries để đảm bảo trang nào đang
        // mở cũng được cập nhật, không phụ thuộc vào orderId có khớp không
        qc.invalidateQueries({ queryKey: ['my-order'] })       // matches tất cả ['my-order', *]
        qc.invalidateQueries({ queryKey: ['my-orders'] })
        qc.invalidateQueries({ queryKey: ['my-reservation'] }) // chi tiết 1 đặt bàn
        qc.invalidateQueries({ queryKey: ['my-reservations'] })// danh sách đặt bàn
        qc.invalidateQueries({ queryKey: ['my-notifications'] })
        qc.invalidateQueries({ queryKey: ['my-notifications-recent'] })
        qc.invalidateQueries({ queryKey: ['my-notifications-count'] })

      } catch (e) {
        console.warn('[WS customer] parse error', e)
      }
    })

    return () => {
      try { sub.unsubscribe() } catch (_) {}
    }
  }, [client, qc])
}
