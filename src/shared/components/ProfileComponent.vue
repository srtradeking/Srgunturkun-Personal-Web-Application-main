<template>
  <div class="profile-box">
    <div v-if="isLoggedIn" class="profile-content">
      <div class="profile-section">
        <div class="profile-header">
          <div class="profile-photo-container">
            <img :src="profilePhoto" alt="Profile Photo" class="profile-photo" />
            <label class="profile-photo-upload">
              <input type="file" @change="handleProfilePhotoSelect" accept="image/*" class="hidden" />
              <span>Change Photo</span>
            </label>
            <div class="coin-balance-section">
              <img :src="tradekingCoin" alt="Tradeking Coin" class="coin-icon" />
              <span class="coin-amount">{{ coinBalance }}</span>
            </div>
          </div>
          <div class="profile-info">
            <div class="name-section">
              <div v-if="!isEditingName" class="name-display">
                <h2>{{ currentUser }}</h2>
                <button @click="startEditingName" class="edit-name-btn" title="Edit Name">
                  ✏️
                </button>
              </div>
              <div v-else class="name-edit">
                <input 
                  v-model="editNameValue" 
                  type="text" 
                  class="name-input"
                  placeholder="Display Name"
                  @keyup.enter="saveDisplayName"
                  @keyup.esc="cancelEditingName"
                />
                <div class="name-actions">
                  <button @click="saveDisplayName" class="save-name-btn" title="Save" :disabled="!editNameValue.trim()">
                    ✅
                  </button>
                  <button @click="cancelEditingName" class="cancel-name-btn" title="Cancel">
                    ❌
                  </button>
                </div>
              </div>
            </div>
            <div class="about-section">
              <textarea
                v-if="editingAbout"
                v-model="aboutText"
                @blur="saveAbout"
                placeholder="Write something about yourself..."
                class="about-edit"
              ></textarea>
              <p v-else @click="editingAbout = true" class="about-text">
                {{ aboutText || 'Click to add bio...' }}
              </p>
            </div>
            <div class="view-social">
              <button @click="togglePostsView" class="view-social-link">
                {{ showPosts ? 'Hide My Posts' : 'View My Posts' }}
              </button>
            </div>
            
            <!-- User Posts Section -->
            <div v-if="showPosts" class="user-posts-section">
              <h3>My Posts</h3>
              <div v-if="postsLoading" class="loading-posts">
                <p>Loading your posts...</p>
              </div>
              <div v-else-if="userPosts.length === 0" class="no-posts">
                <p>You haven't posted anything yet. <router-link to="/social">Share your first post!</router-link></p>
              </div>
              <div v-else class="posts-list">
                <div 
                  v-for="post in userPosts" 
                  :key="post.id" 
                  class="post-item"
                  @click="navigateToPost(post.id)"
                >
                  <div class="post-header">
                    <h4>{{ post.title }}</h4>
                    <div class="post-actions">
                      <button @click.stop="navigateToPost(post.id)" class="redirect-post-btn" title="Go to post">
                        ➡️
                      </button>
                      <button 
                        @click.stop="deleteUserPost(post.id)" 
                        class="delete-post-btn" 
                        title="Delete post"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                  <div class="post-content">
                    <video v-if="post.type === 'video'" controls class="post-media" @click.stop>
                      <source :src="post.url" :type="post.mimeType">
                    </video>
                    <img v-else :src="post.url" :alt="post.title" class="post-media">
                    <p class="post-description">{{ post.description }}</p>
                  </div>
                  <div class="post-stats">
                    <span class="post-date">{{ formatDate(post.timestamp) }}</span>
                    <span class="post-reactions">{{ getTotalReactions(post) }} reactions</span>
                    <span class="post-comments">{{ getCommentsCount(post.id) }} comments</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- Delete Profile Section -->
            <div class="delete-profile-section">
              <button 
                @click="confirmDeleteProfile" 
                :disabled="isDeleting"
                class="delete-profile-btn"
              >
                <span v-if="!isDeleting">🗑️ Delete Profile</span>
                <span v-else>⏳ Deleting...</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="login-message">
      <p>Please log in to view and edit your profile.</p>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { profilesAPI, postsAPI, commentsAPI } from '@/services/apiService'
