<template>
  <div class="choose-game-view">
    <!-- Game Category Selection -->
    <div class="game-filter">
      <label for="game-search" class="filter-label">🎮 Choose Game:</label>
      <div class="game-search-container">
        <input 
          v-model="gameSearchQuery" 
          @input="onGameSearchInput"
          @focus="showGameDropdown = true"
          id="game-search" 
          class="game-search-input" 
          placeholder="Search for a game..." 
          autocomplete="off"
        />
        <div v-if="selectedGame" class="selected-game-display">
          <span class="selected-game-chip">
            🎮 {{ selectedGame }}
            <button @click="clearGameSelection" class="clear-game-btn">✕</button>
          </span>
        </div>
        
        <!-- Game Details Display -->
        <div v-if="gameDisplayInfo && hasGameDetails" class="game-details-card">
          <div class="game-header">
            <div class="game-icon-container">
              <img v-if="gameDisplayInfo.icon" :src="gameDisplayInfo.icon" :alt="gameDisplayInfo.name" class="game-icon" />
              <div v-else class="game-icon-placeholder">🎮</div>
            </div>
            <div class="game-basic-info">
              <h4 class="game-title">{{ gameDisplayInfo.name }}</h4>
              <div class="game-meta">
                <span v-if="gameDisplayInfo.genre" class="meta-item">🏷️ {{ gameDisplayInfo.genre }}</span>
                <span v-if="gameDisplayInfo.rating" class="meta-item">⭐ {{ gameDisplayInfo.rating }}</span>
              </div>
            </div>
          </div>
          <p v-if="gameDisplayInfo.description" class="game-description">{{ gameDisplayInfo.description }}</p>
          <p v-if="gameDisplayInfo.releaseDate" class="game-release">📅 Released: {{ gameDisplayInfo.releaseDate }}</p>
        </div>
      </div>
      
      <!-- Game Buttons Grid -->
      <div v-if="gamesLoading" class="games-loading">
        <div class="loading-spinner-small"></div>
      </div>
      
      
      
      <div v-else-if="showGameDropdown && !selectedGame" class="games-grid">
        <button 
          v-for="game in filteredGameCategories" 
          :key="game" 
          @click="selectGame(game)"
          class="game-button"
          :class="{ 'highlighted': isGameHighlighted(game) }"
        >
          {{ game }}
        </button>
        <div v-if="filteredGameCategories.length === 0" class="no-games-found">
          No games found matching "{{ gameSearchQuery }}"
        </div>
      </div>
    </div>
    
    <!-- Video Type Selection (only show after game is selected) -->
    <div v-if="selectedGame" class="video-type-filter">
      <label class="filter-label">📹 Video Type:</label>
      <div class="video-type-buttons">
        <button 
          @click="selectVideoType('short')" 
          :class="['type-btn', { 'active': selectedVideoType === 'short' }]"
        >
          ⚡ Short (≤1 min)
        </button>
        <button 
          @click="selectVideoType('long')" 
          :class="['type-btn', { 'active': selectedVideoType === 'long' }]"
        >
          🎬 Long (>1 min)
        </button>
      </div>
    </div>

    <!-- Game Selection Required Display -->
    <div v-if="!selectedGame" class="selection-required">
      <div class="selection-card">
        <h4>🎮 Welcome to Gaming Hub!</h4>
        <p>Please select a game category above to view posts</p>
        <div class="popular-games">
          <p><strong>Popular games:</strong></p>
          <div class="game-chips">
            <span 
              v-for="game in gameCategories.slice(0, 5)" 
              :key="game" 
              @click="selectGame(game)"
              class="game-chip"
            >
              {{ game }}
            </span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- Video Type Selection Required -->
    <div v-else-if="!selectedVideoType" class="selection-required">
      <div class="selection-card">
        <h4>📹 Choose Video Type for {{ selectedGame }}</h4>
        <p>Select the type of content you want to view:</p>
        <div class="type-selection-large">
          <button 
            @click="selectVideoType('short')" 
            class="large-type-btn"
          >
            <div class="btn-icon">⚡</div>
            <div class="btn-text">
              <strong>Short Videos</strong>
              <span>Quick gameplay clips (≤1 minute)</span>
            </div>
          </button>
          <button 
            @click="selectVideoType('long')" 
            class="large-type-btn"
          >
            <div class="btn-icon">🎬</div>
            <div class="btn-text">
              <strong>Long Videos</strong>
              <span>Extended gameplay & tutorials (>1 minute max. 100MB)</span>
            </div>
          </button>
        </div>
      </div>
    </div>
    
    <!-- Active filters display -->
    <div v-if="selectedGame && selectedVideoType" class="active-filters">
      <span class="filter-tag">🎮 {{ selectedGame }}</span>
      <span class="filter-tag">📹 {{ selectedVideoType === 'short' ? 'Short Videos' : 'Long Videos' }}</span>
      <button @click="clearFilters" class="clear-filters">✕ Clear</button>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { gamesAPI } from '@/services/apiService'

