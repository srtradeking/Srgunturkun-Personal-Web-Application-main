<template>
  <!-- Full-screen consent wall overlay -->
  <div v-if="cookieStore.shouldShowBanner" class="consent-wall-overlay">
    <div class="consent-wall-container">
      <!-- Header -->
      <div class="consent-wall-header">
        <div class="header-icon">🔒</div>
        <h1 class="header-title">Consent Required</h1>
      </div>

      <!-- Content -->
      <div class="consent-wall-content">
        <p class="main-message">
          This application requires your consent to use cookies before you can proceed.
        </p>

        <div class="consent-categories">
          <div class="category-item">
            <div class="category-info">
              <h3 class="category-name">🔐 Essential Cookies</h3>
              <p class="category-description">
                Required for security, authentication, and core functionality.
              </p>
            </div>
            <div class="category-status">
              <span class="badge badge-required">Required</span>
            </div>
          </div>

          <div class="category-item">
            <div class="category-info">
              <h3 class="category-name">📊 Performance Analytics</h3>
              <p class="category-description">
                Help us improve the app by tracking how you use it.
              </p>
            </div>
            <div class="category-toggle">
              <input
                id="perf-toggle"
                v-model="consentPreferences.performance"
                type="checkbox"
                class="consent-checkbox"
              />
              <label for="perf-toggle" class="checkbox-label"></label>
            </div>
          </div>

          <div class="category-item">
            <div class="category-info">
              <h3 class="category-name">⚙️ Functional Cookies</h3>
              <p class="category-description">
                Enable features like saved preferences and personalization.
              </p>
            </div>
            <div class="category-toggle">
              <input
                id="func-toggle"
                v-model="consentPreferences.functional"
                type="checkbox"
                class="consent-checkbox"
              />
              <label for="func-toggle" class="checkbox-label"></label>
            </div>
          </div>

          <div class="category-item">
            <div class="category-info">
              <h3 class="category-name">📢 Marketing Cookies</h3>
              <p class="category-description">
                Personalized ads and content recommendations.
              </p>
            </div>
            <div class="category-toggle">
              <input
                id="mark-toggle"
                v-model="consentPreferences.marketing"
                type="checkbox"
                class="consent-checkbox"
              />
              <label for="mark-toggle" class="checkbox-label"></label>
            </div>
          </div>
        </div>

        <!-- Policy Links / Expandable Sections -->
        <div class="policy-sections">
          <!-- Privacy Policy Dropdown -->
          <div class="policy-section">
            <button @click="togglePolicy('privacy')" class="policy-toggle">
              <span class="toggle-icon" :class="{ 'open': expandedPolicy === 'privacy' }">▶</span>
              📄 Privacy Policy
            </button>
            <div v-show="expandedPolicy === 'privacy'" class="policy-content-section">
              <div v-html="renderedPolicies.privacy"></div>
            </div>
          </div>

          <!-- Cookie Policy Dropdown -->
          <div class="policy-section">
            <button @click="togglePolicy('cookie')" class="policy-toggle">
              <span class="toggle-icon" :class="{ 'open': expandedPolicy === 'cookie' }">▶</span>
              🍪 Cookie Policy
            </button>
            <div v-show="expandedPolicy === 'cookie'" class="policy-content-section">
              <div v-html="renderedPolicies.cookie"></div>
            </div>
          </div>

          <!-- Terms of Service Dropdown -->
          <div class="policy-section">
            <button @click="togglePolicy('terms')" class="policy-toggle">
              <span class="toggle-icon" :class="{ 'open': expandedPolicy === 'terms' }">▶</span>
              ⚖️ Terms of Service
            </button>
            <div v-show="expandedPolicy === 'terms'" class="policy-content-section">
              <div v-html="renderedPolicies.terms"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="consent-wall-actions">
        <button
          @click="rejectConsent"
          class="btn btn-reject"
          :disabled="isProcessing"
        >
          <span v-if="!isProcessing">❌ Reject & Exit</span>
          <span v-else>Processing...</span>
        </button>

        <button
          @click="acceptConsent"
          class="btn btn-accept"
          :disabled="isProcessing"
        >
          <span v-if="!isProcessing">✅ Accept & Continue</span>
          <span v-else>Processing...</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import { useCookieStore } from '@/store/useCookieStore'

const router = useRouter()
const cookieStore = useCookieStore()
const isProcessing = ref(false)

// Policy expansion state
const expandedPolicy = ref(null)

// Rendered policy content
const renderedPolicies = ref({
  privacy: '',
  cookie: '',
  terms: ''
})

