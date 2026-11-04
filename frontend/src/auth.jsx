import { createContext, useContext, useState } from 'react'
import { api, getToken, setToken } from './api'

const AuthContext = createContext(null)
const USER_KEY = 'jt_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem(USER_KEY)
    return getToken() && saved ? JSON.parse(saved) : null
  })

  const connecter = (res) => {
    setToken(res.token)
    const u = { id: res.id, nom: res.nom, email: res.email }
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
  }

  const value = {
    user,
    login: async (data) => connecter(await api.login(data)),
    register: async (data) => connecter(await api.register(data)),
    logout: () => {
      setToken(null)
      localStorage.removeItem(USER_KEY)
      setUser(null)
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
