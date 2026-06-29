import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { CheckCircle2, AlertCircle } from 'lucide-react'
import { authApi } from '@/api/auth'
import { errMsg } from '@/api/client'
import AuthLayout from '@/components/layout/AuthLayout'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import { Loader } from '@/components/ui/Atoms'

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const nav = useNavigate()
  const token = searchParams.get('token') || ''

  const [verifying, setVerifying] = useState(true)
  const [validToken, setValidToken] = useState(false)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    ;(async () => {
      if (!token) {
        setVerifying(false)
        setValidToken(false)
        return
      }
      try {
        await authApi.verifyResetToken(token)
        if (!cancelled) setValidToken(true)
      } catch {
        if (!cancelled) setValidToken(false)
      } finally {
        if (!cancelled) setVerifying(false)
      }
    })()
    return () => {
      cancelled = true
    }
  }, [token])

  const submit = async (e) => {
    e.preventDefault()
    const { newPassword, confirmPassword } = Object.fromEntries(
      new FormData(e.currentTarget)
    )
    if (newPassword !== confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp')
      return
    }
    if (newPassword.length < 6) {
      toast.error('Mật khẩu cần ít nhất 6 ký tự')
      return
    }
    setLoading(true)
    try {
      await authApi.resetPassword(token, newPassword)
      toast.success('Đặt lại mật khẩu thành công! Vui lòng đăng nhập.')
      nav('/login', { replace: true })
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setLoading(false)
    }
  }

  if (verifying) {
    return (
      <AuthLayout title="Đang xác thực..." subtitle="Vui lòng đợi trong giây lát">
        <Loader />
      </AuthLayout>
    )
  }

  if (!validToken) {
    return (
      <AuthLayout
        title="Link không hợp lệ"
        subtitle="Link đặt lại mật khẩu đã hết hạn hoặc không tồn tại."
        footer={
          <Link to="/forgot-password" className="text-ink-700 hover:text-ink-900">
            Gửi link mới
          </Link>
        }
      >
        <div className="rounded-xl border border-red-200 bg-danger-50 p-4 flex gap-3">
          <AlertCircle className="h-5 w-5 text-danger-600 shrink-0 mt-0.5" />
          <p className="text-sm text-danger-700">
            Vui lòng yêu cầu link đặt lại mật khẩu mới.
          </p>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Đặt lại mật khẩu"
      subtitle="Chọn mật khẩu mới cho tài khoản của bạn."
      footer={
        <Link to="/login" className="text-ink-700 hover:text-ink-900">
          ← Quay lại đăng nhập
        </Link>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <Input
          type="password"
          label="Mật khẩu mới"
          name="newPassword"
          required
          placeholder="Tối thiểu 6 ký tự"
        />
        <Input
          type="password"
          label="Xác nhận mật khẩu"
          name="confirmPassword"
          required
          placeholder="Nhập lại"
        />
        <Button type="submit" loading={loading} className="w-full" size="lg">
          <CheckCircle2 className="h-4 w-4" />
          Đặt lại mật khẩu
        </Button>
      </form>
    </AuthLayout>
  )
}
