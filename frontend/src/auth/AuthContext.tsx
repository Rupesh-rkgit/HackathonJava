import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearToken, getToken, setToken } from '../api/client'
import type { LoginResponse, Role } from '../api/types'

interface AuthState {
  enterpriseId: string
  name: string
  role: Role
}

interface AuthContextValue {
  user: AuthState | null
  isAdmin: boolean
  login: (enterpriseId: string, password: string) => Promise<void>
  logout: () => void
  ready: boolean
}

const SESSION_KEY = 'quizhub.session'
const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthState | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    const raw = localStorage.getItem(SESSION_KEY)
    if (raw && getToken()) {
      try { setUser(JSON.parse(raw)) } catch { /* ignore */ }
    }
    setReady(true)
  }, [])

  async function login(enterpriseId: string, password: string) {
    const res: LoginResponse = await api.login(enterpriseId, password)
    setToken(res.token)
    const session: AuthState = { enterpriseId: res.enterpriseId, name: res.name, role: res.role }
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    setUser(session)
  }

  function logout() {
    clearToken()
    localStorage.removeItem(SESSION_KEY)
    setUser(null)
  }

  const value = useMemo<AuthContextValue>(
    () => ({ user, isAdmin: user?.role === 'ADMIN', login, logout, ready }),
    [user, ready],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
