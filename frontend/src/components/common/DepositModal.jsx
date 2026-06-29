import { Copy, Clock, ShieldCheck } from 'lucide-react'
import toast from 'react-hot-toast'
import { useQueryClient } from '@tanstack/react-query'
import { useMutation } from '@tanstack/react-query'
import Modal from '@/components/ui/Modal'
import Button from '@/components/ui/Button'
import { formatVND } from '@/lib/utils'
import { reservationsApi } from '@/api/reservations'

// Thông tin tài khoản nhận cọc — khớp cấu hình payment.* của backend
const OWNER = {
  bankCode: 'MB',
  account: '0123456789',
  name: 'SMART FOOD SHOP',
}

function buildQrUrl(amount, content) {
  const encodedName = encodeURIComponent(OWNER.name)
  const encodedContent = encodeURIComponent(content)
  const rounded = Math.round(amount || 0)
  return `https://img.vietqr.io/image/${OWNER.bankCode}-${OWNER.account}-compact2.png` +
    `?amount=${rounded}&addInfo=${encodedContent}&accountName=${encodedName}`
}

/**
 * Modal hiển thị QR đặt cọc giữ bàn. Khách chuyển khoản ở nhà rồi bấm
 * "Tôi đã chuyển khoản cọc" -> báo admin xác nhận thủ công.
 */
export default function DepositModal({ open, onClose, reservation, onNotified }) {
  const qc = useQueryClient()

  const notify = useMutation({
    mutationFn: () => reservationsApi.notifyDeposit(reservation.id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-reservations'] })
      qc.invalidateQueries({ queryKey: ['my-reservation', String(reservation?.id)] })
      toast.success('Đã gửi thông báo chuyển khoản cọc. Vui lòng chờ nhà hàng xác nhận.')
      onNotified?.()
    },
    onError: (e) => toast.error(e?.response?.data?.message || 'Không gửi được thông báo'),
  })

  if (!reservation) return null

  const amount = reservation.depositAmount || 0
  const code = reservation.reservationCode || `RSV${reservation.id}`
  const qrUrl = buildQrUrl(amount, code)

  const copy = (text, label = 'Đã sao chép') => {
    navigator.clipboard.writeText(String(text))
    toast.success(label)
  }

  return (
    <Modal open={open} onClose={onClose} title="Đặt cọc giữ bàn qua chuyển khoản" size="md">
      <div className="space-y-4">
        <div className="rounded-xl p-4 text-white relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, #b45309 0%, #d97706 100%)' }}>
          <div className="absolute -right-8 -top-8 h-28 w-28 rounded-full bg-white/10" />
          <p className="text-xs uppercase tracking-wider opacity-75">Tiền cọc giữ bàn</p>
          <p className="mt-0.5 font-display text-3xl font-bold tabular">{formatVND(amount)}</p>
          <div className="mt-2 flex items-center gap-2 text-sm">
            <span className="opacity-75">Mã đặt bàn:</span>
            <span className="font-mono font-bold">{code}</span>
            <button onClick={() => copy(code)} className="opacity-70 hover:opacity-100">
              <Copy className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        <div className="rounded-lg bg-amber-50 border border-amber-200 p-2.5 text-xs text-amber-800 flex items-start gap-1.5">
          <ShieldCheck className="h-4 w-4 shrink-0 mt-0.5" />
          <span>Tiền cọc dùng để giữ bàn và sẽ được <strong>trừ vào hóa đơn</strong> khi bạn đến ăn.
            Phần còn lại thanh toán tại quán.</span>
        </div>

        <div className="flex gap-4 items-start">
          <div className="shrink-0">
            <div className="rounded-xl border-2 border-ink-900 overflow-hidden bg-white p-1">
              <img src={qrUrl} alt="QR đặt cọc" className="h-44 w-44 object-contain"
                onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex' }} />
              <div className="h-44 w-44 items-center justify-center text-center text-xs text-ink-500 p-3"
                style={{ display: 'none' }}>
                QR không tải được.<br />Dùng thông tin bên cạnh.
              </div>
            </div>
            <p className="mt-1 text-center text-[10px] text-ink-400">Quét bằng app ngân hàng</p>
          </div>

          <div className="flex-1 min-w-0 space-y-2.5">
            <p className="text-sm font-semibold text-ink-900">Thông tin chuyển khoản</p>
            <InfoRow label="Ngân hàng" value={OWNER.bankCode} />
            <InfoRow label="Số TK" value={OWNER.account} onCopy={() => copy(OWNER.account)} />
            <InfoRow label="Chủ TK" value={OWNER.name} />
            <InfoRow label="Số tiền" value={formatVND(amount)} valueClass="font-bold text-amber-600"
              onCopy={() => copy(Math.round(amount))} />
            <InfoRow label="Nội dung CK" value={code} valueClass="font-mono font-semibold"
              onCopy={() => copy(code, 'Đã sao chép nội dung')} />
          </div>
        </div>

        <div className="flex gap-2 pt-1">
          <Button variant="secondary" onClick={onClose} className="flex-1">Để sau</Button>
          <Button onClick={() => notify.mutate()} loading={notify.isPending} className="flex-1">
            <Clock className="h-4 w-4" />
            Tôi đã chuyển khoản cọc
          </Button>
        </div>
      </div>
    </Modal>
  )
}

function InfoRow({ label, value, onCopy, valueClass = '' }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-xs text-ink-500 shrink-0">{label}</span>
      <div className="flex items-center gap-1 min-w-0">
        <span className={`text-xs truncate ${valueClass || 'text-ink-900 font-medium'}`}>{value}</span>
        {onCopy && (
          <button onClick={onCopy} className="text-ink-400 hover:text-ink-700 shrink-0">
            <Copy className="h-3 w-3" />
          </button>
        )}
      </div>
    </div>
  )
}
