/**
 * App Readiness Detector
 * Monitors the initialization status of backend, API, and frontend components
 */

import { API_CONFIG } from '@/services/config'

class AppReadinessDetector {
  constructor() {
    this.status = {
      backend: false,
      api: false,
      frontend: false,
      overall: false
    }
    this.progress = 0
    this.isMonitoring = false
    this.monitoringInterval = null
    this.progressCallback = null
    this.readyCallback = null
    this.errorCallback = null
  }

  /**
   * Start monitoring app readiness
   */
  async startMonitoring(progressCallback, readyCallback, errorCallback) {
    this.progressCallback = progressCallback
    this.readyCallback = readyCallback
    this.errorCallback = errorCallback
    this.isMonitoring = true

    try {
      // Check backend connectivity
      await this._checkBackend()

      // Check API availability
      await this._checkAPI()

      // Mark frontend as ready (since this runs in the frontend)
      this.status.frontend = true
      this._updateProgress()

      // Mark overall as ready
      this.status.overall = true
      this._updateProgress()

      // Call ready callback
      if (this.readyCallback) {
        this.readyCallback()
      }
    } catch (error) {
      console.error('App readiness check failed:', error)
      if (this.errorCallback) {
        this.errorCallback(error)
      }
    }
  }

  /**
   * Stop monitoring
   */
  stopMonitoring() {
    this.isMonitoring = false
    if (this.monitoringInterval) {
      clearInterval(this.monitoringInterval)
    }
  }

  /**
   * Check backend connectivity
   */
  async _checkBackend() {
    const apiUrl = API_CONFIG.developmentUrl
    const healthUrl = `${apiUrl}/health`

    try {
      const response = await fetch(healthUrl, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        signal: AbortSignal.timeout(5000)
      })

      if (response.ok) {
        this.status.backend = true
        console.log('✅ Backend is ready')
      } else {
        throw new Error(`Health check returned ${response.status}`)
      }
    } catch (error) {
      console.warn('⚠️ Backend health check failed:', error.message)
      // Don't throw - allow app to continue with warning
    }

    this._updateProgress()
  }

  /**
   * Check API availability
   */
  async _checkAPI() {
    // API is considered available if we can reach the health endpoint
    // This could be expanded to check specific endpoints
    if (this.status.backend) {
      this.status.api = true
      console.log('✅ API is available')
    }

    this._updateProgress()
  }

  /**
   * Update progress
   */
  _updateProgress() {
    // Calculate progress (0-100)
    const completedChecks = Object.values(this.status).filter(v => v === true).length
    const totalChecks = Object.keys(this.status).length - 1 // Don't count 'overall'

    this.progress = Math.round((completedChecks / totalChecks) * 100)

    // Call progress callback
    if (this.progressCallback) {
      this.progressCallback(this.progress, { ...this.status })
    }
  }
}

// Export singleton instance
export default new AppReadinessDetector()
