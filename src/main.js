import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'
import './shared/styles/style.css'
import { setLogoutHandler } from './services/apiService'
import { useAuthStore } from './store/useAuthStore'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Set up global logout handler for API 401 errors
const authStore = useAuthStore()
setLogoutHandler(() => {
  authStore.logout()
})

app.mount('#app')