const consentPreferences = ref({
  performance: false,
  functional: false,
  marketing: false
})

// Toggle policy dropdown
function togglePolicy(policyType) {
  expandedPolicy.value = expandedPolicy.value === policyType ? null : policyType
}

// Load and render policies on mount
onMounted(async () => {
  try {
    const policies = ['privacy', 'cookie', 'terms']
    
    for (const policy of policies) {
      let filePath = ''
      
      switch (policy) {
        case 'privacy':
          filePath = '/PRIVACY_POLICY.md'
          break
        case 'cookie':
          filePath = '/COOKIE_POLICY.md'
          break
        case 'terms':
          filePath = '/TERMS_OF_SERVICE.md'
          break
      }
      
      try {
        const response = await fetch(filePath)
        if (response.ok) {
          const content = await response.text()
          renderedPolicies.value[policy] = marked(content)
        } else {
          renderedPolicies.value[policy] = `<p>Unable to load ${policy} policy.</p>`
        }
      } catch (err) {
        console.error(`Error loading ${policy} policy:`, err)
        renderedPolicies.value[policy] = `<p>Error loading ${policy} policy.</p>`
      }
    }
  } catch (err) {
    console.error('Error loading policies:', err)
  }
})

/**
 * User accepts consent and continues to app
 */
async function acceptConsent() {
  isProcessing.value = true

  try {
    // Save consent preferences locally and record to backend
    // (setConsent now automatically calls recordConsentToBackend())
    await cookieStore.setConsent({
      performance: consentPreferences.value.performance,
      functional: consentPreferences.value.functional,
      marketing: consentPreferences.value.marketing
    })

    console.log('✅ User accepted consent. App unlocked.')
    
    // Redirect to appropriate app section based on subdomain or default
    const hostname = window.location.hostname
    const productionDomain = import.meta.env?.VITE_PRODUCTION_DOMAIN || process.env.VUE_APP_PRODUCTION_DOMAIN
    
    let targetRoute = '/'
    
    // Check if we're on a subdomain and redirect accordingly
    if (hostname.includes(productionDomain)) {
      const subdomain = hostname.split('.')[0]
      switch (subdomain) {
        case 'social':
          targetRoute = '/social'
          break
        case 'account':
        case 'manage':
          targetRoute = '/account'
          break
        case 'profiles':
        case 'users':
          targetRoute = '/profile'
          break
        default:
          targetRoute = '/info' // Default to info view
      }
    }
    
    // Navigate to the appropriate route
    await router.push(targetRoute)
    console.log(`🚀 Navigated to: ${targetRoute}`)
    
  } catch (err) {
    console.error('Error accepting consent:', err)
    // Don't block app access even if backend recording fails
    // Still try to redirect
    try {
      await router.push('/info')
    } catch (redirectErr) {
      console.warn('Redirect failed:', redirectErr)
    }
  } finally {
    isProcessing.value = false
  }
}

/**
 * User rejects consent and exits the app
 */
async function rejectConsent() {
  isProcessing.value = true

  try {
    // Reject all optional cookies
    cookieStore.rejectAll()

    // Clear any temporary data and tokens
    try {
      localStorage.removeItem('jwtToken')
      localStorage.removeItem('authToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')
    } catch (e) {
      console.warn('Error clearing auth tokens on consent reject', e)
    }

    localStorage.clear()
    sessionStorage.clear()

    // Redirect to rejection page
    console.log('❌ User rejected consent. Session ended.')
    window.location.href = '/cookies-rejected'
  } catch (err) {
    console.error('Error rejecting consent:', err)
    // Still redirect even if error
    window.location.href = '/cookies-rejected'
  } finally {
    isProcessing.value = false
  }
}
</script>

<style scoped>
.consent-wall-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  animation: fadeIn 0.5s ease;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.consent-wall-container {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.5s ease;
  display: flex;
  flex-direction: column;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.consent-wall-header {
  padding: 32px 24px;
  border-bottom: 1px solid #e9ecef;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px 16px 0 0;
  color: white;
}

.header-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.header-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: white;
}

.consent-wall-content {
  flex: 1;
  padding: 32px 24px;
  overflow-y: auto;
}

.main-message {
  margin: 0 0 24px 0;
  font-size: 16px;
  color: #212529;
  line-height: 1.6;
  font-weight: 500;
}

.consent-categories {
  display: grid;
  gap: 16px;
  margin: 24px 0;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  background: #f8f9fa;
  transition: all 0.3s ease;
}

.category-item:hover {
  border-color: #667eea;
  background: #f0f2ff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.category-info {
  flex: 1;
}

