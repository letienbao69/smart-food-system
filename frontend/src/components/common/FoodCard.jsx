import { Link } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { Heart, Plus, Flame, ShoppingCart } from 'lucide-react'
import toast from 'react-hot-toast'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { cartApi } from '@/api/cart'
import { wishlistApi } from '@/api/misc'
import { errMsg } from '@/api/client'
import { useCartStore } from '@/store/cart'
import { useAuth } from '@/store/auth'
import { cn, formatVND } from '@/lib/utils'
import { FoodImage, Badge } from '@/components/ui/Atoms'

/**
 * Reusable food card. Accepts food in any of these shapes:
 *   - FoodResponse: { id, name, price, imageUrl, calories, tags, ratingAvg, ... }
 *   - WishlistResponse: { foodId, foodName, ..., calories, tags }
 *   - HealthFoodDTO (from /health/recommendations): { foodId, foodName, ..., matchScore, reason }
 *
 * The `liked` prop is observed live so a parent can pass a `likedIds` set and
 * the heart icon stays in sync without per-card refetches.
 */
export default function FoodCard({ food, liked, showMatchScore, dense }) {
  const token = useAuth((s) => s.token)
  const setCart = useCartStore((s) => s.setCart)
  const refreshCart = useCartStore((s) => s.refresh)
  const qc = useQueryClient()

  // Normalize input shape — works for foods, wishlist entries, and AI recs.
  const foodId = food.id ?? food.foodId
  const name = food.name ?? food.foodName
  const imageUrl = food.imageUrl
  const price = food.price
  const stock = food.stock
  const calories = food.calories
  const ratingAvg = food.ratingAvg
  const tags = (food.tags || '').split(',').map((t) => t.trim()).filter(Boolean)
  const categoryName = food.categoryName

  const [isLiked, setIsLiked] = useState(!!liked)
  // Re-sync when parent passes updated `liked` (e.g. after wishlist Set loads)
  useEffect(() => {
    setIsLiked(!!liked)
  }, [liked])

  const addToCart = useMutation({
    mutationFn: () => cartApi.addItem(foodId, 1),
    onSuccess: (newCart) => {
      // BE returns the updated cart directly — use it instead of refetching
      if (newCart && Array.isArray(newCart.items)) {
        setCart(newCart)
      } else {
        refreshCart()
      }
      qc.invalidateQueries({ queryKey: ['cart'] })
      toast.success('Đã thêm vào giỏ hàng')
    },
    onError: (err) => toast.error(errMsg(err)),
  })

  const toggleLike = useMutation({
    mutationFn: () =>
      isLiked ? wishlistApi.remove(foodId) : wishlistApi.add(foodId),
    onSuccess: () => {
      const wasLiked = isLiked
      setIsLiked(!wasLiked)
      qc.invalidateQueries({ queryKey: ['wishlist'] })
      qc.invalidateQueries({ queryKey: ['wishlist-ids'] })
      qc.invalidateQueries({ queryKey: ['wishlist-check'] })
      toast.success(wasLiked ? 'Đã bỏ khỏi yêu thích' : 'Đã thêm vào yêu thích')
    },
    onError: (err) => toast.error(errMsg(err)),
  })

  const handleAddCart = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (!token) {
      toast('Vui lòng đăng nhập trước', { icon: '🔒' })
      return
    }
    addToCart.mutate()
  }

  const handleLike = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (!token) {
      toast('Vui lòng đăng nhập trước', { icon: '🔒' })
      return
    }
    toggleLike.mutate()
  }

  const lowSugar = tags.includes('LOW_SUGAR')
  const highProtein = tags.includes('HIGH_PROTEIN')

  return (
    <Link
      to={`/foods/${foodId}`}
      className="group relative block rounded-2xl border border-ink-200/80 bg-white overflow-hidden transition-all duration-300 hover:border-gold-300/70 hover:shadow-[0_14px_36px_-14px_rgba(66,96,53,0.30)] hover:-translate-y-1"
    >
      {/* Image */}
      <div className="relative aspect-[4/3] overflow-hidden bg-ink-100">
        <FoodImage
          src={imageUrl}
          name={name}
          size="full"
          className="rounded-none group-hover:scale-105 transition-transform duration-700 ease-out"
        />
        {/* Lớp phủ gradient nhẹ khi hover cho ảnh có chiều sâu */}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-t from-ink-950/25 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

        {/* Badge giảm giá */}
        {(() => {
          const dpct = Number(food.discountPercent) || 0
          return dpct > 0 ? (
            <div className="absolute top-2 left-2 rounded-full bg-promo-500 px-2.5 py-1 text-[11px] font-bold text-white shadow-sm">
              -{dpct}%
            </div>
          ) : null
        })()}

        {/* Match score (AI recommendations) */}
        {showMatchScore && typeof food.matchScore === 'number' && (
          <div className="absolute top-2 left-2 rounded-full bg-ink-900/90 backdrop-blur-sm px-2.5 py-1 text-[11px] font-semibold text-accent-300 flex items-center gap-1">
            <Flame className="h-3 w-3" />
            {Math.round(food.matchScore)} điểm
          </div>
        )}

        {/* Like button */}
        <button
          onClick={handleLike}
          disabled={toggleLike.isPending}
          className={cn(
            'absolute top-2 right-2 grid h-8 w-8 place-items-center rounded-full backdrop-blur-sm transition-all',
            isLiked
              ? 'bg-danger-500 text-white scale-100'
              : 'bg-white/90 text-ink-600 hover:text-danger-500 scale-90 group-hover:scale-100'
          )}
          aria-label={isLiked ? 'Bỏ khỏi yêu thích' : 'Thêm vào yêu thích'}
        >
          <Heart className={cn('h-4 w-4', isLiked && 'fill-current')} />
        </button>

        {stock === 0 && (
          <div className="absolute inset-0 grid place-items-center bg-ink-950/40 backdrop-blur-[2px]">
            <span className="chip border bg-white/95 border-ink-200 text-ink-700">
              Hết hàng
            </span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className={cn('p-4', dense && 'p-3')}>
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-medium text-sm text-ink-900 line-clamp-1">{name}</h3>
          {ratingAvg > 0 && (
            <span className="text-xs text-ink-500 tabular shrink-0">
              ★ {Number(ratingAvg).toFixed(1)}
            </span>
          )}
        </div>

        {categoryName && (
          <p className="mt-0.5 text-xs text-ink-500">{categoryName}</p>
        )}

        {/* Calorie + tags */}
        {(calories > 0 || highProtein || lowSugar) && (
          <div className="mt-2 flex flex-wrap items-center gap-1">
            {calories > 0 && <Badge tone="ink">{calories} kcal</Badge>}
            {highProtein && <Badge tone="success">Giàu đạm</Badge>}
            {lowSugar && <Badge tone="info">Ít đường</Badge>}
          </div>
        )}

        {/* AI reason */}
        {food.reason && (
          <p className="mt-2 text-xs text-ink-500 line-clamp-2 italic">
            {food.reason}
          </p>
        )}

        {/* Price */}
        <div className="mt-3">
          {(() => {
            const dpct = Number(food.discountPercent) || 0
            const finalPrice = dpct > 0 ? Math.round(Number(price) * (100 - dpct) / 100) : Number(price)
            return (
              <span className="flex items-baseline gap-2">
                <span className="font-display text-lg font-bold tabular text-accent-700">{formatVND(finalPrice)}</span>
                {dpct > 0 && <span className="text-xs text-ink-400 line-through tabular">{formatVND(price)}</span>}
              </span>
            )
          })()}
        </div>

        {/* Add to Cart — kiểu WebstaurantStore, màu xanh thương hiệu */}
        <button
          onClick={handleAddCart}
          disabled={stock === 0 || addToCart.isPending}
          className="mt-3 w-full inline-flex items-center justify-center gap-2 rounded-full bg-accent-700 py-2.5 text-sm font-semibold tracking-wide text-warm-50 hover:bg-accent-800 active:scale-[0.98] disabled:bg-ink-300 disabled:cursor-not-allowed transition-all"
        >
          <ShoppingCart className="h-4 w-4" />
          {stock === 0 ? 'Hết hàng' : 'Thêm vào giỏ'}
        </button>
      </div>
    </Link>
  )
}
