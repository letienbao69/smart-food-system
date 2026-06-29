import { useQuery } from '@tanstack/react-query'
import { wishlistApi } from '@/api/misc'
import { useAuth } from '@/store/auth'

/**
 * Pre-fetches the user's wishlist as a Set of foodIds, so list pages
 * can render the heart icon in the correct state on first paint
 * (no per-card check API needed).
 */
export function useWishlistIds() {
  const token = useAuth((s) => s.token)
  const query = useQuery({
    queryKey: ['wishlist-ids'],
    queryFn: wishlistApi.list,
    enabled: !!token,
    staleTime: 60_000,
  })

  const ids = new Set(
    (query.data || []).map((w) => Number(w.foodId ?? w.id))
  )

  return {
    isLiked: (foodId) => ids.has(Number(foodId)),
    set: ids,
    refetch: query.refetch,
    loading: query.isLoading,
  }
}
