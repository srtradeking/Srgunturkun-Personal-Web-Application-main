<template>
  <div class="verify-email-page">
    <div class="card">
      <h2>Email Verification</h2>

      <div v-if="loading" class="status status-info">
        Verifying your email, please wait...
      </div>

      <div v-else-if="success" class="status status-success">
        <p>Your email has been verified successfully.</p>
        <p>You can now log in to your account.</p>
        <router-link to="/account" class="btn btn-primary">Go to Login</router-link>
      </div>

      <div v-else class="status status-error">
        <p>We could not verify your email.</p>
        <p v-if="errorMessage">Reason: {{ errorMessage }}</p>
        <p v-else>The verification link may be invalid or expired.</p>
        <router-link to="/account" class="btn btn-secondary">Back to Login</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { apiRequest } from '@/services/apiService'

const route = useRoute()
const loading = ref(true)
const success = ref(false)
const errorMessage = ref('')

onMounted(async () => {
  const token = route.query.token

  if (!token) {
    loading.value = false
    success.value = false
    errorMessage.value = 'Missing verification token.'
    return
  }

  try {
    await apiRequest(`/auth/verify-email?token=${encodeURIComponent(token)}`, {
      method: 'GET',
    })
    success.value = true
  } catch (e) {
    console.error('Email verification failed:', e)
    success.value = false
    errorMessage.value = (e && e.message) || 'Verification failed.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.verify-email-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.card {
  max-width: 420px;
  width: 100%;
  background: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  padding: 2rem;
  text-align: center;
}

.status {
  margin-top: 1rem;
  padding: 1rem;
  border-radius: 4px;
}

.status-info {
  background: #e2e3e5;
  color: #383d41;
}

.status-success {
  background: #d4edda;
  color: #155724;
}

.status-error {
  background: #f8d7da;
  color: #721c24;
}

.btn {
  display: inline-block;
  margin-top: 1rem;
  padding: 0.5rem 1.25rem;
  border-radius: 4px;
  text-decoration: none;
  font-weight: 500;
}

.btn-primary {
  background: #007bff;
  color: #fff;
}

.btn-secondary {
  background: #6c757d;
  color: #fff;
}
</style>
