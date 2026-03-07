/**
 * Cookie Service
 * Handles reading, writing, and deleting cookies
 */

const CONSENT_COOKIE_NAME = "${CONSENT_COOKIE_NAME}"
const CONSENT_BACKUP_KEY = "${CONSENT_BACKUP_KEY}"

export const cookieService = {
  /**
   * Set a cookie
   */
  setCookie(name, value, days = 365) {
    const date = new Date()
    date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000))
    const expires = `expires=${date.toUTCString()}`
    document.cookie = `${name}=${value};${expires};path=/;SameSite=Lax`
  },

  /**
   * Get a cookie value
   */
  getCookie(name) {
    const nameEQ = `${name}=`
    const cookies = document.cookie.split(';')
    for (let cookie of cookies) {
      cookie = cookie.trim()
      if (cookie.indexOf(nameEQ) === 0) {
        return cookie.substring(nameEQ.length)
      }
    }
    return null
  },

  /**
   * Delete a cookie
   */
  deleteCookie(name) {
    this.setCookie(name, '', -1)
  },

  /**
   * Check if cookie exists
   */
  hasCookie(name) {
    return this.getCookie(name) !== null
  },

  /**
   * Get all cookies as object
   */
  getAllCookies() {
    const cookies = {}
    document.cookie.split(';').forEach(cookie => {
      cookie = cookie.trim()
      const [name, value] = cookie.split('=')
      if (name) {
        cookies[name] = decodeURIComponent(value)
      }
    })
    return cookies
  },

  /**
   * Check if user has Do Not Track enabled
   */
  respectDoNotTrack() {
    return (
      navigator.doNotTrack === '1' ||
      navigator.doNotTrack === 'yes' ||
      window.doNotTrack === '1'
    )
  },

  /**
   * Get saved consent preferences
   */
  getConsent() {
    try {
      const consentStr = this.getCookie(CONSENT_COOKIE_NAME)
      if (consentStr) {
        return JSON.parse(decodeURIComponent(consentStr))
      }
    } catch (e) {
      console.warn('Error reading consent cookie:', e)
    }
    return null
  },

  /**
   * Save consent preferences
   */
  setConsent(consentObj) {
    try {
      const consentStr = encodeURIComponent(JSON.stringify(consentObj))
      this.setCookie(CONSENT_COOKIE_NAME, consentStr, 365)
      
      // Also save backup in localStorage
      try {
        localStorage.setItem(CONSENT_BACKUP_KEY, JSON.stringify(consentObj))
      } catch (e) {
        console.warn('Could not save consent backup to localStorage:', e)
      }
    } catch (e) {
      console.error('Error saving consent:', e)
    }
  },

  /**
   * Delete non-essential cookies
   */
  deleteNonEssentialCookies() {
    // List of non-essential cookie names to delete
    const nonEssentialCookies = [
      '_ga', '_ga_*', '_gid',  // Google Analytics
      '__gads',                 // Google Ads
      'NID',                    // Google tracking
      'IDE',                    // DoubleClick
      'ANID'                    // Google
    ]

    // Get all cookies
    const allCookies = this.getAllCookies()

    // Delete non-essential cookies
    Object.keys(allCookies).forEach(cookieName => {
      // Check if cookie matches any non-essential pattern
      const isNonEssential = nonEssentialCookies.some(pattern => {
        if (pattern.includes('*')) {
          const regex = new RegExp('^' + pattern.replace('*', '.*') + '$')
          return regex.test(cookieName)
        }
        return cookieName === pattern
      })

      if (isNonEssential) {
        this.deleteCookie(cookieName)
      }
    })
  },

  /**
   * Withdraw consent for a specific category
   */
  withdrawConsent(category) {
    try {
      const consent = this.getConsent()
      if (consent) {
        consent[category] = false
        this.setConsent(consent)
      }
    } catch (e) {
      console.error('Error withdrawing consent:', e)
    }
  }
}
