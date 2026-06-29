import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Search, SlidersHorizontal, X, ChevronDown, ChevronUp } from 'lucide-react'
import { foodsApi, categoriesApi } from '@/api/foods'
import FoodCard from '@/components/common/FoodCard'
import { Skeleton, Empty } from '@/components/ui/Atoms'
import Pagination, { usePagination } from '@/components/ui/Pagination'
import { useWishlistIds } from '@/hooks/useWishlistIds'

const SORT_OPTIONS = [
  { value: 'name',          label: 'Tên A → Z' },
  { value: 'price-asc',     label: 'Giá: Thấp → Cao' },
  { value: 'price-desc',    label: 'Giá: Cao → Thấp' },
  { value: 'rating',        label: 'Đánh giá cao' },
  { value: 'calories-asc',  label: 'Calo: Thấp → Cao' },
]

const TAG_FILTERS = [
  { value: 'LOW_SUGAR',    label: 'Ít đường',     emoji: '🍬' },
  { value: 'LOW_FAT',      label: 'Ít béo',       emoji: '🥗' },
  { value: 'HIGH_PROTEIN', label: 'Giàu đạm',     emoji: '💪' },
  { value: 'VEGETARIAN',   label: 'Chay',         emoji: '🌿' },
  { value: 'VEGAN',        label: 'Thuần chay',   emoji: '🌱' },
  { value: 'GLUTEN_FREE',  label: 'Không gluten', emoji: '🌾' },
  { value: 'LOW_SODIUM',   label: 'Ít muối',      emoji: '🧂' },
  { value: 'HIGH_FIBER',   label: 'Nhiều chất xơ', emoji: '🫘' },
]

// Một món có khớp nhãn dinh dưỡng hay không.
// Ưu tiên: (1) đã gắn sẵn tag -> khớp; (2) suy ra từ số liệu đạm/béo/carb;
// (3) suy đoán từ tên/mô tả/nguyên liệu cho các nhãn không tính được từ macro.
function matchesNutriTag(f, tag) {
  const foodTags = (f.tags || '').split(',').map((s) => s.trim().toUpperCase())
  if (foodTags.includes(tag)) return true

  const protein = Number(f.proteinG ?? 0)
  const fat = Number(f.fatG ?? 0)
  const carbs = Number(f.carbsG ?? 0)
  const txt = `${f.name || ''} ${f.description || ''} ${f.ingredients || ''}`.toLowerCase()
  const hasMeat = /(thịt|gà|bò|heo|lợn|cá|tôm|mực|cua|nghêu|hải sản|trứng|xúc xích|giăm bông)/.test(txt)

  switch (tag) {
    case 'HIGH_PROTEIN': return protein >= 20
    case 'LOW_FAT':      return fat > 0 && fat <= 8
    case 'LOW_SUGAR':    return carbs > 0 && carbs <= 15
    case 'HIGH_FIBER':   return /(rau|salad|đậu|ngũ cốc|yến mạch|chất xơ|hạt|nấm)/.test(txt)
    case 'VEGETARIAN':   return /(chay|rau|salad|đậu|nấm)/.test(txt) && !hasMeat
    case 'VEGAN':        return /(thuần chay|rau|salad|đậu|nấm)/.test(txt) && !hasMeat && !/(sữa|phô mai|bơ|trứng|mật ong)/.test(txt)
    case 'GLUTEN_FREE':  return !/(bột mì|mì|bánh mì|pasta|nui|mỳ|lúa mì)/.test(txt)
    case 'LOW_SODIUM':   return /(hấp|luộc|salad|nhạt|ít muối)/.test(txt)
    default:             return false
  }
}

