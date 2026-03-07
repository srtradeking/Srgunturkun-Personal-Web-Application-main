export const API_CONFIG = {
  developmentUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  productionUrl: import.meta.env.VITE_API_PRODUCTION_URL,
  timeout: 30000,
  retryAttempts: 3,

  /**
   * Get the appropriate base URL based on environment
   */
  getBaseUrl() {
    const isDevelopment = !import.meta.env.PROD
    return isDevelopment ? this.developmentUrl : this.productionUrl
  }
}
