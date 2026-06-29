import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useState, useRef } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  ShoppingBag, Heart, Search, Menu, X, User, LogOut,
  Sparkles, Package, Activity, ChevronDown, Bell, CalendarClock,
  CheckCheck, ArrowRight, ShoppingCart, CreditCard, Trash2,
  CheckCircle2, XCircle, Clock, Phone, HelpCircle, Tag, LayoutGrid,
} from 'lucide-react'
import { useAuth } from '@/store/auth'
import { useCartStore } from '@/store/cart'
import { notificationsApi } from '@/api/admin'
import { foodsApi } from '@/api/foods'
import { FoodImage } from '@/components/ui/Atoms'
import { cn, initials, formatDateTime } from '@/lib/utils'

const NAV = [
  { to: '/foods',    label: 'Thực đơn' },
  { to: '/reserve',  label: 'Đặt bàn', auth: true },
  { to: '/health',   label: 'Gợi ý AI', highlight: true },
  { to: '/orders',   label: 'Đơn món', auth: true },
  { to: '/wishlist', label: 'Yêu thích', auth: true },
  { to: '/contact',  label: 'Liên hệ' },
]

// Icon + màu theo loại thông báo
function notifMeta(n) {
  const type = n.type || ''
  if (type === 'ORDER_CREATED')         return { icon: ShoppingCart,  color: 'text-blue-500',    bg: 'bg-blue-50' }
  if (type === 'RESERVATION_CREATED')   return { icon: CalendarClock,  color: 'text-blue-500',    bg: 'bg-blue-50' }
  if (type === 'RESERVATION_STATUS_UPDATED' || type === 'DEPOSIT_CLAIMED')
                                        return { icon: CalendarClock,  color: 'text-violet-600',  bg: 'bg-violet-50' }
  if (type === 'ORDER_STATUS_UPDATED') {
    const msg = n.message || ''
    if (msg.includes('Hoàn thành'))     return { icon: CheckCircle2,  color: 'text-success-600', bg: 'bg-success-50' }
    if (msg.includes('Đã phục vụ'))     return { icon: Package,        color: 'text-cyan-600',    bg: 'bg-cyan-50' }
    if (msg.includes('hủy') || msg.includes('Hủy'))
                                        return { icon: XCircle,       color: 'text-danger-500',  bg: 'bg-danger-50' }
    return                               { icon: Clock,               color: 'text-amber-600',   bg: 'bg-amber-50' }
  }
  if (type === 'PAYMENT_UPDATED' || type === 'PAYMENT_CLAIMED')
                                        return { icon: CreditCard,    color: 'text-violet-600',  bg: 'bg-violet-50' }
  return                                 { icon: Bell,                color: 'text-ink-500',     bg: 'bg-ink-100' }
}

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const m = Math.floor(diff / 60000)
  if (m < 1)  return 'Vừa xong'
  if (m < 60) return `${m} phút trước`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} giờ trước`
  return `${Math.floor(h / 24)} ngày trước`
}

export default function Navbar() {
  const user        = useAuth((s) => s.user)
  const isAdminOrStaff = useAuth((s) => s.isAdminOrStaff())
  const logout      = useAuth((s) => s.logout)
  const token       = useAuth((s) => s.token)
  const count       = useCartStore((s) => s.count)
  const refreshCart = useCartStore((s) => s.refresh)
  const resetCart   = useCartStore((s) => s.reset)
  const nav         = useNavigate()
  const qc          = useQueryClient()

  const [mobileOpen, setMobileOpen] = useState(false)
  const [userMenu,   setUserMenu]   = useState(false)
  const [notifOpen,  setNotifOpen]  = useState(false)
  const [searchQ,    setSearchQ]    = useState('')
  // Gợi ý món khi gõ tìm kiếm (giống Google): hiện danh sách kèm ảnh + tên.
  const [showSuggest, setShowSuggest] = useState(false)
  const [debouncedQ,  setDebouncedQ]  = useState('')
  const searchBoxRef = useRef(null)

  // Trễ 250ms để tránh gọi API liên tục khi đang gõ.
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQ(searchQ.trim()), 250)
    return () => clearTimeout(t)
  }, [searchQ])

  const { data: suggestions = [] } = useQuery({
    queryKey: ['food-suggest', debouncedQ],
    queryFn: () => foodsApi.list({ keyword: debouncedQ }),
    enabled: debouncedQ.length >= 1,
    staleTime: 30_000,
  })

  // Đóng dropdown khi bấm ra ngoài.
  useEffect(() => {
    const onClickOutside = (e) => {
      if (searchBoxRef.current && !searchBoxRef.current.contains(e.target)) {
        setShowSuggest(false)
      }
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const gotoFood = (id) => {
    setShowSuggest(false)
    setSearchQ('')
    nav(`/foods/${id}`)
  }

  const userMenuRef  = useRef(null)
  const notifRef     = useRef(null)

  const submitSearch = (e) => {
    e?.preventDefault?.()
    setShowSuggest(false)
    const q = searchQ.trim()
    nav(q ? `/foods?keyword=${encodeURIComponent(q)}` : '/foods')
  }

  // Unread count — hỏi lại mỗi 30s, WS hook sẽ invalidate ngay khi có tin mới
  const { data: unreadCount = 0 } = useQuery({
    queryKey: ['my-notifications-count'],
    queryFn:  notificationsApi.myUnreadCount,
    enabled:  !!token,
    refetchInterval: 30_000,
    retry: false,
  })

  // Danh sách thông báo gần đây — chỉ fetch khi dropdown đang mở
  const { data: recentNotifs = [] } = useQuery({
    queryKey: ['my-notifications-recent'],
    queryFn:  notificationsApi.mine,
    enabled:  !!token && notifOpen,
    refetchInterval: notifOpen ? 10_000 : false,
    select: (list) => list.slice(0, 8),  // chỉ lấy 8 cái mới nhất
  })

  const markAll = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-notifications-count'] })
      qc.invalidateQueries({ queryKey: ['my-notifications-recent'] })
      qc.invalidateQueries({ queryKey: ['my-notifications'] })
    },
  })

  const markOne = useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-notifications-count'] })
      qc.invalidateQueries({ queryKey: ['my-notifications-recent'] })
      qc.invalidateQueries({ queryKey: ['my-notifications'] })
    },
  })

  const deleteOne = useMutation({
    mutationFn: notificationsApi.deleteOne,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['my-notifications-count'] })
      qc.invalidateQueries({ queryKey: ['my-notifications-recent'] })
      qc.invalidateQueries({ queryKey: ['my-notifications'] })
    },
  })

  useEffect(() => {
    if (token) refreshCart()
    else resetCart()
  }, [token])

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const close = (e) => {
      if (!userMenuRef.current?.contains(e.target))  setUserMenu(false)
      if (!notifRef.current?.contains(e.target))     setNotifOpen(false)
    }
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const handleLogout = () => {
    logout(); resetCart(); setUserMenu(false); nav('/')
  }

  const handleNotifClick = (notif) => {
    // Đánh dấu đã đọc
    if (!notif.readStatus) markOne.mutate(notif.id)
    setNotifOpen(false)
    // Nếu là thông báo đơn hàng, navigate đến đơn đó
    if (notif.referenceType === 'ORDER' && notif.referenceId) {
      nav(`/orders/${notif.referenceId}`)
    } else {
      nav('/notifications')
    }
  }

  return (
    <header className="sticky top-0 z-40 bg-white">
      {/* ── Top bar: thanh lịch, nền espresso ── */}
      <div className="bg-ink-900 text-[12px] text-warm-50/80">
        <div className="mx-auto flex h-9 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-4">
            <Link to="/contact" className="inline-flex items-center gap-1.5 hover:text-gold-300 transition-colors">
              <HelpCircle className="h-3.5 w-3.5" /> Trung tâm hỗ trợ
            </Link>
            <span className="hidden sm:inline-flex items-center gap-1.5">
              <Phone className="h-3.5 w-3.5 text-gold-400" /> 1900&nbsp;1234
            </span>
          </div>
          <div className="flex items-center gap-3">
            <span className="hidden md:inline-flex items-center gap-2 tracking-wide">
              <span className="h-1 w-1 rounded-full bg-gold-400" />
              Mở cửa 10:00 – 22:00 mỗi ngày
              <span className="h-1 w-1 rounded-full bg-gold-400" />
            </span>
          </div>
        </div>
      </div>

      {/* ── Main row: logo + search + account/cart ── */}
      <div className="border-b border-ink-200/70">
        <div className="mx-auto flex h-20 max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8">

          {/* Logo — chữ serif + monogram ánh vàng */}
          <Link to="/" className="flex items-center gap-2.5 group shrink-0">
            <div className="relative grid h-10 w-10 place-items-center rounded-full border border-gold-400/60 bg-accent-800 text-gold-300 transition-transform group-hover:scale-105">
              <span className="font-display text-lg font-semibold leading-none">S</span>
            </div>
            <div className="leading-none">
              <span className="font-display text-2xl font-semibold tracking-tight text-ink-900">
                Smart<span className="italic text-accent-700">Food</span>
              </span>
              <span className="block text-[9px] uppercase tracking-[0.32em] text-gold-600 mt-0.5">Cuisine &amp; Wellness</span>
            </div>
          </Link>

          {/* Search */}
          <form onSubmit={submitSearch} className="flex-1 max-w-xl hidden md:flex" ref={searchBoxRef}>
            <div className="relative flex w-full">
              <input
                type="text"
                value={searchQ}
                onChange={(e) => { setSearchQ(e.target.value); setShowSuggest(true) }}
                onFocus={() => setShowSuggest(true)}
                placeholder="Tìm món trong thực đơn…"
                className="flex-1 rounded-l-full border border-ink-300 border-r-0 bg-white px-4 py-2.5 text-sm focus:outline-none focus:border-gold-400"
              />
              <button type="submit"
                className="grid w-12 place-items-center rounded-r-full bg-accent-700 text-warm-50 hover:bg-accent-800 transition">
                <Search className="h-5 w-5" />
              </button>

              {/* Dropdown gợi ý món */}
              {showSuggest && debouncedQ.length >= 1 && (
                <div className="absolute top-full left-0 right-0 mt-2 z-50 rounded-xl border border-ink-200 bg-white shadow-xl overflow-hidden">
                  {suggestions.length === 0 ? (
                    <div className="px-4 py-3 text-sm text-ink-400">Không tìm thấy món phù hợp.</div>
                  ) : (
                    <>
                      <ul className="max-h-80 overflow-y-auto py-1">
                        {suggestions.slice(0, 6).map((f) => (
                          <li key={f.id}>
                            <button
                              type="button"
                              onClick={() => gotoFood(f.id)}
                              className="flex w-full items-center gap-3 px-3 py-2 text-left hover:bg-accent-50 transition"
                            >
                              <div className="h-11 w-11 shrink-0 overflow-hidden rounded-lg bg-ink-100">
                                <FoodImage src={f.imageUrl} name={f.name} size="full" className="rounded-none" />
                              </div>
                              <div className="min-w-0 flex-1">
                                <p className="truncate text-sm font-medium text-ink-900">{f.name}</p>
                                {f.categoryName && (
                                  <p className="truncate text-xs text-ink-400">{f.categoryName}</p>
                                )}
                              </div>
                              {f.price != null && (
                                <span className="shrink-0 text-sm font-semibold text-accent-700 tabular">
                                  {Number(f.price).toLocaleString('vi-VN')}đ
                                </span>
                              )}
                            </button>
                          </li>
                        ))}
                      </ul>
                      <button
                        type="submit"
                        className="block w-full border-t border-ink-100 px-4 py-2.5 text-center text-sm font-medium text-accent-700 hover:bg-accent-50 transition"
                      >
                        Xem tất cả kết quả cho “{debouncedQ}”
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          </form>

          <div className="flex-1 md:hidden" />

          {/* Right: notifications + cart + account */}
          <div className="flex items-center gap-1 shrink-0">

          {/* ── Bell + Notification Dropdown ── */}
          {token && (
            <div className="relative" ref={notifRef}>
              <button
                onClick={() => setNotifOpen((v) => !v)}
                className={cn(
                  'relative grid h-9 w-9 place-items-center rounded-md text-ink-700 hover:bg-ink-100 transition',
                  notifOpen && 'bg-ink-100'
                )}
                title="Thông báo"
              >
                <Bell className="h-[18px] w-[18px]" />
                {unreadCount > 0 && (
                  <span className="absolute -right-0.5 -top-0.5 grid h-4 w-4 place-items-center rounded-full bg-accent-500 text-[10px] font-bold text-white">
                    {unreadCount > 9 ? '9+' : unreadCount}
                  </span>
                )}
              </button>

              {/* Dropdown */}
              {notifOpen && (
                <div className="absolute right-0 mt-2 w-80 origin-top-right rounded-2xl border border-ink-200 bg-white shadow-pop animate-scale-in overflow-hidden z-50">
                  {/* Header */}
                  <div className="flex items-center justify-between px-4 py-3 border-b border-ink-100">
                    <div>
                      <p className="font-display font-semibold text-sm text-ink-900">Thông báo</p>
                      {unreadCount > 0 && (
                        <p className="text-[10px] text-ink-500">{unreadCount} chưa đọc</p>
                      )}
                    </div>
                    {unreadCount > 0 && (
                      <button
                        onClick={() => markAll.mutate()}
                        disabled={markAll.isPending}
                        className="flex items-center gap-1 text-xs text-accent-600 hover:text-accent-800 font-medium"
                      >
                        <CheckCheck className="h-3.5 w-3.5" />
                        Đọc tất cả
                      </button>
                    )}
                  </div>

                  {/* List */}
                  <div className="max-h-[360px] overflow-y-auto divide-y divide-ink-50">
                    {recentNotifs.length === 0 ? (
                      <div className="py-10 text-center">
                        <Bell className="h-8 w-8 text-ink-200 mx-auto mb-2" />
                        <p className="text-sm text-ink-400">Chưa có thông báo</p>
                      </div>
                    ) : (
                      recentNotifs.map((n) => {
                        const { icon: Icon, color, bg } = notifMeta(n)
                        return (
                          <button
                            key={n.id}
                            onClick={() => handleNotifClick(n)}
                            className={cn(
                              'group/item w-full flex items-start gap-3 px-4 py-3 hover:bg-ink-50 transition text-left',
                              !n.readStatus && 'bg-blue-50/40'
                            )}
                          >
                            {/* Icon */}
                            <div className={cn('grid h-9 w-9 shrink-0 place-items-center rounded-full mt-0.5', bg)}>
                              <Icon className={cn('h-4 w-4', color)} />
                            </div>

                            {/* Text */}
                            <div className="flex-1 min-w-0">
                              <p className={cn(
                                'text-sm leading-snug',
                                n.readStatus ? 'text-ink-600' : 'text-ink-900 font-medium'
                              )}>
                                {n.title}
                              </p>
                              <p className="text-xs text-ink-500 mt-0.5 line-clamp-2 leading-relaxed">
                                {n.message}
                              </p>
                              <p className="text-[10px] text-ink-400 mt-1">
                                {timeAgo(n.createdAt)}
                              </p>
                            </div>

                            {/* Actions */}
                            <div className="flex items-center gap-0.5 shrink-0">
                              {!n.readStatus && (
                                <span className="h-2 w-2 rounded-full bg-blue-500 mt-1" />
                              )}
                              <button
                                onClick={(e) => { e.stopPropagation(); deleteOne.mutate(n.id) }}
                                className="grid h-6 w-6 place-items-center rounded-md text-ink-300 hover:bg-red-50 hover:text-red-500 opacity-0 group-hover/item:opacity-100 transition-all"
                                title="Xóa"
                              >
                                <Trash2 className="h-3 w-3" />
                              </button>
                            </div>
                          </button>
                        )
                      })
                    )}
                  </div>

                  {/* Footer */}
                  <div className="border-t border-ink-100 px-4 py-2.5">
                    <Link
                      to="/notifications"
                      onClick={() => setNotifOpen(false)}
                      className="flex items-center justify-center gap-1.5 text-xs font-medium text-ink-600 hover:text-ink-900 transition"
                    >
                      Xem tất cả thông báo
                      <ArrowRight className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Cart */}
          <Link
            to="/cart"
            className="relative grid h-9 w-9 place-items-center rounded-md text-ink-700 hover:bg-ink-100 transition"
            title="Giỏ hàng"
          >
            <ShoppingBag className="h-[18px] w-[18px]" />
            {count > 0 && (
              <span className="absolute -right-0.5 -top-0.5 grid h-4 w-4 place-items-center rounded-full bg-accent-500 text-[10px] font-bold text-white">
                {count > 9 ? '9+' : count}
              </span>
            )}
          </Link>

          {/* User menu */}
          {token ? (
            <div className="relative ml-2" ref={userMenuRef}>
              <button
                onClick={() => setUserMenu((v) => !v)}
                className="flex items-center gap-2 rounded-full p-0.5 pr-2 hover:bg-ink-100 transition"
              >
                <div className="grid h-8 w-8 place-items-center rounded-full bg-ink-900 text-xs font-semibold text-white">
                  {initials(user?.fullName || user?.email)}
                </div>
                <ChevronDown className="h-3.5 w-3.5 text-ink-500 hidden sm:block" />
              </button>

              {userMenu && (
                <div className="absolute right-0 mt-2 w-60 origin-top-right rounded-xl border border-ink-200 bg-white p-1.5 shadow-pop animate-scale-in">
                  <div className="px-3 py-2 border-b border-ink-100 mb-1">
                    <p className="font-medium text-sm text-ink-900 truncate">
                      {user?.fullName || 'Người dùng'}
                    </p>
                    <p className="text-xs text-ink-500 truncate">{user?.email}</p>
                  </div>
                  {isAdminOrStaff && (
                    <>
                      <MenuItem to="/admin" icon={LayoutGrid} onClick={() => setUserMenu(false)}>Khu quản trị</MenuItem>
                      <div className="my-1 border-t border-ink-100" />
                    </>
                  )}
                  <MenuItem to="/profile"       icon={User}     onClick={() => setUserMenu(false)}>Hồ sơ</MenuItem>
                  <MenuItem to="/reservations" icon={CalendarClock} onClick={() => setUserMenu(false)}>Lượt đặt bàn của tôi</MenuItem>
                  <MenuItem to="/health"        icon={Activity} onClick={() => setUserMenu(false)}>Hồ sơ sức khỏe</MenuItem>
                  <MenuItem to="/orders"        icon={Package}  onClick={() => setUserMenu(false)}>Đơn món của tôi</MenuItem>
                  <MenuItem to="/notifications" icon={Bell}     onClick={() => setUserMenu(false)}>Thông báo</MenuItem>
                  <div className="my-1 border-t border-ink-100" />
                  <button
                    onClick={handleLogout}
                    className="flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-sm text-danger-600 hover:bg-danger-50 transition"
                  >
                    <LogOut className="h-4 w-4" />
                    Đăng xuất
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="ml-2 flex items-center gap-1">
              <Link to="/login" className="btn-ghost btn">Đăng nhập</Link>
              <Link to="/register" className="btn-primary btn hidden sm:inline-flex">Đăng ký</Link>
            </div>
          )}

          {/* Mobile menu button */}
          <button
            className="md:hidden grid h-9 w-9 place-items-center rounded-md text-ink-700 hover:bg-ink-100"
            onClick={() => setMobileOpen(!mobileOpen)}
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
          </div>
        </div>
      </div>

      {/* ── Dải danh mục: nền xanh rừng trầm, chữ hoa thanh lịch ── */}
      <div className="bg-accent-800 text-warm-50 hidden md:block">
        <div className="mx-auto flex max-w-7xl items-center justify-center gap-2 px-4 sm:px-6 lg:px-8">
          {NAV.filter((n) => !n.auth || token).map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              className={({ isActive }) =>
                cn(
                  'relative px-4 py-3 text-[12.5px] font-medium uppercase tracking-[0.16em] transition-colors',
                  isActive ? 'text-gold-300' : 'text-warm-50/85 hover:text-gold-200',
                  n.highlight && !isActive && 'text-gold-300/90'
                )
              }
            >
              {({ isActive }) => (
                <span className="inline-flex items-center gap-1.5">
                  {n.highlight && <Sparkles className="h-3.5 w-3.5" />}
                  {n.label}
                  {isActive && <span className="absolute inset-x-3 -bottom-px h-0.5 bg-gold-400" />}
                </span>
              )}
            </NavLink>
          ))}
          {isAdminOrStaff && (
            <Link to="/admin"
              className="ml-2 my-1.5 inline-flex items-center gap-1.5 rounded-full border border-gold-400/50 px-3.5 py-1.5 text-[12px] font-semibold uppercase tracking-wide text-gold-200 hover:bg-gold-400/10 transition">
              <LayoutGrid className="h-4 w-4" /> Quản trị
            </Link>
          )}
        </div>
      </div>
      {mobileOpen && (
        <div className="md:hidden border-t border-ink-200 bg-white animate-slide-up">
          {/* Search mobile */}
          <form onSubmit={(e) => { submitSearch(e); setMobileOpen(false) }} className="p-3 border-b border-ink-100">
            <div className="flex">
              <input type="text" value={searchQ} onChange={(e) => setSearchQ(e.target.value)}
                placeholder="Tìm món..." className="flex-1 rounded-l-lg border border-ink-300 border-r-0 px-3 py-2 text-sm focus:outline-none" />
              <button type="submit" className="grid w-11 place-items-center rounded-r-lg bg-accent-600 text-white">
                <Search className="h-4 w-4" />
              </button>
            </div>
          </form>
          <nav className="flex flex-col p-2">
            {NAV.filter((n) => !n.auth || token).map((n) => (
              <NavLink
                key={n.to}
                to={n.to}
                onClick={() => setMobileOpen(false)}
                className={({ isActive }) =>
                  cn(
                    'px-3 py-2.5 rounded-md text-sm font-medium',
                    isActive ? 'bg-ink-100 text-ink-900' : 'text-ink-700'
                  )
                }
              >
                {n.label}
              </NavLink>
            ))}
          </nav>
        </div>
      )}
    </header>
  )
}

function MenuItem({ to, icon: Icon, children, onClick }) {
  return (
    <Link
      to={to}
      onClick={onClick}
      className="flex items-center gap-2.5 rounded-md px-3 py-2 text-sm text-ink-700 hover:bg-ink-100 hover:text-ink-900 transition"
    >
      <Icon className="h-4 w-4 text-ink-500" />
      {children}
    </Link>
  )
}
