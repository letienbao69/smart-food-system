import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Sparkles, ArrowRight, Activity, Heart,
  Salad, ShieldCheck, Star, Clock, Flame,
  ChevronRight, Zap, Quote, Utensils, CalendarClock,
} from 'lucide-react'
import { foodsApi, categoriesApi } from '@/api/foods'
import { reviewsApi } from '@/api/misc'
import { healthApi } from '@/api/health'
import FoodCard from '@/components/common/FoodCard'
import { Skeleton } from '@/components/ui/Atoms'
import { useAuth } from '@/store/auth'
import { useWishlistIds } from '@/hooks/useWishlistIds'

// BMI helpers
function bmiLabel(bmi) {
  if (!bmi) return null
  if (bmi < 18.5) return { text: 'Thiếu cân', color: 'text-blue-600',   bg: 'bg-blue-50',   ring: 'ring-blue-200' }
  if (bmi < 25)   return { text: 'Bình thường', color: 'text-success-600', bg: 'bg-success-50', ring: 'ring-green-200' }
  if (bmi < 30)   return { text: 'Thừa cân',  color: 'text-amber-600',  bg: 'bg-amber-50',  ring: 'ring-amber-200' }
  return           { text: 'Béo phì',    color: 'text-danger-600',  bg: 'bg-danger-50',  ring: 'ring-red-200' }
}

function fmt(n) { return n ? Math.round(n).toLocaleString('vi-VN') : '—' }

// Chọn emoji phù hợp với tên danh mục (cho lưới "Danh mục nổi bật")
function categoryEmoji(name = '') {
  const n = name.toLowerCase()
  if (n.includes('khai vị')) return '🥗'
  if (n.includes('chính')) return '🍛'
  if (n.includes('cơm')) return '🍚'
  if (n.includes('mì') || n.includes('bún') || n.includes('phở')) return '🍜'
  if (n.includes('lẩu')) return '🍲'
  if (n.includes('nướng')) return '🍢'
  if (n.includes('hải sản')) return '🦐'
  if (n.includes('chay')) return '🥬'
  if (n.includes('salad')) return '🥙'
  if (n.includes('tráng miệng') || n.includes('ngọt')) return '🍮'
  if (n.includes('uống') || n.includes('trà') || n.includes('nước')) return '🥤'
  if (n.includes('combo') || n.includes('set')) return '🍱'
  return '🍽️'
}

