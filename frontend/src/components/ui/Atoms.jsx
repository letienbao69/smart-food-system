import { Loader2 } from 'lucide-react'
import { cn, hashHue } from '@/lib/utils'

export function Loader({ className }) {
  return (
    <div className={cn('flex justify-center py-12', className)}>
      <Loader2 className="h-6 w-6 animate-spin text-ink-400" />
    </div>
  )
}

export function Empty({ icon: Icon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center">
      {Icon && (
        <div className="mb-4 rounded-full bg-ink-100 p-4">
          <Icon className="h-7 w-7 text-ink-400" />
        </div>
      )}
      <h3 className="font-display text-lg font-semibold text-ink-900">{title}</h3>
      {description && (
        <p className="mt-1 text-sm text-ink-500 max-w-md">{description}</p>
      )}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}

export function Badge({ children, className, tone = 'ink' }) {
  const tones = {
    ink:     'bg-ink-100 text-ink-700 border-ink-200',
    accent:  'bg-accent-50 text-accent-700 border-accent-200',
    success: 'bg-success-50 text-success-700 border-green-200',
    danger:  'bg-danger-50 text-danger-700 border-red-200',
    info:    'bg-blue-50 text-blue-700 border-blue-200',
  }
  return (
    <span className={cn('chip border', tones[tone], className)}>{children}</span>
  )
}

export function Skeleton({ className }) {
  return (
    <div className={cn('animate-pulse rounded-md bg-ink-200/70', className)} />
  )
}

/**
 * Food image with deterministic gradient placeholder if no imageUrl.
 * Stripe-style: no random emoji, just a refined gradient block.
 */
export function FoodImage({ src, name, className, size = 'md' }) {
  const hue = hashHue(name || '?')
  const sizes = {
    sm: 'h-12 w-12 text-base',
    md: 'h-16 w-16 text-lg',
    lg: 'h-32 w-32 text-2xl',
    full: 'aspect-square w-full text-3xl',
  }
  if (src) {
    return (
      <img
        src={src}
        alt={name || ''}
        className={cn('rounded-lg object-cover bg-ink-100', sizes[size], className)}
        loading="lazy"
        onError={(e) => {
          e.currentTarget.style.display = 'none'
          e.currentTarget.nextSibling.style.display = 'flex'
        }}
      />
    )
  }
  return (
    <div
      className={cn(
        'flex items-center justify-center rounded-lg font-display font-semibold text-white shadow-subtle',
        sizes[size],
        className
      )}
      style={{
        background: `linear-gradient(135deg, hsl(${hue} 70% 55%) 0%, hsl(${(hue + 30) % 360} 65% 45%) 100%)`,
      }}
    >
      {(name?.[0] || '?').toUpperCase()}
    </div>
  )
}

export function Divider({ children, className }) {
  if (children) {
    return (
      <div className={cn('relative flex items-center py-4', className)}>
        <div className="flex-grow border-t border-ink-200" />
        <span className="mx-3 text-xs uppercase tracking-wider text-ink-400">
          {children}
        </span>
        <div className="flex-grow border-t border-ink-200" />
      </div>
    )
  }
  return <hr className={cn('border-t border-ink-200', className)} />
}
