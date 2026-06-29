import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  TrendingUp,
  ShoppingBag,
  CreditCard,
  Trophy,
} from 'lucide-react'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts'
import { reportsApi } from '@/api/admin'
import { Loader, Skeleton, FoodImage } from '@/components/ui/Atoms'
import { Input } from '@/components/ui/Input'
import { formatVND, formatDate } from '@/lib/utils'

const COLORS = ['#1c1917', '#10b981', '#22c55e', '#3b82f6', '#a855f7']

export default function AdminReportsPage() {
  const today = new Date()
  const monthAgo = new Date()
  monthAgo.setDate(today.getDate() - 30)

  const [from, setFrom] = useState(monthAgo.toISOString().slice(0, 10))
  const [to, setTo] = useState(today.toISOString().slice(0, 10))

  const { data: summary } = useQuery({
    queryKey: ['report-summary', from, to],
    queryFn: () => reportsApi.summary(from, to),
  })

  const { data: revenue, isLoading: revLoading } = useQuery({
    queryKey: ['report-revenue', from, to],
    queryFn: () => reportsApi.dailyRevenue(from, to),
  })

  const { data: payments } = useQuery({
    queryKey: ['report-payments', from, to],
    queryFn: () => reportsApi.payments(from, to),
  })

  const { data: bestSelling } = useQuery({
    queryKey: ['report-best', from, to],
    queryFn: () => reportsApi.bestSelling(from, to, 8),
  })

  const chartData = (revenue || []).map((r) => ({
    day: formatDate(r.date).slice(0, 5),
    revenue: Number(r.revenue || 0),
    orders: Number(r.orderCount || 0),
  }))

  const PAYMENT_COLORS = {
    CASH: '#374151', BANK_TRANSFER: '#005baa',
  }
  const PAYMENT_LABELS = {
    CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản',
  }
  const paymentChart = (payments || []).map((p) => {
    const m = p.method || p.paymentMethod
    return {
      name: PAYMENT_LABELS[m] || m || 'Khác',
      value: Number(p.totalAmount || p.count || 0),
      count: Number(p.orderCount || p.count || 0),
      fill: PAYMENT_COLORS[m] || '#6b7280',
    }
  })

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-wider text-ink-500">Báo cáo</p>
        <h1 className="font-display text-3xl font-bold text-ink-900">Phân tích kinh doanh</h1>
      </div>

      <div className="card p-4 flex flex-wrap gap-3 items-end">
        <Input type="date" label="Từ ngày" value={from} onChange={(e) => setFrom(e.target.value)} className="w-auto" />
        <Input type="date" label="Đến ngày" value={to} onChange={(e) => setTo(e.target.value)} className="w-auto" />
      </div>

      {/* KPIs */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <KPICard icon={TrendingUp} tone="accent" label="Doanh thu" value={formatVND(summary?.totalRevenue)} />
        <KPICard icon={ShoppingBag} label="Đơn hàng" value={summary?.totalOrders || 0} />
        <KPICard icon={CreditCard} label="Giá trị TB/đơn" value={summary?.totalOrders > 0 ? formatVND((summary?.totalRevenue || 0) / summary.totalOrders) : '—'} />
        <KPICard icon={Trophy} label="Đơn hoàn thành" value={summary?.completedOrders || 0} />
      </div>

      {/* Revenue + orders chart */}
      <div className="card p-5">
        <h2 className="font-display font-semibold text-ink-900 mb-3">Doanh thu & số đơn theo ngày</h2>
        {revLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : chartData.length === 0 ? (
          <div className="h-64 grid place-items-center text-sm text-ink-500">
            Chưa có dữ liệu trong khoảng này
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" vertical={false} />
              <XAxis dataKey="day" stroke="#a8a29e" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis stroke="#a8a29e" fontSize={11} tickLine={false} axisLine={false}
                     tickFormatter={(v) => (v >= 1e6 ? `${(v / 1e6).toFixed(1)}M` : `${v / 1e3}k`)} />
              <Tooltip
                contentStyle={{ background: '#1c1917', border: 'none', borderRadius: 10, color: '#fafafa', fontSize: 12 }}
                formatter={(v, n) => (n === 'revenue' ? [formatVND(v), 'Doanh thu'] : [v, 'Số đơn'])}
              />
              <Bar dataKey="revenue" fill="#1c1917" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      <div className="grid gap-5 lg:grid-cols-2">
        {/* Payment methods */}
        <div className="card p-5">
          <h2 className="font-display font-semibold text-ink-900 mb-3">
            Phân bổ thanh toán
          </h2>
          {paymentChart.length === 0 ? (
            <div className="h-64 grid place-items-center text-sm text-ink-500">Chưa có dữ liệu</div>
          ) : (
            <div className="space-y-3">
              {paymentChart.map((p) => {
                const total = paymentChart.reduce((s, x) => s + x.value, 0)
                const pct   = total > 0 ? Math.round((p.value / total) * 100) : 0
                return (
                  <div key={p.name}>
                    <div className="flex items-center justify-between mb-1 text-sm">
                      <div className="flex items-center gap-2">
                        <span className="inline-block h-3 w-3 rounded-sm" style={{ background: p.fill }} />
                        <span className="font-medium text-ink-900">{p.name}</span>
                        {p.count > 0 && <span className="text-xs text-ink-400">{p.count} đơn</span>}
                      </div>
                      <div className="text-right">
                        <span className="font-semibold tabular text-ink-900">{formatVND(p.value)}</span>
                        <span className="ml-2 text-xs text-ink-400">{pct}%</span>
                      </div>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-ink-100 overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all"
                        style={{ width: `${pct}%`, background: p.fill }}
                      />
                    </div>
                  </div>
                )
              })}
              <div className="border-t border-ink-100 pt-3 flex justify-between text-sm">
                <span className="text-ink-500">Tổng</span>
                <span className="font-bold tabular">{formatVND(paymentChart.reduce((s, p) => s + p.value, 0))}</span>
              </div>
            </div>
          )}
        </div>

        {/* Best selling */}
        <div className="card p-5">
          <h2 className="font-display font-semibold text-ink-900 mb-3 inline-flex items-center gap-2">
            <Trophy className="h-4 w-4 text-accent-500" /> Món bán chạy
          </h2>
          {!bestSelling || bestSelling.length === 0 ? (
            <div className="h-64 grid place-items-center text-sm text-ink-500">Chưa có dữ liệu</div>
          ) : (
            <div className="space-y-2">
              {bestSelling.map((f, i) => {
                // Backend trả về: foodId, foodName, totalQuantity, totalRevenue
                const name = f.foodName || f.name
                const sold = f.totalQuantity ?? f.soldQuantity ?? f.quantity ?? 0
                const revenue = f.totalRevenue ?? f.revenue ?? 0

                return (
                  <div
                    key={f.foodId || f.id || i}
                    className="flex items-center gap-3 rounded-lg border border-ink-100 p-2.5"
                  >
                    <span className="grid h-7 w-7 shrink-0 place-items-center rounded-md bg-ink-900 text-xs font-bold text-white tabular">
                      {i + 1}
                    </span>
                    <FoodImage src={f.imageUrl} name={name} size="sm" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-ink-900 truncate">{name}</p>
                      <p className="text-xs text-ink-500">
                        Đã bán:{' '}
                        <span className="font-semibold tabular text-ink-900">
                          {sold.toLocaleString('vi-VN')}
                        </span>
                      </p>
                    </div>
                    <p className="text-sm font-semibold tabular shrink-0">
                      {formatVND(revenue)}
                    </p>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function KPICard({ icon: Icon, label, value, tone }) {
  return (
    <div className="card p-5">
      <div
        className={`grid h-9 w-9 place-items-center rounded-lg ${
          tone === 'accent' ? 'bg-accent-50 text-accent-600' : 'bg-ink-100 text-ink-700'
        }`}
      >
        <Icon className="h-4 w-4" />
      </div>
      <p className="mt-4 font-display text-xl font-bold tabular text-ink-900">{value ?? '—'}</p>
      <p className="mt-0.5 text-xs uppercase tracking-wider text-ink-500">{label}</p>
    </div>
  )
}
