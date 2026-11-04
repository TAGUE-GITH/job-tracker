import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En dev, les appels /api sont redirigés vers le backend Spring Boot (pas besoin de CORS)
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
