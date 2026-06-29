import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Mail, KeyRound, Lock, CheckCircle2, ArrowLeft } from 'lucide-react'
import toast from 'react-hot-toast'
import { authApi } from '@/api/auth'
import { errMsg } from '@/api/client'
import AuthLayout from '@/components/layout/AuthLayout'
import Button from '@/components/ui/Button'

export default function ForgotPasswordPage() {
  const nav = useNavigate()
  const [step, setStep] = useState(1) // 1: nhập email · 2: nhập mã + mật khẩu mới
  const [loading, setLoading] = useState(false)
  const [email, setEmail] = useState('')
  const [maskedEmail, setMaskedEmail] = useState('')
  const [devCode, setDevCode] = useState('') // mã hiển thị khi chưa bật email (chế độ thử nghiệm)

  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  // Bước 1: gửi yêu cầu lấy mã
  const sendCode = async (e) => {
    e.preventDefault()
    if (!email.trim()) return toast.error('Vui lòng nhập email')
    setLoading(true)
    try {
      const res = await authApi.forgotPassword(email.trim())
      setMaskedEmail(res?.email || email.trim())
      // Nếu BE đang ở chế độ thử nghiệm (chưa bật email) sẽ trả về resetToken để thử
      if (res?.resetToken) setDevCode(res.resetToken)
      setStep(2)
      toast.success('Đã gửi mã xác nhận')
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setLoading(false)
    }
  }

  // Gửi lại mã
  const resend = async () => {
    setLoading(true)
    try {
      const res = await authApi.forgotPassword(email.trim())
      if (res?.resetToken) setDevCode(res.resetToken)
      toast.success('Đã gửi lại mã')
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setLoading(false)
    }
  }

  // Bước 2: xác nhận mã + đặt mật khẩu mới
  const resetPassword = async (e) => {
    e.preventDefault()
    if (!/^\d{6}$/.test(code.trim())) return toast.error('Mã xác nhận gồm 6 chữ số')
    if (newPassword.length < 6) return toast.error('Mật khẩu cần ít nhất 6 ký tự')
    if (newPassword !== confirmPassword) return toast.error('Mật khẩu xác nhận không khớp')
    setLoading(true)
    try {
      await authApi.resetPassword(code.trim(), newPassword)
      toast.success('Đặt lại mật khẩu thành công! Vui lòng đăng nhập.')
      nav('/login', { replace: true })
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title={step === 1 ? 'Quên mật khẩu?' : 'Nhập mã xác nhận'}
      subtitle={
        step === 1
          ? 'Nhập email tài khoản — chúng tôi sẽ gửi mã xác nhận 6 chữ số tới hộp thư của bạn.'
          : `Mã xác nhận đã được gửi tới ${maskedEmail}. Nhập mã và đặt mật khẩu mới.`
      }
      footer={
        <Link to="/login" className="inline-flex items-center gap-1 text-ink-700 hover:text-ink-900">
          <ArrowLeft className="h-4 w-4" /> Quay lại đăng nhập
        </Link>
      }
    >
      {step === 1 ? (
        <form onSubmit={sendCode} className="space-y-4">
          <div>
            <label className="label">Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="email@example.com"
                className="input pl-10"
              />
            </div>
          </div>
          <Button type="submit" loading={loading} className="w-full" size="lg">
            <Mail className="h-4 w-4" /> Gửi mã xác nhận
          </Button>
        </form>
      ) : (
        <form onSubmit={resetPassword} className="space-y-4">
          {devCode && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              <b>Chế độ thử nghiệm</b> (chưa bật gửi email): mã của bạn là{' '}
              <span className="font-mono font-bold tracking-widest">{devCode}</span>
            </div>
          )}
          <div>
            <label className="label">Mã xác nhận (6 chữ số)</label>
            <div className="relative">
              <KeyRound className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
              <input
                inputMode="numeric"
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                required
                placeholder="______"
                className="input pl-10 tracking-[0.5em] font-mono text-lg"
              />
            </div>
          </div>
          <div>
            <label className="label">Mật khẩu mới</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                placeholder="Tối thiểu 6 ký tự"
                className="input pl-10"
              />
            </div>
          </div>
          <div>
            <label className="label">Xác nhận mật khẩu</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                placeholder="Nhập lại mật khẩu mới"
                className="input pl-10"
              />
            </div>
          </div>
          <Button type="submit" loading={loading} className="w-full" size="lg">
            <CheckCircle2 className="h-4 w-4" /> Đặt lại mật khẩu
          </Button>
          <div className="flex items-center justify-between text-sm">
            <button type="button" onClick={() => setStep(1)} className="text-ink-500 hover:text-ink-800">
              ← Đổi email
            </button>
            <button type="button" onClick={resend} disabled={loading} className="text-accent-700 hover:underline disabled:opacity-50">
              Gửi lại mã
            </button>
          </div>
        </form>
      )}
    </AuthLayout>
  )
}