export default function FoodsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const categoryId = searchParams.get('categoryId') || ''
  const keyword    = searchParams.get('keyword')    || ''

  const [sort, setSort]             = useState('name')
  const [tags, setTags]             = useState([])
  const [filterOpen, setFilterOpen] = useState(false)
  const [priceMin, setPriceMin]     = useState('')
  const [priceMax, setPriceMax]     = useState('')
  const [calMax, setCalMax]         = useState('')

  const wishlist = useWishlistIds()

  const { data: foods, isLoading } = useQuery({
    queryKey: ['foods', { categoryId, keyword }],
    queryFn: () => foodsApi.list({ categoryId: categoryId || undefined, keyword: keyword || undefined }),
  })

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.list,
  })

  const filtered = useMemo(() => {
    if (!foods) return []
    let result = foods.filter((f) => f.status !== 'HIDDEN')

    // Tag filter — ưu tiên theo số dinh dưỡng thực tế, không chỉ dựa vào cột tags.
    if (tags.length > 0) {
      result = result.filter((f) => tags.every((t) => matchesNutriTag(f, t)))
    }

    // Price range
    if (priceMin) result = result.filter((f) => Number(f.price) >= Number(priceMin))
    if (priceMax) result = result.filter((f) => Number(f.price) <= Number(priceMax))

    // Calorie cap
    if (calMax) result = result.filter((f) => !f.calories || Number(f.calories) <= Number(calMax))

    // Sort
    const cmp = {
      name:          (a, b) => a.name.localeCompare(b.name, 'vi'),
      'price-asc':   (a, b) => Number(a.price) - Number(b.price),
      'price-desc':  (a, b) => Number(b.price) - Number(a.price),
      rating:        (a, b) => Number(b.ratingAvg || 0) - Number(a.ratingAvg || 0),
      'calories-asc':(a, b) => (a.calories || 9999) - (b.calories || 9999),
    }[sort]
    return [...result].sort(cmp)
  }, [foods, tags, sort, priceMin, priceMax, calMax])

  const { page, setPage, totalPages, paged } = usePagination(filtered, 12)

  const setQuery = (k, v) => {
    const sp = new URLSearchParams(searchParams)
    if (v) sp.set(k, v); else sp.delete(k)
    setSearchParams(sp)
  }

  const toggleTag = (t) =>
    setTags((prev) => prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t])

  const clearAll = () => { setTags([]); setPriceMin(''); setPriceMax(''); setCalMax('') }

  const activeFilters = tags.length + (priceMin ? 1 : 0) + (priceMax ? 1 : 0) + (calMax ? 1 : 0)

  const catName = categoryId
    ? categories?.find((c) => String(c.id) === String(categoryId))?.name || 'Danh mục'
    : 'Tất cả món ăn'

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">

      {/* Header */}
      <div className="mb-6">
        <p className="text-xs uppercase tracking-wider text-ink-400 font-medium">Thực đơn</p>
        <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">{catName}</h1>
      </div>

      {/* Search bar + sort + filter toggle */}
      <div className="flex flex-wrap gap-2 items-center mb-4">
        <div className="relative flex-1 min-w-[240px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
          <input
            value={keyword}
            onChange={(e) => setQuery('keyword', e.target.value)}
            placeholder="Tìm món ăn..."
            className="input pl-10 rounded-xl"
          />
        </div>

        <select
          value={sort}
          onChange={(e) => setSort(e.target.value)}
          className="input w-auto cursor-pointer rounded-xl"
        >
          {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>

        <button
          onClick={() => setFilterOpen((v) => !v)}
          className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2 text-sm font-medium transition ${
            filterOpen || activeFilters > 0
              ? 'border-ink-900 bg-ink-900 text-white'
              : 'border-ink-300 bg-white text-ink-700 hover:border-ink-400'
          }`}
        >
          <SlidersHorizontal className="h-4 w-4" />
          Bộ lọc
          {activeFilters > 0 ? (
            <span className="grid h-4.5 w-4.5 place-items-center rounded-full bg-white text-ink-900 text-[10px] font-bold px-1">
              {activeFilters}
            </span>
          ) : filterOpen ? (
            <ChevronUp className="h-3.5 w-3.5" />
          ) : (
            <ChevronDown className="h-3.5 w-3.5" />
          )}
        </button>
      </div>

      {/* ── Filter panel (dropdown style) ── */}
      {filterOpen && (
        <div className="mb-5 rounded-2xl border border-ink-200 bg-white p-5 shadow-sm animate-scale-in">
          <div className="flex items-center justify-between mb-4">
            <p className="font-display font-semibold text-ink-900">Bộ lọc nâng cao</p>
            {activeFilters > 0 && (
              <button onClick={clearAll} className="text-xs text-ink-500 hover:text-ink-900 inline-flex items-center gap-1">
                <X className="h-3 w-3" /> Xoá tất cả ({activeFilters})
              </button>
            )}
          </div>

          <div className="grid gap-6 md:grid-cols-3">
            {/* Nutritional tags */}
            <div className="md:col-span-3">
              <p className="text-xs font-semibold uppercase tracking-wider text-ink-400 mb-2">Nhãn dinh dưỡng</p>
              <div className="flex flex-wrap gap-2">
                {TAG_FILTERS.map((t) => (
                  <button
                    key={t.value}
                    onClick={() => toggleTag(t.value)}
                    className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
                      tags.includes(t.value)
                        ? 'border-ink-900 bg-ink-900 text-white'
                        : 'border-ink-200 bg-white text-ink-700 hover:border-ink-400'
                    }`}
                  >
                    <span>{t.emoji}</span>
                    {t.label}
                    {tags.includes(t.value) && <X className="h-2.5 w-2.5 ml-0.5" />}
                  </button>
                ))}
              </div>
            </div>

            {/* Price range */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-ink-400 mb-2">Khoảng giá (đ)</p>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  placeholder="Từ"
                  value={priceMin}
                  onChange={(e) => setPriceMin(e.target.value)}
                  className="input text-sm rounded-xl"
                  min={0}
                />
                <span className="text-ink-400 text-xs shrink-0">—</span>
                <input
                  type="number"
                  placeholder="Đến"
                  value={priceMax}
                  onChange={(e) => setPriceMax(e.target.value)}
                  className="input text-sm rounded-xl"
                  min={0}
                />
              </div>
            </div>

            {/* Max calories */}
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-ink-400 mb-2">Calo tối đa (kcal)</p>
              <input
                type="number"
                placeholder="VD: 500"
                value={calMax}
                onChange={(e) => setCalMax(e.target.value)}
                className="input text-sm rounded-xl"
                min={0}
              />
            </div>
          </div>
        </div>
      )}

      {/* Category chips (quick filter) */}
      {categories?.length > 0 && (
        <div className="flex gap-2 flex-wrap mb-5">
          <button
            onClick={() => setQuery('categoryId', '')}
            className={`rounded-full border px-3 py-1 text-xs font-medium transition ${
              !categoryId ? 'border-ink-900 bg-ink-900 text-white' : 'border-ink-200 bg-white text-ink-600 hover:border-ink-400'
            }`}
          >
            Tất cả
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              onClick={() => setQuery('categoryId', String(c.id))}
              className={`rounded-full border px-3 py-1 text-xs font-medium transition ${
                String(categoryId) === String(c.id)
                  ? 'border-ink-900 bg-ink-900 text-white'
                  : 'border-ink-200 bg-white text-ink-600 hover:border-ink-400'
              }`}
            >
              {c.name}
            </button>
          ))}
        </div>
      )}

      {/* Result count */}
      <p className="text-sm text-ink-500 mb-4">
        Hiển thị <span className="font-semibold text-ink-900">{filtered.length}</span> món
      </p>

      {/* Grid */}
      {isLoading ? (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} className="rounded-2xl overflow-hidden border border-ink-200">
              <Skeleton className="aspect-[4/3] rounded-none" />
              <div className="p-4 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <Empty title="Không tìm thấy món ăn nào" description="Thử bỏ bớt điều kiện lọc hoặc tìm từ khoá khác." />
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {paged.map((f) => <FoodCard key={f.id} food={f} liked={wishlist.isLiked(f.id)} />)}
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} className="justify-center" />
    </div>
  )
}
