import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Minus, Plus, Trash2, ShoppingBag, ArrowRight } from 'lucide-react'
import toast from 'react-hot-toast'
import { cartApi } from '@/api/cart'
import { errMsg } from '@/api/client'
import { useCartStore } from '@/store/cart'
import { Loader, FoodImage, Empty } from '@/components/ui/Atoms'
import Button from '@/components/ui/Button'
import { formatVND } from '@/lib/utils'

export default function CartPage() {
  const qc = useQueryClient()
  const nav = useNavigate()
  const setCart = useCartStore((s) => s.setCart)
  const refresh = useCartStore((s) => s.refresh)

  const { data: cart, isLoading } = useQuery({
    queryKey: ['cart'],
    queryFn: cartApi.get,
  })

  const updateItem = useMutation({
    mutationFn: ({ id, qty }) => cartApi.updateItem(id, qty),
    onSuccess: (newCart) => {
      if (newCart && Array.isArray(newCart.items)) {
        setCart(newCart)
        qc.setQueryData(['cart'], newCart)
      } else {
        refresh()
      }
      qc.invalidateQueries({ queryKey: ['cart'] })
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const removeItem = useMutation({
    mutationFn: (id) => cartApi.removeItem(id),
    onSuccess: () => {
      toast.success('Đã xóa khỏi giỏ hàng')
      qc.invalidateQueries({ queryKey: ['cart'] })
      refresh()
    },
    onError: (e) => toast.error(errMsg(e)),
  })

  const clear = useMutation({
    mutationFn: cartApi.clear,
    onSuccess: () => {
      toast.success('Đã làm trống giỏ hàng')
      qc.invalidateQueries({ queryKey: ['cart'] })
      refresh()
    },
  })

  if (isLoading) return <Loader className="min-h-[50vh]" />

  const items = cart?.items || []
  const subtotal = items.reduce(
    (sum, it) => sum + Number(it.unitPrice || 0) * (it.quantity || 0),
    0
  )

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <Empty
          icon={ShoppingBag}
          title="Giỏ hàng đang trống"
          description="Hãy ghé thực đơn và thêm vài món bạn yêu thích."
          action={
            <Link to="/foods" className="btn-primary btn">
              Xem thực đơn
              <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex items-end justify-between mb-6">
        <div>
          <p className="text-xs uppercase tracking-wider text-ink-500">Giỏ hàng</p>
          <h1 className="mt-1 font-display text-3xl font-bold text-ink-900">
            {items.length} món trong giỏ
          </h1>
        </div>
        <button
          onClick={() => clear.mutate()}
          className="text-sm text-danger-600 hover:text-danger-700 inline-flex items-center gap-1"
        >
          <Trash2 className="h-3.5 w-3.5" />
          Xóa hết
        </button>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        {/* Items */}
        <div className="space-y-3">
          {items.map((it) => (
            <div key={it.id} className="card flex gap-4 p-4 items-center">
              <FoodImage src={it.imageUrl} name={it.foodName} size="md" />
              <div className="flex-1 min-w-0">
                <Link
                  to={`/foods/${it.foodId}`}
                  className="font-medium text-ink-900 hover:underline line-clamp-1"
                >
                  {it.foodName}
                </Link>
                <p className="mt-0.5 text-sm text-ink-500 tabular">
                  {formatVND(it.unitPrice)}
                </p>
                <div className="mt-2 inline-flex items-center rounded-lg border border-ink-200">
                  <button
                    onClick={() =>
                      it.quantity > 1
                        ? updateItem.mutate({ id: it.id, qty: it.quantity - 1 })
                        : removeItem.mutate(it.id)
                    }
                    className="grid h-8 w-8 place-items-center text-ink-600 hover:bg-ink-50 rounded-l-lg"
                  >
                    <Minus className="h-3.5 w-3.5" />
                  </button>
                  <span className="w-8 text-center text-sm font-medium tabular">
                    {it.quantity}
                  </span>
                  <button
                    onClick={() => updateItem.mutate({ id: it.id, qty: it.quantity + 1 })}
                    className="grid h-8 w-8 place-items-center text-ink-600 hover:bg-ink-50 rounded-r-lg"
                  >
                    <Plus className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
              <div className="text-right">
                <p className="font-semibold tabular text-ink-900">
                  {formatVND(Number(it.unitPrice) * it.quantity)}
                </p>
                <button
                  onClick={() => removeItem.mutate(it.id)}
                  className="mt-2 text-ink-400 hover:text-danger-600 transition"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Summary */}
        <div className="card p-5 h-fit lg:sticky lg:top-20">
          <h3 className="font-display font-semibold text-ink-900 mb-4">
            Tổng cộng
          </h3>
          <div className="space-y-2.5 text-sm">
            <div className="flex justify-between text-ink-700">
              <span>Tạm tính</span>
              <span className="tabular">{formatVND(subtotal)}</span>
            </div>
            <div className="flex justify-between text-ink-700">
              <span>Hình thức</span>
              <span className="text-ink-500">Đặt món trước · ăn tại quán</span>
            </div>
          </div>
          <div className="my-4 border-t border-ink-200" />
          <div className="flex justify-between items-baseline">
            <span className="text-sm text-ink-600">Tổng đơn</span>
            <span className="font-display text-2xl font-bold tabular text-ink-900">
              {formatVND(subtotal)}
            </span>
          </div>
          <Button onClick={() => nav('/reserve')} className="mt-5 w-full">
            Đặt bàn & gọi món
            <ArrowRight className="h-4 w-4" />
          </Button>
          <Link
            to="/foods"
            className="mt-2 block text-center text-xs text-ink-500 hover:text-ink-900"
          >
            ← Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    </div>
  )
}
