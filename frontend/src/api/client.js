import axios from 'axios'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

// Inject JWT on every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401: drop token; let route guards handle redirect
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      const path = window.location.pathname
      const onAuthPage = path.startsWith('/login') || path.startsWith('/register')
      if (!onAuthPage) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login?expired=1'
      }
    }
    return Promise.reject(err)
  }
)

/** Unwrap the BE's ApiResponse<T> envelope { success, message, data }. */
export const unwrap = (res) => {
  const body = res?.data
  if (body && typeof body === 'object' && 'data' in body) return body.data
  return body
}

/** Extract a user-facing error message from an axios error. */
export const errMsg = (err, fallback = 'Có lỗi xảy ra, vui lòng thử lại') => {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.error ||
    err?.message ||
    fallback
  )
}
