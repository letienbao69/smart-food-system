import { Link } from 'react-router-dom'
import { useState } from 'react'
import { Instagram, Facebook, Youtube, Send, HelpCircle, Truck } from 'lucide-react'
import toast from 'react-hot-toast'

export default function Footer() {
  const [email, setEmail] = useState('')
  const subscribe = (e) => {
    e.preventDefault()
    if (!email.trim()) return
    toast.success('Cảm ơn bạn đã đăng ký nhận ưu đãi!')
    setEmail('')
  }

  return (
    <footer>
      {/* ── Dải đăng ký email — kem thanh lịch ── */}
      <div className="bg-warm-100 border-t border-ink-200">
        <div className="mx-auto grid max-w-7xl gap-6 px-6 py-10 lg:px-8 md:grid-cols-3 items-center">
          <form onSubmit={subscribe} className="md:col-span-2 flex flex-col sm:flex-row items-start sm:items-center gap-4">
            <div className="shrink-0">
              <span className="eyebrow">Bản tin</span>
              <p className="font-display text-xl font-semibold text-ink-900 leading-tight mt-1">
                Nhận ưu đãi &amp; thực đơn mới
              </p>
            </div>
            <div className="flex w-full max-w-md">
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                placeholder="Email của bạn"
                className="flex-1 rounded-l-full border border-ink-300 bg-white px-4 py-2.5 text-sm focus:outline-none focus:border-accent-400" />
              <button type="submit"
                className="inline-flex items-center gap-1.5 rounded-r-full bg-accent-700 px-5 text-sm font-semibold text-warm-50 hover:bg-accent-800 transition">
                <Send className="h-4 w-4" /> Đăng ký
              </button>
            </div>
          </form>
          <div className="flex items-center justify-start md:justify-end gap-6 text-sm">
            <Link to="/contact" className="inline-flex items-center gap-2 text-ink-700 hover:text-accent-700">
              <HelpCircle className="h-5 w-5" /> Hỗ trợ
            </Link>
            <Link to="/reservations" className="inline-flex items-center gap-2 text-ink-700 hover:text-accent-700">
              <Truck className="h-5 w-5" /> Theo dõi đặt bàn
            </Link>
          </div>
        </div>
      </div>

      {/* ── Footer chính — espresso sang trọng ── */}
      <div className="text-warm-50" style={{ background: 'linear-gradient(160deg,#125b30 0%,#0e4926 100%)' }}>
        <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
          <div className="grid gap-10 md:grid-cols-5">
            {/* Brand */}
            <div className="md:col-span-2">
              <Link to="/" className="flex items-center gap-2.5 mb-4">
                <div className="grid h-10 w-10 place-items-center rounded-full border border-accent-300/60 bg-accent-700 text-accent-200">
                  <span className="font-display text-lg font-semibold leading-none">S</span>
                </div>
                <div className="leading-none">
                  <span className="font-display text-2xl font-semibold">Smart<span className="italic text-accent-200">Food</span></span>
                  <span className="block text-[9px] uppercase tracking-[0.32em] text-accent-300 mt-0.5">Cuisine &amp; Wellness</span>
                </div>
              </Link>
              <p className="text-sm text-warm-50/60 max-w-sm leading-relaxed">
                Nhà hàng đặt bàn và đánh giá ẩm thực — gợi ý món ăn tinh tế theo
                chỉ số sức khỏe, mang đến trải nghiệm hài hòa giữa hương vị và dinh dưỡng.
              </p>
              <div className="flex gap-2 mt-5">
                <a className="grid h-9 w-9 place-items-center rounded-full border border-warm-50/15 text-warm-50/80 hover:border-accent-300/60 hover:text-accent-200 transition" href="#"><Instagram className="h-4 w-4" /></a>
                <a className="grid h-9 w-9 place-items-center rounded-full border border-warm-50/15 text-warm-50/80 hover:border-accent-300/60 hover:text-accent-200 transition" href="#"><Youtube className="h-4 w-4" /></a>
                <a className="grid h-9 w-9 place-items-center rounded-full border border-warm-50/15 text-warm-50/80 hover:border-accent-300/60 hover:text-accent-200 transition" href="#"><Facebook className="h-4 w-4" /></a>
              </div>
            </div>

            <FooterCol title="Khám phá" links={[
              ['Thực đơn', '/foods'],
              ['Đặt bàn', '/reserve'],
              ['Gợi ý AI sức khỏe', '/health'],
              ['Món yêu thích', '/wishlist'],
            ]} />

            <FooterCol title="Tài khoản" links={[
              ['Hồ sơ', '/profile'],
              ['Lượt đặt bàn', '/reservations'],
              ['Đơn món của tôi', '/orders'],
              ['Thông báo', '/notifications'],
            ]} />

            <FooterCol title="Hỗ trợ" links={[
              ['Liên hệ & phản ánh', '/contact'],
              ['Trung tâm hỗ trợ', '/contact'],
              ['Hotline: 1900 1234', '/contact'],
            ]} />
          </div>

          <div className="mt-12 border-t border-warm-50/15 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-warm-50/50">
            <p>© {new Date().getFullYear()} SmartFood — Đồ án tốt nghiệp. All Rights Reserved.</p>
            <div className="flex gap-5">
              <Link to="/contact" className="hover:text-accent-200 transition">Điều khoản</Link>
              <Link to="/contact" className="hover:text-accent-200 transition">Bảo mật</Link>
              <Link to="/contact" className="hover:text-accent-200 transition">Trợ giúp</Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}

function FooterCol({ title, links }) {
  return (
    <div>
      <h4 className="text-[11px] font-semibold uppercase tracking-[0.18em] text-accent-300 mb-4">{title}</h4>
      <ul className="space-y-2.5 text-sm text-warm-50/65">
        {links.map(([label, to]) => (
          <li key={label}><Link to={to} className="hover:text-accent-200 transition">{label}</Link></li>
        ))}
      </ul>
    </div>
  )
}
