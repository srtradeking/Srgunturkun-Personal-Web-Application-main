<template>
  <div class="account-management-container">
    <div class="auth-section">
      <SignupComponent />
      <LoginComponent />
    </div>
  </div>
</template>
<script>
import SignupComponent from '@/shared/components/SignupComponent.vue'
import LoginComponent from '@/shared/components/LoginComponent.vue'

export default {
  name: 'AppAccManagement',
  components: {
    SignupComponent,
    LoginComponent
  }
}
</script>

<style scoped>
.account-management-container {
  padding: 20px;
  min-height: 100vh;
  background: #f8f9fa;
}

.auth-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

@media (min-width: 768px) {
  .auth-section {
    flex-direction: row;
    justify-content: space-around;
  }
}
</style>