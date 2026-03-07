<template>
  <div class="auth-info">
    <div class="alert alert-info">
      Third-party social login is not available. Please use your email and password to register and log in.
    </div>
  </div>
</template>

<script setup>
// Neutral auth information component; no third-party login providers are exposed here.
</script>

<style scoped>
.auth-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.alert { margin-top: 10px; padding: 8px; border-radius: 4px; background: #f8d7da; color: #721c24; }
</style>

