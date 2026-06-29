import { create } from 'zustand'
import { cartApi } from '@/api/cart'

export const useCartStore = create((set, get) => ({
  cart: null,
  count: 0,

  setCart: (cart) => {
    const count =
      cart?.items?.reduce((sum, it) => sum + (it.quantity || 0), 0) || 0
    set({ cart, count })
  },

  refresh: async () => {
    try {
      const cart = await cartApi.get()
      get().setCart(cart)
      return cart
    } catch {
      // not logged in or empty
      set({ cart: null, count: 0 })
      return null
    }
  },

  reset: () => set({ cart: null, count: 0 }),
}))