export default {
  name: 'ChooseGameView',
  
  emits: ['gameSelected', 'videoTypeSelected', 'filtersCleared'],
  
  setup(props, { emit }) {
    // State
    const gameSearchQuery = ref('')
    const showGameDropdown = ref(false)
    
    // Self-contained state (previously props)
    const gameCategories = ref([])
    const gamesLoading = ref(true)
    const selectedGame = ref('')
    const selectedVideoType = ref('')
    
    // PostgreSQL Game Fields
    const gameDescription = ref('')
    const gameGenre = ref('')
    const gameIconUrl = ref('')
    const gameId = ref('')
    const gameName = ref('')
    const gameRating = ref('')
    const gameReleaseDate = ref('')

    // Load game categories from PostgreSQL backend API
    async function loadGameCategories() {
      try {
        gamesLoading.value = true
        
        // Call backend API to get active games from PostgreSQL
        const gamesData = await gamesAPI.getActiveGames()
        
        const games = []
        const gameDocuments = [] // Store complete game objects with all fields
        
        if (Array.isArray(gamesData)) {
          gamesData.forEach((gameData) => {
            // Create complete game object with actual PostgreSQL schema fields
            const gameDocument = {
              // Database metadata
              id: gameData.id,
              
              // Core game fields from PostgreSQL games table (10 fields only)
              name: gameData.name || '',
              description: gameData.description || '',
              genre: gameData.genre || '',
              icon: gameData.icon || '',
              rating: gameData.rating || '',
              releaseDate: gameData.releaseDate || '',
              isActive: gameData.isActive !== undefined ? gameData.isActive : true,
              createdAt: gameData.createdAt,
              updatedAt: gameData.updatedAt
            }
            
            // Add to complete documents array
            gameDocuments.push(gameDocument)
            
            // Add name to simple array for UI compatibility (only if active)
            if (gameDocument.name && gameDocument.isActive !== false) {
              games.push(gameDocument.name)
            }
          })
        }
        
        // Store both the simple names array and complete documents
        gameCategories.value = games
        
        // Store complete game documents globally for reference
        window.gameDocuments = gameDocuments
        
        // Always ensure at least 'Other' is available for fallback
        if (games.length === 0) {
          games.push('Other')
          gameCategories.value = games
        } else if (!games.includes('Other')) {
          games.push('Other')
          gameCategories.value = games
        }
        
        gamesLoading.value = false
        
      } catch (error) {
        console.error('Failed to load games from PostgreSQL backend:', error)
        gamesLoading.value = false
        
        // Minimal fallback - just 'Other' category
        gameCategories.value = ['Other']
      }
    }

    // Computed for filtered game categories based on search
    const filteredGameCategories = computed(() => {
      if (!gameSearchQuery.value) {
        return gameCategories.value
      }
      return gameCategories.value.filter(game => 
        game.toLowerCase().includes(gameSearchQuery.value.toLowerCase())
      )
    })

    // Computed to check if we have detailed game information loaded
    const hasGameDetails = computed(() => {
      return gameId.value || gameDescription.value || 
             gameGenre.value || gameIconUrl.value || gameRating.value || gameReleaseDate.value
    })

    // Computed to format game info for display
    const gameDisplayInfo = computed(() => {
      if (!hasGameDetails.value) return null
      
      return {
        id: gameId.value,
        name: gameName.value || selectedGame.value,
        description: gameDescription.value,
        genre: gameGenre.value,
        icon: gameIconUrl.value,
        rating: gameRating.value,
        releaseDate: gameReleaseDate.value
      }
    })

    // Load detailed game information from PostgreSQL backend API
    async function loadGameDropdown(selectedGameName) {
      if (!selectedGameName) {
        // Clear all game details
        gameDescription.value = ''
        gameGenre.value = ''
        gameIconUrl.value = ''
        gameId.value = ''
        gameName.value = ''
        gameRating.value = ''
        gameReleaseDate.value = ''
        return
      }

      try {
        // Find the game from our loaded documents by name
        const gameDocument = window.gameDocuments?.find(g => g.name === selectedGameName)
        
        if (gameDocument) {
          // Populate game details from PostgreSQL data
          gameId.value = gameDocument.id || ''
          gameName.value = gameDocument.name || ''
          gameDescription.value = gameDocument.description || ''
          gameGenre.value = gameDocument.genre || ''
          gameIconUrl.value = gameDocument.icon || ''
          gameRating.value = gameDocument.rating || ''
          gameReleaseDate.value = gameDocument.releaseDate || ''
          
          // Store complete game data from PostgreSQL
          window.currentGameData = {
            id: gameDocument.id,
            ...gameDocument
          }
          
        } else if (selectedGameName === 'Other') {
          // Handle 'Other' game category
          gameName.value = 'Other'
          gameDescription.value = 'Other games not listed in our database'
          gameId.value = ''
          gameGenre.value = ''
          gameIconUrl.value = ''
          gameRating.value = ''
          gameReleaseDate.value = ''
          
          if (window.currentGameData) {
            delete window.currentGameData
          }
        } else {
          // Game not found - clear details but keep name
          gameDescription.value = ''
          // gameDeveloper.value = '' // Removed unused ref
          gameGenre.value = ''
          gameIconUrl.value = ''
          gameId.value = ''
          gameName.value = selectedGameName
          gameRating.value = ''
          gameReleaseDate.value = ''
          
          if (window.currentGameData) {
            delete window.currentGameData
          }
        }
      } catch (error) {
        console.error('Error loading game details:', error)
        // Keep basic info but clear detailed fields on error
        gameName.value = selectedGameName
        gameDescription.value = ''
        // gameDeveloper.value = '' // Removed unused ref
        gameGenre.value = ''
        gameIconUrl.value = ''
        gameId.value = ''
        gameRating.value = ''
        gameReleaseDate.value = ''
        
        if (window.currentGameData) {
          delete window.currentGameData
        }
      }
    }

    // Load game details by ID (alternative method - using API)
    async function loadGameById(gameId) {
      if (!gameId) return

      try {
        const gameData = await gamesAPI.getGameByIdPublic(gameId)
        
        if (gameData) {
          // Populate the game details
          gameId.value = gameData.id || ''
          gameName.value = gameData.name || ''
          gameDescription.value = gameData.description || ''
          gameGenre.value = gameData.genre || ''
          gameIconUrl.value = gameData.icon || ''
          gameRating.value = gameData.rating || ''
          gameReleaseDate.value = gameData.releaseDate || ''
        }
      } catch (error) {
        // Error handled silently
      }
    }

    // Methods
    const selectGame = async (game) => {
      selectedGame.value = game
      selectedVideoType.value = '' // Reset video type when game changes
      gameSearchQuery.value = ''
      showGameDropdown.value = false
      
      // Emit event
      emit('gameSelected', game)
      
      // Load detailed game information when a game is selected
      await loadGameDropdown(game)
    }
    
    const selectVideoType = (type) => {
      selectedVideoType.value = type
      // Emit event
      emit('videoTypeSelected', type)
    }
    
    const clearGameSelection = () => {
      selectedGame.value = ''
      gameSearchQuery.value = ''
      showGameDropdown.value = false
      
      // Emit event
      emit('filtersCleared')
      
      // Clear game details when selection is cleared
      loadGameDropdown('') // This will clear all the game detail fields
    }
    
    const onGameSearchInput = () => {
      showGameDropdown.value = true
    }
    
    const isGameHighlighted = (game) => {
      if (!gameSearchQuery.value) return false
      return game.toLowerCase().includes(gameSearchQuery.value.toLowerCase())
    }
    
    const clearFilters = () => {
      selectedGame.value = ''
      selectedVideoType.value = ''
      gameSearchQuery.value = ''
      showGameDropdown.value = false
      
      // Emit event
      emit('filtersCleared')
      
      // Clear game details when filters are cleared
      loadGameDropdown('')
    }
    
    // Click outside handler to close dropdown
    const handleClickOutside = (event) => {
      const gameFilter = document.querySelector('.game-filter')
      if (gameFilter && !gameFilter.contains(event.target)) {
        showGameDropdown.value = false
      }
    }

    // Lifecycle
    onMounted(() => {
      document.addEventListener('click', handleClickOutside)
      // Load game categories on component mount
      loadGameCategories()
    })

    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside)
    })

    return {
      // Search and UI state
      gameSearchQuery,
      showGameDropdown,
      filteredGameCategories,
      hasGameDetails,
      gameDisplayInfo,
      
      // Self-contained state (previously props)
      gameCategories,
      gamesLoading,
      selectedGame,
      selectedVideoType,
      
      // Game details from gamesCollection
      gameDescription,
      gameGenre,
      gameIconUrl,
      gameId,
      gameName,
      gameRating,
      gameReleaseDate,
      
      // Methods
      selectGame,
      selectVideoType,
      clearGameSelection,
      onGameSearchInput,
      isGameHighlighted,
      clearFilters,
      loadGameDropdown,
      loadGameById,
      loadGameCategories
    }
  }
}
</script>

