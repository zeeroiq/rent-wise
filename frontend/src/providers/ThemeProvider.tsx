import React, { useEffect, useMemo, useState } from 'react'
import { ThemeContext, type Theme, type ThemePreference } from './themeContext'

function getSystemTheme(): Theme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function readStoredPreference(): ThemePreference {
  const stored = localStorage.getItem('theme')
  if (stored === 'light' || stored === 'dark' || stored === 'system') {
    return stored
  }
  return 'system'
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [themePreference, setThemePreference] = useState<ThemePreference>(() =>
    readStoredPreference(),
  )
  const [systemTheme, setSystemTheme] = useState<Theme>(() => getSystemTheme())

  const resolvedTheme = useMemo<Theme>(() => {
    if (themePreference === 'system') return systemTheme
    return themePreference
  }, [systemTheme, themePreference])

  useEffect(() => {
    localStorage.setItem('theme', themePreference)
  }, [themePreference])

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    const sync = () => setSystemTheme(media.matches ? 'dark' : 'light')
    sync()
    media.addEventListener('change', sync)
    return () => media.removeEventListener('change', sync)
  }, [])

  useEffect(() => {
    const root = document.documentElement
    root.dataset.theme = resolvedTheme
    root.style.colorScheme = resolvedTheme
    if (resolvedTheme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  }, [resolvedTheme])

  const toggleTheme = () => {
    // Keep toggle semantics: flip between explicit light/dark, leaving "system" behind.
    setThemePreference((prev) => {
      const current = prev === 'system' ? systemTheme : prev
      return current === 'light' ? 'dark' : 'light'
    })
  }

  return (
    <ThemeContext.Provider
      value={{ themePreference, resolvedTheme, setThemePreference, toggleTheme }}
    >
      {children}
    </ThemeContext.Provider>
  )
}
