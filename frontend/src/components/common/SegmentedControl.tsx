import { cn } from '@/lib/utils'

type Option<T extends string> = {
  value: T
  label: string
}

type SegmentedControlProps<T extends string> = {
  'aria-label': string
  className?: string
  options: readonly Option<T>[]
  value: T
  onChange: (next: T) => void
}

export function SegmentedControl<T extends string>({
  className,
  options,
  value,
  onChange,
  ...props
}: SegmentedControlProps<T>) {
  return (
    <div
      {...props}
      role='group'
      className={cn(
        'inline-flex w-full rounded-lg border border-input bg-background p-1 text-sm shadow-sm',
        className,
      )}
    >
      {options.map((option) => (
        <button
          key={option.value}
          type='button'
          onClick={() => onChange(option.value)}
          className={cn(
            'flex-1 rounded-md px-3 py-1.5 font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background',
            option.value === value
              ? 'bg-primary text-primary-foreground shadow-sm'
              : 'text-foreground/80 hover:bg-accent/40 hover:text-foreground',
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
