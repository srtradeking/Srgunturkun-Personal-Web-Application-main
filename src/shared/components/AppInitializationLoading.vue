<template>
  <div v-if="isLoading" class="app-initialization-loading" :class="{ 'loading-complete': isComplete }">
    <div class="loading-container">
      <div class="spinner">
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
        <div class="spinner-ring"></div>
      </div>

      <h1 class="loading-title">{{ title }}</h1>
      <p class="loading-message">{{ message }}</p>
      
      <div v-if="showProgress" class="progress-bar">
        <div class="progress-fill" :style="{ width: progress + '%' }"></div>
      </div>

      <div v-if="showDetails" class="loading-details">
        <p>{{ details }}</p>
        <div class="status-checks">
          <div class="status-item" :class="{ 'status-complete': status.backend }">
            <span class="status-icon">{{ status.backend ? '✓' : '○' }}</span>
            <span>Backend Connection</span>
          </div>
          <div class="status-item" :class="{ 'status-complete': status.api }">
            <span class="status-icon">{{ status.api ? '✓' : '○' }}</span>
            <span>API Services</span>
          </div>
          <div class="status-item" :class="{ 'status-complete': status.frontend }">
            <span class="status-icon">{{ status.frontend ? '✓' : '○' }}</span>
            <span>Frontend Components</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineExpose, watch, defineProps } from 'vue'

// Props for receiving data from parent
const props = defineProps({
  progress: {
    type: Number,
    default: 0
  },
  status: {
    type: Object,
    default: () => ({
      backend: false,
      api: false,
      frontend: false,
      overall: false
    })
  },
  message: {
    type: String,
    default: 'Initializing application...'
  }
})

// Internal state
const isLoading = ref(true)
const isComplete = ref(false)
const title = ref('Starting Application')
const message = ref(props.message)
const details = ref('Initializing services…')
const showProgress = ref(true)
const showDetails = ref(true)
const progress = ref(props.progress)
const status = ref(props.status)

// Watch for prop changes
watch(() => props.progress, (newProgress) => {
  progress.value = newProgress
})

watch(() => props.status, (newStatus) => {
  status.value = newStatus
  
  // Update details based on status
  const completedItems = Object.values(newStatus).filter(Boolean).length
  if (completedItems === 0) {
    details.value = 'Initializing services…'
  } else if (completedItems === 1) {
    details.value = 'Backend connected, checking API…'
  } else if (completedItems === 2) {
    details.value = 'API ready, loading frontend components…'
  } else if (completedItems === 3) {
    details.value = 'All systems ready!'
  }
}, { deep: true })

watch(() => props.message, (newMessage) => {
  message.value = newMessage
})

// Update progress method for external calls
function updateProgress(newProgress, newMessage = null) {
  progress.value = newProgress
  if (newMessage) {
    message.value = newMessage
  }
  
  // Hide loading when complete
  if (newProgress >= 100) {
    setTimeout(() => {
      isComplete.value = true
      setTimeout(() => {
        isLoading.value = false
      }, 700) // Wait for fade-out animation to complete
    }, 300) // Small delay before starting fade
  }
}

// Hide loading method for external calls
const hideLoading = () => {
  console.log(' Hiding loading screen')
  isComplete.value = true
  setTimeout(() => {
    isLoading.value = false
  }, 700) // Wait for fade-out animation to complete
}

// Expose methods for parent component
defineExpose({
  isLoading,
  isComplete,
  progress,
  message,
  title,
  details,
  status,
  updateProgress,
  hideLoading
})

onMounted(() => {
  console.log('📱 Loading component mounted with initial props:', {
    progress: props.progress,
    status: props.status,
    message: props.message
  })
})
</script>

<style scoped>
.app-initialization-loading {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 9999;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

.loading-container {
  text-align: center;
  color: white;
  max-width: 500px;
  padding: 2rem;
}

.spinner {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 2rem;
}

.spinner-ring {
  position: absolute;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  animation: spin 1.5s linear infinite;
}

.spinner-ring:nth-child(1) {
  width: 100%;
  height: 100%;
  animation-delay: 0s;
}

.spinner-ring:nth-child(2) {
  width: 80%;
  height: 80%;
  top: 10%;
  left: 10%;
  animation-delay: 0.3s;
  border-color: rgba(255, 255, 255, 0.5);
}

.spinner-ring:nth-child(3) {
  width: 60%;
  height: 60%;
  top: 20%;
  left: 20%;
  animation-delay: 0.6s;
  border-color: rgba(255, 255, 255, 0.7);
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.loading-title {
  margin: 0 0 0.5rem;
  font-size: 1.75rem;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.loading-message {
  margin: 0.5rem 0;
  font-size: 1.1rem;
  font-weight: 400;
  opacity: 0.95;
  line-height: 1.5;
}

.progress-bar {
  margin: 2rem 0 1rem;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.loading-details {
  margin: 1rem 0 0;
  font-size: 0.95rem;
  opacity: 0.85;
  font-style: italic;
}

.status-checks {
  margin-top: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0;
  transition: all 0.3s ease;
}

.status-item.status-complete {
  opacity: 1;
  color: rgba(255, 255, 255, 0.95);
}

.status-icon {
  font-size: 0.9rem;
  font-weight: bold;
  width: 1.2rem;
  text-align: center;
}

.status-complete .status-icon {
  color: #4ade80; /* Green color for completed items */
}

/* Fade out animation - only when loading is complete */
@keyframes fadeOut {
  from {
    opacity: 1;
  }
  to {
    opacity: 0;
  }
}

.app-initialization-loading.loading-complete {
  animation: fadeOut 0.5s ease-out forwards;
  animation-delay: 0.2s;
  pointer-events: none;
}

/* Ensure it's completely hidden when v-if removes it */
.app-initialization-loading:not(v-if) {
  display: none;
}
</style>
