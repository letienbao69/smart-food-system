import { useQuery } from '@tanstack/react-query'
import {
  Settings,
  Server,
  Database,
  Sparkles,
  Shield,
  ExternalLink,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react'
import { api } from '@/api/client'
import { Badge } from '@/components/ui/Atoms'
import { useAuth } from '@/store/auth'

export default function AdminSettingsPage() {
  const user = useAuth((s) => s.user)

  // Probe chatbot endpoint to confirm AI is reachable
  const { data: aiPing } = useQuery({
    queryKey: ['admin-ai-ping'],
    queryFn: () => api.get('/chatbot/ping').then((r) => r.data),
    retry: false,
  })

  const aiOk = aiPing === 'pong' || aiPing?.message === 'pong' || !!aiPing

  return (
    <div className="space-y-5">
      <div>
        <p className="text-xs uppercase tracking-wider text-ink-500">Hệ thống</p>
        <h1 className="font-display text-3xl font-bold text-ink-900">
          Cài đặt & Thông tin
        </h1>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <InfoCard
          icon={Server}
          title="Backend API"
          status="online"
          rows={[
            ['Framework', 'Spring Boot 3.3.2'],
            ['Java', '17'],
            ['Endpoint', 'http://localhost:8080/api'],
            ['Swagger', 'http://localhost:8080/swagger-ui.html'],
          ]}
        />

        <InfoCard
          icon={Database}
          title="Cơ sở dữ liệu"
          status="online"
          rows={[
            ['Loại', 'MySQL / MariaDB'],
            ['Schema', 'food_shop'],
            ['ORM', 'JPA + Hibernate'],
          ]}
        />

        <InfoCard
          icon={Sparkles}
          title="Gemini AI"
          status={aiOk ? 'online' : 'unknown'}
          rows={[
            ['Model', 'gemini-2.0-flash'],
            ['Tính năng', 'Chatbot, Gợi ý theo BMI'],
            ['Trạng thái', aiOk ? 'Hoạt động' : 'Chưa kiểm tra'],
          ]}
        />

        <InfoCard
          icon={Shield}
          title="Bảo mật"
          status="online"
          rows={[
            ['Auth', 'JWT Bearer'],
            ['Password hash', 'BCrypt (cost 10)'],
            ['CORS', 'Configured'],
          ]}
        />
      </div>

      <div className="card p-5">
        <h2 className="font-display font-semibold text-ink-900 mb-3 inline-flex items-center gap-2">
          <Settings className="h-4 w-4" /> Tài khoản Admin
        </h2>
        <div className="space-y-2 text-sm">
          <Row label="Họ tên" value={user?.fullName || '—'} />
          <Row label="Email" value={user?.email || '—'} />
          <Row label="Quyền" value="ADMIN" />
        </div>
      </div>

      <div className="card p-5">
        <h2 className="font-display font-semibold text-ink-900 mb-3">Tài liệu</h2>
        <div className="space-y-2">
          <DocLink
            href="http://localhost:8080/swagger-ui.html"
            label="Swagger UI"
            desc="Tất cả endpoints + thử nghiệm trực tiếp"
          />
          <DocLink
            href="http://localhost:8080/v3/api-docs"
            label="OpenAPI JSON"
            desc="Spec JSON cho việc tích hợp"
          />
        </div>
      </div>

      <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 flex gap-3">
        <AlertCircle className="h-5 w-5 shrink-0 mt-0.5" />
        <div>
          <p className="font-semibold">Lưu ý production</p>
          <ul className="mt-1 space-y-0.5 list-disc list-inside text-amber-800">
            <li>Đổi <code className="font-mono">JWT_SECRET</code> ngay sau khi triển khai</li>
            <li>Đổi password admin mặc định</li>
            <li>Set <code className="font-mono">GEMINI_API_KEY</code> qua biến môi trường, không hardcode</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

function InfoCard({ icon: Icon, title, status, rows }) {
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between mb-3">
        <div className="grid h-9 w-9 place-items-center rounded-lg bg-ink-100 text-ink-700">
          <Icon className="h-4 w-4" />
        </div>
        <Badge tone={status === 'online' ? 'success' : 'ink'}>
          {status === 'online' ? (
            <span className="inline-flex items-center gap-1">
              <CheckCircle2 className="h-3 w-3" /> Hoạt động
            </span>
          ) : (
            'Chưa rõ'
          )}
        </Badge>
      </div>
      <h3 className="font-display font-semibold text-ink-900">{title}</h3>
      <div className="mt-3 space-y-1.5 text-xs">
        {rows.map(([k, v]) => (
          <div key={k} className="flex justify-between gap-2">
            <span className="text-ink-500">{k}</span>
            <span className="font-medium text-ink-900 text-right truncate">{v}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function Row({ label, value }) {
  return (
    <div className="flex justify-between gap-3 py-1.5 border-b border-ink-100 last:border-0">
      <span className="text-ink-500">{label}</span>
      <span className="font-medium text-ink-900">{value}</span>
    </div>
  )
}

function DocLink({ href, label, desc }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className="flex items-center justify-between rounded-lg border border-ink-200 p-3 hover:border-ink-900 hover:bg-ink-50 transition group"
    >
      <div>
        <p className="text-sm font-medium text-ink-900">{label}</p>
        <p className="text-xs text-ink-500">{desc}</p>
      </div>
      <ExternalLink className="h-4 w-4 text-ink-400 group-hover:text-ink-900" />
    </a>
  )
}