.category-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #212529;
}

.category-description {
  margin: 0;
  font-size: 12px;
  color: #6c757d;
  line-height: 1.4;
}

.category-status {
  margin-left: 12px;
}

.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge-required {
  background: #dc3545;
  color: white;
}

.category-toggle {
  margin-left: 12px;
  display: flex;
  align-items: center;
}

.consent-checkbox {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  width: 24px;
  height: 24px;
  border: 2px solid #dee2e6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.consent-checkbox:hover {
  border-color: #667eea;
  box-shadow: 0 0 8px rgba(102, 126, 234, 0.2);
}

.consent-checkbox:checked {
  background: #667eea;
  border-color: #667eea;
}

.consent-checkbox:checked::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: white;
  font-size: 14px;
  font-weight: bold;
}

.checkbox-label {
  cursor: pointer;
}

/* Expandable Policy Sections */
.policy-sections {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e9ecef;
}

.policy-section {
  margin-bottom: 12px;
}

.policy-toggle {
  width: 100%;
  padding: 12px 16px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  cursor: pointer;
  text-align: left;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.policy-toggle:hover {
  background-color: #e9ecef;
  border-color: #dee2e6;
}

.toggle-icon {
  display: inline-block;
  transition: transform 0.3s ease;
  transform: rotate(0deg);
  font-size: 12px;
}

.toggle-icon.open {
  transform: rotate(90deg);
}

.policy-content-section {
  padding: 16px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-top: none;
  border-radius: 0 0 6px 6px;
  margin-bottom: 8px;
  max-height: 400px;
  overflow-y: auto;
  font-size: 13px;
  color: #495057;
  line-height: 1.6;
}

.policy-content-section :deep(h1),
.policy-content-section :deep(h2),
.policy-content-section :deep(h3) {
  color: #667eea;
  margin-top: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}

.policy-content-section :deep(p) {
  margin-bottom: 8px;
}

.policy-content-section :deep(ul),
.policy-content-section :deep(ol) {
  padding-left: 20px;
  margin-bottom: 8px;
}

.policy-content-section :deep(li) {
  margin-bottom: 4px;
}

.separator {
  color: #dee2e6;
}

.consent-wall-actions {
  display: flex;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
  border-radius: 0 0 16px 16px;
}

.btn {
  flex: 1;
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none !important;
}

.btn-reject {
  background: #dc3545;
  color: white;
}

.btn-reject:hover:not(:disabled) {
  background: #bb2d3b;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.3);
}

.btn-accept {
  background: #28a745;
  color: white;
}

.btn-accept:hover:not(:disabled) {
  background: #218838;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.3);
}

.btn:active:not(:disabled) {
  transform: translateY(0);
}

/* Scrollbar */
.consent-wall-container::-webkit-scrollbar {
  width: 8px;
}

.consent-wall-container::-webkit-scrollbar-track {
  background: transparent;
}

.consent-wall-container::-webkit-scrollbar-thumb {
  background: #dee2e6;
  border-radius: 4px;
}

.consent-wall-container::-webkit-scrollbar-thumb:hover {
  background: #adb5bd;
}

/* Responsive */
@media (max-width: 768px) {
  .consent-wall-header {
    padding: 24px 16px;
  }

  .header-icon {
    font-size: 40px;
  }

  .header-title {
    font-size: 24px;
  }

  .consent-wall-content {
    padding: 24px 16px;
  }

  .main-message {
    font-size: 14px;
  }

  .category-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .category-toggle {
    margin-left: 0;
    align-self: flex-end;
  }

  .category-status {
    margin-left: 0;
    align-self: flex-end;
  }

  .consent-wall-actions {
    flex-direction: column;
    gap: 8px;
  }

  .btn {
    width: 100%;
  }

  .policy-links {
    flex-wrap: wrap;
    gap: 4px;
  }
}

@media (max-width: 480px) {
  .consent-wall-container {
    width: 95%;
    max-height: 95vh;
  }

  .consent-wall-header {
    padding: 20px 12px;
  }

  .header-icon {
    font-size: 32px;
    margin-bottom: 8px;
  }

  .header-title {
    font-size: 20px;
  }

  .consent-wall-content {
    padding: 16px 12px;
  }

  .main-message {
    font-size: 13px;
  }

  .category-name {
    font-size: 13px;
  }

  .category-description {
    font-size: 11px;
  }

  .consent-wall-actions {
    padding: 16px 12px;
  }

  .btn {
    padding: 10px 16px;
    font-size: 12px;
  }
}
</style>
