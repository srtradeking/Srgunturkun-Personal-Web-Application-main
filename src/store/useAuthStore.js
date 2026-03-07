import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authAPI } from '@/services/apiService'
import { useCookieStore } from '@/store/useCookieStore'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loading = ref(true)
  const error = ref(null)

  const isLoggedIn = computed(() => !!user.value)
  const currentUser = computed(() => user.value)

  // Initialize auth state from localStorage
  const initAuth = () => {
    loading.value = true
    try {
      const token = localStorage.getItem('jwtToken')
      const userId = localStorage.getItem('userId')
      const email = localStorage.getItem('loginEmail')
      const displayName = localStorage.getItem('displayName')

      if (token && userId) {
        user.value = {
          uid: userId,
          email: email,
          displayName: displayName,
          token: token
        }
      } else {
        user.value = null
      }
    } catch (e) {
      console.error('Error initializing auth state:', e)
      user.value = null
    } finally {
      loading.value = false
    }
  }

  const login = async (credentials) => {
    loading.value = true
    error.value = null
    try {
      const response = await authAPI.login(credentials)
      
      // Update local state from response and localStorage (which authAPI sets)
      const userId = localStorage.getItem('userId')
      const email = localStorage.getItem('loginEmail')
      const displayName = localStorage.getItem('displayName')
      const token = localStorage.getItem('jwtToken')

      user.value = {
        uid: userId,
        email: email,
        displayName: displayName,
        token: token
      }

      // Record consent if needed
      try {
        const cookieStore = useCookieStore()
        await cookieStore.recordConsentToBackend()
      } catch (err) {
        console.warn('Error recording consent after login:', err)
      }

      return response
    } catch (err) {
      console.error('Login error:', err)
      error.value = err.message || 'Failed to login'
      throw err
    } finally {
      loading.value = false
    }
  }

  const logout = async () => {
    loading.value = true
    error.value = null
    try {
      authAPI.clearTokens()
      user.value = null
    } catch (err) {
      console.error('Logout error:', err)
      error.value = 'Failed to logout'
    } finally {
      loading.value = false
    }
  }

  const resendVerification = async (email) => {
    loading.value = true
    error.value = null
    try {
      await authAPI.resendVerification(email)
    } catch (err) {
      console.error('Resend verification error:', err)
      error.value = err.message || 'Failed to resend verification email'
      throw err
    } finally {
      loading.value = false
    }
  }

  const register = async (credentials) => {
    loading.value = true
    error.value = null
    try {
      return await authAPI.register(credentials)
    } catch (err) {
      console.error('Registration error:', err)
      error.value = err.message || 'Failed to register'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Initial check
  initAuth()

  return {
    user,
    loading,
    error,
    isLoggedIn,
    currentUser,
    initAuth,
    login,
    logout,
    resendVerification,
    register
  }
})