<style scoped>
.choose-game-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.filter-label {
  font-weight: 600;
  margin-bottom: 8px;
  display: block;
}

.game-filter {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
}

.game-search-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.game-search-input {
  padding: 12px 15px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  background: white;
  font-size: 1em;
  transition: all 0.3s;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.game-search-input:focus {
  outline: none;
  border-color: #dd7724ff;
  box-shadow: 0 0 0 3px rgba(221, 119, 36, 0.1);
}

.selected-game-display {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.selected-game-chip {
  background: #dd7724ff;
  color: white;
  padding: 8px 12px;
  border-radius: 20px;
  font-size: 0.9em;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.clear-game-btn {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  font-size: 1.1em;
  padding: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.clear-game-btn:hover {
  background: rgba(255,255,255,0.2);
}

.games-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 30px 15px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  text-align: center;
}

.loading-spinner-small {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #dd7724ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.games-loading p {
  color: #666;
  margin: 0;
  font-size: 0.9em;
}



.error-icon {
  font-size: 2.5em;
  margin-bottom: 5px;
}

.retry-btn {
  background: #dd7724ff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9em;
  font-weight: 500;
  transition: all 0.3s;
  margin-top: 8px;
}

.retry-btn:hover {
  background: #c66821;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(221, 119, 36, 0.3);
}

.games-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
  padding: 15px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  z-index: 1000;
}

.game-button {
  background: #f8f9fa;
  border: 2px solid #e9ecef;
  color: #495057;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9em;
  font-weight: 500;
  transition: all 0.3s;
  text-align: center;
  min-height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-button:hover {
  background: #e9ecef;
  border-color: #dee2e6;
  transform: translateY(-1px);
}

.game-button.highlighted {
  background: #fff3cd;
  border-color: #ffc107;
  color: #856404;
  font-weight: 600;
}

.game-button:active {
  transform: translateY(0);
}

.no-games-found {
  grid-column: 1 / -1;
  text-align: center;
  padding: 20px;
  color: #6c757d;
  font-style: italic;
}

.video-type-filter {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.video-type-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.type-btn {
  background: white;
  color: #6c757d;
  border: 2px solid #e0e0e0;
  padding: 12px 20px;
  border-radius: 25px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s;
  flex: 1;
  min-width: 150px;
}

.type-btn:hover {
  border-color: #dd7724ff;
  color: #dd7724ff;
}

.type-btn.active {
  background: #dd7724ff;
  color: white;
  border-color: #dd7724ff;
  box-shadow: 0 4px 12px rgba(221, 119, 36, 0.3);
}

/* Selection screens */
.selection-required {
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.selection-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 15px 35px rgba(0,0,0,0.2);
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.selection-card h4 {
  font-size: 1.8em;
  margin-bottom: 15px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.selection-card p {
  margin-bottom: 25px;
  opacity: 0.9;
  font-size: 1.1em;
}

.popular-games {
  margin-top: 25px;
}

.game-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 15px;
}

.game-chip {
  background: rgba(255,255,255,0.2);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
  font-weight: 500;
}

.game-chip:hover {
  background: rgba(255,255,255,0.3);
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.2);
}

.type-selection-large {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 25px;
}

.large-type-btn {
  background: rgba(255,255,255,0.1);
  border: 2px solid rgba(255,255,255,0.3);
  color: white;
  padding: 25px;
  border-radius: 15px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 15px;
  min-width: 250px;
  backdrop-filter: blur(10px);
}

.large-type-btn:hover {
  background: rgba(255,255,255,0.2);
  border-color: rgba(255,255,255,0.5);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0,0,0,0.2);
}

.btn-icon {
  font-size: 2.5em;
  flex-shrink: 0;
}

.btn-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.btn-text strong {
  font-size: 1.2em;
}

.btn-text span {
  opacity: 0.8;
  font-size: 0.9em;
}

/* Active filters display */
.active-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 15px;
  background: #e8f5e8;
  border-radius: 10px;
  border-left: 4px solid #28a745;
  flex-wrap: wrap;
}

.filter-tag {
  background: #28a745;
  color: white;
  padding: 6px 12px;
  border-radius: 15px;
  font-size: 0.9em;
  font-weight: 500;
}

.clear-filters {
  background: #dc3545;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 15px;
  cursor: pointer;
  font-size: 0.9em;
  font-weight: 500;
  transition: all 0.3s;
  margin-left: auto;
}

.clear-filters:hover {
  background: #c82333;
  transform: scale(1.05);
}

/* Game Details Card */
.game-details-card {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 1px solid #dee2e6;
  border-radius: 15px;
  padding: 20px;
  margin-top: 15px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.game-header {
  display: flex;
  gap: 15px;
  align-items: flex-start;
  margin-bottom: 15px;
}

.game-icon-container {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: 12px;
  overflow: hidden;
  background: #ffffff;
  border: 2px solid #e9ecef;
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.game-icon-placeholder {
  font-size: 24px;
  color: #6c757d;
}

.game-basic-info {
  flex: 1;
  min-width: 0;
}

.game-title {
  font-size: 1.3em;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 8px 0;
  line-height: 1.2;
}

.game-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 5px;
}

.meta-item {
  background: rgba(221, 119, 36, 0.1);
  color: #dd7724ff;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.85em;
  font-weight: 500;
  border: 1px solid rgba(221, 119, 36, 0.2);
}

.game-description {
  color: #495057;
  font-size: 0.95em;
  line-height: 1.5;
  margin: 10px 0;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 10px;
  border-left: 3px solid #dd7724ff;
  font-style: italic;
}

.game-release {
  color: #6c757d;
  font-size: 0.9em;
  margin: 8px 0 0 0;
  font-weight: 500;
}

/* Responsive adjustments for game details */
@media (max-width: 768px) {
  .game-header {
    flex-direction: column;
    gap: 10px;
  }
  
  .game-icon-container {
    width: 50px;
    height: 50px;
  }
  
  .game-title {
    font-size: 1.2em;
  }
  
  .game-meta {
    gap: 8px;
  }
  
  .meta-item {
    font-size: 0.8em;
    padding: 3px 8px;
  }
}
</style>