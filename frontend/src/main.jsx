import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import { WebSocketProvider } from './providers/WebSocketProvider'
import App from './App'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30_000,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <WebSocketProvider>
          <App />
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 3000,
              style: {
                background: '#1c1917',
                color: '#fafafa',
                fontSize: '14px',
                borderRadius: '10px',
                padding: '12px 16px',
              },
              success: { iconTheme: { primary: '#10b981', secondary: '#1c1917' } },
            }}
          />
        </WebSocketProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
)
