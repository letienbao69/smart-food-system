import { useEffect, useRef } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { notificationsApi } from '@/api/admin'

const TOAST_STYLE = {
  background: '#1c1917',
  color: '#fafafa',
  fontSize: '13px',
  borderRadius: '12px',
  padding: '14px 16px',
  maxWidth: '380px',
  whiteSpace: 'pre-line',
}

/**
 * Polls admin notifications every 5s as a fallback safety net.
 * The primary real-time channel is WebSocket (useAdminOrderNotifications).
 * This catches any push that WebSocket missed (server restart, network blip).
 *
 * De-duplicates with a Set of seen IDs so no toast fires twice even when
 * both WebSocket and poll deliver the same notification.
 */
export function useAdminNotificationPoll() {
  const qc = useQueryClient()
  // null = first load (baseline), Set = subsequent loads
  const seenIdsRef = useRef(null)

  const { data: notifications } = useQuery({
    queryKey: ['admin-notifications-poll'],
    queryFn: notificationsApi.adminList,
    refetchInterval: 5_000,   // ↓ 15s → 5s for snappier fallback
    retry: false,
    notifyOnChangeProps: ['data'],
  })

  useEffect(() => {
    if (!notifications || !Array.isArray(notifications)) return

    const unread = notifications.filter((n) => !n.readStatus)
    const currentIds = new Set(unread.map((n) => n.id))

    // First load — just baseline, never toast on mount
    if (seenIdsRef.current === null) {
      seenIdsRef.current = currentIds
      return
    }

    const newNotifs = unread.filter((n) => !seenIdsRef.current.has(n.id))

    if (newNotifs.length > 0) {
      newNotifs.forEach((n) => {
        // Mark as seen immediately to prevent double-toast with WS push
        seenIdsRef.current.add(n.id)

        toast(`🔔 ${n.title}${n.message ? '\n' + n.message : ''}`, {
          duration: 4000,
          style: TOAST_STYLE,
        })
      })

      qc.invalidateQueries({ queryKey: ['admin-notifications'] })
    }

    seenIdsRef.current = currentIds
  }, [notifications, qc])
}

/**
 * Call this from useAdminOrderNotifications to de-duplicate:
 * when WS delivers a new notification, pre-register its id so the
 * poll doesn't fire a second toast for the same event.
 */
export function useRegisterSeenNotification() {
  const seenIdsRef = useRef(null)
  return (id) => {
    if (seenIdsRef.current) seenIdsRef.current.add(id)
  }
}
