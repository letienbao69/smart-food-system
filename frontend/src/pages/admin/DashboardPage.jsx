import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { TrendingUp, ShoppingBag, Users, ArrowUpRight, ArrowRight, FileDown } from 'lucide-react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  PieChart, Pie, Cell, Legend,
} from 'recharts'
import { reportsApi } from '@/api/admin'
import { ordersApi } from '@/api/cart'
import { reservationsApi } from '@/api/reservations'
import { tablesApi } from '@/api/tables'
import { Loader } from '@/components/ui/Atoms'
import { formatVND, formatDateTime, formatDate, orderStatusLabel, orderStatusTone } from '@/lib/utils'

const PIE_COLORS = ['#8b5cf6', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#ec4899']

export default function DashboardPage() {
  const today = new Date()
  const weekAgo = new Date(); weekAgo.setDate(today.getDate() - 7)
  const [from, setFrom] = useState(weekAgo.toISOString().slice(0, 10))
  const [to, setTo] = useState(today.toISOString().slice(0, 10))

  const { data: summary, isLoading: sumLoading } = useQuery({
    queryKey: ['report-summary', from, to],
    queryFn: () => reportsApi.summary(from, to),
  })
  const { data: revenue } = useQuery({
    queryKey: ['report-revenue', from, to],
    queryFn: () => reportsApi.dailyRevenue(from, to),
  })
  const { data: bestSelling } = useQuery({
    queryKey: ['report-best', from, to],
    queryFn: () => reportsApi.bestSelling(from, to, 7),
  })
  const { data: orders } = useQuery({
    queryKey: ['admin-orders'],
    queryFn: ordersApi.adminList,
  })
  const { data: reservations } = useQuery({
    queryKey: ['admin-reservations'],
    queryFn: reservationsApi.adminList,
  })
  const { data: tables } = useQuery({ queryKey: ['admin-tables'], queryFn: tablesApi.list })

  const revenueChart = (revenue || []).map((r) => ({
    date: r.date ? String(r.date).slice(5) : '',
    revenue: Number(r.revenue || r.totalRevenue || 0),
  }))

  const pieData = (bestSelling || []).map((b) => ({
    name: b.foodName,
    value: Number(b.totalQuantity || 0),
  }))

  // Đặt bàn theo giờ — tính theo múi giờ Việt Nam (đồng nhất với hiển thị)
  const bookingsByHour = useMemo(() => {
    // Lấy giờ (0-23) của một mốc thời gian theo giờ VN
    const vnHour = (d) => {
      const s = new Intl.DateTimeFormat('en-GB', {
        hour: '2-digit', hour12: false, timeZone: 'Asia/Ho_Chi_Minh',
      }).format(new Date(d))
      return parseInt(s, 10) % 24
    }
    const nowHour = vnHour(Date.now())
    const hours = []
    for (let i = -2; i <= 9; i++) {
      hours.push((nowHour + i + 24) % 24)
    }
    const buckets = {}
    hours.forEach((h) => { buckets[h] = 0 })
    ;(reservations || []).forEach((r) => {
      if (!r.reservationTime) return
      const h = vnHour(r.reservationTime)
      if (buckets[h] != null) buckets[h] += 1
    })
    return hours.map((h) => ({ hour: `${String(h).padStart(2, '0')}h`, count: buckets[h] }))
  }, [reservations])

  const recentOrders = (orders || []).slice(0, 6)
  const totalTables = tables?.length || 0

  const handleExportPDF = () => window.print()

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3 no-print">
        <div>
          <h1 className="font-display text-3xl font-bold text-ink-900">Tổng quan</h1>
          <p className="text-sm text-ink-500">Thống kê hoạt động của nhà hàng</p>
        </div>
        <div className="flex flex-wrap items-end gap-2">
          <div>
            <label className="block text-xs text-ink-500 mb-1">Từ ngày</label>
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="input" />
          </div>
          <div>
            <label className="block text-xs text-ink-500 mb-1">Đến ngày</label>
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="input" />
          </div>
          <button onClick={handleExportPDF}
            className="inline-flex items-center gap-2 rounded-lg bg-accent-600 px-4 py-2 text-sm font-semibold text-white hover:bg-accent-700 transition h-[42px]">
            <FileDown className="h-4 w-4" /> Xuất PDF
          </button>
        </div>
      </div>

      {/* Tiêu đề chỉ hiện khi in PDF */}
      <div className="hidden print:block mb-4">
        <h1 className="text-2xl font-bold text-ink-900">Báo cáo tổng quan kinh doanh — SmartFood</h1>
        <p className="text-sm text-ink-600">Khoảng thời gian: {formatDate(from)} — {formatDate(to)}</p>
      </div>

      {/* Stat cards */}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard icon={TrendingUp} iconBg="bg-green-100 text-green-600"
          label="Tổng doanh thu" value={sumLoading ? '—' : formatVND(summary?.totalRevenue)} pct="+100.0%" />
        <StatCard icon={ShoppingBag} iconBg="bg-blue-100 text-blue-600"
          label="Đơn hàng mới" value={sumLoading ? '—' : (summary?.totalOrders ?? 0)} pct="+100.0%" />
        <StatCard icon={Users} iconBg="bg-violet-100 text-violet-600"
          label="Khách hàng mới" value={sumLoading ? '—' : (summary?.totalUsers ?? 0)} pct="+100.0%" />
      </div>

      {/* Revenue + popular */}
      <div className="grid gap-5 lg:grid-cols-[1.6fr_1fr]">
        <div className="card p-5">
          <h2 className="font-display font-semibold text-ink-900 mb-4">Doanh thu theo ngày</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={revenueChart}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eee" vertical={false} />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `${Math.round(v / 1000)}N`} />
              <Tooltip formatter={(v) => [formatVND(v), 'Doanh thu']} />
              <Bar dataKey="revenue" fill="#4ade80" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-5">
          <h2 className="font-display font-semibold text-ink-900 mb-4">Món ăn phổ biến</h2>
          {pieData.length === 0 ? (
            <div className="grid place-items-center h-[260px] text-sm text-ink-400">Chưa có dữ liệu</div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="45%" outerRadius={80} innerRadius={45}>
                  {pieData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                </Pie>
                <Tooltip />
                <Legend wrapperStyle={{ fontSize: 11 }} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Bookings by hour + recent orders */}
      <div className="grid gap-5 lg:grid-cols-[1.4fr_1.6fr]">
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-semibold text-ink-900">Thống kê đặt bàn theo giờ</h2>
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs text-blue-600">Tổng số bàn: {totalTables}</span>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={bookingsByHour}>
              <CartesianGrid strokeDasharray="3 3" stroke="#eee" vertical={false} />
              <XAxis dataKey="hour" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
              <Tooltip formatter={(v) => [v, 'Lượt đặt']} />
              <Bar dataKey="count" fill="#f87171" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card overflow-hidden">
          <div className="flex items-center justify-between p-5 pb-3">
            <h2 className="font-display font-semibold text-ink-900">Đơn hàng gần đây</h2>
            <Link to="/admin/orders" className="text-sm text-ink-500 hover:text-ink-900 inline-flex items-center gap-1">
              Xem tất cả <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-ink-500 border-y border-ink-100">
                <tr>
                  <th className="px-5 py-2.5 font-medium">Khách hàng</th>
                  <th className="px-5 py-2.5 font-medium text-center">Số món</th>
                  <th className="px-5 py-2.5 font-medium text-right">Tổng tiền</th>
                  <th className="px-5 py-2.5 font-medium">Trạng thái</th>
                  <th className="px-5 py-2.5 font-medium">Thời gian</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ink-100">
                {recentOrders.map((o) => (
                  <tr key={o.id}>
                    <td className="px-5 py-3 text-ink-900">{o.customerName || '—'}</td>
                    <td className="px-5 py-3 text-center">{o.items?.length || 0}</td>
                    <td className="px-5 py-3 text-right tabular">{formatVND(o.finalAmount)}</td>
                    <td className="px-5 py-3">
                      <span className={`chip border text-xs ${orderStatusTone(o.orderStatus)}`}>
                        {orderStatusLabel(o.orderStatus)}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-xs text-ink-500">{formatDateTime(o.createdAt)}</td>
                  </tr>
                ))}
                {recentOrders.length === 0 && (
                  <tr><td colSpan={5} className="px-5 py-8 text-center text-ink-400">Chưa có đơn hàng</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

    </div>
  )
}

function StatCard({ icon: Icon, iconBg, label, value, pct }) {
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-ink-500">{label}</p>
          <p className="mt-1 font-display text-2xl font-bold text-ink-900">{value}</p>
        </div>
        <div className={`grid h-10 w-10 place-items-center rounded-xl ${iconBg}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <div className="mt-3 inline-flex items-center gap-1 rounded-full bg-green-50 px-2 py-0.5 text-xs text-green-600">
        <ArrowUpRight className="h-3 w-3" /> {pct} so với tuần trước
      </div>
    </div>
  )
}
