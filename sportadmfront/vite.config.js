import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@pages': path.resolve(__dirname, './src/pages'),
      '@assets': path.resolve(__dirname, './src/assets'),
      '@components': path.resolve(__dirname, './src/components'),
    },
  },
  server: {
    port: 5175,
    strictPort: true, // якщо зайнято — впаде, не переключиться
    host: true,
  },
  preview: {
    port: 5175,
    strictPort: true,
    host: true,
  },
})
