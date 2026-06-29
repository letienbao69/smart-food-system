import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Printer } from 'lucide-react'
import { ordersApi } from '@/api/cart'
import { Loader } from '@/components/ui/Atoms'
import { formatVND, formatDateTime } from '@/lib/utils'

const PAY_LABEL = { PAYOS: 'Chuyển khoản', CASH: 'Tiền mặt' }

// Tài khoản nhận tiền của quán
const BANK = { bankCode: 'VPB', account: '0978250838', name: 'LE TRAN TIEN BAO' }

function vietQrUrl(amount, content) {
  const rounded = Math.round(amount || 0)
  return `https://img.vietqr.io/image/${BANK.bankCode}-${BANK.account}-compact2.png` +
    `?amount=${rounded}&addInfo=${encodeURIComponent(content || '')}&accountName=${encodeURIComponent(BANK.name)}`
}

export default function InvoicePage() {
  const { id } = useParams()
  const { data: order, isLoading } = useQuery({
    queryKey: ['admin-order', id],
    queryFn: () => ordersApi.adminGet(id),
  })

  if (isLoading) return <Loader className="min-h-[40vh]" />
  if (!order) return <div className="p-12 text-center">Không tìm thấy đơn</div>

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="flex items-center justify-between mb-6 print:hidden">
        <h1 className="font-display text-2xl font-bold text-ink-900">🧾 HÓA ĐƠN THANH TOÁN</h1>
        <button onClick={() => window.print()}
          className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
          <Printer className="h-4 w-4" /> In hoá đơn
        </button>
      </div>

      <div className="rounded-xl border border-ink-200 p-6">
        <div className="space-y-1.5 text-sm border-b border-ink-200 pb-4">
          <p><span className="font-semibold">Khách hàng:</span> {order.customerName || '—'}</p>
          {order.guestPhone && <p><span className="font-semibold">Điện thoại:</span> {order.guestPhone}</p>}
          <p><span className="font-semibold">Bàn:</span> {order.tableNumber ? `Bàn ${order.tableNumber}` : '—'}</p>
          <p><span className="font-semibold">Ngày đặt:</span> {order.reservationTime ? formatDateTime(order.reservationTime) : formatDateTime(order.createdAt)}</p>
          <p><span className="font-semibold">Phương thức thanh toán:</span> {PAY_LABEL[order.paymentMethod] || order.paymentMethod || '—'}</p>
        </div>

        <p className="font-semibold text-ink-900 my-4">Chi tiết món ăn</p>
        <table className="w-full text-sm">
          <thead className="text-left text-ink-500 border-b border-ink-200">
            <tr>
              <th className="py-2 font-medium">Tên món</th>
              <th className="py-2 font-medium text-center">Số lượng</th>
              <th className="py-2 font-medium text-right">Đơn giá</th>
              <th className="py-2 font-medium text-right">Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            {order.items?.map((it) => (
              <tr key={it.id} className="border-b border-ink-100">
                <td className="py-2.5">{it.foodName}</td>
                <td className="py-2.5 text-center">{it.quantity}</td>
                <td className="py-2.5 text-right tabular">{formatVND(it.unitPrice)}</td>
                <td className="py-2.5 text-right tabular">{formatVND(it.subtotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="mt-4 space-y-1 text-right">
          {order.discountAmount > 0 && (
            <p className="text-sm text-ink-500">Giảm giá: −{formatVND(order.discountAmount)}</p>
          )}
          {Number(order.depositAmount) > 0 && order.depositStatus === 'PAID' && (
            <p className="text-sm text-green-600">Đã đặt cọc: −{formatVND(order.depositAmount)}</p>
          )}
          {Number(order.depositAmount) > 0 && order.depositStatus === 'PAID' ? (
            <>
              <p className="text-sm text-ink-500">Tổng đơn: {formatVND(order.finalAmount)}</p>
              <p className="font-display text-xl font-bold">
                Còn phải thanh toán: {formatVND(Math.max(0, Number(order.finalAmount) - Number(order.depositAmount)))}
              </p>
            </>
          ) : (
            <p className="font-display text-xl font-bold">Tổng cộng: {formatVND(order.finalAmount)}</p>
          )}
        </div>

        {/* QR thanh toán: hiện QR ngân hàng cho hóa đơn chưa PAID; ẩn nếu thanh toán PayOS đã PAID. */}
        {order.paymentStatus !== 'PAID' && (() => {
          const content = order.reservationCode || order.orderCode || ''
          const qr = vietQrUrl(order.finalAmount, content)
          return (
            <div className="mt-6 flex flex-col items-center border-t border-ink-200 pt-6">
              <p className="text-sm font-medium text-ink-700 mb-3">Quét QR ngân hàng để thanh toán</p>
              <div className="rounded-xl border-2 border-ink-900 bg-white p-2">
                <img src={qr} alt="QR thanh toán" className="h-44 w-44 object-contain"
                  onError={(e) => { e.target.style.display = 'none' }} />
              </div>
              <p className="mt-2 text-xs text-ink-500">{BANK.bankCode} · {BANK.account} · {BANK.name}</p>
              <p className="text-xs text-ink-400">Nội dung: {content}</p>
            </div>
          )
        })()}
      </div>
    </div>
  )
}