import { useAuthStore } from '@/store/useAuthStore'
import { storeToRefs } from 'pinia'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import tradekingCoin from '@/shared/assets/TradekingCoin.png'
import { uploadProfilePhoto } from '@/services/mediaService'

export default {
  name: 'ProfileComponent',
  setup() {
    // State
    const authStore = useAuthStore()
    const { user, isLoggedIn } = storeToRefs(authStore)
    const displayName = ref('')
    const ppUrl = ref('')
    const userBio = ref('')
    const isEditingName = ref(false)
    const editNameValue = ref('')
    const editingAbout = ref(false)
    const isDeleting = ref(false)
    const showPosts = ref(false)
    const userPosts = ref([])
    const postsLoading = ref(false)
    const postComments = ref({})
    const uploadProgress = ref(0)
    const coinBalance = ref(0)
    const router = useRouter()
    // Firebase auth listener is no longer used; we rely on JWT/localStorage instead

    // Load user profile data from backend API using backend userId
    async function loadUserProfileById(userId) {
      if (!userId) return
      
      try {
        // First, try to load from localStorage for immediate display
        const localPhoto = localStorage.getItem(`profilePhoto_${userId}`)
        if (localPhoto) {
          ppUrl.value = localPhoto
        }
        
        // Load profile from backend API
        const profileData = await profilesAPI.getProfileByUserId(userId)
        
        if (profileData) {
          ppUrl.value = profileData.photoUrl || localPhoto || ''
          displayName.value = profileData.displayName || ''
          userBio.value = profileData.bio || ''
          
          // Update localStorage with latest API data
          if (profileData.photoUrl) {
            localStorage.setItem(`profilePhoto_${userId}`, profileData.photoUrl)
          }
        } else {
          // Profile doesn't exist yet - will be created on first edit
          ppUrl.value = localPhoto || ''
          displayName.value = ''
          userBio.value = ''
        }
      } catch (error) {
        console.error('Error loading profile:', error)
        // Use localStorage data as fallback
        const localPhoto = localStorage.getItem(`profilePhoto_${userId}`)
        ppUrl.value = localPhoto || ''
        displayName.value = ''
        userBio.value = ''
      }
    }

    onMounted(async () => {
      // Initialize user from store
      if (user.value?.uid) {
        await loadUserProfileById(user.value.uid)
        await loadCoinBalance(user.value.uid)
      }
      cleanupOldProfilePhotos()
    })

    // Watch for user changes (e.g. login/logout)
    watch(() => user.value, async (newUser) => {
      if (newUser?.uid) {
        await loadUserProfileById(newUser.uid)
        await loadCoinBalance(newUser.uid)
      } else {
        // Clear local state if logged out
        ppUrl.value = ''
        displayName.value = ''
        userBio.value = ''
        userPosts.value = []
        coinBalance.value = 0
      }
    })

    const currentUser = computed(() => displayName.value || user.value?.displayName || user.value?.email || '')
    const profilePhoto = computed(() => ppUrl.value || defaultAvatar)
    const aboutText = computed({
      get: () => userBio.value,
      set: (value) => userBio.value = value
    })

    async function handleProfilePhotoSelect(event) {
      const file = event.target.files[0]
      if (!file || !user.value) return

      // Check file size (optional - warn if too large)
      const maxSize = 5 * 1024 * 1024 // 5MB
      if (file.size > maxSize) {
        alert('Image is too large. Please choose a smaller image (max 5MB).')
        return
      }

      try {
        // Local preview for better UX
        const reader = new FileReader()
        reader.onload = (e) => {
          const previewUrl = e.target.result
          ppUrl.value = previewUrl
        }
        reader.readAsDataURL(file)

        const newPhotoURL = await uploadProfilePhoto({
          file,
          userId: user.value.uid,
          onProgress: (p) => {
            uploadProgress.value = p
          }
        })

        ppUrl.value = newPhotoURL
        localStorage.setItem(`profilePhoto_${user.value.uid}`, newPhotoURL)

        console.log('Profile photo updated successfully')
      } catch (error) {
        console.error('Error updating profile photo:', error)
        const fallbackPhoto = localStorage.getItem(`profilePhoto_${user.value.uid}`) || ''
        ppUrl.value = fallbackPhoto || defaultAvatar
      }
    }

    async function saveAbout() {
      if (!user.value) return
      
      editingAbout.value = false
      
      try {
        // Update profile via backend API
        await profilesAPI.updateProfile(user.value.uid, {
          photoUrl: ppUrl.value,
          displayName: displayName.value,
          bio: userBio.value
        })
        console.log('Bio updated successfully')
        
      } catch (error) {
        console.error('Error updating bio:', error)
      }
    }

    // Utility function to clean up old localStorage entries
    function cleanupOldProfilePhotos() {
      try {
        const currentUserId = user.value?.uid
        if (!currentUserId) return

        // Keep only current user's photo and remove old ones
        Object.keys(localStorage).forEach(key => {
          if (key.startsWith('profilePhoto_') && !key.endsWith(currentUserId)) {
            localStorage.removeItem(key)
          }
        })
      } catch (error) {
        console.log('Error cleaning up old photos:', error)
      }
    }

    // Posts Management Functions
    async function loadCoinBalance(userId) {
      if (!userId) return
      try {
        const total = await postsAPI.getTotalLikesReceivedByUser(userId)
        coinBalance.value = total || 0
      } catch (error) {
        console.error('Error loading coin balance:', error)
        coinBalance.value = 0
      }
    }

    function togglePostsView() {
      showPosts.value = !showPosts.value
      if (showPosts.value && user.value) {
        loadUserPosts()
        loadUserComments()
      }
    }

    function loadUserPosts() {
      if (!user.value) return
      
      postsLoading.value = true
      
      // Load posts from backend API
      postsAPI.getPostsByUserId(user.value.uid)
        .then((posts) => {
          userPosts.value = posts || []
          postsLoading.value = false
        })
        .catch((error) => {
          console.error('Error loading user posts:', error)
          userPosts.value = []
          postsLoading.value = false
        })
    }

    async function loadUserComments() {
      if (!user.value) return
      
      try {
        // Load all comments for the user
        const comments = await commentsAPI.getCommentsByUserId(user.value.uid)
        
        // Organize comments by post ID
        const commentsByPost = {}
        if (Array.isArray(comments)) {
          comments.forEach((comment) => {
            const postId = comment.postId
            if (!commentsByPost[postId]) {
              commentsByPost[postId] = []
            }
            commentsByPost[postId].push(comment)
          })
        }
        postComments.value = commentsByPost
      } catch (error) {
        console.error('Error loading user comments:', error)
        // Set empty comments to prevent further errors
        postComments.value = {}
      }
    }

    async function deleteUserPost(postId) {
      if (!confirm('Are you sure you want to delete this post?')) return
      
      try {
        // Delete post via API (this will also delete associated comments)
        await postsAPI.deletePost(postId)
        
        console.log('Post deleted successfully')
        // Reload posts after deletion
        loadUserPosts()
      } catch (error) {
        console.error('Error deleting post:', error)
        alert('Failed to delete post. Please try again.')
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

    function formatDate(timestamp) {
      if (!timestamp) return 'Unknown date'
      
      // Handle both ISO string and Date object
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
    }

    function navigateToPost(postId) {
      router.push(`/posts/${postId}`)
    }

    // Delete Profile Functions
    function confirmDeleteProfile() {
      const confirmed = confirm(
        "Are you sure about deleting your profile?\n\nYour data will be deleted UNRECOVERABLY!\n\nThis action cannot be undone."
      )
      
      if (confirmed) {
        const password = prompt("Please enter your password to confirm deletion:")
        if (password) {
           // Ideally we should verify password with backend here, 
           // but for now we proceed with deletion if confirmed.
           // A real implementation would have a verify-password endpoint.
           deleteProfile()
        }
      }
    }

    async function deleteProfile() {
      if (!user.value || isDeleting.value) return
      
      isDeleting.value = true
      const userId = user.value.uid
      
      try {
        console.log('Starting profile deletion process...')
        
        // Delete account and all related data via backend API (atomic transaction)
        await profilesAPI.deleteAccountCascade(userId)
        
        // Clean up localStorage
        cleanupUserLocalStorage(userId)
        
        alert('Profile deleted successfully. You will be redirected to the homepage.')
        
        // Logout and redirect
        await authStore.logout()
        window.location.href = '/'
        
      } catch (error) {
        console.error('Error deleting profile:', error)

        let message = 'Failed to delete profile. Please try again or contact support.'
        alert(message)
      } finally {
        isDeleting.value = false
      }
    }

    // Note: Individual delete functions are no longer needed as the backend API handles
    // cascading deletes atomically.

    function cleanupUserLocalStorage(userId) {
      try {
        // Remove user-specific localStorage items
        localStorage.removeItem(`profilePhoto_${userId}`)
        
        // Remove any other user-specific data
        Object.keys(localStorage).forEach(key => {
          if (key.includes(userId)) {
            localStorage.removeItem(key)
          }
        })
        
        console.log('localStorage cleaned up')
      } catch (error) {
        console.error('Error cleaning up localStorage:', error)
      }
    }

    // Name Editing Functions
    function startEditingName() {
      editNameValue.value = displayName.value
      isEditingName.value = true
    }

    function cancelEditingName() {
      isEditingName.value = false
      editNameValue.value = ''
    }

    async function saveDisplayName() {
      if (!user.value) return
      
      const newName = editNameValue.value.trim()
      if (!newName) return
      
      try {
        // Update profile via backend API
        // Note: We're sending all fields to ensure nothing gets overwritten with empty values
        await profilesAPI.updateProfile(user.value.uid, {
          photoUrl: ppUrl.value,
          displayName: newName,
          bio: userBio.value
        })
        
        // Update local state
        displayName.value = newName
        
        // Update localStorage
        localStorage.setItem('displayName', newName)
        
        console.log('Display name updated successfully')
        isEditingName.value = false
        
      } catch (error) {
        console.error('Error updating display name:', error)
        alert('Failed to update display name. Please try again.')
      }
    }

    return {
      // State
      ppUrl,
      userBio,
      displayName,
      editingAbout,
      defaultAvatar,
      tradekingCoin,
      isDeleting,
      showPosts,
      userPosts,
      postsLoading,
      coinBalance,
      
      // Computed
      isLoggedIn,
      currentUser,
      profilePhoto,
      aboutText,
      isEditingName,
      editNameValue,

      // Methods
      handleProfilePhotoSelect,
      saveAbout,
      startEditingName,
      cancelEditingName,
      saveDisplayName,
      confirmDeleteProfile,
      togglePostsView,
      deleteUserPost,
      getTotalReactions,
      getCommentsCount,
      formatDate,
      navigateToPost
    }
  }
};
</script>

<style scoped>
.profile-box {
  max-width: 800px;
  margin: 2rem auto;
  padding: 2rem;
  background: #f8f9fa;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.profile-content {
  background: white;
  border-radius: 8px;
  overflow: hidden;
}

.profile-section {
  padding: 24px;
  background: white;
  border-radius: 12px;
}

.profile-header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-photo-container {
  position: relative;
  width: 150px;
  height: 150px;
  flex-shrink: 0;
}

.profile-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  border: 3px solid #dd7724ff;
}

.profile-photo-upload {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  background: #dd7724ff;
  color: white;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.9em;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
}

.profile-photo-container:hover .profile-photo-upload {
  opacity: 1;
}

.hidden {
  display: none;
}

.profile-info {
  flex: 1;
}

.coin-balance-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #ffd700 0%, #ffb347 100%);
  border-radius: 25px;
  box-shadow: 0 3px 10px rgba(255, 215, 0, 0.3);
}

