import { cn } from '@/lib/utils'

export function Input({ className, label, error, ...rest }) {
  return (
    <div>
      {label && <label className="label">{label}</label>}
      <input className={cn('input', error && 'border-danger-500', className)} {...rest} />
      {error && <p className="mt-1 text-xs text-danger-600">{error}</p>}
    </div>
  )
}

export function Textarea({ className, label, rows = 4, ...rest }) {
  return (
    <div>
      {label && <label className="label">{label}</label>}
      <textarea rows={rows} className={cn('input resize-none', className)} {...rest} />
    </div>
  )
}

export function Select({ className, label, children, ...rest }) {
  return (
    <div>
      {label && <label className="label">{label}</label>}
      <select className={cn('input pr-9 cursor-pointer', className)} {...rest}>
        {children}
      </select>
    </div>
  )
}

export function Field({ label, hint, children }) {
  return (
    <div>
      {label && <label className="label">{label}</label>}
      {children}
      {hint && <p className="mt-1 text-xs text-ink-500">{hint}</p>}
    </div>
  )
}
