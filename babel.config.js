// Babel configuration for Vite
// Vite handles transpilation natively for ES2020+ targets
// This file is minimal and kept for potential pre-build scripts

module.exports = {
  presets: [
    ['@babel/preset-env', {
      targets: '> 0.5%, last 2 versions, not dead'
    }]
  ]
}
