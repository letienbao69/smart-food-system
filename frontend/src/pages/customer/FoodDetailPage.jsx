import { useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Heart,
  ShoppingCart,
  ArrowLeft,
  Star,
  Flame,
  Beef,
  Wheat,
  Droplet,
  ChevronRight,
  Minus,
  Plus,
} from 'lucide-react'
import toast from 'react-hot-toast'
import { foodsApi } from '@/api/foods'
import { reviewsApi, wishlistApi } from '@/api/misc'
import { cartApi } from '@/api/cart'
import { errMsg } from '@/api/client'
import { useAuth } from '@/store/auth'
import { useCartStore } from '@/store/cart'
import { formatVND, formatDate } from '@/lib/utils'
import { Loader, FoodImage, Badge } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'

export default function FoodDetailPage() {
  const { id } = useParams()
  const nav = useNavigate()
  const qc = useQueryClient()
  const token = useAuth((s) => s.token)
  const setCart = useCartStore((s) => s.setCart)
  const refreshCart = useCartStore((s) => s.refresh)
  const [qty, setQty] = useState(1)

  const { data: food, isLoading } = useQuery({
    queryKey: ['food', id],
    queryFn: () => foodsApi.get(id),
  })

  const { data: reviews } = useQuery({
    queryKey: ['reviews', id],
    queryFn: () => reviewsApi.byFood(id),
  })

  const { data: liked } = useQuery({
    queryKey: ['wishlist-check', id],
    queryFn: () => wishlistApi.check(id),
    enabled: !!token,
  })

  const isLiked = liked?.liked

  const toggleLike = useMutation({
    mutationFn: () => (isLiked ? wishlistApi.remove(id) : wishlistApi.add(id)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wishlist-check', id] })
      qc.invalidateQueries({ queryKey: ['wishlist'] })
      qc.invalidateQueries({ queryKey: ['wishlist-ids'] })
      toast.success(isLiked ? 'Đã bỏ khỏi yêu thích' : 'Đã thêm vào yêu thích')
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const addCart = useMutation({
    mutationFn: () => cartApi.addItem(id, qty),
    onSuccess: (newCart) => {
      if (newCart && Array.isArray(newCart.items)) {
        setCart(newCart)
      } else {
        refreshCart()
      }
      qc.invalidateQueries({ queryKey: ['cart'] })
      toast.success(`Đã thêm ${qty} món vào giỏ`)
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  if (isLoading) return <Loader className="min-h-[60vh]" />
  if (!food) return <div className="p-12 text-center">Không tìm thấy món</div>

  const requireLogin = () => {
    if (!token) {
      toast('Vui lòng đăng nhập trước', { icon: '🔒' })
      return false
    }
    return true
  }

  const tags = (food.tags || '').split(',').map((t) => t.trim()).filter(Boolean)
  const tagLabel = {
    HIGH_PROTEIN: 'Giàu đạm',
    LOW_FAT: 'Ít béo',
    LOW_SUGAR: 'Ít đường',
    LOW_SODIUM: 'Ít muối',
    HIGH_FIBER: 'Nhiều chất xơ',
    VEGETARIAN: 'Chay',
    VEGAN: 'Thuần chay',
    KETO: 'Keto',
    GLUTEN_FREE: 'Không gluten',
    DIABETIC_FRIENDLY: 'Tiểu đường thân thiện',
    CONTAINS_SEAFOOD: 'Có hải sản',
    CONTAINS_NUTS: 'Có hạt',
    CONTAINS_DAIRY: 'Có sữa',
    HIGH_SUGAR: 'Nhiều đường',
    HIGH_FAT: 'Nhiều béo',
    HIGH_SODIUM: 'Nhiều muối',
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-6 sm:px-6 lg:px-8">
      {/* Breadcrumb */}
      <nav className="mb-4 flex items-center gap-1.5 text-xs text-ink-500">
        <Link to="/" className="hover:text-ink-900">Trang chủ</Link>
        <ChevronRight className="h-3 w-3" />
        <Link to="/foods" className="hover:text-ink-900">Thực đơn</Link>
        <ChevronRight className="h-3 w-3" />
        <span className="text-ink-900 truncate">{food.name}</span>
      </nav>

      <div className="grid gap-10 lg:grid-cols-2">
        {/* Image */}
        <div className="relative">
          <div className="overflow-hidden rounded-2xl border border-ink-200 bg-white">
            <FoodImage src={food.imageUrl} name={food.name} size="full" className="rounded-none" />
          </div>
          {food.ratingAvg > 0 && (
            <div className="absolute top-3 left-3 flex items-center gap-1 rounded-full bg-ink-900/90 backdrop-blur-sm px-3 py-1 text-xs font-medium text-white">
              <Star className="h-3.5 w-3.5 fill-accent-400 text-accent-400" />
              {Number(food.ratingAvg).toFixed(1)}
            </div>
          )}
        </div>

        {/* Detail */}
        <div>
          {food.categoryName && (
            <p className="text-xs uppercase tracking-wider text-ink-500">{food.categoryName}</p>
          )}
          <h1 className="mt-1 font-display text-4xl font-bold text-ink-900">{food.name}</h1>

          {food.description && (
            <p className="mt-3 text-ink-600 leading-relaxed">{food.description}</p>
          )}

          {/* Nutrition cards */}
          {(food.calories || food.proteinG || food.fatG || food.carbsG) && (
            <div className="mt-5 grid grid-cols-4 gap-2">
              <NutriCard icon={Flame} label="Calo" value={food.calories} unit="kcal" />
              <NutriCard icon={Beef} label="Đạm" value={food.proteinG} unit="g" />
              <NutriCard icon={Droplet} label="Béo" value={food.fatG} unit="g" />
              <NutriCard icon={Wheat} label="Carb" value={food.carbsG} unit="g" />
            </div>
          )}

          {/* Tags */}
          {tags.length > 0 && (
            <div className="mt-4 flex flex-wrap gap-1.5">
              {tags.map((t) => (
                <Badge key={t} tone={t.startsWith('HIGH_') || t.startsWith('CONTAINS_') ? 'danger' : 'success'}>
                  {tagLabel[t] || t}
                </Badge>
              ))}
            </div>
          )}

          <div className="my-6 border-t border-ink-200" />

          {(() => {
            const discount = Number(food.discountPercent) || 0
            const finalPrice = discount > 0 ? Math.round(Number(food.price) * (100 - discount) / 100) : Number(food.price)
            return (
              <>
                <div className="flex items-baseline gap-3 flex-wrap">
                  <p className="font-display text-3xl font-bold text-danger-600 tabular">{formatVND(finalPrice)}</p>
                  {discount > 0 && (
                    <>
                      <span className="text-lg text-ink-400 line-through tabular">{formatVND(food.price)}</span>
                      <Badge tone="danger">Giảm {discount}%</Badge>
                    </>
                  )}
                </div>
                <p className="mt-1 text-sm text-ink-500">
                  {food.stock > 0 ? `Còn ${food.stock} suất` : 'Tạm hết hàng'}
                </p>
              </>
            )
          })()}

          {/* Nguyên liệu */}
          {food.ingredients && (
            <div className="mt-5">
              <p className="font-display font-semibold text-ink-900 mb-2">Nguyên liệu:</p>
              <div className="flex flex-wrap gap-1.5">
                {food.ingredients.split(',').map((it, i) => (
                  <span key={i} className="rounded-md bg-blue-50 border border-blue-100 px-2.5 py-1 text-xs text-blue-700">
                    {it.trim()}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Thời gian chuẩn bị + trạng thái */}
          <div className="mt-5 flex gap-3 flex-wrap">
            {food.prepTimeMinutes && (
              <div className="rounded-xl bg-ink-50 border border-ink-200 px-4 py-3">
                <p className="text-xs text-ink-500">Thời gian chuẩn bị:</p>
                <p className="font-display font-bold text-ink-900">{food.prepTimeMinutes} phút</p>
              </div>
            )}
            <div className="rounded-xl bg-ink-50 border border-ink-200 px-4 py-3">
              <p className="text-xs text-ink-500">Trạng thái:</p>
              <Badge tone={food.status === 'AVAILABLE' ? 'success' : 'danger'}>
                {food.status === 'AVAILABLE' ? 'Có sẵn' : 'Hết hàng'}
              </Badge>
            </div>
          </div>

          {/* Quantity + actions */}
          <div className="mt-5 flex flex-wrap items-center gap-3">
            <div className="inline-flex items-center rounded-lg border border-ink-200 bg-white">
              <button
                onClick={() => setQty((q) => Math.max(1, q - 1))}
                className="grid h-10 w-10 place-items-center text-ink-600 hover:bg-ink-50 rounded-l-lg"
              >
                <Minus className="h-4 w-4" />
              </button>
              <span className="w-10 text-center font-medium tabular">{qty}</span>
              <button
                onClick={() => setQty((q) => q + 1)}
                className="grid h-10 w-10 place-items-center text-ink-600 hover:bg-ink-50 rounded-r-lg"
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
            <Button
              size="lg"
              loading={addCart.isPending}
              disabled={food.stock === 0}
              onClick={() => requireLogin() && addCart.mutate()}
              className="flex-1 min-w-[160px]"
            >
              <ShoppingCart className="h-4 w-4" />
              Thêm vào giỏ
            </Button>
            <Button
              size="lg"
              variant="secondary"
              onClick={() => requireLogin() && toggleLike.mutate()}
              className={isLiked ? 'text-danger-600 border-danger-200' : ''}
            >
              <Heart className={`h-4 w-4 ${isLiked ? 'fill-current' : ''}`} />
            </Button>
          </div>
        </div>
      </div>

      {/* Reviews */}
      <div className="mt-14">
        <h2 className="font-display text-2xl font-bold text-ink-900">Đánh giá</h2>
        <p className="mt-1 text-sm text-ink-500">
          {reviews?.length || 0} đánh giá từ khách hàng
        </p>
        <div className="mt-5 space-y-4">
          {reviews?.length === 0 && (
            <div className="rounded-xl border border-dashed border-ink-300 p-8 text-center text-sm text-ink-500">
              Chưa có đánh giá nào. Hãy là người đầu tiên!
            </div>
          )}
          {reviews?.map((r) => (
            <div key={r.id} className="card p-4">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-1">
                    {Array.from({ length: 5 }).map((_, i) => (
                      <Star
                        key={i}
                        className={`h-3.5 w-3.5 ${
                          i < r.rating
                            ? 'fill-accent-400 text-accent-400'
                            : 'text-ink-200'
                        }`}
                      />
                    ))}
                  </div>
                  <p className="mt-1 text-sm font-medium text-ink-900">{r.userName || 'Người dùng'}</p>
                </div>
                <span className="text-xs text-ink-500">{formatDate(r.createdAt)}</span>
              </div>
              {r.comment && <p className="mt-2 text-sm text-ink-700">{r.comment}</p>}
              {r.sentimentLabel && (
                <Badge
                  className="mt-2"
                  tone={
                    r.sentimentLabel === 'POSITIVE'
                      ? 'success'
                      : r.sentimentLabel === 'NEGATIVE'
                      ? 'danger'
                      : 'ink'
                  }
                >
                  {r.sentimentLabel}
                </Badge>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function NutriCard({ icon: Icon, label, value, unit }) {
  return (
    <div className="rounded-lg border border-ink-200 bg-white p-2.5 text-center">
      <Icon className="mx-auto h-4 w-4 text-ink-500" />
      <p className="mt-1 font-semibold text-sm tabular text-ink-900">
        {value || 0}
        <span className="text-[10px] text-ink-500 ml-0.5">{unit}</span>
      </p>
      <p className="text-[10px] text-ink-500">{label}</p>
    </div>
  )
}
