import { createContext } from 'react'

export type Theme = 'light' | 'dark'
export type ThemePreference = Theme | 'system'

export interface ThemeContextType {
  themePreference: ThemePreference
  resolvedTheme: Theme
  setThemePreference: (next: ThemePreference) => void
  toggleTheme: () => void
}

export const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

