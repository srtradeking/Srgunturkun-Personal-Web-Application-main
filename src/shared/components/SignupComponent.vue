<template>
  <div class="signup-box">
    <h2>Sign Up</h2>
    <form @submit.prevent="handleSignup">
      <div class="mb-3">
        <label>Email</label>
        <input 
          v-model="email" 
          type="email" 
          class="form-control" 
          required 
          autocomplete="email"
        />
      </div>
      <div class="mb-3">
        <label>Display Name</label>
        <input 
          v-model="username" 
          class="form-control" 
          required 
          autocomplete="username"
        />
      </div>
      <div class="mb-3">
        <label>Password</label>
        <input 
          v-model="password" 
          type="password" 
          class="form-control" 
          required 
          minlength="6"
          autocomplete="new-password"
        />
        <small class="form-text text-muted">Password must be at least 6 characters long</small>
      </div>
      <button type="submit" class="btn btn-primary">Sign Up</button>
      
      <!-- Divider -->
      <div v-if="error" class="alert alert-danger mt-2">{{ error }}</div>
      <div v-if="success" class="alert alert-success mt-2">
        Account created successfully! <strong>Please check your email and click the verification link before logging in.</strong> You cannot access your account until your email is verified.
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'

// Form state
const authStore = useAuthStore()
const email = ref('')
const username = ref('')
const password = ref('')
const error = ref('')
const success = ref(false)

// Password validation
const validatePassword = (password) => {
  if (password.length < 6) {
    return 'Password must be at least 6 characters long'
  }
  return null
}

// Signup handler
const handleSignup = async () => {
  error.value = ''
  success.value = false

  // Validate password
  const passwordError = validatePassword(password.value)
  if (passwordError) {
    error.value = passwordError
    return
  }

  try {
    await authStore.register({ email: email.value, username: username.value, password: password.value })

    // Clear form and show success message
    success.value = true
    email.value = ''
    username.value = ''
    password.value = ''
  } catch (e) {
    console.error('Signup error:', e)
    const message = e && e.message ? e.message : 'Failed to create account. Please try again.'
    if (message.toLowerCase().includes('email is already registered')) {
      error.value = 'This email is already registered'
    } else if (message.toLowerCase().includes('username is already taken')) {
      error.value = 'This username is already taken'
    } else {
      error.value = message
    }
  }
}
</script>

<style scoped>
.signup-box {
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
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 10px;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover {
  background: #0056b3;
}

.form-control {
  width: 100%;
  padding: 10px;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 16px;
}

.form-control:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0,123,255,0.25);
}

.mb-3 {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 5px;
  font-weight: 500;
  color: #333;
}

.alert {
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

.alert-danger {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.form-text {
  font-size: 12px;
  margin-top: 5px;
}

.text-muted {
  color: #6c757d;
}
</style>
