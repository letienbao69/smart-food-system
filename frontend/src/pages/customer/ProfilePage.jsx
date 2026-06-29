import { useState, useRef, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { User, Mail, Phone, Calendar, MapPin, Activity, ArrowRight, Upload, CheckCircle2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import toast from 'react-hot-toast'
import { useAuth } from '@/store/auth'
import { errMsg } from '@/api/client'
import { profileApi, uploadApi } from '@/api/misc'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import { Loader } from '@/components/ui/Atoms'
import { initials, cn } from '@/lib/utils'

export default function ProfilePage() {
  const user = useAuth((s) => s.user)
  const setAuth = useAuth((s) => s.setAuth)
  const token = useAuth((s) => s.token)
  const fileRef = useRef(null)

  const [tab, setTab] = useState('info')
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', dateOfBirth: '', address: '', avatarUrl: '' })
  const [saving, setSaving] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [pw, setPw] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [savingPw, setSavingPw] = useState(false)

  const { data: me, isLoading } = useQuery({ queryKey: ['profile-me'], queryFn: profileApi.me })

  useEffect(() => {
    if (me) {
      setForm({
        fullName: me.fullName || '',
        email: me.email || '',
        phone: me.phone || '',
        dateOfBirth: me.dateOfBirth ? String(me.dateOfBirth).substring(0, 10) : '',
        address: me.address || '',
        avatarUrl: me.avatarUrl || '',
      })
    }
  }, [me])

  const syncAuthAvatar = (avatarUrl, fullName) => {
    const next = { ...(user || {}), avatarUrl, fullName }
    setAuth(token, next)
  }

  const pickAvatar = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const res = await uploadApi.image(file)
      setForm((f) => ({ ...f, avatarUrl: res.url }))
      toast.success('Đã tải ảnh lên')
    } catch (err) {
      toast.error(errMsg(err, 'Tải ảnh thất bại'))
    } finally {
      setUploading(false)
      if (fileRef.current) fileRef.current.value = ''
    }
  }

  const save = async () => {
    setSaving(true)
    try {
      const updated = await profileApi.update({
        fullName: form.fullName,
        phone: form.phone,
        dateOfBirth: form.dateOfBirth || null,
        address: form.address,
        avatarUrl: form.avatarUrl || null,
      })
      syncAuthAvatar(updated.avatarUrl, updated.fullName)
      toast.success('Thông tin cá nhân đã được cập nhật thành công!')
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setSaving(false)
    }
  }

  const changePassword = async () => {
    if (pw.newPassword !== pw.confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp')
      return
    }
    setSavingPw(true)
    try {
      await profileApi.changePassword(pw.oldPassword, pw.newPassword)
      toast.success('Đổi mật khẩu thành công')
      setPw({ oldPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setSavingPw(false)
    }
  }

  if (isLoading) return <Loader className="min-h-[40vh]" />

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        {/* Sidebar */}
        <div className="h-fit">
          <div className="rounded-2xl p-5 text-white text-center"
            style={{ background: 'linear-gradient(160deg, #ef4444 0%, #b91c1c 100%)' }}>
            <div className="mx-auto h-24 w-24 rounded-full ring-4 ring-white/30 overflow-hidden bg-white/20 grid place-items-center">
              {form.avatarUrl ? (
                <img src={form.avatarUrl} alt="avatar" className="h-full w-full object-cover" />
              ) : (
                <span className="text-2xl font-display font-bold">{initials(form.fullName || form.email)}</span>
              )}
            </div>
            <p className="mt-3 font-display font-bold text-lg">{form.fullName || 'Người dùng'}</p>
            <p className="text-xs text-white/80">{form.email}</p>
          </div>

          <div className="mt-3 card p-2">
            <SideItem active={tab === 'info'} onClick={() => setTab('info')} icon={User} label="Thông tin cá nhân" />
            <Link to="/orders" className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-ink-700 hover:bg-ink-100">
              <Activity className="h-4 w-4" /> Đơn món của bạn
            </Link>
            <Link to="/health" className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm text-ink-700 hover:bg-ink-100">
              <ArrowRight className="h-4 w-4" /> Hồ sơ sức khỏe
            </Link>
          </div>
        </div>

        {/* Main */}
        <div>
          <h1 className="font-display text-2xl font-bold text-ink-900">Cài đặt tài khoản</h1>
          <p className="text-sm text-ink-500 mb-5">Quản lý thông tin và bảo mật tài khoản</p>

          <div className="flex gap-6 border-b border-ink-200 mb-6">
            <TabBtn active={tab === 'info'} onClick={() => setTab('info')}>Thông tin cá nhân</TabBtn>
            <TabBtn active={tab === 'password'} onClick={() => setTab('password')}>Đổi mật khẩu</TabBtn>
          </div>

          {tab === 'info' ? (
            <div className="space-y-6">
              {/* Avatar */}
              <div>
                <p className="text-sm font-medium text-ink-700 mb-3">Ảnh đại diện</p>
                <div className="flex items-center gap-4">
                  <div className="h-20 w-20 rounded-full overflow-hidden bg-ink-100 grid place-items-center">
                    {form.avatarUrl ? (
                      <img src={form.avatarUrl} alt="avatar" className="h-full w-full object-cover" />
                    ) : (
                      <span className="font-display font-bold text-ink-400">{initials(form.fullName || form.email)}</span>
                    )}
                  </div>
                  <button type="button" onClick={() => fileRef.current?.click()}
                    className="rounded-xl border-2 border-dashed border-ink-300 px-6 py-4 text-center hover:border-ink-400 transition">
                    {uploading ? <Loader className="h-5 w-5" /> : <Upload className="h-5 w-5 mx-auto text-ink-500" />}
                    <span className="block text-xs text-ink-600 mt-1">Tải lên</span>
                  </button>
                  <input ref={fileRef} type="file" accept="image/*" className="hidden" onChange={pickAvatar} />
                </div>
              </div>

              {/* Contact */}
              <div>
                <p className="text-sm font-medium text-ink-700 mb-3">Thông tin liên hệ</p>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Field label="Họ và tên" required icon={User}>
                    <input className="input" value={form.fullName}
                      onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Họ và tên" />
                  </Field>
                  <Field label="Email" icon={Mail}>
                    <input className="input bg-ink-50" value={form.email} disabled />
                  </Field>
                  <Field label="Số điện thoại" required icon={Phone}>
                    <input className="input" value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })} placeholder="09xxxxxxxx" />
                  </Field>
                  <Field label="Ngày sinh" icon={Calendar}>
                    <input type="date" className="input" value={form.dateOfBirth}
                      onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })} />
                  </Field>
                </div>
              </div>

              {/* Address */}
              <div>
                <p className="text-sm font-medium text-ink-700 mb-3 flex items-center gap-1.5">
                  <MapPin className="h-4 w-4 text-ink-400" /> Địa chỉ
                </p>
                <textarea className="input resize-none" rows={2} value={form.address}
                  onChange={(e) => setForm({ ...form, address: e.target.value })} placeholder="Địa chỉ liên hệ của bạn" />
              </div>

              <Button onClick={save} loading={saving}>
                <CheckCircle2 className="h-4 w-4" /> Lưu
              </Button>
            </div>
          ) : (
            <div className="space-y-4 max-w-md">
              <Field label="Mật khẩu hiện tại">
                <input type="password" className="input" value={pw.oldPassword}
                  onChange={(e) => setPw({ ...pw, oldPassword: e.target.value })} />
              </Field>
              <Field label="Mật khẩu mới">
                <input type="password" className="input" value={pw.newPassword}
                  onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
              </Field>
              <Field label="Xác nhận mật khẩu mới">
                <input type="password" className="input" value={pw.confirmPassword}
                  onChange={(e) => setPw({ ...pw, confirmPassword: e.target.value })} />
              </Field>
              <Button onClick={changePassword} loading={savingPw}>Đổi mật khẩu</Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function SideItem({ active, onClick, icon: Icon, label }) {
  return (
    <button onClick={onClick}
      className={cn('flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-sm transition',
        active ? 'bg-red-50 text-red-600 font-medium' : 'text-ink-700 hover:bg-ink-100')}>
      <Icon className="h-4 w-4" /> {label}
    </button>
  )
}

function TabBtn({ active, onClick, children }) {
  return (
    <button onClick={onClick}
      className={cn('pb-2.5 text-sm font-medium border-b-2 -mb-px transition',
        active ? 'border-red-500 text-red-600' : 'border-transparent text-ink-500 hover:text-ink-800')}>
      {children}
    </button>
  )
}

function Field({ label, required, icon: Icon, children }) {
  return (
    <div>
      <label className="block text-xs text-ink-500 mb-1">
        {required && <span className="text-red-500">* </span>}{label}
      </label>
      {children}
    </div>
  )
}