.coin-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.coin-amount {
  font-size: 1.3em;
  font-weight: bold;
  color: #5a4a00;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.5);
}

.about-section {
  margin: 15px 0;
}

.about-text {
  align-items: center;
  color: #666;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.about-text:hover {
  background-color: #f8f9fa;
}

.about-edit {
  width: 100%;
  min-height: 80px;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
}

.name-section {
  margin-bottom: 15px;
}

.name-display {
  align-items: center;
  gap: 10px;
}

.name-display h2 {
  margin: 0;
}

.edit-name-btn {
  background: none;
  border: 1px solid #ccc;
  cursor: pointer;
  font-size: 1.2em;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.edit-name-btn:hover {
  background-color: #f0f0f0;
}

.name-edit {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-input {
  font-size: 1.5em;
  font-weight: bold;
  padding: 5px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 100%;
  max-width: 300px;
}

.name-actions {
  display: flex;
  gap: 5px;
}

.save-name-btn,
.cancel-name-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2em;
  padding: 5px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.save-name-btn:hover {
  background-color: #d4edda;
}

.cancel-name-btn:hover {
  background-color: #f8d7da;
}

.save-name-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-message {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.view-social {
  text-align: center;
  margin-top: 20px;
}

.view-social-link {
  display: inline-block;
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 25px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.view-social-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
  color: white;
  text-decoration: none;
}

/* User Posts Section */
.user-posts-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

.user-posts-section h3 {
  color: #333;
  margin-bottom: 20px;
  text-align: center;
}

.loading-posts,
.no-posts {
  text-align: center;
  padding: 20px;
  color: #666;
}

.no-posts a {
  color: #667eea;
  text-decoration: none;
}

.no-posts a:hover {
  text-decoration: underline;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  border: 1px solid #e9ecef;
  cursor: pointer;
  transition: all 0.2s ease;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  border-color: #667eea;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.post-header h4 {
  margin: 0;
  color: #333;
  font-size: 1.1em;
  flex-grow: 1;
}

.post-actions {
  display: flex;
  gap: 10px;
}

.delete-post-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.2s;
}

.delete-post-btn:hover {
  background: #c82333;
}

.redirect-post-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.2s;
}

.redirect-post-btn:hover {
  background: #218838;
}

.post-content {
  margin-bottom: 15px;
}

.post-media {
  max-width: 50%;
  max-height: 25%;
  border-radius: 4px;
  margin-bottom: 10px;
  filter: blur(5px);
}

.post-description {
  color: #555;
  margin: 0;
  line-height: 1.4;
}

.post-stats {
  display: flex;
  gap: 15px;
  font-size: 0.9em;
  color: #666;
  padding-top: 10px;
  border-top: 1px solid #dee2e6;
}

.post-date,
.post-reactions,
.post-comments {
  display: flex;
  align-items: center;
}

/* Delete Profile Section */
.delete-profile-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
  text-align: center;
}

.delete-profile-btn {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 0.9em;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 3px 10px rgba(220, 53, 69, 0.3);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.delete-profile-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 5px 15px rgba(220, 53, 69, 0.4);
  background: linear-gradient(135deg, #c82333 0%, #a71e2a 100%);
}

.delete-profile-btn:active {
  transform: translateY(0);
}

.delete-profile-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>
