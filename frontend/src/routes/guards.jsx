import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '@/store/auth'

export function ProtectedRoute({ children }) {
  const token = useAuth((s) => s.token)
  const location = useLocation()
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return children
}

export function AdminRoute({ children }) {
  const token = useAuth((s) => s.token)
  const allowed = useAuth((s) => s.isAdminOrStaff())
  const location = useLocation()
  if (!token) return <Navigate to="/login" replace state={{ from: location }} />
  if (!allowed) return <Navigate to="/" replace />
  return children
}

export function GuestOnly({ children }) {
  const token = useAuth((s) => s.token)
  if (token) return <Navigate to="/" replace />
  return children
}
