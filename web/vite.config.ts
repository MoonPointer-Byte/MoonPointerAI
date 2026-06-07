import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/** file:// / WebView cannot load module scripts with crossorigin */
function stripCrossOriginForFileProtocol() {
  return {
    name: 'strip-crossorigin-for-file-protocol',
    transformIndexHtml(html: string) {
      return html.replace(/\s+crossorigin/g, '')
    },
  }
}

export default defineConfig({
  base: './',
  plugins: [react(), stripCrossOriginForFileProtocol()],
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
})
