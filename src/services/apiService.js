/**
 * Backend API Service
 * 
 * Centralized service for communicating with the Spring Boot backend API.
 * Provides unified interface for all API calls with authentication and error handling.
 */

import { API_CONFIG } from './config'

// Base API URL - configured via environment variables or configuration
const API_BASE_URL = API_CONFIG.getBaseUrl()

// Global logout handler to be set by the app (e.g. main.js) to avoid circular dependencies
let globalLogoutHandler = null

export function setLogoutHandler(handler) {
  globalLogoutHandler = handler
}

/**
 * Get JWT access token from localStorage (backend JWT auth)
 * Falls back to any legacy auth token key if present.
 */
async function getAuthToken() {
  // Fallback to any stored legacy token
  return localStorage.getItem("jwtToken") || localStorage.getItem("authToken")
}

/**
 * CSRF API Service
 * Handles fetching and managing CSRF tokens
 */
export const csrfAPI = {
  /**
   * Fetch a new CSRF token from the backend
   * @returns {Promise<string|null>} The CSRF token
   */
  async fetchToken() {
    try {
      // Explicitly use GET method and avoid the CSRF check recursion
      const response = await apiRequest('/csrf/token', { 
        method: 'GET',
        skipCsrfCheck: true 
      })
      
      if (response && response.token) {
        localStorage.setItem('csrfToken', response.token)
        return response.token
      }
    } catch (error) {
      console.warn('Failed to fetch CSRF token', error)
    }
    return null
  }
}

/**
 * Enhanced API request function with user ID validation and CSRF protection
 */
export async function apiRequest(endpoint, options = {}) {
  // If the client is globally blocked (e.g., waiting for Google sign-in confirmation), prevent requests
  const url = `${API_BASE_URL}${endpoint}`
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
    },
  }

  // Add authentication token if available
  let token = null
  try {
    token = await getAuthToken()
    if (token) {
      defaultOptions.headers.Authorization = `Bearer ${token}`
    }
  } catch (e) {
    console.warn('Error resolving auth token for API request', e)
  }
  
  // CSRF Protection
  const method = (options.method || 'GET').toUpperCase()
  const isStateChanging = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)
  
  // Skip CSRF check if explicitly requested (e.g. for fetching the token itself)
  if (isStateChanging && !options.skipCsrfCheck) {
    let csrfToken = localStorage.getItem('csrfToken')
    
    // If we are authenticated but don't have a CSRF token, try to fetch one first
    if (!csrfToken && token) {
      // We must avoid infinite loops here. 
      // fetchToken calls apiRequest with skipCsrfCheck=true, so it's safe.
      csrfToken = await csrfAPI.fetchToken()
    }
    
    if (csrfToken) {
      defaultOptions.headers['X-CSRF-Token'] = csrfToken
    }
  }
  
  const requestOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...options.headers,
    },
  }
  // Remove custom options that shouldn't be sent to fetch
  delete requestOptions.skipCsrfCheck

  console.log(`🚀 API Request: ${requestOptions.method || 'GET'} ${url}`)
  
  try {
    const response = await fetch(url, requestOptions)
    
    // Handle authentication errors
    if (response.status === 401) {
      console.warn('🔒 Authentication failed - clearing tokens')
      localStorage.removeItem("authToken")
      localStorage.removeItem("jwtToken")
      
      if (globalLogoutHandler) {
        globalLogoutHandler()
      }
      
      throw new Error('Authentication required')
    }
    
    if (!response.ok) {
      const errorText = await response.text()
      let errorMessage = `HTTP ${response.status}: ${response.statusText}`
      let errorData = null
      
      try {
        errorData = JSON.parse(errorText)
        errorMessage = errorData.message || errorData.error || errorMessage
      } catch {
        // Error response is not JSON
        errorMessage = errorText || errorMessage
      }
      
      // Check for CSRF errors
      if (response.status === 403 && 
          (errorMessage.includes('CSRF') || (errorData && errorData.error && errorData.error.includes('CSRF')))) {
        
        console.warn('🔄 CSRF token invalid or expired. Attempting to refresh...')
        
        // Clear invalid token
        localStorage.removeItem('csrfToken')
        
        // Only retry once to avoid infinite loops
        if (!options.isRetry) {
          const newToken = await csrfAPI.fetchToken()
          if (newToken) {
            console.log('✅ CSRF token refreshed. Retrying request...')
            return apiRequest(endpoint, {
              ...options,
              isRetry: true
            })
          }
        }
      }
      
      // Provide more user-friendly error messages for common type conversion issues
      if (errorMessage.includes('Failed to convert value of type') && errorMessage.includes('String') && errorMessage.includes('Long')) {
        errorMessage = 'User ID format mismatch. The system is updating to handle this automatically.'
      }
      
      const err = new Error(errorMessage)
      // Attach HTTP status for callers to inspect
      try { err.status = response.status } catch (e) { /* ignore */ }
      throw err
    }
    
    // Handle responses with no content (204 No Content)
    if (response.status === 204) {
      console.log(`✅ API Response: ${response.status} No Content`)
      return null
    }
    
    const data = await response.json()
  // Only log endpoint and status to avoid printing large response payloads (e.g. base64 images)
  console.log(`✅ API Response: ${response.status} ${endpoint}`)
  return data
    
  } catch (error) {
    console.error(`❌ API request failed for ${endpoint}:`, error)
    throw error
  }
}

