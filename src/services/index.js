/**
 * Services Index - Export all API services
 * 
 * This file provides centralized exports for all API services.
 * Use this for cleaner imports across the application.
 * 
 * Usage examples:
 * import { gamesAPI } from '@/services'
 * import { apiRequest } from '@/services'
 * import { reportingService } from '@/services'
 */

import { apiRequest, gamesAPI, postsAPI, commentsAPI, profilesAPI, notificationsAPI } from './apiService'
import { reportingService } from './reportingService'

// Re-export named services so other modules can import from '@/services'
export { apiRequest, gamesAPI, postsAPI, commentsAPI, profilesAPI, notificationsAPI, reportingService }

// Re-export for backward compatibility
export { default as apiService } from './apiService'

/**
 * Convenience exports for common operations
 */

// Games shortcuts  
export const games = {
  // Public access
  getActive: () => gamesAPI.getActiveGames(),
  getById: (id) => gamesAPI.getGameByIdPublic(id),
  search: (term) => gamesAPI.searchGamesPublic(term),
  
  // Authenticated access
  getAll: () => gamesAPI.getAllGames(),
  create: (data) => gamesAPI.createGame(data),
  update: (id, data) => gamesAPI.updateGame(id, data),
  delete: (id) => gamesAPI.deleteGame(id),
}

// Default export for backward compatibility
export default {
  apiRequest,
  gamesAPI,
  postsAPI,
  commentsAPI,
  profilesAPI,
  notificationsAPI,
  
  games
}