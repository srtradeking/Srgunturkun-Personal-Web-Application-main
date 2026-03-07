// vite.config.js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

// Use environment-based config: mode is 'development' or 'production'
export default defineConfig(({ mode }) => {
  // Enforce strict base path per environment
  const baseByMode = {
    development: '/',           // Dev server: from root
    production: '/'             // Production: adjust if deploying to subpath
  }

  return {
    root: path.resolve(__dirname),
    // Critical: base must match environment, never use relative paths
    base: baseByMode[mode] || '/',
    
    plugins: [vue()],
    
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    
    // ============ DEVELOPMENT SERVER ============
    // Dev server NEVER touches dist or public
    server: {
      port: 8180,
      open: false,
      // Setup dev server middleware for dist blocking and security headers
      configureServer: (server) => {
        return () => {
          server.middlewares.use((req, res, next) => {
            // Block dist/ requests during dev
            if (req.url.startsWith('/dist')) {
              res.writeHead(404)
              res.end('404 - dist not available in dev mode')
              return
            }
            // COOP header to allow popups (e.g. for third-party auth flows)
            res.setHeader('Cross-Origin-Opener-Policy', 'same-origin-allow-popups')
            next()
          })
        }
      },
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false
        }
      }
    },
    
    // ============ PRODUCTION BUILD ============
    // One-way build: dist is immutable output
    build: {
      outDir: 'dist',
      // Always delete dist before building
      emptyOutDir: true,
      // Modern target (ES2020)
      target: 'es2020',
      // Chunk size warning limit
      chunkSizeWarningLimit: 1000,
      // Rollup configuration for optimal output
      rollupOptions: {
        input: path.resolve(__dirname, 'index.html'),
        output: {
          // Assets go to dist/assets/ automatically
          assetFileNames: (assetInfo) => {
            const info = assetInfo.name.split('.')
            const ext = info[info.length - 1]
            if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'].includes(ext)) {
              return `assets/images/[name]-[hash][extname]`
            } else if (['mp4', 'webm', 'ogg'].includes(ext)) {
              return `assets/videos/[name]-[hash][extname]`
            } else if (ext === 'css') {
              return `assets/css/[name]-[hash][extname]`
            }
            return `assets/[name]-[hash][extname]`
          },
          chunkFileNames: 'assets/js/[name]-[hash].js',
          entryFileNames: 'assets/js/[name]-[hash].js',
          // Manual chunk splitting for better caching
          manualChunks: {
            // Vendor libraries (rarely change)
            'vendor-vue': ['vue', 'vue-router', 'pinia']
            // Removed vendor-bootstrap as it's not actually used in the frontend
          }
        }
      },
      // Source maps for production debugging (optional)
      sourcemap: false,
      // Minify
      minify: 'terser',
      // Report compressed size
      reportCompressedSize: true
    },
    
    // ============ ENVIRONMENT VARIABLES ============
    // Only VITE_ prefixed vars are exposed to frontend
    // Backend configs stay server-side
    envPrefix: 'VITE_'
  }
})