/**
 * Storage API Service
 * Provides signed URLs for direct uploads to local storage
 */
export const storageAPI = {
  /**
   * Request a signed upload URL for direct local storage upload
   * @param {{fileName: string, mimeType: string, fileSize: number, purpose: string}} payload
   */
  async getSignedUploadUrl(payload) {
    return apiRequest('/storage/signed-upload', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  }
}

/**
 * Authentication API (email + password, JWT-based)
 * Uses /auth endpoints on the backend.
 */
export const authAPI = {
  /**
   * Login with email and password.
   * On success, stores accessToken, refreshToken, and userId in localStorage.
   */
  async login({ email, password }) {
    const body = { email, password }
    const response = await apiRequest('/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    })

    if (response && response.accessToken) {
      try {
        localStorage.setItem('jwtToken', response.accessToken)
        if (response.refreshToken) {
          localStorage.setItem('refreshToken', response.refreshToken)
        }
        if (response.userId !== undefined && response.userId !== null) {
          localStorage.setItem('userId', String(response.userId))
        }
        if (response.email) {
          localStorage.setItem('loginEmail', response.email)
        }
        if (response.displayName) {
          localStorage.setItem('displayName', response.displayName)
        }
        if (response.role) {
          localStorage.setItem('role', response.role)
        }
        
        // Fetch CSRF token immediately after login
        await csrfAPI.fetchToken()
        
      } catch (e) {
        console.warn('Failed to persist auth tokens to localStorage', e)
      }
    }

    return response
  },

  /**
   * Register a new user with email, username, and password.
   * Does not automatically log the user in.
   */
  async register({ email, username, password }) {
    const body = { email, username, password }
    return apiRequest('/auth/register', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  /**
   * Resend verification email for a given address.
   */
  async resendVerification(email) {
    const body = { email }
    return apiRequest('/auth/resend-verification', {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },

  /**
   * Clear stored tokens (client-side logout helper).
   */
  clearTokens() {
    try {
      localStorage.removeItem('jwtToken')
      localStorage.removeItem('authToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')
      localStorage.removeItem('csrfToken')
    } catch (e) {
      console.warn('Failed to clear auth tokens from localStorage', e)
    }
  },
}

/**
 * Helper to perform XMLHttpRequest to support upload progress callbacks.
 * @param {string} url
 * @param {string} method
 * @param {any} body - string or FormData
 * @param {object} headers
 * @param {(evt: ProgressEvent) => void} onProgress
 */
function xhrRequest(url, method = 'POST', body = null, headers = {}, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open(method, url, true)

    // Set headers
    Object.keys(headers || {}).forEach((k) => {
      try { xhr.setRequestHeader(k, headers[k]) } catch (e) { /* ignore */ }
    })

    xhr.onreadystatechange = () => {
      if (xhr.readyState === 4) {
        const status = xhr.status
        if (status >= 200 && status < 300) {
          try {
            const data = xhr.responseText ? JSON.parse(xhr.responseText) : null
            resolve(data)
          } catch (e) {
            resolve(xhr.responseText)
          }
        } else {
          reject(new Error(`HTTP ${status}: ${xhr.statusText || xhr.responseText}`))
        }
      }
    }

    xhr.onerror = () => reject(new Error('Network error during XHR request'))

    if (xhr.upload && typeof onProgress === 'function') {
      xhr.upload.onprogress = onProgress
    }

    // Allow sending null/undefined bodies
    xhr.send(body)
  })
}

/**
 * Wait for the backend to become ready by polling a set of health endpoints.
 * Resolves when a 200 OK response is received from any health endpoint, or rejects on timeout.
 * @param {object} opts - options: { timeoutMs, intervalMs, endpoints }
 */
export async function waitForBackend(opts = {}) {
  const timeoutMs = opts.timeoutMs || API_CONFIG.TIMEOUT || 30000
  const intervalMs = opts.intervalMs || 1000
  const endpoints = opts.endpoints || [
    '/actuator/health',
    '/health',
    '/',
  ]

  const start = Date.now()

  const tryEndpoint = async (ep) => {
    try {
      const res = await fetch(`${API_BASE_URL}${ep}`, { method: 'GET' })
      if (res.ok) return true
      // Some health endpoints return 503/200; consider non-2xx as not ready
      return false
    } catch (e) {
      return false
    }
  }

  return new Promise((resolve, reject) => {
    const tick = async () => {
      for (const ep of endpoints) {
        // If API_BASE_URL already ends with ep, avoid double slashes
        const normalizedEp = ep.startsWith('/') ? ep : `/${ep}`
        // Try each endpoint
        // eslint-disable-next-line no-await-in-loop
        const ok = await tryEndpoint(normalizedEp)
        if (ok) {
          console.info(`✅ Backend ready (endpoint ${normalizedEp} responded OK)`)
          return resolve(true)
        }
      }

      if (Date.now() - start >= timeoutMs) {
        return reject(new Error('Timeout waiting for backend readiness'))
      }

      setTimeout(tick, intervalMs)
    }

    tick()
  })
}

/**
 * Posts API Service
 */
export const postsAPI = {
  // Get all posts
  async getAllPosts() {
    return apiRequest('/posts')
  },
  
  // Get posts with filters
  async getPostsWithFilters(filters = {}) {
    const queryParams = new URLSearchParams()
    
    if (filters.userId) queryParams.append('userId', filters.userId)
    if (filters.limit) queryParams.append('limit', filters.limit)
    if (filters.orderBy) queryParams.append('orderBy', filters.orderBy)
    if (filters.orderDirection) queryParams.append('orderDirection', filters.orderDirection)
    
    const endpoint = queryParams.toString() ? `/posts?${queryParams.toString()}` : '/posts'
    return apiRequest(endpoint)
  },
  
  // Get post by ID
  async getPostById(postId) {
    return apiRequest(`/posts/${postId}`)
  },
  
  // Create new post
  async createPost(postData, { onProgress } = {}) {
    const url = `${API_BASE_URL}/posts`

    // If caller requested progress notifications, use XHR
    if (typeof onProgress === 'function') {
      // Resolve auth header synchronously via apiRequest helper
      const headers = { 'Content-Type': 'application/json' }
      try {
        const token = await getAuthToken()
        if (token) headers.Authorization = `Bearer ${token}`
      } catch (e) {
        console.warn('Failed to attach token for XHR post', e)
      }

      return xhrRequest(url, 'POST', JSON.stringify(postData), headers, onProgress)
    }

    return apiRequest('/posts', {
      method: 'POST',
      body: JSON.stringify(postData),
    })
  },
  
  // Update post
  async updatePost(postId, postData) {
    return apiRequest(`/posts/${postId}`, {
      method: 'PUT',
      body: JSON.stringify(postData),
    })
  },
  
  // Delete post
  async deletePost(postId) {
    return apiRequest(`/posts/${postId}`, {
      method: 'DELETE',
    })
  },
  
  // Get posts by user
  async getPostsByUserId(userId) {
    return apiRequest(`/posts/user/${userId}`)
  },
  
  // React to post (like/unlike)
  async reactToPost(postId, reactionData) {
    const { emoji, userId } = reactionData
    const queryParams = new URLSearchParams()
    queryParams.append('userId', userId)
    queryParams.append('reactionType', emoji)
    
    return apiRequest(`/posts/${postId}/reactions?${queryParams.toString()}`, {
      method: 'POST',
    })
  },
  
  // Get posts count
  async getPostsCount() {
    return apiRequest('/posts/count')
  },
  
  // Get posts count by user
  async getPostsCountByUserId(userId) {
    return apiRequest(`/posts/count/user/${userId}`)
  },
  
  // Get total likes received by user across all their posts
  async getTotalLikesReceivedByUser(userId) {
    return apiRequest(`/posts/likes/user/${userId}`)
  },

  // Get pending posts for moderation (Admin/Moderator only)
  async getPendingPosts() {
    return apiRequest('/posts/pending')
  },

  // Update post status (Admin/Moderator only)
  async updatePostStatus(postId, status) {
    return apiRequest(`/posts/${postId}/status?status=${encodeURIComponent(status)}`, {
      method: 'PUT',
    })
  },

  // Reject and fully delete a post (Admin/Moderator only)
  // This removes all data including media from local storage
  async rejectPost(postId) {
    return apiRequest(`/posts/${postId}/reject`, {
      method: 'DELETE',
    })
  }
}

/**
 * Profiles API Service
 */
export const profilesAPI = {
  // Get all profiles
  async getAllProfiles() {
    return apiRequest('/profiles')
  },
  
  // Get profile by user ID
  // Get profile by user ID. If opts.public === true, call the public profiles endpoint
  // which returns only non-sensitive fields and is accessible without authentication.
  async getProfileByUserId(userId, opts = {}) {
    if (opts && opts.public === true) {
      return apiRequest(`/public/profiles/${userId}`)
    }
    return apiRequest(`/profiles/${userId}`)
  },
  
  // Create or update profile
    async saveProfile(profileData, opts = {}) {
    const headers = {}
    if (opts && opts.token) headers.Authorization = `Bearer ${opts.token}`
    return apiRequest('/profiles', {
      method: 'POST',
      body: JSON.stringify(profileData),
      headers,
    })
  },
  
  // Update profile
  async updateProfile(userId, profileData) {
    return apiRequest(`/profiles/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(profileData),
    })
  },
  
  // Delete profile
  async deleteProfile(userId) {
    return apiRequest(`/profiles/${userId}`, {
      method: 'DELETE',
    })
  },

  // Delete account and all related data atomically (server-side transaction)
  async deleteAccountCascade(userId) {
    return apiRequest(`/profiles/${userId}/cascade`, {
      method: 'DELETE',
    })
  },
  
  // Search profiles by display name
  async searchProfilesByDisplayName(displayName) {
    return apiRequest(`/profiles/search?displayName=${encodeURIComponent(displayName)}`)
  },
  
  // Update profile photo
  async updateProfilePhoto(userId, photoUrl) {
    return apiRequest(`/profiles/${userId}/photo`, {
      method: 'PUT',
      body: JSON.stringify({ photoUrl }),
    })
  },
  
  // Update profile bio
  async updateProfileBio(userId, bio) {
    return apiRequest(`/profiles/${userId}/bio`, {
      method: 'PUT',
      body: JSON.stringify({ bio }),
    })
  },
  
  // Check if profile exists
  async profileExists(userId, opts = {}) {
    try {
      const headers = {}
      if (opts && opts.token) headers.Authorization = `Bearer ${opts.token}`
      const result = await apiRequest(`/profiles/${userId}/exists`, { headers })
      // The backend returns a boolean true/false. Return it directly only when
      // the response is boolean. If the API returned something unexpected, treat
      // truthy values as existence as a defensive fallback.
      if (typeof result === 'boolean') return result
      return !!result
    } catch (error) {
      // Treat 404 as non-existing profile. Also treat blocked requests or
      // authentication failures (401/403) as 'not exists' in the client flow so
      // the frontend can abort sign-in instead of attempting protected calls.
      if (
        (error && error.status === 404) ||
        (error && (error.status === 401 || error.status === 403)) ||
        (error && error.blocked === true) ||
        (error && error.message && error.message.includes('404'))
      ) {
        return false
      }
      throw error
    }
  },
  
  // Get profiles with post counts
  async getProfilesWithPostCounts() {
    return apiRequest('/profiles/with-post-counts')
  },
  
  // Get total profiles count
  async getTotalProfilesCount() {
    return apiRequest('/profiles/count')
  }
}

/**
 * Comments API Service
 */
export const commentsAPI = {
  // Get comment by ID
  async getCommentById(commentId) {
    return apiRequest(`/comments/${commentId}`)
  },
  
  // Get comments by post ID
  async getCommentsByPostId(postId) {
    return apiRequest(`/comments/posts/${postId}`)
  },

  // Get top-level comments by post ID (parentCommentId is null)
  async getTopLevelCommentsByPostId(postId) {
    return apiRequest(`/comments/posts/${postId}/top-level`)
  },

  // Get replies to a specific comment
  async getCommentReplies(commentId) {
    return apiRequest(`/comments/${commentId}/replies`)
  },
  
  // Get comments by user ID
  async getCommentsByUserId(userId) {
    return apiRequest(`/comments/user/${userId}`)
  },
  
  // Create new comment
  async createComment(commentData) {
    return apiRequest('/comments', {
      method: 'POST',
      body: JSON.stringify(commentData),
    })
  },
  
  // Update comment
  async updateComment(commentId, commentData) {
    return apiRequest(`/comments/${commentId}`, {
      method: 'PUT',
      body: JSON.stringify(commentData),
    })
  },
  
  // Delete comment
  async deleteComment(commentId) {
    return apiRequest(`/comments/${commentId}`, {
      method: 'DELETE',
    })
  },
  
  // Delete all comments for a post
  async deleteCommentsByPostId(postId) {
    return apiRequest(`/comments/posts/${postId}`, {
      method: 'DELETE',
    })
  },
  
  // Delete all comments by a user
  async deleteCommentsByUserId(userId) {
    return apiRequest(`/comments/user/${userId}`, {
      method: 'DELETE',
    })
  },
  
  // Like a comment
  async likeComment(commentId, userId) {
    const queryParams = new URLSearchParams()
    queryParams.append('userId', userId)
    return apiRequest(`/comments/${commentId}/like?${queryParams.toString()}`, {
      method: 'POST',
    })
  },
  
  // Get comments count for a post
  async getCommentsCountByPostId(postId) {
    return apiRequest(`/comments/count/posts/${postId}`)
  },
  
  // Get comments count by user
  async getCommentsCountByUserId(userId) {
    return apiRequest(`/comments/count/user/${userId}`)
  },
  
  // Get total comments count
  async getTotalCommentsCount() {
    return apiRequest('/comments/count')
  }
}

/**
 * Games API Service (consolidated and updated for Spring Boot backend)
 */
export const gamesAPI = {
  // Public endpoints (no authentication required)
  
  // Get all active games (public access)
  async getActiveGames() {
    return apiRequest('/public/games/active')
  },

  // Get game by ID (public access for active games only)
  async getGameByIdPublic(gameId) {
    return apiRequest(`/public/games/${gameId}`)
  },
  
  // Search games by name (public access)
  async searchGamesPublic(searchTerm) {
    return apiRequest(`/public/games/search?name=${encodeURIComponent(searchTerm)}`)
  },
  
  // Get games by genre (public access)
  async getGamesByGenrePublic(genre) {
    return apiRequest(`/public/games/genre/${genre}`)
  },

  // Private endpoints (authentication required)
  
  // Get all games (requires authentication)
  async getAllGames() {
    return apiRequest('/games')
  },
  
  // Get game by ID
  async getGameById(gameId) {
    return apiRequest(`/games/${gameId}`)
  },
  
  // Get game by name
  async getGameByName(name) {
    return apiRequest(`/games/name/${name}`)
  },

  // Search games by name (includes inactive)
  async searchGames(searchTerm) {
    return apiRequest(`/games/search?name=${encodeURIComponent(searchTerm)}`)
  },

  // Get games by genre (includes inactive)
  async getGamesByGenre(genre) {
    return apiRequest(`/games/genre/${genre}`)
  },

  // Get games by platform
  async getGamesByPlatform(platform) {
    return apiRequest(`/games/platform/${platform}`)
  },

  // Get games created by user
  async getGamesByUser(userId) {
    return apiRequest(`/games/user/${userId}`)
  },

  // Create new game
  async createGame(gameData) {
    return apiRequest('/games', {
      method: 'POST',
      body: JSON.stringify(gameData),
    })
  },
  
  // Update game
  async updateGame(gameId, gameData) {
    return apiRequest(`/games/${gameId}`, {
      method: 'PUT',
      body: JSON.stringify(gameData),
    })
  },

  // Partially update game
  async patchGame(gameId, partialData) {
    return apiRequest(`/games/${gameId}`, {
      method: 'PATCH',
      body: JSON.stringify(partialData),
    })
  },
  
  // Delete game (soft delete)
  async deleteGame(gameId) {
    return apiRequest(`/games/${gameId}`, {
      method: 'DELETE',
    })
  },

  // Toggle game active status
  async toggleGameActive(gameId) {
    return apiRequest(`/games/${gameId}/toggle-active`, {
      method: 'PATCH',
    })
  },

  // Utility methods
  
  // Get distinct genres
  async getGenres() {
    return apiRequest('/games/genres')
  },

  // Get distinct platforms
  async getPlatforms() {
    return apiRequest('/games/platforms')
  },

  // Get game statistics
  async getGameStats() {
    return apiRequest('/games/stats')
  }
}

/**
 * Consent Audit API
 * Handles recording user consent decisions to the backend
 */
export const consentAuditAPI = {
  /**
   * Record user consent decision to backend
   * @param {object} consentData - Consent information to record
   * @returns {Promise} Response from the consent audit endpoint
   */
  async recordConsent(consentData) {
    return apiRequest('/consent-audits', {
      method: 'POST',
      body: JSON.stringify(consentData),
    })
  },


  /**
   * Get client IP address from backend
   * Used to retrieve the visitor's IP for IP-based consent tracking
   * @returns {Promise} Object with ipAddress property
   */
  async getClientIP() {
    return apiRequest('/ip-address')
  }
}

/**
 * Notifications API Service
 */
export const notificationsAPI = {
  // Get notifications for current user
  async getMyNotifications() {
    return apiRequest('/notifications')
  },

  // Mark notification as read
  async markAsRead(id) {
    return apiRequest(`/notifications/${id}/read`, {
      method: 'POST',
    })
  }
}

/**
 * Files API Service
 * Handles file uploads (images, etc)
 */
export const filesAPI = {
  /**
   * Upload an image file
   * @param {File} file - The image file to upload
   * @returns {Promise<{status: string, message: string, url: string}>}
   */
  async uploadImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    // We can't use the standard apiRequest wrapper easily for FormData 
    // because it assumes JSON content-type usually.
    // So we'll use a custom fetch here or adapt apiRequest.
    // Let's use apiRequest but override headers.
    
    // Note: When sending FormData, do NOT set Content-Type header manually,
    // let the browser set it with the boundary.
    
    const token = await getAuthToken()
    const headers = {}
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
    
    const response = await fetch(`${API_BASE_URL}/images/upload`, {
      method: 'POST',
      headers,
      body: formData
    })
    
    if (!response.ok) {
      const errorText = await response.text()
      try {
        const errorJson = JSON.parse(errorText)
        throw new Error(errorJson.message || 'Image upload failed')
      } catch (e) {
        throw new Error(errorText || 'Image upload failed')
      }
    }
    
    return response.json()
  }
}

// Default export for convenience
export default {
  games: gamesAPI,
  posts: postsAPI,
  profiles: profilesAPI,
  comments: commentsAPI,
  consentAudit: consentAuditAPI,
  notifications: notificationsAPI,
  auth: authAPI,
  files: filesAPI,
  storage: storageAPI,
  csrf: csrfAPI,
  apiRequest,
  waitForBackend
}