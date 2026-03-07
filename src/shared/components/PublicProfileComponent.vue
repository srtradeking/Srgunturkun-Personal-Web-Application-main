<template>
  <div class="public-profile-container">
    <div class="public-profile-overlay"></div>
    
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <p>Loading user profile...</p>
    </div>
    
    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="$router.push('/social')" class="back-btn">
        ← Back to Social
      </button>
    </div>
    
    <!-- Profile Content -->
    <div v-else class="profile-content">
      <div class="profile-header">
        <div class="profile-photo-section">
          <img 
            :src="userProfile.ppUrl || defaultAvatar" 
            :alt="userProfile.displayName + ' profile photo'"
            class="profile-photo"
          />
        </div>
        
        <div class="profile-info">
          <h1 class="profile-name">{{ userProfile.displayName || 'Anonymous User' }}</h1>
          <p v-if="userProfile.userBio" class="profile-bio">{{ userProfile.userBio }}</p>
          <p v-else class="no-bio">This user hasn't added a bio yet.</p>
          
          <div class="profile-stats">
            <div class="stat">
              <span class="stat-number">{{ userPosts.length }}</span>
              <span class="stat-label">Posts</span>
            </div>
            <div class="stat">
              <span class="stat-number">{{ totalReactions }}</span>
              <span class="stat-label">Reactions</span>
            </div>
            <div class="stat">
              <span class="stat-number">{{ totalComments }}</span>
              <span class="stat-label">Comments</span>
            </div>
          </div>
          
          <div class="profile-actions">
            <button @click="$router.push('/social')" class="back-btn">
              ← Back to Social
            </button>
            <button 
              v-if="isCurrentUser" 
              @click="$router.push('/profile')" 
              class="edit-profile-btn"
            >
              Edit Profile
            </button>
          </div>
        </div>
      </div>
      
      <!-- User Posts Section -->
      <div class="user-posts-section">
        <h2>Posts by {{ userProfile.displayName || 'Anonymous User' }}</h2>
        
        <div v-if="postsLoading" class="loading-posts">
          <p>Loading posts...</p>
        </div>
        
        <div v-else-if="userPosts.length === 0" class="no-posts">
          <p>This user hasn't posted anything yet.</p>
        </div>
        
        <div v-else class="posts-grid">
          <div 
            v-for="post in userPosts" 
            :key="post.id" 
            class="post-card"
            @click="canViewPost(post) && navigateToPost(post.id)"
            :class="{ 'clickable': canViewPost(post), 'private-post': !canViewPost(post) }"
          >
            <div class="post-header">
              <h3>{{ post.title }}</h3>
              <span class="post-date">{{ formatDate(post.timestamp) }}</span>
              <span v-if="!canViewPost(post)" class="private-badge">🔒 Private</span>
            </div>
            
            <div class="post-media">
              <video v-if="post.type === 'video'" controls @click.stop>
                <source :src="post.url" :type="post.mimeType">
              </video>
              <img v-else :src="post.url" :alt="post.title">
            </div>
            
            <p v-if="post.description" class="post-description">{{ post.description }}</p>
            
                        <div class="post-stats">
              <span class="post-reactions">{{ getTotalReactions(post) }} reactions</span>
              <span class="post-comments">{{ getCommentsCount(post.id) }} comments</span>
              <button @click="navigateToPost(post.id)" class="redirect-post-btn" title="Go to post">
                View Post
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import infoSocialBackground from '@/shared/assets/InfoSocialBackground.png'
import { profilesAPI, postsAPI, commentsAPI } from '@/services/apiService'

