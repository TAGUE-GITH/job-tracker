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
    const u = { id: res.id, nom: res.nom, email: res.email, demo: !!res.demo }
    localStorage.setItem(USER_KEY, JSON.stringify(u))
    setUser(u)
  }

  const value = {
    user,
    login: async (data) => connecter(await api.login(data)),
    register: async (data) => connecter(await api.register(data)),
    demo: async () => connecter(await api.demo()),
    // Nouveau jeton apres un changement de mot de passe ou une deconnexion des autres appareils
    rafraichir: connecter,
    // Apres une modification du nom dans "Mon compte"
    majUtilisateur: (modifs) => {
      setUser((u) => {
        const nouveau = { ...u, ...modifs }
        localStorage.setItem(USER_KEY, JSON.stringify(nouveau))
        return nouveau
      })
    },
    logout: () => {
      setToken(null)
      localStorage.removeItem(USER_KEY)
      setUser(null)
    },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
