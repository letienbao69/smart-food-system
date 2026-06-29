import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Heart, ArrowRight } from 'lucide-react'
import { wishlistApi } from '@/api/misc'
import FoodCard from '@/components/common/FoodCard'
import { Loader, Empty, Skeleton } from '@/components/ui/Atoms'

export default function WishlistPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['wishlist'],
    queryFn: wishlistApi.list,
  })

  if (isLoading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <div className="mb-6">
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-4 w-72" />
        </div>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="card overflow-hidden">
              <Skeleton className="aspect-[4/3] rounded-none" />
              <div className="p-4 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  // WishlistResponse from BE: { id (wishlist row id), foodId, foodName, ... }
  // We must map to a food-shaped object so FoodCard uses foodId, not id.
  const items = (data || []).map((w) => {
    if (w.food) return w.food
    if (w.foodId) {
      // Normalize: discard wishlist row's `id` so it can't be mistaken for foodId
      const { id: _wishlistId, foodId, foodName, ...rest } = w
      return { id: foodId, name: foodName, ...rest }
    }
    return w
  })

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <p className="text-xs uppercase tracking-wider text-ink-500">Yêu thích</p>
        <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">
          Món bạn đã lưu
        </h1>
        <p className="mt-1 text-sm text-ink-500">
          {items.length} món trong danh sách yêu thích
        </p>
      </div>

      {items.length === 0 ? (
        <Empty
          icon={Heart}
          title="Chưa có món yêu thích"
          description="Bấm vào biểu tượng trái tim trên món bạn thích để lưu lại tại đây."
          action={
            <Link to="/foods" className="btn-primary btn">
              Khám phá thực đơn
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {items.map((f) => (
            <FoodCard key={f.id || f.foodId} food={f} liked />
          ))}
        </div>
      )}
    </div>
  )
}
