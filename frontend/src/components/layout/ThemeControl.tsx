import { SegmentedControl } from '@/components/common'
import { useTheme } from '@/hooks/useTheme'

export function ThemeControl() {
  const { themePreference, resolvedTheme, setThemePreference } = useTheme()

  return (
    <div className='space-y-2'>
      <div className='flex items-baseline justify-between gap-4'>
        <p className='text-sm font-medium leading-none'>Theme</p>
        <p className='text-xs text-muted-foreground'>
          {themePreference === 'system'
            ? `System (${resolvedTheme})`
            : themePreference}
        </p>
      </div>
      <SegmentedControl
        aria-label='Theme'
        options={[
          { value: 'system', label: 'System' },
          { value: 'light', label: 'Light' },
          { value: 'dark', label: 'Dark' },
        ]}
        value={themePreference}
        onChange={setThemePreference}
      />
    </div>
  )
}

