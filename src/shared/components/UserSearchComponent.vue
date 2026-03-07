<template>
  <div class="user-search-container">
    <div class="search-header">
      <h3>🔍 Find Users</h3>
      <div class="search-input-container">
        <input
          v-model="searchQuery"
          @input="handleSearch"
          placeholder="Search users by name..."
          class="search-input"
          type="text"
        />
        <div v-if="searchQuery && isSearching" class="search-loading">
          Searching...
        </div>
      </div>
    </div>

    <!-- Search Results -->
    <div v-if="searchQuery && !isSearching" class="search-results">
      <div v-if="searchResults.length === 0" class="no-results">
        <p>No users found matching "{{ searchQuery }}"</p>
      </div>
      
      <div v-else class="results-list">
        <h4>Found {{ searchResults.length }} user{{ searchResults.length !== 1 ? 's' : '' }}:</h4>
        <div 
          v-for="user in searchResults" 
          :key="user.id" 
          class="user-result-item"
          @click="navigateToProfile(user.id)"
        >
          <div class="user-avatar">
            <img 
              :src="user.ppUrl || defaultAvatar" 
              :alt="user.displayName + ' profile photo'"
              class="avatar-img"
              @error="$event.target.src = defaultAvatar"
            />
          </div>
          
          <div class="user-info">
            <h5 class="user-name">{{ user.displayName }}</h5>
            <p v-if="user.userBio" class="user-bio">{{ truncateBio(user.userBio) }}</p>
            <p v-else class="no-bio">No bio available</p>
            <div class="user-stats">
              <span class="stat">{{ user.postsCount || 0 }} posts</span>
            </div>
          </div>
          
          <div class="view-profile-btn">
            <span>View →</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Search Tips -->
    <div v-if="!searchQuery" class="search-tips">
      <div class="tip-item">
        <span class="tip-icon">💡</span>
        <span>Search for users by their display name to view their profiles</span>
      </div>
      <div class="tip-item">
        <span class="tip-icon">⚡</span>
        <span>Results appear as you type - no need to press enter</span>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { profilesAPI, postsAPI } from '@/services/apiService'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'

export default {
  name: 'UserSearchComponent',
  setup() {
    const router = useRouter()
    
    // State
    const searchQuery = ref('')
    const searchResults = ref([])
    const isSearching = ref(false)
    const searchTimeout = ref(null)
    
    // Methods
    function handleSearch() {
      // Clear previous timeout
      if (searchTimeout.value) {
        clearTimeout(searchTimeout.value)
      }
      
      // If query is empty, clear results
      if (!searchQuery.value.trim()) {
        searchResults.value = []
        return
      }
      
      // Debounce search to avoid too many queries
      searchTimeout.value = setTimeout(() => {
        performSearch()
      }, 300)
    }
    
    async function performSearch() {
      const query = searchQuery.value.trim().toLowerCase()
      if (!query) return
      
      isSearching.value = true
      
      try {
        // Search for users via backend API
        const users = await profilesAPI.searchProfilesByDisplayName(query)
        
        // Get post counts for all users
        const userPostsCounts = {}
        const allPosts = await postsAPI.getAllPosts()
        
        if (Array.isArray(allPosts)) {
          allPosts.forEach((post) => {
            if (post.userId) {
              userPostsCounts[post.userId] = (userPostsCounts[post.userId] || 0) + 1
            }
          })
        }
        
        // Format results with post counts
        const formattedUsers = (Array.isArray(users) ? users : []).map((userData) => ({
          id: userData.uid || userData.id,
          displayName: userData.displayName || 'Anonymous User',
          ppUrl: userData.photoUrl || defaultAvatar,
          userBio: userData.bio || '',
          postsCount: userPostsCounts[userData.uid || userData.id] || 0,
          ...userData
        }))
        
        // Sort by relevance (exact matches first, then by posts count)
        formattedUsers.sort((a, b) => {
          const aExact = a.displayName.toLowerCase() === query
          const bExact = b.displayName.toLowerCase() === query
          
          if (aExact && !bExact) return -1
          if (!aExact && bExact) return 1
          
          // If both or neither are exact matches, sort by posts count
          return b.postsCount - a.postsCount
        })
        
        searchResults.value = formattedUsers
        
      } catch (error) {
        console.error('Error searching users:', error)
        searchResults.value = []
      } finally {
        isSearching.value = false
      }
    }
    
    function navigateToProfile(userId) {
      router.push(`/user/${userId}`)
    }
    
    function truncateBio(bio) {
      if (!bio) return ''
      return bio.length > 100 ? bio.substring(0, 100) + '...' : bio
    }
    
    // Cleanup
    onUnmounted(() => {
      if (searchTimeout.value) {
        clearTimeout(searchTimeout.value)
      }
    })
    
    return {
      searchQuery,
      searchResults,
      isSearching,
      defaultAvatar,
      handleSearch,
      navigateToProfile,
      truncateBio
    }
  }
}
</script>

<style scoped>
.user-search-container {
  background: white;
  border-radius: 15px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.08);
  border: 1px solid #f0f0f0;
  max-width: 800px
}

.search-header h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.3em;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input-container {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #f0f0f0;
  border-radius: 25px;
  font-size: 1em;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: #dd7724ff;
  box-shadow: 0 0 0 3px rgba(221, 119, 36, 0.1);
}

.search-loading {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
  font-size: 0.9em;
}

.search-results {
  margin-top: 20px;
}

.no-results {
  text-align: center;
  padding: 30px;
  color: #666;
  background: #f8f9fa;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.results-list h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.1em;
}

.user-result-item {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.user-result-item:hover {
  background: #f8f9fa;
  border-color: #dd7724ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(221, 119, 36, 0.15);
}

.user-avatar {
  flex-shrink: 0;
}

.avatar-img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #dd7724ff;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 1.1em;
  font-weight: 600;
}

.user-bio {
  margin: 0 0 8px 0;
  color: #666;
  font-size: 0.9em;
  line-height: 1.4;
}

.no-bio {
  margin: 0 0 8px 0;
  color: #999;
  font-size: 0.9em;
  font-style: italic;
}

.user-stats {
  display: flex;
  gap: 15px;
}

.stat {
  font-size: 0.85em;
  color: #dd7724ff;
  font-weight: 500;
}

.view-profile-btn {
  flex-shrink: 0;
  color: #dd7724ff;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.user-result-item:hover .view-profile-btn {
  background: #dd7724ff;
  color: white;
}

.search-tips {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #666;
  font-size: 0.9em;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-icon {
  font-size: 1.1em;
}

/* Responsive Design */
@media (max-width: 768px) {
  .user-result-item {
    gap: 12px;
    padding: 12px;
  }
  
  .avatar-img {
    width: 40px;
    height: 40px;
  }
  
  .user-name {
    font-size: 1em;
  }
  
  .user-bio {
    font-size: 0.85em;
  }
  
  .view-profile-btn {
    padding: 6px 8px;
    font-size: 0.9em;
  }
}
</style>