export default function HomePage() {
  const token   = useAuth((s) => s.token)
  const user    = useAuth((s) => s.user)
  const wishlist = useWishlistIds()

  const { data: foods, isLoading: foodsLoading } = useQuery({
    queryKey: ['foods', 'home-list'],
    queryFn: () => foodsApi.list(),
  })

  const { data: featuredFoods } = useQuery({
    queryKey: ['foods', 'featured-home'],
    queryFn: () => foodsApi.featured(),
  })

  const { data: testimonials } = useQuery({
    queryKey: ['testimonials'],
    queryFn: reviewsApi.testimonials,
    retry: false,
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.list,
  })

  // Fetch health analysis khi đã đăng nhập — cần data thật cho Hero card
  const { data: analysis } = useQuery({
    queryKey: ['health-analysis'],
    queryFn: healthApi.getAnalysis,
    enabled: !!token,
    retry: false,
    staleTime: 5 * 60_000,
  })

  const { data: healthRecs } = useQuery({
    queryKey: ['health-recs-home'],
    queryFn: () => healthApi.getRecommendations(3, false),
    enabled: !!token && !!analysis?.bmi,
    retry: false,
    staleTime: 5 * 60_000,
  })

  const allFoods = (foods || [])
    .filter((f) => f.status === 'AVAILABLE' || !f.status)
    .slice(0, 8)
  const featured = (featuredFoods || [])
    .filter((f) => f.status === 'AVAILABLE' || !f.status)
    .slice(0, 8)

  const bmi     = analysis?.bmi ? Number(analysis.bmi).toFixed(1) : null
  const bmiMeta = bmiLabel(bmi)
  const hasHealthData = !!analysis?.bmr

  // Fallback static card items when no health data
  const staticRecs = [
    { name: 'Salad ức gà sốt mè',       cal: '320 kcal', score: 92 },
    { name: 'Cá hồi áp chảo bông cải',  cal: '480 kcal', score: 89 },
    { name: 'Cơm gạo lứt rau củ',        cal: '520 kcal', score: 85 },
  ]
  const heroRecs = healthRecs?.length > 0
    ? healthRecs.slice(0, 3).map((r) => ({ name: r.name, cal: `${r.calories || '—'} kcal`, score: r.score || r.healthScore || '—' }))
    : staticRecs

  return (
    <div className="bg-white">

      {/* ═══ HERO ═══════════════════════════════════════════════ */}
      <section className="relative overflow-hidden">
        {/* Banner nền ẩm thực */}
        <div className="pointer-events-none absolute inset-0">
          <img
            src="https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1920&q=70"
            alt=""
            className="h-full w-full object-cover"
            onError={(e) => { e.target.style.display = 'none' }}
          />
          {/* Overlay kem ấm, tinh tế — để ảnh hiện rõ bên phải */}
          <div className="absolute inset-0" style={{ background: 'linear-gradient(105deg,#f6faf7 0%,rgba(247,244,237,0.92) 38%,rgba(247,244,237,0.55) 60%,rgba(247,244,237,0.1) 100%)' }} />
          <div className="absolute inset-0 bg-gradient-to-t from-[#f6faf7]/40 via-transparent to-transparent" />
        </div>

        {/* Họa tiết mờ */}
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute right-[-120px] top-[-80px] h-[500px] w-[500px] rounded-full bg-gold-200/25 blur-3xl" />
          <div className="absolute left-[-80px] bottom-[-60px] h-[400px] w-[400px] rounded-full bg-accent-200/30 blur-3xl" />
        </div>

        <div className="relative mx-auto max-w-7xl px-6 py-20 lg:px-8 lg:py-28">
          <div className="grid items-center gap-14 lg:grid-cols-2">

            {/* Left copy */}
            <div>
              <div className="flex items-center gap-3">
                <span className="rule-gold" />
                <span className="eyebrow">Cuisine &amp; Wellness · Est. 2024</span>
              </div>

              <h1 className="mt-5 font-display text-[3.25rem] font-semibold leading-[1.05] tracking-tight text-ink-900 lg:text-7xl">
                Ẩm thực tinh tế,
                <br /><span className="italic font-normal text-accent-700">vì sức khỏe</span> của bạn.
              </h1>

              <p className="mt-6 max-w-lg text-lg leading-relaxed text-ink-600">
                Mỗi món ăn được chăm chút và phân tích dinh dưỡng tỉ mỉ. Cập nhật chỉ số cơ thể,
                để SmartFood gợi ý thực đơn hài hòa giữa hương vị và sức khỏe của riêng bạn.
              </p>

              <div className="mt-9 flex flex-wrap gap-3">
                <Link to={token ? '/health' : '/login'} className="btn-primary">
                  <Sparkles className="h-4 w-4" />
                  {token ? 'Xem gợi ý AI' : 'Khám phá gợi ý AI'}
                </Link>
                <Link to="/reserve" className="btn-secondary">
                  Đặt bàn <ArrowRight className="h-4 w-4" />
                </Link>
              </div>

              <div className="mt-10 flex flex-wrap items-center gap-x-7 gap-y-3 text-xs uppercase tracking-[0.14em] text-ink-500">
                <span className="flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-gold-500" /> Thanh toán an toàn</span>
                <span className="flex items-center gap-2"><Activity className="h-4 w-4 text-gold-500" /> Tích hợp Gemini AI</span>
                <span className="flex items-center gap-2"><Heart className="h-4 w-4 text-gold-500" /> Theo dõi sức khỏe</span>
              </div>
            </div>

            {/* Right: real BMI card */}
            <div className="relative mx-auto w-full max-w-md">
              <div className="absolute -inset-3 rounded-3xl bg-gradient-to-br from-accent-200/30 to-accent-100/30 blur-2xl" />
              <div className="relative rounded-2xl border border-accent-200 bg-white p-6 shadow-pop overflow-hidden">
                <div className="absolute inset-x-0 top-0 h-1.5 bg-accent-500" />

                {/* Header */}
                <div className="flex items-start justify-between">
                  <div>
                    <p className="eyebrow !text-ink-400">
                      {token && hasHealthData ? `Sức khỏe của ${user?.fullName?.split(' ').pop() || 'bạn'}` : 'Phân tích sức khỏe'}
                    </p>
                    {bmi ? (
                      <>
                        <p className="mt-2 font-display text-5xl font-semibold tabular text-ink-900">{bmi}</p>
                        <span className={`mt-1 inline-block text-sm font-semibold ${bmiMeta?.color}`}>{bmiMeta?.text}</span>
                      </>
                    ) : (
                      <>
                        <p className="mt-2 font-display text-5xl font-semibold tabular text-ink-900">—</p>
                        <Link to={token ? '/health' : '/login'}
                          className="mt-1 inline-block text-xs text-accent-700 font-medium hover:underline">
                          {token ? 'Cập nhật hồ sơ sức khỏe →' : 'Đăng nhập để xem →'}
                        </Link>
                      </>
                    )}
                  </div>
                  <div className={`grid h-12 w-12 place-items-center rounded-full ${bmiMeta?.bg || 'bg-warm-100'} ring-1 ${bmiMeta?.ring || 'ring-ink-200'}`}>
                    <Heart className={`h-5 w-5 ${bmiMeta?.color || 'text-gold-500'}`} />
                  </div>
                </div>

                {/* Stats grid */}
                <div className="my-5 grid grid-cols-3 gap-3 text-center">
                  {[
                    ['BMR', hasHealthData ? fmt(analysis?.bmr) : '—'],
                    ['TDEE', hasHealthData ? fmt(analysis?.tdee) : '—'],
                    ['Mục tiêu', hasHealthData ? fmt(analysis?.targetCalories ?? analysis?.tdee) : '—'],
                  ].map(([k, v]) => (
                    <div key={k} className="rounded-xl bg-warm-50 border border-ink-100 p-3">
                      <p className="text-[10px] uppercase tracking-wider text-ink-400 font-medium">{k}</p>
                      <p className="mt-0.5 font-bold text-sm text-ink-900 tabular">
                        {v}<span className="text-[10px] text-ink-400 font-normal"> kcal</span>
                      </p>
                    </div>
                  ))}
                </div>

                {/* Recommended foods */}
                <div className="space-y-2">
                  <p className="text-[10px] uppercase tracking-wider text-ink-400 font-medium mb-2">
                    {hasHealthData ? 'Gợi ý phù hợp cho bạn' : 'Món ăn nổi bật'}
                  </p>
                  {heroRecs.map((r) => (
                    <div key={r.name}
                      className="flex items-center justify-between rounded-xl border border-ink-100 bg-warm-50/70 px-3 py-2.5 hover:bg-warm-100 transition">
                      <div>
                        <p className="text-sm font-medium text-ink-900 leading-tight">{r.name}</p>
                        <p className="text-[11px] text-ink-400 mt-0.5">{r.cal}</p>
                      </div>
                      <span className="ml-3 shrink-0 rounded-lg bg-accent-50 border border-accent-200 px-2 py-0.5 text-xs font-bold text-accent-700">
                        {r.score}/100
                      </span>
                    </div>
                  ))}
                </div>

                {token && !hasHealthData && (
                  <Link to="/health" className="btn-primary mt-4 w-full">
                    <Zap className="h-4 w-4" />
                    Cập nhật hồ sơ sức khỏe
                  </Link>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ═══ FEATURE STRIP ══════════════════════════════════════ */}
      <section className="border-y border-ink-200 bg-warm-50/60">
        <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
          <div className="grid gap-10 md:grid-cols-3">
            {[
              { icon: Activity, title: 'Cá nhân hoá theo BMI', desc: 'Nhập chiều cao, cân nặng, bệnh nền — chấm điểm từng món theo cơ thể bạn.' },
              { icon: Salad,    title: 'Đủ nhãn dinh dưỡng',   desc: 'Mỗi món có calo, đạm, béo, tinh bột rõ ràng. Lọc low-sugar, high-protein...' },
              { icon: Sparkles, title: 'Tư vấn bằng AI',        desc: 'Chatbot Gemini giải thích vì sao món này phù hợp và đưa lời khuyên dinh dưỡng.' },
            ].map((f) => (
              <div key={f.title} className="flex gap-4">
                <div className="shrink-0 grid h-12 w-12 place-items-center rounded-full border border-gold-300/50 bg-white text-accent-700">
                  <f.icon className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="font-display text-lg font-semibold text-ink-900">{f.title}</h3>
                  <p className="mt-1 text-sm text-ink-500 leading-relaxed">{f.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ 4 Ô ĐIỀU HƯỚNG — thanh lịch ════════════════════════ */}
      <section className="bg-white">
        <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {[
              { to: '/health',  Icon: Activity,      eyebrow: 'Sức khỏe',   title: 'Gợi ý món theo BMI',   desc: 'Nhập chỉ số — nhận gợi ý phù hợp cơ thể', cta: 'Khám phá' },
              { to: '/reserve', Icon: CalendarClock, eyebrow: 'Trải nghiệm', title: 'Đặt bàn trực tuyến',   desc: 'Giữ chỗ trước, không phải chờ đợi',        cta: 'Đặt bàn' },
              { to: '/foods',   Icon: Sparkles,      eyebrow: 'Ưu đãi',     title: 'Combo & Món đặc sắc',  desc: 'Khám phá món chọn lọc mỗi ngày',           cta: 'Xem món' },
              { to: '/foods',   Icon: Star,          eyebrow: 'Cộng đồng',  title: 'Đánh giá thực khách',  desc: 'Chọn món ngon qua review thật',            cta: 'Xem đánh giá' },
            ].map(({ to, Icon, eyebrow, title, desc, cta }) => (
              <Link key={title + eyebrow} to={to}
                className="group relative overflow-hidden rounded-2xl border border-ink-200/70 bg-white p-6 transition-all hover:-translate-y-1 hover:border-gold-300/70 hover:shadow-card">
                <div className="absolute inset-x-0 top-0 h-0.5 bg-gradient-to-r from-transparent via-gold-400/70 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
                <div className="grid h-11 w-11 place-items-center rounded-full border border-gold-300/50 bg-warm-50 text-accent-700">
                  <Icon className="h-5 w-5" />
                </div>
                <p className="eyebrow mt-4">{eyebrow}</p>
                <p className="mt-1 font-display text-xl font-semibold text-ink-900 leading-tight">{title}</p>
                <p className="mt-2 text-xs text-ink-500 leading-relaxed">{desc}</p>
                <span className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-accent-700">
                  {cta} <ChevronRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
                </span>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ CATEGORIES (lưới tròn — Featured Categories) ═══════ */}
      {categories?.length > 0 && (
        <section className="bg-warm-50/60 border-y border-ink-200">
          <div className="mx-auto max-w-7xl px-6 py-16 lg:px-8">
            <div className="mb-12 text-center">
              <div className="flex items-center justify-center gap-3">
                <span className="rule-gold" />
                <span className="eyebrow">Thực đơn</span>
                <span className="rule-gold" />
              </div>
              <h2 className="mt-3 font-display text-4xl font-semibold text-ink-900">Danh mục nổi bật</h2>
            </div>
            <div className="grid grid-cols-3 gap-6 sm:grid-cols-4 lg:grid-cols-6">
              {categories.slice(0, 12).map((c) => (
                <Link key={c.id} to={`/foods?categoryId=${c.id}`}
                  className="group flex flex-col items-center text-center">
                  <div className="grid h-24 w-24 place-items-center rounded-full bg-white border border-ink-200 group-hover:border-gold-400 group-hover:bg-accent-50 transition-all">
                    <span className="text-4xl leading-none select-none" aria-hidden="true">{categoryEmoji(c.name)}</span>
                  </div>
                  <span className="mt-3 text-sm font-medium text-ink-700 group-hover:text-accent-700 leading-tight">{c.name}</span>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ═══ TOP FOODS (danh sách món) ═══════════════════════════ */}
      <section className="bg-white">
        <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
          <div className="mb-8 flex items-end justify-between">
            <div>
              <p className="eyebrow !text-gold-600">Thực đơn</p>
              <h2 className="mt-1 font-display text-2xl font-bold text-ink-900">Danh sách món ăn</h2>
            </div>
            <Link to="/foods" className="text-sm font-medium text-ink-600 hover:text-ink-900 inline-flex items-center gap-1">
              Xem tất cả <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {foodsLoading
              ? Array.from({ length: 8 }).map((_, i) => (
                  <div key={i} className="rounded-2xl overflow-hidden border border-ink-200">
                    <Skeleton className="aspect-[4/3] rounded-none" />
                    <div className="p-4 space-y-2">
                      <Skeleton className="h-4 w-3/4" />
                      <Skeleton className="h-3 w-1/2" />
                    </div>
                  </div>
                ))
              : allFoods.map((f) => <FoodCard key={f.id} food={f} liked={wishlist.isLiked(f.id)} />)}
          </div>
        </div>
      </section>

      {/* ═══ MÓN ĂN NỔI BẬT (thay cho Đầu bếp) ═══════════════════ */}
      {featured.length > 0 && (
        <section className="bg-warm-50 border-t border-ink-200">
          <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
            <div className="mb-8 flex items-end justify-between">
              <div>
                <p className="eyebrow">Đặc sắc</p>
                <h2 className="mt-1 font-display text-2xl font-bold text-ink-900">Món ăn nổi bật</h2>
              </div>
              <Link to="/foods" className="text-sm font-medium text-ink-600 hover:text-ink-900 inline-flex items-center gap-1">
                Xem tất cả <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </div>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {featured.map((f) => <FoodCard key={f.id} food={f} liked={wishlist.isLiked(f.id)} />)}
            </div>
          </div>
        </section>
      )}

      {/* ═══ KHÁCH HÀNG NÓI GÌ ══════════════════════════════════ */}
      {testimonials?.length > 0 && (
        <section className="bg-white border-t border-ink-200">
          <div className="mx-auto max-w-7xl px-6 py-14 lg:px-8">
            <div className="text-center mb-10">
              <p className="eyebrow !text-gold-600">Đánh giá</p>
              <h2 className="mt-1 font-display text-3xl font-bold text-ink-900">Khách hàng nói gì?</h2>
            </div>
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {testimonials.slice(0, 6).map((t) => (
                <div key={t.id} className="card p-5 relative">
                  <Quote className="h-7 w-7 text-gold-200 absolute top-4 right-4" />
                  <div className="flex gap-0.5 mb-3">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star key={i} className="h-4 w-4 fill-gold-400 text-gold-400" />
                    ))}
                  </div>
                  <p className="text-sm text-ink-700 leading-relaxed line-clamp-4">"{t.comment}"</p>
                  <div className="mt-4 flex items-center gap-2.5">
                    <div className="h-9 w-9 rounded-full bg-ink-900 text-white grid place-items-center text-xs font-bold">
                      {(t.userName?.[0] || '?').toUpperCase()}
                    </div>
                    <div>
                      <p className="text-sm font-medium text-ink-900">{t.userName}</p>
                      {t.foodName && <p className="text-xs text-ink-500">{t.foodName}</p>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ═══ AI CTA ══════════════════════════════════════════════ */}
      <section className="bg-white">
        <div className="mx-auto max-w-7xl px-6 py-16 lg:px-8">
          <div className="relative overflow-hidden rounded-3xl border border-white/20"
            style={{ background: 'linear-gradient(120deg,#1a8d46 0%,#1d9b50 55%,#23a95b 100%)' }}>
            {/* Ảnh nền: món ăn + phân tích sức khỏe bằng AI */}
            <div className="pointer-events-none absolute inset-y-0 right-0 w-full lg:w-3/5">
              <img
                src="/ai-nutrition-banner.png"
                alt=""
                className="h-full w-full object-cover"
                onError={(e) => { e.target.style.display = 'none' }}
              />
              <div className="absolute inset-0" style={{ background: 'linear-gradient(90deg,#1a8d46 0%,rgba(26,141,70,0.90) 32%,rgba(26,141,70,0.34) 72%,rgba(26,141,70,0) 100%)' }} />
            </div>
            <div className="pointer-events-none absolute -right-10 -top-10 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
            <div className="pointer-events-none absolute left-1/3 -bottom-16 h-72 w-72 rounded-full bg-accent-200/20 blur-3xl" />

            <div className="relative px-8 py-14 lg:px-14 lg:py-20 max-w-2xl">
              <div className="flex items-center gap-3">
                <span className="inline-block h-px w-10 bg-white/70" />
                <span className="text-[11px] font-semibold uppercase tracking-[0.22em] text-white/90">Powered by Gemini AI</span>
              </div>
              <h2 className="mt-4 font-display text-4xl lg:text-5xl font-semibold text-warm-50 leading-tight" style={{ textShadow: '0 2px 8px rgba(0,0,0,0.3)' }}>
                Để AI chọn món <span className="italic font-normal text-white">giúp bạn</span>
              </h2>
              <p className="mt-4 text-warm-50/85 leading-relaxed max-w-lg" style={{ textShadow: '0 1px 4px rgba(0,0,0,0.25)' }}>
                Cập nhật hồ sơ sức khoẻ một lần. Mỗi lần đặt món sau đó, bạn sẽ thấy
                những lựa chọn được chấm điểm theo cơ thể và mục tiêu của riêng bạn.
              </p>
              <Link to={token ? '/health' : '/login'} className="btn bg-white text-accent-700 hover:bg-warm-50 mt-7 shadow-subtle">
                {token ? 'Mở phân tích AI' : 'Bắt đầu ngay'}
                <ArrowRight className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </div>
      </section>
    </div>
  )
}
