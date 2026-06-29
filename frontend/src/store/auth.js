import { create } from 'zustand'
import { authApi } from '@/api/auth'

const persisted = () => {
  try {
    const user = localStorage.getItem('user')
    return {
      token: localStorage.getItem('token') || null,
      user: user ? JSON.parse(user) : null,
    }
  } catch {
    return { token: null, user: null }
  }
}

export const useAuth = create((set, get) => ({
  ...persisted(),
  loading: false,

  isLoggedIn: () => !!get().token,

  isAdmin: () => {
    const u = get().user
    if (!u) return false
    if (typeof u.role === 'string') return u.role.toUpperCase() === 'ADMIN'
    const roles = u.roles || []
    return roles.some((r) => {
      const name = typeof r === 'string' ? r : r?.name
      return name?.toUpperCase() === 'ADMIN'
    })
  },

  hasRole: (target) => {
    const u = get().user
    if (!u) return false
    const t = target.toUpperCase()
    if (typeof u.role === 'string' && u.role.toUpperCase() === t) return true
    return (u.roles || []).some((r) => {
      const name = typeof r === 'string' ? r : r?.name
      return name?.toUpperCase() === t
    })
  },

  isStaff: () => get().hasRole('STAFF'),

  // Được vào khu quản trị: admin hoặc nhân viên
  isAdminOrStaff: () => get().hasRole('ADMIN') || get().hasRole('STAFF'),

  setAuth: (token, user) => {
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(user))
    set({ token, user })
  },

  login: async (email, password) => {
    set({ loading: true })
    try {
      const data = await authApi.login(email, password)
      // BE shape: { token, email, fullName, role }
      const token = data.token || data.accessToken || data.jwt
      const user = {
        id: data.id,
        email: data.email,
        fullName: data.fullName,
        phone: data.phone,
        avatarUrl: data.avatarUrl,
        role: data.role, // singular for current BE
        roles: data.roles, // plural for future-proofing
      }
      if (!token) throw new Error('Phản hồi đăng nhập thiếu token')
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
      set({ token, user, loading: false })
      return user
    } catch (err) {
      set({ loading: false })
      throw err
    }
  },

  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    set({ token: null, user: null })
  },

  refreshMe: async () => {
    if (!get().token) return null
    try {
      const me = await authApi.me()
      localStorage.setItem('user', JSON.stringify(me))
      set({ user: me })
      return me
    } catch {
      return null
    }
  },
}))