export default {
  name: 'PublicProfileComponent',
  props: {
    userId: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()
    
    // State
    const loading = ref(true)
    const postsLoading = ref(true)
    const error = ref('')
    const userProfile = ref({})
    const userPosts = ref([])
    const postComments = ref({})
    const currentUser = ref(null)
    
    let unsubscribePosts = null
    let unsubscribeAuth = null
    let unsubscribeProfile = null
    
    // Computed
    const isCurrentUser = computed(() => {
      try {
        const storedId = localStorage.getItem('userId')
        if (!storedId) return false
        const numericId = Number(storedId)
        if (Number.isNaN(numericId)) return false
        return String(numericId) === String(props.userId)
      } catch {
        return false
      }
    })
    
    const totalReactions = computed(() => {
      return userPosts.value.reduce((total, post) => {
        return total + getTotalReactions(post)
      }, 0)
    })
    
    const totalComments = computed(() => {
      return userPosts.value.reduce((total, post) => {
        return total + getCommentsCount(post.id)
      }, 0)
    })
    
    // Methods
    async function loadUserProfile() {
      try {
        loading.value = true
        error.value = ''
        
        // Load profile from backend API
        const profileData = await profilesAPI.getProfileByUserId(props.userId, { public: true })
        
        if (!profileData) {
          error.value = 'User profile not found.'
          loading.value = false
          return
        }
        
        userProfile.value = {
          displayName: profileData.displayName || 'Anonymous User',
          ppUrl: profileData.photoUrl || defaultAvatar,
          userBio: profileData.bio || '',
          ...profileData
        }
        
        loading.value = false
        
      } catch (err) {
        console.error('Error loading user profile:', err)
        error.value = 'Failed to load user profile. Please try again.'
        loading.value = false
      }
    }
    
    async function loadUserPosts() {
      try {
        postsLoading.value = true
        
        // Load posts from backend API
        const posts = await postsAPI.getPostsByUserId(props.userId)
        
        // Sort posts by timestamp (newest first)
        if (Array.isArray(posts)) {
          posts.sort((a, b) => {
            const aTime = new Date(a.timestamp || 0).getTime()
            const bTime = new Date(b.timestamp || 0).getTime()
            return bTime - aTime
          })
        }
        
        userPosts.value = posts || []
        postsLoading.value = false
        
        // Load comments for these posts
        await loadPostComments()
      } catch (err) {
        console.error('Error loading user posts:', err)
        postsLoading.value = false
      }
    }
    
    async function loadPostComments() {
      try {
        const postIds = userPosts.value.map(post => post.id)
        if (postIds.length === 0) return
        
        const comments = {}
        
        // Get comments for each post
        for (const postId of postIds) {
          try {
            const postComments = await commentsAPI.getCommentsByPostId(postId)
            if (Array.isArray(postComments)) {
              comments[postId] = postComments
            }
          } catch (err) {
            console.error(`Error loading comments for post ${postId}:`, err)
          }
        }
        
        postComments.value = comments
      } catch (err) {
        console.error('Error loading comments:', err)
      }
    }
    
    function getTotalReactions(post) {
      if (!post.reactions) return 0
      return Object.values(post.reactions)
        .reduce((total, reactions) => total + reactions.length, 0)
    }
    
    function getCommentsCount(postId) {
      return postComments.value[postId]?.length || 0
    }
    
    function navigateToPost(postId) {
      router.push(`/posts/${postId}`)
    }

    function formatDate(timestamp) {
      if (!timestamp) return 'Unknown date'
      
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    function canViewPost(post) {
      const isPublic = post.isPublished && !post.isHidden && !post.isDeleted
      return isPublic
    }
    
    // Lifecycle
    onMounted(() => {
      // Load profile and posts
      loadUserProfile()
      loadUserPosts()
    })
    
    return {
      loading,
      postsLoading,
      error,
      userProfile,
      userPosts,
      isCurrentUser,
      totalReactions,
      totalComments,
      defaultAvatar,
      getTotalReactions,
      getCommentsCount,
      formatDate,
      navigateToPost,
      canViewPost,
      infoSocialBackground
    }
  },
  mounted() {
    this.$el.style.setProperty('--info-social-background', `url('${this.infoSocialBackground}')`)
  }
}
</script>

<style scoped>
.public-profile-container {
  position: relative;
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

.public-profile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--info-social-background);
  background-position: center;
  background-size: cover;
  opacity: 0.7;
  z-index: -1;
  filter: brightness(0.7);
}

.loading-state,
.error-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.loading-state p {
  color: #666;
  font-size: 1.2em;
}

.error-state p {
  color: #dc3545;
  font-size: 1.1em;
  margin-bottom: 20px;
}

.profile-content {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.profile-header {
  display: flex;
  gap: 30px;
  padding: 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.profile-photo-section {
  flex-shrink: 0;
}

.profile-photo {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  object-fit: cover;
  border: 4px solid white;
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.profile-info {
  flex: 1;
}

.profile-name {
  margin: 0 0 15px 0;
  font-size: 2.2em;
  font-weight: 600;
}

.profile-bio {
  font-size: 1.1em;
  line-height: 1.5;
  margin-bottom: 25px;
  opacity: 0.9;
}

.no-bio {
  font-size: 1em;
  margin-bottom: 25px;
  opacity: 0.7;
  font-style: italic;
}

.profile-stats {
  display: flex;
  gap: 30px;
  margin-bottom: 25px;
}

.stat {
  text-align: center;
}

.stat-number {
  display: block;
  font-size: 1.8em;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 0.9em;
  opacity: 0.8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.profile-actions {
  display: flex;
  gap: 15px;
}

.back-btn,
.edit-profile-btn {
  padding: 10px 20px;
  border: 2px solid white;
  background: transparent;
  color: white;
  border-radius: 25px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.back-btn:hover,
.edit-profile-btn:hover {
  background: white;
  color: #667eea;
}

.edit-profile-btn {
  background: rgba(255,255,255,0.2);
}

.user-posts-section {
  padding: 40px;
}

.user-posts-section h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 1.8em;
}

.loading-posts,
.no-posts {
  text-align: center;
  padding: 40px;
  color: #666;
}

.posts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
}

.post-card {
  background: #f8f9fa;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.post-card.clickable {
  cursor: pointer;
}

.post-card.clickable:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.post-card.private-post {
  opacity: 0.7;
  cursor: not-allowed;
}

.post-card.private-post:hover {
  transform: none;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.post-header {
  padding: 15px;
  background: white;
  border-bottom: 1px solid #e9ecef;
  position: relative;
}

.post-header h3 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 1.1em;
}

.post-date {
  font-size: 0.8em;
  color: #666;
}

.private-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #ffc107;
  color: #333;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.75em;
  font-weight: 600;
}

.post-media {
  width: 100%;
}

.post-media img,
.post-media video {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.post-description {
  padding: 15px;
  margin: 0;
  color: #555;
  line-height: 1.4;
  background: white;
}

.post-stats {
  padding: 10px 15px;
  background: white;
  border-top: 1px solid #e9ecef;
  display: flex;
  gap: 15px;
  font-size: 0.9em;
  color: #666;
  align-items: center;
}

.redirect-post-btn {
  margin-left: auto;
  background: #667eea;
  color: white;
  border: none;
  padding: 5px 12px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.85em;
  font-weight: 500;
  transition: background-color 0.2s;
}

.redirect-post-btn:hover {
  background: #5a6fd8;
}

/* Responsive Design */
@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
    padding: 30px 20px;
  }
  
  .profile-stats {
    justify-content: center;
  }
  
  .posts-grid {
    grid-template-columns: 1fr;
  }
  
  .profile-actions {
    justify-content: center;
  }
}
</style>