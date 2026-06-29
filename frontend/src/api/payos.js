import { api, unwrap } from './client'

/**
 * Gọi cổng thanh toán PayOS để tạo link/QR đặt cọc cho một lượt đặt bàn.
 * Trả về: { checkoutUrl, qrCode, paymentLinkId, status, amount, orderCode, ... }
 *
 * Sau khi nhận được:
 *   - mở `checkoutUrl` (PayOS hosted page) HOẶC hiển thị `qrCode` (chuỗi VietQR)
 *   - PayOS sẽ gọi webhook về BE để cập nhật trạng thái cọc tự động
 */
export const payosApi = {
  createForReservation: (reservationId) =>
    api.post(`/payment/payos/reservation/${reservationId}`).then(unwrap),
  // Hỏi trạng thái thanh toán (frontend tự poll vì webhook không gọi được về localhost)
  status: (orderCode) =>
    api.get(`/payment/payos/status/${orderCode}`).then(unwrap),
}
