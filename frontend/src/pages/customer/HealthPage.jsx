import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Sparkles,
  Activity,
  Save,
  AlertCircle,
  Info,
  TrendingUp,
  Heart,
  Flame,
  Loader2,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { healthApi } from '@/api/health'
import { errMsg } from '@/api/client'
import { Loader, Badge, Empty } from '@/components/ui/Atoms'
import { Input, Select, Field, Textarea } from '@/components/ui/Input'
import Button from '@/components/ui/Button'
import FoodCard from '@/components/common/FoodCard'
import { useWishlistIds } from '@/hooks/useWishlistIds'
import { bmiBadgeTone } from '@/lib/utils'

const ACTIVITY_OPTIONS = [
  { v: 'SEDENTARY', l: 'Ít vận động — Ngồi văn phòng' },
  { v: 'LIGHT', l: 'Nhẹ — Đi bộ, vận động nhẹ 1-3 ngày/tuần' },
  { v: 'MODERATE', l: 'Vừa — Tập 3-5 ngày/tuần' },
  { v: 'ACTIVE', l: 'Tích cực — Tập 6-7 ngày/tuần' },
  { v: 'VERY_ACTIVE', l: 'Rất tích cực — Tập nặng mỗi ngày' },
]

const DIET_OPTIONS = [
  { v: 'NORMAL', l: 'Bình thường' },
  { v: 'VEGETARIAN', l: 'Chay' },
  { v: 'VEGAN', l: 'Thuần chay' },
  { v: 'DIABETIC', l: 'Tiểu đường — Ít đường' },
  { v: 'LOW_SODIUM', l: 'Ít muối — Cao huyết áp' },
  { v: 'LOW_FAT', l: 'Ít béo' },
  { v: 'KETO', l: 'Keto' },
  { v: 'GLUTEN_FREE', l: 'Không gluten' },
]

const GOAL_OPTIONS = [
  { v: 'LOSE_WEIGHT', l: 'Giảm cân' },
  { v: 'MAINTAIN', l: 'Duy trì' },
  { v: 'GAIN_WEIGHT', l: 'Tăng cân' },
  { v: 'GAIN_MUSCLE', l: 'Tăng cơ' },
]

