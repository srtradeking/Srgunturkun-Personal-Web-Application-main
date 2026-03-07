/**
 * Cookie Store
 * Pinia store for managing cookie consent state across the application
 * 
 * Usage:
 *   import { useCookieStore } from '@/store/useCookieStore'
 *   
 *   const cookieStore = useCookieStore()
 *   cookieStore.setConsent({ performance: true, marketing: false })
 *   cookieStore.hasConsent('analytics') // true/false
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cookieService } from '@/services/cookieService'
import { consentAuditAPI } from '@/services/apiService'

export const useCookieStore = defineStore('cookies', () => {
  // State
  const consentGiven = ref(false)
  const consentDetails = ref({
    essential: true,
    performance: false,
    functional: false,
    marketing: false,
    timestamp: null,
    version: '1.0'
  })
  const showBanner = ref(false)
  const consentDeclined = ref(false)
  const dntDetected = ref(false)

  // Computed
  const hasGivenConsent = computed(() => consentGiven.value)
  const consentStatus = computed(() => consentDetails.value)
  const hasPerformanceConsent = computed(() => consentDetails.value.performance)
  const hasFunctionalConsent = computed(() => consentDetails.value.functional)
  const hasMarketingConsent = computed(() => consentDetails.value.marketing)
  const shouldShowBanner = computed(() => showBanner.value && !consentGiven.value)

  /**
   * Initialize cookie store from saved preferences
   */
  function initializeConsent() {
    // Check for Do Not Track
    if (cookieService.respectDoNotTrack()) {
      dntDetected.value = true
      consentDetails.value.performance = false
      consentDetails.value.marketing = false
    }

    // Load existing consent - try cookies first, then localStorage backup
    let savedConsent = cookieService.getConsent()
    
    if (!savedConsent) {
      // Try localStorage backup if cookie not found
      try {
        const backupConsent = localStorage.getItem('app_cookie_consent_backup')
        if (backupConsent) {
          savedConsent = JSON.parse(backupConsent)
          console.log('📂 Loaded consent from localStorage backup')
        }
      } catch (e) {
        console.warn('Could not load backup consent from localStorage:', e)
      }
    }
    
    if (savedConsent) {
      consentGiven.value = true
      consentDetails.value = { ...consentDetails.value, ...savedConsent }
      showBanner.value = false
      console.log('✅ Existing consent loaded from storage')
    } else {
      // Show banner if no consent given
      showBanner.value = true
      console.log('📋 No saved consent found - showing banner')
    }

    console.log('🍪 Cookie store initialized:', {
      consentGiven: consentGiven.value,
      dntDetected: dntDetected.value,
      details: consentDetails.value
    })
  }

  /**
   * Update individual consent preferences without closing banner
   * Used when user toggles checkboxes in customize view
   * @param {string} category - Cookie category to update
   * @param {boolean} accepted - Whether category is accepted
   */
  function updateConsentPreference(category, accepted) {
    consentDetails.value[category] = accepted
    consentDetails.value.timestamp = new Date().toISOString()
    
    // Save to cookies
    cookieService.setConsent(consentDetails.value)
    
    // Also save to localStorage as backup
    try {
      localStorage.setItem('app_cookie_consent_backup', JSON.stringify(consentDetails.value))
    } catch (e) {
      console.warn('Could not save consent to localStorage:', e)
    }
    
    console.log(`✅ Consent preference updated: ${category} = ${accepted}`)
  }

  /**
   * Set consent for cookie categories
   * Closes banner and marks consent as given
   * @param {object} preferences - Consent preferences
   */
  async function setConsent(preferences) {
    const updatedConsent = {
      essential: true, // Always required
      performance: preferences.performance ?? consentDetails.value.performance,
      functional: preferences.functional ?? consentDetails.value.functional,
      marketing: preferences.marketing ?? consentDetails.value.marketing,
      timestamp: new Date().toISOString(),
      version: '1.0'
    }

    consentDetails.value = updatedConsent
    consentGiven.value = true
    showBanner.value = false
    consentDeclined.value = false

    // Save to cookies (primary storage)
    cookieService.setConsent(updatedConsent)

    // Also save to localStorage as backup (for reliability)
    try {
      localStorage.setItem('app_cookie_consent_backup', JSON.stringify(updatedConsent))
      console.log('💾 Consent saved to both cookies and localStorage')
    } catch (e) {
      console.warn('Could not save consent to localStorage:', e)
    }

    console.log('✅ Consent updated:', updatedConsent)

    // Immediately record consent to backend (with or without authentication)
    console.log('📤 Recording consent to backend...')
    await recordConsentToBackend()

    // Trigger compliance actions
    loadConsentRequiredServices()
  }

  /**
   * Accept all cookies
   */
  function acceptAll() {
    setConsent({
      performance: true,
      functional: true,
      marketing: true
    })
  }

  /**
   * Accept only essential cookies
   */
  function acceptEssentialOnly() {
    setConsent({
      performance: false,
      functional: false,
      marketing: false
    })
  }

  /**
   * Reject all optional cookies
   */
  function rejectAll() {
    acceptEssentialOnly()
    consentDeclined.value = true
  }

  /**
   * Hide banner without deciding (will show again later)
   */
  function hideBanner() {
    showBanner.value = false
  }

  /**
   * Reset consent (for testing)
   */
  function resetConsent() {
    consentGiven.value = false
    consentDeclined.value = false
    showBanner.value = true
    consentDetails.value = {
      essential: true,
      performance: false,
      functional: false,
      marketing: false,
      timestamp: null,
      version: '1.0'
    }
    
    // Clear cookies
    cookieService.deleteNonEssentialCookies()
    
    // Clear localStorage backup
    try {
      localStorage.removeItem('app_cookie_consent_backup')
      console.log('🗑️ Consent cleared from both cookies and localStorage')
    } catch (e) {
      console.warn('Could not clear localStorage:', e)
    }
  }

  /**
   * Withdraw consent from a category
   */
  function withdrawConsent(category) {
    if (category === 'essential') {
      console.warn('Cannot withdraw consent from essential cookies')
      return
    }

    consentDetails.value[category] = false
    cookieService.withdrawConsent(category)
    
    console.log(`Consent withdrawn from: ${category}`)
  }

  /**
   * Check if user has consented to a category
   */
  function hasConsent(category) {
    if (category === 'essential') {
      return true
    }
    return consentDetails.value[category] === true
  }

  /**
   * Load services based on consent
   * This is called after consent is given
   */
  function loadConsentRequiredServices() {
    // Performance/Analytics
    if (hasPerformanceConsent.value) {
      console.log('📊 Loading analytics services...')
      // Analytics will be loaded here
      // This triggers the analytics initialization
      window.dispatchEvent(new CustomEvent('consentAnalytics'))
    }

    // Functional services
    if (hasFunctionalConsent.value) {
      console.log('⚙️ Loading functional services...')
      window.dispatchEvent(new CustomEvent('consentFunctional'))
    }

    // Marketing services
    if (hasMarketingConsent.value) {
      console.log('📢 Loading marketing services...')
      window.dispatchEvent(new CustomEvent('consentMarketing'))
    }
  }

  /**
   * Handle cookie consent changed event
   */
  function handleConsentChanged(event) {
    if (event.detail) {
      consentDetails.value = event.detail
      consentGiven.value = true
    }
  }

  /**
   * Record consent to backend immediately when given
   * Records consent with or without user authentication
   * If user authenticated: records with numeric userId from backend
   * If not authenticated: records with IP address for tracking
   * @param {string} ipAddress - Optional client IP address (for anonymous users)
   * @returns {Promise} Resolves when consent is recorded or skipped
   */
  async function recordConsentToBackend(ipAddress = null) {
    try {
      // Only record if user has given consent
      if (!consentGiven.value) {
        console.log('ℹ️ No consent to record - user has not given consent')
        return
      }

      // Get IP address if not provided
      if (!ipAddress) {
        try {
          ipAddress = await getClientIP()
          console.log('📍 Client IP detected:', ipAddress)
        } catch (err) {
          console.warn('Could not detect client IP:', err)
          ipAddress = 'unknown'
        }
      }

      // Resolve backend user id from localStorage (set after login), if available
      let userId = null
      try {
        const stored = localStorage.getItem('userId')
        if (stored != null && stored !== '') {
          userId = Number.isNaN(Number(stored)) ? null : Number(stored)
        }
      } catch (e) {
        console.warn('Could not resolve userId from localStorage for consent audit', e)
      }

      // Build consent audit data
      const consentAuditData = {
        // Use authenticated user ID if available, otherwise null (allow IP-based tracking)
        userId,
        consentGiven: true,
        ipAddress: ipAddress,
        userAgent: navigator.userAgent,
        categories: {
          essential: consentDetails.value.essential,
          performance: consentDetails.value.performance,
          functional: consentDetails.value.functional,
          marketing: consentDetails.value.marketing
        },
        consentVersion: consentDetails.value.version,
        isActive: true
      }

      const userType = userId != null ? 'authenticated user' : 'anonymous visitor'
      console.log(`📤 Recording consent to backend for ${userType}...`, consentAuditData)
      
      await consentAuditAPI.recordConsent(consentAuditData)
      console.log('✅ Consent audit recorded successfully in backend')
    } catch (err) {
      console.error('❌ Error recording consent to backend:', err)
      // Don't throw - just log the error so consent wall doesn't block user
    }
  }

  /**
   * Get client IP address from backend
   * @returns {Promise<string>} Client IP address
   */
  async function getClientIP() {
    try {
      const response = await consentAuditAPI.getClientIP()
      return response.ipAddress || null
    } catch (err) {
      console.warn('Could not fetch client IP:', err)
      return null
    }
  }

  return {
    // State
    consentGiven,
    consentDetails,
    showBanner,
    consentDeclined,
    dntDetected,

    // Computed
    hasGivenConsent,
    consentStatus,
    hasPerformanceConsent,
    hasFunctionalConsent,
    hasMarketingConsent,
    shouldShowBanner,

    // Methods
    initializeConsent,
    updateConsentPreference,
    setConsent,
    acceptAll,
    acceptEssentialOnly,
    rejectAll,
    hideBanner,
    resetConsent,
    withdrawConsent,
    hasConsent,
    loadConsentRequiredServices,
    handleConsentChanged,
    recordConsentToBackend
  }
})
