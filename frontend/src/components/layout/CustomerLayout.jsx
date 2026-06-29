import { Outlet, Navigate } from 'react-router-dom'
import Navbar from './Navbar'
import Footer from './Footer'
import ChatbotWidget from '@/components/common/ChatbotWidget'
import { useAuth } from '@/store/auth'
import { useCustomerOrderNotifications } from '@/hooks/useCustomerOrderNotifications'

function CustomerLayoutInner() {
  // Subscribe to order status updates via WebSocket
  useCustomerOrderNotifications()

  return (
    <div className="flex min-h-screen flex-col bg-ink-50">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
      <ChatbotWidget />
    </div>
  )
}

export default function CustomerLayout() {
  // Admin và nhân viên vẫn được xem trang khách (có nút "Khu quản trị" để quay lại).
  return <CustomerLayoutInner />
}