export default function HealthPage() {
  const qc = useQueryClient()
  const [useAi, setUseAi] = useState(false)
  const wishlist = useWishlistIds()

  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['health-profile'],
    queryFn: healthApi.getProfile,
  })

  const { data: analysis } = useQuery({
    queryKey: ['health-analysis'],
    queryFn: healthApi.getAnalysis,
    enabled: !!profile?.profileComplete,
  })

  const { data: recs, isFetching: recsFetching } = useQuery({
    queryKey: ['health-recs', useAi],
    queryFn: () => healthApi.getRecommendations(12, useAi),
    enabled: !!profile?.profileComplete,
  })

  const update = useMutation({
    mutationFn: (data) => healthApi.updateProfile(data),
    onSuccess: () => {
      toast.success('Đã lưu hồ sơ sức khỏe')
      qc.invalidateQueries({ queryKey: ['health-profile'] })
      qc.invalidateQueries({ queryKey: ['health-analysis'] })
      qc.invalidateQueries({ queryKey: ['health-recs'] })
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (profileLoading) return <Loader className="min-h-[60vh]" />

  const onSubmit = (e) => {
    e.preventDefault()
    const fd = new FormData(e.currentTarget)
    const data = Object.fromEntries(fd.entries())
    const body = {
      gender: data.gender || null,
      dateOfBirth: data.dateOfBirth || null,
      heightCm: data.heightCm ? Number(data.heightCm) : null,
      weightKg: data.weightKg ? Number(data.weightKg) : null,
      healthCondition: data.healthCondition || null,
      dietPreference: data.dietPreference || null,
      activityLevel: data.activityLevel || null,
      goal: data.goal || null,
    }
    update.mutate(body)
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      {/* Header */}
      <div className="mb-8">
        <div className="inline-flex items-center gap-2 rounded-full bg-ink-900 text-accent-300 px-3 py-1 text-xs font-medium">
          <Sparkles className="h-3 w-3" />
          Tính năng AI cốt lõi
        </div>
        <h1 className="mt-3 font-display text-4xl font-bold tracking-tight text-ink-900">
          Hồ sơ sức khỏe & gợi ý món
        </h1>
        <p className="mt-2 text-ink-600 max-w-2xl">
          Cập nhật các thông tin bên dưới — hệ thống tính BMI, BMR, TDEE rồi
          chấm điểm từng món theo cơ thể, mục tiêu và bệnh lý của bạn.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        {/* Profile form */}
        <form onSubmit={onSubmit} className="card p-6 lg:sticky lg:top-20 self-start">
          <h2 className="font-display text-lg font-semibold text-ink-900 mb-4">
            Thông tin cá nhân
          </h2>

          <div className="space-y-3.5">
            <div className="grid grid-cols-2 gap-3">
              <Select label="Giới tính" name="gender" defaultValue={profile?.gender || ''}>
                <option value="">Chọn...</option>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác</option>
              </Select>
              <Input
                type="date"
                label="Ngày sinh"
                name="dateOfBirth"
                defaultValue={profile?.dateOfBirth || ''}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <Input
                type="number"
                step="0.1"
                label="Chiều cao (cm)"
                name="heightCm"
                placeholder="170"
                defaultValue={profile?.heightCm || ''}
              />
              <Input
                type="number"
                step="0.1"
                label="Cân nặng (kg)"
                name="weightKg"
                placeholder="65"
                defaultValue={profile?.weightKg || ''}
              />
            </div>

            <Select
              label="Mức vận động"
              name="activityLevel"
              defaultValue={profile?.activityLevel || ''}
            >
              <option value="">Chọn...</option>
              {ACTIVITY_OPTIONS.map((o) => (
                <option key={o.v} value={o.v}>{o.l}</option>
              ))}
            </Select>

            <Select
              label="Chế độ ăn"
              name="dietPreference"
              defaultValue={profile?.dietPreference || ''}
            >
              <option value="">Chọn...</option>
              {DIET_OPTIONS.map((o) => (
                <option key={o.v} value={o.v}>{o.l}</option>
              ))}
            </Select>

            <Select label="Mục tiêu" name="goal" defaultValue={profile?.goal || ''}>
              <option value="">Chọn...</option>
              {GOAL_OPTIONS.map((o) => (
                <option key={o.v} value={o.v}>{o.l}</option>
              ))}
            </Select>

            <Textarea
              label="Tình trạng sức khỏe / Dị ứng"
              name="healthCondition"
              rows={3}
              placeholder="VD: Tiểu đường tuýp 2, dị ứng hải sản..."
              defaultValue={profile?.healthCondition || ''}
            />
          </div>

          <Button type="submit" loading={update.isPending} className="mt-5 w-full">
            <Save className="h-4 w-4" />
            Lưu hồ sơ
          </Button>

          <p className="mt-3 text-[11px] text-ink-500 flex items-start gap-1.5">
            <Info className="h-3 w-3 mt-0.5 shrink-0" />
            Thông tin được dùng cho việc gợi ý món. Không chia sẻ ra bên ngoài.
          </p>
        </form>

        {/* Analysis + Recommendations */}
        <div className="space-y-6 min-w-0">
          {/* Analysis */}
          {!profile?.profileComplete ? (
            <div className="card p-8">
              <Empty
                icon={AlertCircle}
                title="Hồ sơ chưa hoàn thiện"
                description="Vui lòng nhập đủ giới tính, ngày sinh, chiều cao và cân nặng để hệ thống phân tích BMI."
              />
            </div>
          ) : analysis ? (
            <AnalysisCard analysis={analysis} />
          ) : (
            <Loader />
          )}

          {/* AI Recommendations */}
          {profile?.profileComplete && (
            <div>
              <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
                <div>
                  <h2 className="font-display text-2xl font-bold text-ink-900">
                    Món được gợi ý cho bạn
                  </h2>
                  <p className="mt-0.5 text-sm text-ink-500">
                    Xếp hạng theo điểm match với hồ sơ sức khỏe
                  </p>
                </div>
                <label className="flex items-center gap-2 cursor-pointer text-sm text-ink-700">
                  <input
                    type="checkbox"
                    checked={useAi}
                    onChange={(e) => setUseAi(e.target.checked)}
                    className="h-4 w-4 rounded border-ink-300 text-ink-900 focus:ring-ink-900"
                  />
                  <span className="inline-flex items-center gap-1">
                    <Sparkles className="h-3.5 w-3.5" />
                    Bật lời khuyên AI
                  </span>
                </label>
              </div>

              {/* AI advice */}
              {recs?.aiAdvice && (
                <div className="relative mb-5 rounded-xl bg-ink-900 p-5 text-white overflow-hidden">
                  <div className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-accent-500/20 blur-2xl" />
                  <div className="relative flex gap-3">
                    <div className="shrink-0 grid h-9 w-9 place-items-center rounded-lg bg-accent-500/20">
                      <Sparkles className="h-4 w-4 text-accent-300" />
                    </div>
                    <div>
                      <p className="text-xs uppercase tracking-wider text-accent-300/80">
                        Tư vấn từ Gemini AI
                      </p>
                      <p className="mt-1.5 leading-relaxed text-ink-100 whitespace-pre-wrap">
                        {recs.aiAdvice}
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {recsFetching ? (
                <Loader />
              ) : recs?.recommendations?.length > 0 ? (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {recs.recommendations.map((f) => (
                    <FoodCard key={f.foodId} food={f} liked={wishlist.isLiked(f.foodId)} showMatchScore />
                  ))}
                </div>
              ) : (
                <Empty
                  icon={AlertCircle}
                  title="Chưa có món phù hợp"
                  description="Hãy thử nới lỏng tiêu chí ăn kiêng trong hồ sơ."
                />
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function AnalysisCard({ analysis }) {
  const bmiClass = bmiBadgeTone(analysis.bmiCategory)
  const fillPct = Math.min(100, Math.max(0, ((analysis.bmi || 0) / 40) * 100))

  return (
    <div className="card p-6">
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">
            Chỉ số BMI
          </p>
          <p className="mt-1 font-display text-5xl font-bold tabular text-ink-900">
            {analysis.bmi || '—'}
          </p>
          <span className={`mt-2 inline-flex chip border ${bmiClass}`}>
            {analysis.bmiCategoryLabel}
          </span>
        </div>
        <div className="grid h-14 w-14 place-items-center rounded-xl bg-ink-50">
          <Heart className="h-6 w-6 text-ink-700" />
        </div>
      </div>

      {/* BMI scale */}
      <div className="mt-5">
        <div className="relative h-2 rounded-full bg-gradient-to-r from-blue-400 via-success-500 via-amber-400 to-danger-500">
          <div
            className="absolute -top-1 h-4 w-4 rounded-full border-2 border-white bg-ink-900 shadow-pop transition-all"
            style={{ left: `calc(${fillPct}% - 8px)` }}
          />
        </div>
        <div className="mt-1 flex justify-between text-[10px] text-ink-500">
          <span>Gầy</span>
          <span>Bình thường</span>
          <span>Thừa cân</span>
          <span>Béo phì</span>
        </div>
      </div>

      {/* Stats */}
      <div className="mt-6 grid grid-cols-2 sm:grid-cols-4 gap-3">
        <Stat icon={Flame} label="BMR" value={analysis.bmr} unit="kcal" />
        <Stat icon={Activity} label="TDEE" value={analysis.tdee} unit="kcal" />
        <Stat icon={TrendingUp} label="Mục tiêu/ngày" value={analysis.targetDailyCalories} unit="kcal" />
        <Stat
          icon={Sparkles}
          label="Calo/bữa"
          value={`${analysis.targetMealCaloriesMin}-${analysis.targetMealCaloriesMax}`}
          unit="kcal"
        />
      </div>

      {analysis.summary && (
        <p className="mt-5 rounded-lg bg-ink-50 px-3.5 py-2.5 text-sm text-ink-700">
          {analysis.summary}
        </p>
      )}

      {/* Prefer / Avoid tags */}
      {(analysis.preferTags?.length || analysis.avoidTags?.length) > 0 && (
        <div className="mt-5 space-y-2">
          {analysis.preferTags?.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-xs text-ink-500 mr-1">Nên ưu tiên:</span>
              {analysis.preferTags.map((t) => (
                <Badge key={t} tone="success">{t}</Badge>
              ))}
            </div>
          )}
          {analysis.avoidTags?.length > 0 && (
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-xs text-ink-500 mr-1">Nên hạn chế:</span>
              {analysis.avoidTags.map((t) => (
                <Badge key={t} tone="danger">{t}</Badge>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function Stat({ icon: Icon, label, value, unit }) {
  return (
    <div className="rounded-lg border border-ink-200 bg-white p-3">
      <Icon className="h-4 w-4 text-ink-500" />
      <p className="mt-1.5 font-semibold tabular text-sm text-ink-900">
        {value || '—'}
        <span className="text-[10px] text-ink-500 ml-1">{unit}</span>
      </p>
      <p className="text-[10px] uppercase tracking-wider text-ink-500">{label}</p>
    </div>
  )
}
