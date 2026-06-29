import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  LayoutDashboard,
  ShoppingBag,
  Utensils,
  Tags,
  Users,
  UserCog,
  Ticket,
  BarChart3,
  Bell,
  LogOut,
  Home,
  Menu,
  X,
  MessageSquare,
  Settings,
  CalendarClock,
  Armchair,
} from 'lucide-react'
import { useAuth } from '@/store/auth'
import { cn, initials } from '@/lib/utils'
import { useAdminNotificationPoll } from '@/hooks/useAdminNotificationPoll'
import { useAdminOrderNotifications } from '@/hooks/useAdminOrderNotifications'
import { notificationsApi } from '@/api/admin'
import NewReservationPopup from '@/components/common/NewReservationPopup'

const NAV = [
  { to: '/admin', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/admin/reservations', label: 'Đặt bàn', icon: CalendarClock, staff: true },
  { to: '/admin/tables', label: 'Bàn ăn', icon: Armchair, staff: true },
  { to: '/admin/orders', label: 'Đơn món', icon: ShoppingBag, staff: true },
  { to: '/admin/foods', label: 'Món ăn', icon: Utensils },
  { to: '/admin/categories', label: 'Danh mục', icon: Tags },
  { to: '/admin/vouchers', label: 'Voucher', icon: Ticket },
  { to: '/admin/users', label: 'Người dùng', icon: Users },
  { to: '/admin/employees', label: 'Nhân viên', icon: UserCog },
  { to: '/admin/contacts', label: 'Liên hệ', icon: MessageSquare },
  { to: '/admin/notifications', label: 'Thông báo', icon: Bell, staff: true },
]

export default function AdminLayout() {
  const user = useAuth((s) => s.user)
  const logout = useAuth((s) => s.logout)
  const isAdmin = useAuth((s) => s.isAdmin())
  const nav = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)

  // STAFF chỉ thấy các mục được phép; ADMIN thấy tất cả
  const menu = isAdmin ? NAV : NAV.filter((n) => n.staff)

  // WebSocket: real-time NEW ORDER push (instant)
  useAdminOrderNotifications()
  // HTTP poll fallback: catches any notifications not arriving via WS
  useAdminNotificationPoll()

  // Số thông báo chưa đọc -> hiển thị badge trên mục "Thông báo" (giúp staff/admin không bỏ sót đơn)
  const { data: adminNotifs } = useQuery({
    queryKey: ['admin-notifications-poll'],
    queryFn: notificationsApi.adminList,
    refetchInterval: 5000,
    retry: false,
  })
  const unreadCount = Array.isArray(adminNotifs) ? adminNotifs.filter((n) => !n.readStatus).length : 0

  const handleLogout = () => {
    logout()
    nav('/')
  }

  return (
    <div className="min-h-screen bg-ink-50 flex">
      {/* Sidebar */}
      <aside
        className={cn(
          'fixed lg:sticky top-0 z-40 h-screen w-64 bg-ink-900 text-ink-200 flex flex-col transition-transform lg:translate-x-0',
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex items-center justify-between px-5 h-16 border-b border-ink-800">
          <Link to="/admin" className="flex items-center gap-2">
            <div className="grid h-8 w-8 place-items-center rounded-lg bg-accent-500/20 text-accent-400">
              <svg viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
                <path d="M5 7h14v2H5zM5 12h10v2H5zM5 17h14v2H5z" />
                <circle cx="18" cy="13" r="1.2" />
              </svg>
            </div>
            <div>
              <p className="font-display font-bold text-white text-sm leading-tight">
                Smart Food
              </p>
              <p className="text-[10px] uppercase tracking-wider text-ink-400">Admin</p>
            </div>
          </Link>
          <button
            onClick={() => setMobileOpen(false)}
            className="lg:hidden text-ink-400 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto p-3 space-y-0.5">
          {menu.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              onClick={() => setMobileOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
                  isActive
                    ? 'bg-white/10 text-white font-medium'
                    : 'text-ink-400 hover:text-white hover:bg-white/5'
                )
              }
            >
              <n.icon className="h-4 w-4" />
              <span className="flex-1">{n.label}</span>
              {n.to === '/admin/notifications' && unreadCount > 0 && (
                <span className="inline-flex items-center justify-center min-w-[18px] h-[18px] rounded-full bg-accent-500 px-1 text-[10px] font-bold text-white">
                  {unreadCount > 99 ? '99+' : unreadCount}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Nút về trang chủ (xem giao diện khách) */}
        <div className="px-3 pb-1">
          <Link to="/"
            className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-ink-200 hover:bg-ink-800 hover:text-white transition">
            <Home className="h-4 w-4" />
            Về trang chủ
          </Link>
        </div>

        {/* User */}
        <div className="border-t border-ink-800 p-3">
          <div className="flex items-center gap-3 rounded-lg px-2 py-2">
            <div className="grid h-8 w-8 place-items-center rounded-full bg-accent-500 text-white text-xs font-semibold">
              {initials(user?.fullName || user?.email)}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">
                {user?.fullName || 'Admin'}
              </p>
              <p className="text-[11px] text-ink-400 truncate">{user?.email}</p>
            </div>
            <button
              onClick={handleLogout}
              className="text-ink-400 hover:text-danger-400 transition"
              title="Đăng xuất"
            >
              <LogOut className="h-4 w-4" />
            </button>
          </div>
        </div>
      </aside>

      {mobileOpen && (
        <div
          className="fixed inset-0 z-30 bg-ink-950/50 lg:hidden"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Content */}
      <div className="flex-1 min-w-0">
        {/* Top bar */}
        <header className="sticky top-0 z-20 bg-white/85 backdrop-blur-md border-b border-ink-200 lg:hidden">
          <div className="px-4 h-14 flex items-center justify-between">
            <button
              onClick={() => setMobileOpen(true)}
              className="grid h-9 w-9 place-items-center rounded-md hover:bg-ink-100"
            >
              <Menu className="h-5 w-5" />
            </button>
            <span className="font-display font-bold">Admin</span>
          </div>
        </header>

        <main className="px-4 py-6 lg:px-8 lg:py-8">
          <Outlet />
        </main>
      </div>
      <NewReservationPopup />
    </div>
  )
}
