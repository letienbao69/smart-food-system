import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import { authApi } from '@/api/auth'
import { useAuth } from '@/store/auth'
import { errMsg } from '@/api/client'
import AuthLayout from '@/components/layout/AuthLayout'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'

export default function RegisterPage() {
  const nav = useNavigate()
  const login = useAuth((s) => s.login)
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const data = Object.fromEntries(new FormData(e.currentTarget))
    if (data.password !== data.confirmPassword) {
      toast.error('Mật khẩu xác nhận không khớp')
      return
    }
    if (data.password.length < 6) {
      toast.error('Mật khẩu cần ít nhất 6 ký tự')
      return
    }
    setLoading(true)
    try {
      await authApi.register({
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        phone: data.phone || null,
      })
      toast.success('Tạo tài khoản thành công!')
      // auto-login
      try {
        await login(data.email, data.password)
        nav('/health', { replace: true })
      } catch {
        nav('/login', { replace: true })
      }
    } catch (err) {
      toast.error(errMsg(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Tạo tài khoản"
      subtitle="Chỉ mất 30 giây. Sau khi đăng ký, cập nhật BMI để nhận gợi ý món phù hợp."
      footer={
        <>
          Đã có tài khoản?{' '}
          <Link to="/login" className="font-medium text-ink-900 hover:underline">
            Đăng nhập
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <Input label="Họ và tên" name="fullName" required placeholder="Nguyễn Văn A" />
        <Input
          label="Email"
          name="email"
          type="email"
          required
          autoComplete="email"
          placeholder="ban@email.com"
        />
        <Input
          label="Số điện thoại (tuỳ chọn)"
          name="phone"
          type="tel"
          placeholder="0901234567"
        />
        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Mật khẩu"
            name="password"
            type="password"
            required
            placeholder="Tối thiểu 6 ký tự"
          />
          <Input
            label="Xác nhận"
            name="confirmPassword"
            type="password"
            required
            placeholder="Nhập lại"
          />
        </div>

        <Button type="submit" loading={loading} className="w-full" size="lg">
          Đăng ký
        </Button>
      </form>
    </AuthLayout>
  )
}
