import { useState, useRef, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { MessageCircle, X, Send, Sparkles, Loader2, Bot } from 'lucide-react'
import { chatbotApi } from '@/api/admin'
import { FoodImage } from '@/components/ui/Atoms'
import { errMsg } from '@/api/client'
import { cn } from '@/lib/utils'

const SUGGESTIONS = [
  'Gợi ý món ít calo để giảm cân',
  'Món giàu đạm cho người tập gym',
  'Có món chay nào ngon không?',
  'Món phù hợp người tiểu đường',
]

export default function ChatbotWidget() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      text: 'Xin chào! Mình là **trợ lý dinh dưỡng** của SmartFood. Bạn có thể hỏi mình gợi ý món theo sức khỏe, BMI hoặc khẩu vị nhé!',
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const endRef = useRef(null)
  const showSuggestions = messages.length <= 1 && !loading

  useEffect(() => {
    if (open) endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, open, loading])

  const send = async (e, preset) => {
    e?.preventDefault()
    const msg = (preset ?? input).trim()
    if (!msg || loading) return
    setMessages((m) => [...m, { role: 'user', text: msg }])
    setInput('')
    setLoading(true)
    try {
      const res = await chatbotApi.send(msg)
      const reply =
        res?.response ||
        res?.answer ||
        res?.message ||
        'Mình chưa hiểu câu hỏi này. Bạn có thể nói rõ hơn không?'
      setMessages((m) => [...m, { role: 'assistant', text: reply, foods: res?.suggestedFoods || [] }])
    } catch (err) {
      setMessages((m) => [
        ...m,
        { role: 'assistant', text: errMsg(err, 'Xin lỗi, hệ thống đang bận.') },
      ])
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <button
        onClick={() => setOpen(!open)}
        className={cn(
          'fixed bottom-6 right-6 z-[60] flex items-center gap-2 rounded-full shadow-pop transition-all',
          open
            ? 'h-14 w-14 justify-center bg-ink-900 text-white scale-95'
            : 'h-14 px-5 bg-gradient-to-r from-accent-600 to-accent-500 text-white hover:scale-105 hover:shadow-xl'
        )}
        aria-label="Mở chatbot AI"
      >
        {open ? (
          <X className="h-5 w-5" />
        ) : (
          <>
            <Sparkles className="h-5 w-5" />
            <span className="font-semibold text-sm text-white">Trợ lý AI</span>
            <span className="h-2 w-2 rounded-full bg-white/90 animate-pulse" />
          </>
        )}
      </button>

      {open && (
        <div className="fixed bottom-24 right-6 z-[60] flex h-[560px] w-[380px] max-w-[calc(100vw-2rem)] flex-col rounded-3xl border border-ink-200 bg-white shadow-pop overflow-hidden animate-slide-up">
          {/* Header */}
          <div className="relative flex items-center justify-between bg-gradient-to-br from-accent-700 via-accent-600 to-accent-500 px-4 py-3.5 overflow-hidden">
            <div className="absolute -right-6 -top-8 h-24 w-24 rounded-full bg-white/10" />
            <div className="absolute right-10 top-6 h-12 w-12 rounded-full bg-white/10" />
            <div className="relative flex items-center gap-3">
              <div className="grid h-10 w-10 place-items-center rounded-full bg-white/20 backdrop-blur ring-2 ring-white/30">
                <Bot className="h-5 w-5 text-white" />
              </div>
              <div>
                <p className="text-sm font-bold text-white">Smart Assistant</p>
                <p className="text-[11px] text-white/80 flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-300 animate-pulse" />
                  Trợ lý dinh dưỡng AI
                </p>
              </div>
            </div>
          </div>

          {/* Messages */}
          <div className="flex-1 space-y-3 overflow-y-auto bg-gradient-to-b from-ink-50/60 to-white p-4">
            {messages.map((m, i) => (
              <div key={i}>
                <div
                  className={cn('flex items-end gap-2', m.role === 'user' ? 'justify-end' : 'justify-start')}
                >
                  {m.role === 'assistant' && (
                    <div className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-accent-100 text-accent-700">
                      <Sparkles className="h-3.5 w-3.5" />
                    </div>
                  )}
                  <div
                    className={cn(
                      'max-w-[80%] rounded-2xl px-3.5 py-2.5 text-sm leading-relaxed',
                      m.role === 'user'
                        ? 'bg-accent-600 text-white rounded-br-md whitespace-pre-wrap shadow-sm'
                        : 'bg-white text-ink-800 border border-ink-200 rounded-bl-md shadow-subtle'
                    )}
                  >
                    {m.role === 'user' ? m.text : <RichText text={m.text} />}
                  </div>
                </div>

                {/* Món gợi ý kèm ảnh — bấm để xem chi tiết */}
                {m.role === 'assistant' && m.foods?.length > 0 && (
                  <div className="mt-2 ml-9 space-y-2">
                    {m.foods.map((f) => (
                      <Link
                        key={f.id}
                        to={`/foods/${f.id}`}
                        onClick={() => setOpen(false)}
                        className="flex items-center gap-2.5 rounded-xl border border-ink-200 bg-white p-2 hover:border-accent-300 hover:shadow-sm transition"
                      >
                        <div className="h-12 w-12 shrink-0 overflow-hidden rounded-lg bg-ink-100">
                          <FoodImage src={f.imageUrl} name={f.name} size="full" className="rounded-none" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-ink-900">{f.name}</p>
                          <p className="truncate text-xs text-ink-400">
                            {f.calories ? `${f.calories} kcal` : ''}
                            {f.calories && f.categoryName ? ' · ' : ''}
                            {f.categoryName || ''}
                          </p>
                        </div>
                        {f.price != null && (
                          <span className="shrink-0 text-sm font-semibold text-accent-700 tabular">
                            {Number(f.price).toLocaleString('vi-VN')}đ
                          </span>
                        )}
                      </Link>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* Gợi ý câu hỏi nhanh */}
            {showSuggestions && (
              <div className="flex flex-col gap-2 pt-1">
                <p className="text-[11px] font-medium text-ink-400 px-1">Gợi ý câu hỏi:</p>
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    onClick={() => send(null, s)}
                    className="text-left text-[13px] rounded-xl border border-accent-200 bg-accent-50/60 px-3 py-2 text-accent-800 hover:bg-accent-100 hover:border-accent-300 transition"
                  >
                    {s}
                  </button>
                ))}
              </div>
            )}

            {loading && (
              <div className="flex items-end gap-2 justify-start">
                <div className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-accent-100 text-accent-700">
                  <Sparkles className="h-3.5 w-3.5" />
                </div>
                <div className="rounded-2xl rounded-bl-md bg-white border border-ink-200 px-4 py-3 shadow-subtle">
                  <div className="flex gap-1">
                    <span className="h-2 w-2 rounded-full bg-accent-400 animate-bounce" style={{ animationDelay: '0ms' }} />
                    <span className="h-2 w-2 rounded-full bg-accent-400 animate-bounce" style={{ animationDelay: '150ms' }} />
                    <span className="h-2 w-2 rounded-full bg-accent-400 animate-bounce" style={{ animationDelay: '300ms' }} />
                  </div>
                </div>
              </div>
            )}
            <div ref={endRef} />
          </div>

          {/* Input */}
          <form onSubmit={send} className="border-t border-ink-200 bg-white p-3 flex gap-2">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Nhập câu hỏi về món ăn, dinh dưỡng..."
              className="flex-1 rounded-full border border-ink-200 bg-ink-50/50 px-4 py-2.5 text-sm focus:border-accent-500 focus:bg-white focus:outline-none focus:ring-2 focus:ring-accent-100 transition"
            />
            <button
              type="submit"
              disabled={!input.trim() || loading}
              className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-gradient-to-br from-accent-600 to-accent-500 text-white hover:shadow-md disabled:opacity-40 disabled:hover:shadow-none transition"
            >
              <Send className="h-4 w-4" />
            </button>
          </form>
        </div>
      )}
    </>
  )
}

// ── Mini markdown renderer cho tin nhắn của trợ lý ──
// Hỗ trợ: **đậm**, dòng "- " thành gạch đầu dòng, xuống dòng giữ nguyên.
function RichText({ text }) {
  if (!text) return null
  const lines = String(text).split(/\r?\n/)

  // Render inline **bold** trong 1 dòng
  const renderInline = (line, keyPrefix) => {
    const parts = line.split(/(\*\*[^*]+\*\*)/g)
    return parts.map((p, i) => {
      if (/^\*\*[^*]+\*\*$/.test(p)) {
        return <strong key={`${keyPrefix}-${i}`} className="font-semibold text-ink-900">{p.slice(2, -2)}</strong>
      }
      return <span key={`${keyPrefix}-${i}`}>{p}</span>
    })
  }

  // Gom các dòng "- " thành 1 <ul>
  const nodes = []
  let bulletBuf = []
  const flushBullets = () => {
    if (bulletBuf.length) {
      nodes.push(
        <ul key={`ul-${nodes.length}`} className="list-disc pl-5 space-y-1 my-1">
          {bulletBuf.map((b, i) => <li key={i}>{renderInline(b, `b${nodes.length}-${i}`)}</li>)}
        </ul>
      )
      bulletBuf = []
    }
  }
  lines.forEach((raw, i) => {
    const line = raw.trimEnd()
    const m = line.match(/^\s*[-•]\s+(.*)$/)
    if (m) {
      bulletBuf.push(m[1])
      return
    }
    flushBullets()
    if (line.trim() === '') {
      nodes.push(<div key={`sp-${i}`} className="h-1" />)
    } else {
      nodes.push(<p key={`p-${i}`} className="my-0.5">{renderInline(line, `p${i}`)}</p>)
    }
  })
  flushBullets()
  return <div>{nodes}</div>
}
