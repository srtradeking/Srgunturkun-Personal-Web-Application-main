<template>
  <!-- 1) LOADING SCREEN - Shows while backend, API, and frontend are loading -->
  <AppInitializationLoading 
    v-if="isLoading && !isOnPolicyPage" 
    ref="loadingComponent" 
    :progress="loadingProgress"
    :status="loadingStatus"
    :message="loadingMessage"
  />
  
  <!-- 2) POLICY PAGES - No consent wall, no navbar, just content -->
  <div v-else-if="isOnPolicyPage" class="policy-page-wrapper">
    <router-view v-slot="{ Component, route }">
      <transition name="fade" mode="out-in" appear>
        <div :key="route.path">
          <component :is="Component" />
        </div>
      </transition>
    </router-view>
  </div>

  <!-- 3) CONSENT WALL - Shows after loading is complete but before consent is given -->
  <ConsentWall v-else-if="cookieStore.shouldShowBanner" />
  
  <!-- 4) MAIN APP - Shows after consent is given -->
  <div v-else class="app-wrapper">
    <NavbarComponent />

    <main class="app-content">
      <Suspense suspensible>
        <template #default>
          <router-view v-slot="{ Component, route }">
            <transition name="fade" mode="out-in" appear>
              <div :key="route.path" class="route-wrapper">
                <component :is="Component" />
              </div>
            </transition>
          </router-view>
        </template>

        <!-- Fallback for lazy pages -->
        <template #fallback>
          <div class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">Loading application...</p>
          </div>
        </template>
      </Suspense>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import NavbarComponent from './shared/components/NavbarComponent.vue'
import ConsentWall from './shared/components/ConsentWall.vue'
import AppInitializationLoading from './shared/components/AppInitializationLoading.vue'
import { useCookieStore } from './store/useCookieStore'
import appReadinessDetector from './utils/appReadiness'

const router = useRouter()
const route = useRoute()
const cookieStore = useCookieStore()

// Policy routes that should not show consent wall or navbar
const policyRoutes = ['/privacy-policy', '/cookie-policy', '/terms-of-service']
const isOnPolicyPage = computed(() => {
  return policyRoutes.includes(route.path)
})

// Loading state management
const isLoading = ref(true)
const loadingProgress = ref(0)
const loadingStatus = ref({
  backend: false,
  api: false,
  frontend: false,
  overall: false
})
const loadingMessage = ref('Initializing application...')

// Loading component reference
const loadingComponent = ref(null)

// Cleanup function
let cleanup = () => {}

onMounted(async () => {
  console.log('🚀 App mounted - starting initialization sequence')
  
  try {
    // Initialize cookie store first
    cookieStore.initializeConsent()

    // Initialize theme from localStorage
    const savedTheme = localStorage.getItem('theme')
    if (savedTheme === 'dark') {
      document.body.classList.add('dark-mode')
    }
    
    // Start readiness detection
    await appReadinessDetector.startMonitoring(
      // Progress callback
      (progress, status) => {
        loadingProgress.value = progress
        loadingStatus.value = status
        
        // Update loading message based on what's loading
        if (!status.backend) {
          loadingMessage.value = 'Connecting to backend...'
        } else if (!status.api) {
          loadingMessage.value = 'Initializing API services...'
        } else if (!status.frontend) {
          loadingMessage.value = 'Loading frontend components...'
        } else {
          loadingMessage.value = 'Finalizing initialization...'
        }
        
        // Update loading component if available
        if (loadingComponent.value && loadingComponent.value.updateProgress) {
          loadingComponent.value.updateProgress(progress, loadingMessage.value)
        }
      },
      // Ready callback
      async () => {
        console.log('✅ Application is ready - transitioning to consent flow')
        
        // Add a small delay for smooth transition
        await new Promise(resolve => setTimeout(resolve, 500))
        
        // Hide loading screen
        isLoading.value = false
        
        // Redirect to Info view with consent if no consent has been given yet
        if (cookieStore.shouldShowBanner) {
          console.log('📱 Loading complete - redirecting to Info view for consent')
          
          // Check if we're not already on the Info page
          const currentRoute = router.currentRoute.value
          if (currentRoute.name !== 'AppInfo' && currentRoute.path !== '/info') {
            // Redirect to Info view for consent
            await router.push('/info')
          }
        } else {
          console.log('📱 Loading complete - showing main app (consent already given)')
        }
      },
      // Error callback
      (error) => {
        console.error('❌ Application readiness failed:', error)
        
        // Show error message and try to proceed anyway
        loadingMessage.value = 'Application loaded with warnings'
        
        // Force proceed after a delay
        setTimeout(() => {
          isLoading.value = false
        }, 2000)
      }
    )
    
  } catch (error) {
    console.error('❌ Critical error during app initialization:', error)
    
    // Show error state but still try to load the app
    loadingMessage.value = 'Error during initialization, attempting to continue...'
    
    // Force proceed after error
    setTimeout(() => {
      isLoading.value = false
    }, 3000)
  }
})

onUnmounted(() => {
  // Cleanup readiness detector
  appReadinessDetector.stopMonitoring()
  cleanup()
})
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

.app-wrapper {
  transition: all 0.3s ease;
}

.app-wrapper.consent-blocking {
  filter: blur(5px);
  pointer-events: none;
  opacity: 0.5;
}

/* Policy page wrapper - full width, no navbar */
.policy-page-wrapper {
  width: 100%;
  min-height: 100vh;
}

/* Main content area - account for fixed navbar */
.app-content {
  min-height: 100vh;
}

/* Route wrapper for smooth transitions */
.route-wrapper {
  width: 100%;
  min-height: calc(100vh - 80px);
}

/* Loading styles for lazy-loaded components */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  padding: 2rem;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #646cff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 1rem;
}

.loading-text {
  color: #666;
  font-size: 1.1rem;
  margin: 0;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Smooth transition for route changes */
.router-link-active {
  color: #646cff;
}

/* Improve performance for lazy loading */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
