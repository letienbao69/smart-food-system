import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

export function formatVND(amount) {
  if (amount == null) return '—'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount)
}

// Voucher giảm theo phần trăm. Chấp nhận cả hai quy ước lưu trữ: '%' và 'PERCENT'
// (seed dùng 'PERCENT', form quản trị dùng '%'), tránh tính giảm không nhất quán.
export function isPercentDiscount(type) {
  return type === '%' || String(type).toUpperCase() === 'PERCENT'
}

// Chuẩn hoá giá trị ngày giờ từ backend.
// Backend trả LocalDateTime dạng "2025-09-11T15:10:00" (không có Z) = giờ VN.
// Nếu chuỗi không có offset/Z, coi như giờ địa phương (đúng) — new Date() xử lý được.
function parseDate(d) {
  if (!d) return null
  if (d instanceof Date) return d
  return new Date(d)
}

// Backend trả LocalDateTime dạng "2026-05-29T11:27:00" (KHÔNG có offset).
// Đây là giờ Việt Nam tuyệt đối — không được để JS tự suy theo timezone máy.
// Vì vậy ta tách trực tiếp các thành phần ngày/giờ từ chuỗi và hiển thị nguyên văn.
function parseLocalParts(d) {
  if (!d) return null
  if (typeof d === 'string') {
    // Khớp YYYY-MM-DDTHH:mm(:ss)? không có Z/offset
    const m = d.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?$/)
    if (m) {
      return {
        year: m[1], month: m[2], day: m[3],
        hour: m[4], minute: m[5], second: m[6] || '00',
      }
    }
  }
  // Có offset hoặc là Date → để JS xử lý rồi quy về giờ VN
  const date = d instanceof Date ? d : new Date(d)
  if (isNaN(date)) return null
  const fmt = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false, timeZone: 'Asia/Ho_Chi_Minh',
  }).formatToParts(date)
  const get = (t) => fmt.find((p) => p.type === t)?.value
  let hour = get('hour')
  if (hour === '24') hour = '00'
  return { year: get('year'), month: get('month'), day: get('day'), hour, minute: get('minute'), second: get('second') }
}

export function formatDate(d) {
  const p = parseLocalParts(d)
  if (!p) return '—'
  return `${p.day}/${p.month}/${p.year}`
}

export function formatDateTime(d) {
  const p = parseLocalParts(d)
  if (!p) return '—'
  return `${p.hour}:${p.minute} ${p.day}/${p.month}/${p.year}`
}

/** First letters of a name for an avatar fallback. */
export function initials(name) {
  if (!name) return '?'
  return name
    .split(' ')
    .map((s) => s[0])
    .slice(-2)
    .join('')
    .toUpperCase()
}

/** Stable hash → hue for consistent placeholder colors per food. */
export function hashHue(s = '') {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return h % 360
}

/** Translate order status to Vietnamese */
export function orderStatusLabel(s) {
  return (
    {
      PENDING: 'Chờ xác nhận',
      CONFIRMED: 'Đã xác nhận',
      PREPARING: 'Đang chuẩn bị',
      SERVED: 'Đã phục vụ',
      COMPLETED: 'Hoàn thành',
      CANCELLED: 'Đã hủy',
    }[s] || s
  )
}

export function orderStatusTone(s) {
  return (
    {
      PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
      CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200',
      PREPARING: 'bg-violet-50 text-violet-700 border-violet-200',
      SERVED: 'bg-cyan-50 text-cyan-700 border-cyan-200',
      COMPLETED: 'bg-success-50 text-success-700 border-green-200',
      CANCELLED: 'bg-danger-50 text-danger-700 border-red-200',
    }[s] || 'bg-ink-100 text-ink-700 border-ink-200'
  )
}

export function reservationStatusLabel(s) {
  return (
    {
      PENDING: 'Chờ xác nhận',
      CONFIRMED: 'Đã xác nhận',
      SEATED: 'Đã nhận bàn',
      COMPLETED: 'Hoàn tất',
      CANCELLED: 'Đã hủy',
      NO_SHOW: 'Khách không đến',
    }[s] || s
  )
}

export function reservationStatusTone(s) {
  return (
    {
      PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
      CONFIRMED: 'bg-blue-50 text-blue-700 border-blue-200',
      SEATED: 'bg-violet-50 text-violet-700 border-violet-200',
      COMPLETED: 'bg-success-50 text-success-700 border-green-200',
      CANCELLED: 'bg-danger-50 text-danger-700 border-red-200',
      NO_SHOW: 'bg-ink-100 text-ink-600 border-ink-300',
    }[s] || 'bg-ink-100 text-ink-700 border-ink-200'
  )
}

export function depositStatusLabel(s) {
  return (
    {
      NONE: 'Chưa cọc',
      PENDING: 'Chờ xác nhận cọc',
      PAID: 'Đã cọc',
    }[s] || s
  )
}

export function paymentStatusLabel(s) {
  return (
    {
      UNPAID: 'Chưa thanh toán',
      PENDING: 'Chờ xác nhận thanh toán',
      PAID: 'Đã thanh toán',
      FAILED: 'Thất bại',
      REFUNDED: 'Hoàn tiền',
    }[s] || s
  )
}

export function bmiBadgeTone(category) {
  return (
    {
      UNDERWEIGHT: 'bg-blue-50 text-blue-700 border-blue-200',
      NORMAL: 'bg-success-50 text-success-700 border-green-200',
      OVERWEIGHT: 'bg-amber-50 text-amber-700 border-amber-200',
      OBESE: 'bg-danger-50 text-danger-700 border-red-200',
    }[category] || 'bg-ink-100 text-ink-700 border-ink-200'
  )
}
