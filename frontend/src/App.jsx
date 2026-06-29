import { Routes, Route, Navigate } from 'react-router-dom'

// Layouts
import CustomerLayout from '@/components/layout/CustomerLayout'
import AdminLayout from '@/components/layout/AdminLayout'

// Guards
import { ProtectedRoute, AdminRoute, GuestOnly } from '@/routes/guards'

// Customer pages
import HomePage from '@/pages/customer/HomePage'
import FoodsPage from '@/pages/customer/FoodsPage'
import FoodDetailPage from '@/pages/customer/FoodDetailPage'
import HealthPage from '@/pages/customer/HealthPage'
import CartPage from '@/pages/customer/CartPage'
import OrdersPage from '@/pages/customer/OrdersPage'
import OrderDetailPage from '@/pages/customer/OrderDetailPage'
import ReservationPage from '@/pages/customer/ReservationPage'
import MyReservationsPage from '@/pages/customer/MyReservationsPage'
import ReservationDetailPage from '@/pages/customer/ReservationDetailPage'
import DepositPaymentPage from '@/pages/customer/DepositPaymentPage'
import PaymentSuccessPage from '@/pages/customer/PaymentSuccessPage'
import PaymentCancelPage from '@/pages/customer/PaymentCancelPage'
import ContactPage from '@/pages/customer/ContactPage'
import WishlistPage from '@/pages/customer/WishlistPage'
import ProfilePage from '@/pages/customer/ProfilePage'
import NotificationsPage from '@/pages/customer/NotificationsPage'

// Auth pages
import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'

// Admin pages
import DashboardPage from '@/pages/admin/DashboardPage'
import AdminFoodsPage from '@/pages/admin/AdminFoodsPage'
import AdminCategoriesPage from '@/pages/admin/AdminCategoriesPage'
import AdminOrdersPage from '@/pages/admin/AdminOrdersPage'
import InvoicePage from '@/pages/admin/InvoicePage'
import AdminTablesPage from '@/pages/admin/AdminTablesPage'
import AdminReservationsPage from '@/pages/admin/AdminReservationsPage'
import AdminContactsPage from '@/pages/admin/AdminContactsPage'
import AdminVouchersPage from '@/pages/admin/AdminVouchersPage'
import AdminUsersPage from '@/pages/admin/AdminUsersPage'
import AdminEmployeesPage from '@/pages/admin/AdminEmployeesPage'
import AdminNotificationsPage from '@/pages/admin/AdminNotificationsPage'

import { useAuth } from '@/store/auth'

// Trang gốc khu quản trị: admin -> Tổng quan; nhân viên (staff) -> Đặt bàn
function AdminHome() {
  const isAdmin = useAuth((s) => s.isAdmin())
  return isAdmin ? <DashboardPage /> : <Navigate to="/admin/reservations" replace />
}

export default function App() {
  return (
    <Routes>
      {/* Public auth routes (no nav, no layout) */}
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><RegisterPage /></GuestOnly>} />

      {/* Customer routes */}
      <Route element={<CustomerLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/foods" element={<FoodsPage />} />
        <Route path="/foods/:id" element={<FoodDetailPage />} />

        {/* Protected customer pages */}
        <Route path="/health" element={<ProtectedRoute><HealthPage /></ProtectedRoute>} />
        <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
        <Route path="/reserve" element={<ProtectedRoute><ReservationPage /></ProtectedRoute>} />
        <Route path="/reservations" element={<ProtectedRoute><MyReservationsPage /></ProtectedRoute>} />
        <Route path="/reservations/:id" element={<ProtectedRoute><ReservationDetailPage /></ProtectedRoute>} />
        <Route path="/deposit/:id" element={<ProtectedRoute><DepositPaymentPage /></ProtectedRoute>} />
        <Route path="/payment-success/:id" element={<ProtectedRoute><PaymentSuccessPage /></ProtectedRoute>} />
        <Route path="/payment-success" element={<ProtectedRoute><PaymentSuccessPage /></ProtectedRoute>} />
        <Route path="/payment-cancel" element={<ProtectedRoute><PaymentCancelPage /></ProtectedRoute>} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/orders" element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
        <Route path="/orders/:id" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
        <Route path="/wishlist" element={<ProtectedRoute><WishlistPage /></ProtectedRoute>} />
        <Route path="/profile" element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />
        <Route path="/notifications" element={<ProtectedRoute><NotificationsPage /></ProtectedRoute>} />
      </Route>

      {/* Admin routes */}
      <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
        <Route index element={<AdminHome />} />
        <Route path="reservations" element={<AdminReservationsPage />} />
        <Route path="contacts" element={<AdminContactsPage />} />
        <Route path="tables" element={<AdminTablesPage />} />
        <Route path="orders" element={<AdminOrdersPage />} />
        <Route path="orders/:id/invoice" element={<InvoicePage />} />
        <Route path="foods" element={<AdminFoodsPage />} />
        <Route path="categories" element={<AdminCategoriesPage />} />
        <Route path="vouchers" element={<AdminVouchersPage />} />
        <Route path="users" element={<AdminUsersPage />} />
        <Route path="employees" element={<AdminEmployeesPage />} />
        <Route path="reports" element={<Navigate to="/admin" replace />} />
        <Route path="notifications" element={<AdminNotificationsPage />} />
      </Route>

      {/* 404 → home */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
