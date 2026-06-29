import { Link } from 'react-router-dom'
import { Sparkles, ArrowLeft } from 'lucide-react'

export default function AuthLayout({ children, title, subtitle, footer }) {
  return (
    <div className="min-h-screen grid lg:grid-cols-[1fr_520px] bg-white">
      {/* Left visual side */}
      <div className="relative hidden lg:flex flex-col justify-between p-12 overflow-hidden"
        style={{ background: 'linear-gradient(150deg,#1a8d46 0%,#15803d 55%,#0f6b32 100%)' }}>
        {/* Ảnh món ăn làm nền, phủ lớp xanh để chữ vẫn rõ */}
        <img
          src="/auth-food.jpg"
          alt=""
          className="absolute inset-0 h-full w-full object-cover"
          onError={(e) => { e.target.src = '/ai-nutrition-banner.png' }}
        />
        <div className="absolute inset-0" style={{ background: 'linear-gradient(150deg,rgba(13,74,38,0.90) 0%,rgba(15,107,50,0.78) 55%,rgba(8,46,26,0.92) 100%)' }} />
        <div className="absolute inset-0 bg-dot-grid opacity-[0.06]" />
        <div className="absolute -right-24 -top-24 h-96 w-96 rounded-full bg-white/15 blur-3xl" />
        <div className="absolute -left-24 -bottom-24 h-96 w-96 rounded-full bg-white/10 blur-3xl" />

        <div className="relative">
          <Link to="/" className="inline-flex items-center gap-2">
            <div className="grid h-9 w-9 place-items-center rounded-lg bg-white/20 text-white">
              <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
                <path d="M5 7h14v2H5zM5 12h10v2H5zM5 17h14v2H5z" />
                <circle cx="18" cy="13" r="1.2" />
              </svg>
            </div>
            <span className="font-display text-lg font-bold text-white">Smart Food</span>
          </Link>
        </div>

        <div className="relative max-w-md">
          <Sparkles className="h-8 w-8 text-white mb-4" />
          <h2 className="font-display text-4xl font-bold text-white leading-tight">
            Bữa ăn được cá nhân hoá theo cơ thể bạn.
          </h2>
          <p className="mt-4 text-white/85 leading-relaxed">
            Nhập chiều cao, cân nặng và mục tiêu — AI sẽ chọn món phù hợp với BMI
            và tình trạng sức khỏe của bạn mỗi ngày.
          </p>

          <div className="mt-8 flex gap-6">
            <Stat value="500+" label="Món ăn dinh dưỡng" />
            <Stat value="AI" label="Gợi ý cá nhân hoá" />
            <Stat value="24/7" label="Hỗ trợ chatbot" />
          </div>
        </div>

        <div className="relative">
          <Link
            to="/"
            className="inline-flex items-center gap-1.5 text-sm text-white/80 hover:text-white transition"
          >
            <ArrowLeft className="h-3.5 w-3.5" />
            Về trang chủ
          </Link>
        </div>
      </div>

      {/* Right form side */}
      <div className="flex items-center justify-center p-6 sm:p-10 lg:p-12">
        <div className="w-full max-w-md">
          <Link to="/" className="lg:hidden inline-flex items-center gap-2 mb-8">
            <div className="grid h-9 w-9 place-items-center rounded-lg bg-ink-900 text-accent-400">
              <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
                <path d="M5 7h14v2H5zM5 12h10v2H5zM5 17h14v2H5z" />
                <circle cx="18" cy="13" r="1.2" />
              </svg>
            </div>
            <span className="font-display text-lg font-bold">Smart Food</span>
          </Link>

          <h1 className="font-display text-3xl font-bold tracking-tight text-ink-900">
            {title}
          </h1>
          {subtitle && (
            <p className="mt-2 text-sm text-ink-600">{subtitle}</p>
          )}

          <div className="mt-8">{children}</div>

          {footer && <div className="mt-6 text-center text-sm">{footer}</div>}
        </div>
      </div>
    </div>
  )
}

function Stat({ value, label }) {
  return (
    <div>
      <p className="font-display text-2xl font-bold text-white tabular">{value}</p>
      <p className="mt-0.5 text-xs text-white/70">{label}</p>
    </div>
  )
}
