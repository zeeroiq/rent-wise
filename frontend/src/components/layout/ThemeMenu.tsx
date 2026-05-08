import { Laptop, Moon, Sun } from 'lucide-react'

import {
  Button,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/components/common'
import { useTheme } from '@/hooks/useTheme'

export function ThemeMenu() {
  const { themePreference, setThemePreference } = useTheme()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant='ghost' size='icon' aria-label='Theme'>
          {themePreference === 'dark' ? (
            <Moon className='h-4 w-4' />
          ) : themePreference === 'light' ? (
            <Sun className='h-4 w-4' />
          ) : (
            <Laptop className='h-4 w-4' />
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align='end'>
        <DropdownMenuRadioGroup
          value={themePreference}
          onValueChange={(next) => setThemePreference(next as typeof themePreference)}
        >
          <DropdownMenuRadioItem value='system'>System</DropdownMenuRadioItem>
          <DropdownMenuRadioItem value='light'>Light</DropdownMenuRadioItem>
          <DropdownMenuRadioItem value='dark'>Dark</DropdownMenuRadioItem>
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

