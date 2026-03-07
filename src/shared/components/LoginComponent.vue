<template>
  <div class="login-box">
    <h2>Login</h2>
    <form @submit.prevent="handleLogin" v-if="!isLoggedIn">
      <div class="mb-3">
        <label>Email</label>
        <input v-model="email" type="email" class="form-control" required autocomplete="email" />
      </div>
      <div class="mb-3">
        <label>Password</label>
        <input v-model="password" type="password" class="form-control" required autocomplete="current-password" />
      </div>
      <button type="submit" class="btn btn-success">Login</button>

      <div v-if="error" class="alert alert-danger mt-2">
        {{ error }}
        <div v-if="showResendButton" class="mt-2">
          <button type="button" @click="resendVerificationEmail" class="btn btn-sm btn-outline-primary" :disabled="resendingEmail">
            {{ resendingEmail ? 'Sending...' : 'Resend Verification Email' }}
          </button>
        </div>
      </div>
    </form>
    <div v-else class="alert alert-info">
      Logged in as <strong>{{ loggedInEmail }}</strong>
      <button class="btn btn-sm btn-outline-secondary ms-2" @click="handleLogout">Logout</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { storeToRefs } from 'pinia'

const authStore = useAuthStore()
const { isLoggedIn, currentUser } = storeToRefs(authStore)
const email = ref('')
const password = ref('')
const error = ref('')
const loggedInEmail = computed(() => currentUser.value?.email || '')
const showResendButton = ref(false)
const resendingEmail = ref(false)

const handleLogin = async () => {
  error.value = ''
  showResendButton.value = false

  if (!email.value || !password.value) {
    error.value = 'Please fill in all fields'
    return
  }

  try {
    await authStore.login({ email: email.value, password: password.value })

    email.value = ''
    password.value = ''
    window.location.reload() // Added for autorefresh
  } catch (e) {
    console.error('Login error:', e)
    const message = e && e.message ? e.message : 'Failed to login. Please try again.'
    const status = e && e.status ? e.status : null

    if (status === 403 && message && message.toUpperCase().includes('EMAIL_NOT_VERIFIED')) {
      error.value = 'Your email address is not verified. Please check your inbox for the verification email or resend it.'
      showResendButton.value = true
    } else if (message.toLowerCase().includes('invalid email or password')) {
      error.value = 'Invalid email or password'
    } else {
      error.value = message
    }
  }
}

const resendVerificationEmail = async () => {
  if (!email.value) {
    error.value = 'Please enter your email address first.'
    return
  }

  try {
    resendingEmail.value = true
    await authStore.resendVerification(email.value)
    error.value = 'If an account exists for this email and is not verified, a verification email has been sent.'
  } catch (e) {
    console.error('Resend verification error:', e)
    error.value = 'Failed to resend verification email. Please try again later.'
  } finally {
    resendingEmail.value = false
  }
}

const handleLogout = async () => {
  try {
    await authStore.logout()
    window.location.reload() // Added for autorefresh
  } catch (e) {
    error.value = 'Failed to logout. Please try again.'
  }
}
</script>

<style scoped>
.login-box {
  max-width: 350px;
  margin: 2rem auto;
  padding: 2rem;
  background: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.btn {
  width: 100%;
  padding: 12px;
  border: 1px solid #52595c;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover {
  background: #218838;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 16px;
}

.mb-3 { margin-bottom: 1rem; }
.ms-2 { margin-left: 0.5rem; }
.mt-2 { margin-top: 0.5rem; }
.alert { padding: 12px; border-radius: 4px; margin-top: 1rem; }
.alert-danger { background: #f8d7da; color: #721c24; border: 1px solid #f5c6cb; }
.alert-info { background: #d1ecf1; color: #0c5460; border: 1px solid #bee5eb; }
.btn-sm { font-size: 0.875rem; padding: 6px 12px; }
.btn-outline-primary { background: white; color: #007bff; border: 1px solid #007bff; }
.signup-link { text-align: center; margin-top: 20px; }
</style>
