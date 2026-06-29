import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { Clock, Copy, ShieldCheck, Loader2, XCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { payosApi } from '@/api/payos'
import { reservationsApi } from '@/api/reservations'
import { errMsg } from '@/api/client'
import Button from '@/components/ui/Button'
import { formatVND } from '@/lib/utils'

const TIMEOUT_SECONDS = 180 // 3 phút — khớp app.reservation.payment-timeout-minutes ở backend
const POLL_MS = 5000

export default function DepositPaymentPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const qc = useQueryClient()

  const [link, setLink] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [secondsLeft, setSecondsLeft] = useState(TIMEOUT_SECONDS)
  const [paid, setPaid] = useState(false)
  const doneRef = useRef(false) // tránh xử lý trùng (thanh toán xong / hết hạn)
  const createdRef = useRef(false) // tránh tạo link 2 lần (React StrictMode gọi effect 2 lần ở dev)

  // 1) Tạo link/QR cọc khi mở trang
  useEffect(() => {
    if (createdRef.current) return
    createdRef.current = true
    setLoading(true)
    payosApi.createForReservation(id)
      .then((data) => { setLink(data); setSecondsLeft(TIMEOUT_SECONDS) })
      .catch((e) => setError(errMsg(e, 'Không tạo được mã thanh toán cọc')))
      .finally(() => setLoading(false))
  }, [id])

  // 2) Đếm ngược 3 phút
  useEffect(() => {
    if (!link || paid) return
    const timer = setInterval(() => {
      setSecondsLeft((s) => {
        if (s <= 1) { clearInterval(timer); handleExpire(); return 0 }
        return s - 1
      })
    }, 1000)
    return () => clearInterval(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [link, paid])

  // 3) Hỏi trạng thái thanh toán định kỳ
  useEffect(() => {
    if (!link?.orderCode || paid) return
    const poll = setInterval(async () => {
      try {
        const res = await payosApi.status(link.orderCode)
        if (res?.depositStatus === 'PAID' || res?.payosStatus === 'PAID') {
          handleSuccess()
          clearInterval(poll)
        }
      } catch (_) { /* bỏ qua lỗi mạng tạm thời */ }
    }, POLL_MS)
    return () => clearInterval(poll)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [link, paid])

  const handleSuccess = () => {
    if (doneRef.current) return
    doneRef.current = true
    setPaid(true)
    qc.invalidateQueries({ queryKey: ['my-reservations'] })
    qc.invalidateQueries({ queryKey: ['my-reservation', String(id)] })
    toast.success('Thanh toán cọc thành công!')
    setTimeout(() => nav(`/payment-success/${id}`), 600)
  }

  const handleExpire = () => {
    if (doneRef.current) return
    doneRef.current = true
    reservationsApi.cancel(id).catch(() => {})
    qc.invalidateQueries({ queryKey: ['my-reservations'] })
    toast.error('Đã hết thời gian thanh toán. Lượt đặt bàn đã bị hủy.')
    setTimeout(() => nav('/reservations'), 1200)
  }

  const cancelMutation = () => {
    if (doneRef.current) return
    doneRef.current = true
    reservationsApi.cancel(id)
      .then(() => { toast('Đã hủy lượt đặt bàn'); qc.invalidateQueries({ queryKey: ['my-reservations'] }) })
      .catch((e) => toast.error(errMsg(e, 'Không hủy được')))
      .finally(() => nav('/reservations'))
  }

  const copy = (t, label = 'Đã sao chép') => { navigator.clipboard.writeText(String(t)); toast.success(label) }

  const mm = String(Math.floor(secondsLeft / 60)).padStart(2, '0')
  const ss = String(secondsLeft % 60).padStart(2, '0')
  const urgent = secondsLeft <= 30

  const qrImg = link?.qrCode
    ? `https://api.qrserver.com/v1/create-qr-code/?size=260x260&margin=8&data=${encodeURIComponent(link.qrCode)}`
    : null

  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6">
      <h1 className="text-center font-display text-2xl font-bold text-ink-900">Thanh toán cọc giữ bàn</h1>
      <p className="mt-1 text-center text-sm text-ink-500">Quét mã QR bằng ứng dụng ngân hàng để hoàn tất đặt cọc.</p>

      {/* Đồng hồ đếm ngược */}
      {!error && (
        <div className={`mx-auto mt-5 flex w-fit items-center gap-2 rounded-full border px-4 py-2 text-sm font-semibold ${
          urgent ? 'bg-red-50 border-red-200 text-red-700' : 'bg-amber-50 border-amber-200 text-amber-800'}`}>
          <Clock className="h-4 w-4" />
          {paid ? 'Đã thanh toán' : <>Tự động hủy sau <span className="tabular font-bold">{mm}:{ss}</span></>}
        </div>
      )}

      <div className="card mt-5 p-6">
        {loading && (
          <div className="flex flex-col items-center gap-3 py-10 text-ink-500">
            <Loader2 className="h-8 w-8 animate-spin" />
            <span>Đang tạo mã thanh toán…</span>
          </div>
        )}

        {error && (
          <div className="py-8 text-center">
            <XCircle className="mx-auto h-10 w-10 text-red-500" />
            <p className="mt-3 text-ink-700">{error}</p>
            <Button className="mt-4" variant="secondary" onClick={() => nav(`/reservations/${id}`)}>Về lượt đặt bàn</Button>
          </div>
        )}

        {!loading && !error && link && (
          <>
            {paid ? (
              <div className="flex flex-col items-center gap-3 py-10 text-green-600">
                <ShieldCheck className="h-10 w-10" />
                <span className="font-semibold">Thanh toán thành công! Đang chuyển trang…</span>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-5 sm:flex-row sm:items-start">
                <div className="shrink-0">
                  <div className="rounded-xl border-2 border-ink-900 bg-white p-2">
                    {qrImg
                      ? <img src={qrImg} alt="QR thanh toán cọc" className="h-56 w-56 object-contain" />
                      : <div className="grid h-56 w-56 place-items-center text-center text-xs text-ink-400">Không tải được mã QR</div>}
                  </div>
                  <p className="mt-1 text-center text-[11px] text-ink-400">Quét bằng app ngân hàng bất kỳ</p>
                </div>

                <div className="min-w-0 flex-1 space-y-3">
                  <div className="rounded-xl p-4 text-white"
                    style={{ background: 'linear-gradient(135deg,#127a3a 0%,#1a8d46 100%)' }}>
                    <p className="text-xs uppercase tracking-wider opacity-80">Số tiền cọc</p>
                    <p className="font-display text-3xl font-bold tabular">{formatVND(link.amount)}</p>
                  </div>

                  <div className="rounded-lg bg-emerald-50 border border-emerald-200 p-2.5 text-xs text-emerald-800 flex items-start gap-1.5">
                    <ShieldCheck className="h-4 w-4 shrink-0 mt-0.5" />
                    <span>Tiền cọc được <strong>trừ vào hóa đơn</strong> khi bạn đến ăn. Sau khi chuyển khoản, trang sẽ tự xác nhận trong giây lát.</span>
                  </div>

                  {link.accountNumber && (
                    <div className="space-y-1.5 text-sm">
                      {link.accountName && <Row label="Chủ tài khoản" value={link.accountName} />}
                      <Row label="Số tài khoản" value={link.accountNumber} onCopy={() => copy(link.accountNumber)} />
                      <Row label="Số tiền" value={formatVND(link.amount)} valueClass="font-bold text-emerald-600" onCopy={() => copy(Math.round(link.amount || 0))} />
                    </div>
                  )}
                </div>
              </div>
            )}

            {!paid && (
              <div className="mt-6 flex justify-center">
                <Button variant="danger" onClick={cancelMutation}>
                  <XCircle className="h-4 w-4" /> Hủy thanh toán
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

function Row({ label, value, onCopy, valueClass = '', hide = false }) {
  if (hide) return null
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-xs text-ink-500 shrink-0">{label}</span>
      <div className="flex items-center gap-1 min-w-0">
        <span className={`text-sm truncate ${valueClass || 'text-ink-900 font-medium'}`}>{value}</span>
        {onCopy && (
          <button onClick={onCopy} className="text-ink-400 hover:text-ink-700 shrink-0">
            <Copy className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </div>
  )
}
