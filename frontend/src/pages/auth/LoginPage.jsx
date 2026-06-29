import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import { Mail, Lock, Eye, EyeOff } from 'lucide-react'
import toast from 'react-hot-toast'
import { useAuth } from '@/store/auth'
import { errMsg } from '@/api/client'
import AuthLayout from '@/components/layout/AuthLayout'
import { Input } from '@/components/ui/Input'
import Button from '@/components/ui/Button'

export default function LoginPage() {
  const nav = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const login = useAuth((s) => s.login)
  const [loading, setLoading] = useState(false)
  const [showPwd, setShowPwd] = useState(false)

  useEffect(() => {
    if (searchParams.get('expired')) {
      toast('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.', { icon: '⏱️' })
    }
  }, [searchParams])

  const submit = async (e) => {
    e.preventDefault()
    const { email, password } = Object.fromEntries(new FormData(e.currentTarget))
    setLoading(true)
    try {
      const user = await login(email, password)
      toast.success(`Xin chào, ${user?.fullName || 'bạn'}!`)
      // Admin và nhân viên (STAFF) vào khu quản trị; khách vào trang trước đó.
      const roleNames = [
        typeof user?.role === 'string' ? user.role : null,
        ...(user?.roles || []).map((r) => (typeof r === 'string' ? r : r?.name)),
      ].filter(Boolean).map((s) => s.toUpperCase())
      const goAdmin = roleNames.includes('ADMIN') || roleNames.includes('STAFF')
      if (goAdmin) {
        nav('/admin', { replace: true })
      } else {
        const from = location.state?.from?.pathname || '/'
        nav(from, { replace: true })
      }
    } catch (err) {
      toast.error(errMsg(err, 'Email hoặc mật khẩu không đúng'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Chào mừng trở lại"
      subtitle="Đăng nhập để tiếp tục đặt món và xem gợi ý AI dành riêng cho bạn."
      footer={
        <>
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-medium text-ink-900 hover:underline">
            Đăng ký
          </Link>
        </>
      }
    >
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className="label">Email</label>
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
            <input
              name="email"
              type="email"
              autoComplete="email"
              required
              placeholder="ban@email.com"
              className="input pl-10"
            />
          </div>
        </div>

        <div>
          <div className="flex justify-between items-baseline">
            <label className="label">Mật khẩu</label>
          </div>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
            <input
              name="password"
              type={showPwd ? 'text' : 'password'}
              autoComplete="current-password"
              required
              placeholder="••••••••"
              className="input pl-10 pr-10"
            />
            <button
              type="button"
              onClick={() => setShowPwd(!showPwd)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 hover:text-ink-700"
            >
              {showPwd ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          </div>
        </div>

        <Button type="submit" loading={loading} className="w-full" size="lg">
          Đăng nhập
        </Button>
      </form>

      <p className="mt-6 text-center text-xs text-ink-500">
        Bằng việc đăng nhập, bạn đồng ý với{' '}
        <a className="underline hover:text-ink-900" href="#">Điều khoản</a> và{' '}
        <a className="underline hover:text-ink-900" href="#">Chính sách bảo mật</a>.
      </p>
    </AuthLayout>
  )
}
