import { createContext, useContext, useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { useAuth } from '@/store/auth'

const WebSocketContext = createContext(null)

/**
 * Provides a connected STOMP client to all children.
 * Connects automatically when the user is logged in; disconnects on logout.
 * Access via: const client = useWebSocketClient()
 */
export function WebSocketProvider({ children }) {
  const token = useAuth((s) => s.token)
  const [client, setClient] = useState(null)
  const clientRef = useRef(null)

  useEffect(() => {
    // If no token, deactivate any existing client
    if (!token) {
      if (clientRef.current) {
        clientRef.current.deactivate().catch(() => {})
        clientRef.current = null
        setClient(null)
      }
      return
    }

    const stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        setClient(stompClient)
      },
      onStompError: (frame) => {
        console.warn('[WS] STOMP error:', frame.headers?.message)
      },
      onDisconnect: () => {
        setClient(null)
      },
    })

    stompClient.activate()
    clientRef.current = stompClient

    return () => {
      stompClient.deactivate().catch(() => {})
      clientRef.current = null
      setClient(null)
    }
  }, [token])

  return (
    <WebSocketContext.Provider value={client}>
      {children}
    </WebSocketContext.Provider>
  )
}

/** Returns the live STOMP client (null while disconnected). */
export function useWebSocketClient() {
  return useContext(WebSocketContext)
}
