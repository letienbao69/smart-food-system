import { useState, useEffect } from 'react'
import { MessageSquare, Send, MapPin, Phone, Mail, Clock } from 'lucide-react'
import toast from 'react-hot-toast'
import { contactsApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { useAuth } from '@/store/auth'
import Button from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'

export default function ContactPage() {
  const user = useAuth((s) => s.user)
  const [form, setForm] = useState({ name: '', email: '', phone: '', subject: '', message: '' })
  const [sending, setSending] = useState(false)

  useEffect(() => {
    if (user) {
      setForm((f) => ({
        ...f,
        name: f.name || user.fullName || '',
        email: f.email || user.email || '',
        phone: f.phone || user.phone || '',
      }))
    }
  }, [user])

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async () => {
    if (!form.name.trim() || !form.message.trim()) {
      toast.error('Vui lòng nhập họ tên và nội dung phản ánh')
      return
    }
    setSending(true)
    try {
      await contactsApi.create(form)
      toast.success('Đã gửi phản ánh, cảm ơn bạn!')
      setForm((f) => ({ ...f, subject: '', message: '' }))
    } catch (e) {
      toast.error(errMsg(e, 'Gửi phản ánh thất bại'))
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="text-center mb-8">
        <p className="text-xs uppercase tracking-wider text-ink-400 font-medium">Liên hệ</p>
        <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">Liên hệ & Phản ánh</h1>
        <p className="mt-2 text-sm text-ink-500">Mọi ý kiến đóng góp của bạn giúp chúng tôi phục vụ tốt hơn.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Form */}
        <div className="card p-6">
          <h2 className="font-display font-semibold text-ink-900 mb-4 inline-flex items-center gap-2">
            <MessageSquare className="h-5 w-5" /> Gửi phản ánh
          </h2>
          <div className="space-y-4">
            <div className="grid sm:grid-cols-2 gap-3">
              <Input label="Họ và tên" value={form.name} onChange={set('name')} placeholder="Nhập họ tên" />
              <Input label="Số điện thoại" value={form.phone} onChange={set('phone')} placeholder="09xxxxxxxx" />
            </div>
            <Input label="Email" value={form.email} onChange={set('email')} placeholder="email@example.com" />
            <Input label="Tiêu đề" value={form.subject} onChange={set('subject')} placeholder="Tiêu đề phản ánh (tuỳ chọn)" />
            <div>
              <label className="block text-xs text-ink-500 mb-1"><span className="text-red-500">* </span>Nội dung</label>
              <textarea className="input resize-none" rows={5} value={form.message} onChange={set('message')}
                placeholder="Nhập nội dung phản ánh / góp ý của bạn..." />
            </div>
            <Button onClick={submit} loading={sending}>
              <Send className="h-4 w-4" /> Gửi phản ánh
            </Button>
          </div>
        </div>

        {/* Thông tin liên hệ */}
        <div className="space-y-4">
          <div className="card p-6 space-y-4">
            <h2 className="font-display font-semibold text-ink-900">Thông tin nhà hàng</h2>
            <InfoItem icon={MapPin} label="Địa chỉ" value="175 Tây Sơn, Đống Đa, Hà Nội" />
            <InfoItem icon={Phone} label="Hotline" value="1900 1234" />
            <InfoItem icon={Mail} label="Email" value="tienbao37fc@gmail.com" />
            <InfoItem icon={Clock} label="Giờ mở cửa" value="09:00 - 22:00 hằng ngày" />
          </div>
        </div>
      </div>
    </div>
  )
}

function InfoItem({ icon: Icon, label, value }) {
  return (
    <div className="flex items-start gap-3">
      <div className="grid h-9 w-9 place-items-center rounded-lg bg-ink-100 text-ink-600 shrink-0">
        <Icon className="h-4 w-4" />
      </div>
      <div>
        <p className="text-xs text-ink-500">{label}</p>
        <p className="text-sm font-medium text-ink-900">{value}</p>
      </div>
    </div>
  )
}
