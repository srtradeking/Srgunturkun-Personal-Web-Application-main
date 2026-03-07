<template>
  <div class="social-container">

    <!-- Main Content Grid -->
    <div class="main-content">
      <!-- Upload Section (Left Column) -->
      <UploadPosts
        :gameCategories="gameCategories"
        :gamesLoading="gamesLoading"
        :isLoggedIn="isLoggedIn"
        @reloadGames="loadGameCategories"
        @postUploaded="handlePostUploaded"
      />

      <!-- Posts Viewing Section -->
      <div class="posts-section">
        <div class="posts-header">
          <h3>🌟 Community Posts</h3>
          <div v-if="!isLoggedIn && showLoginPrompt" class="guest-prompt">
            <p>👋 Enjoying the posts? <router-link to="/account">Join us</router-link> to interact and share!</p>
          </div>
        </div>

        <!-- Filter Controls -->
        <div class="filter-controls">
          <ChooseGameView 
            @gameSelected="handleGameSelected"
            @videoTypeSelected="handleVideoTypeSelected"
            @filtersCleared="handleFiltersCleared"
          />
        </div>

        <!-- Game View with Posts -->
        <GameView
          :selectedGame="selectedGame || 'All Games'"
          :selectedVideoType="selectedVideoType || 'All Types'"
          :posts="mediaItems"
          :comments="postComments"
          :userProfiles="userProfiles"
          :isLoggedIn="isLoggedIn"
          :postsLoading="postsLoading"
          :hasPostsAccess="hasPostsAccess"
          :error="error"
          :hiddenPosts="hiddenPosts"
          @addReaction="addReaction"
          @addComment="handleAddComment"
          @addReply="handleAddReply"
          @loadCommentReplies="handleLoadCommentReplies"
          @hidePost="hidePost"
          @navigateToProfile="navigateToUserProfile"
          @promptLogin="promptLogin"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import UploadPosts from '@/shared/components/UploadPosts.vue'
import ChooseGameView from '@/shared/components/ChooseGameView.vue'
import GameView from '@/shared/components/GameView.vue'
import { useGameStore, usePostStore, useCommentStore, useProfileStore } from '@/store/useSocialStore'
import { useAuthStore } from '@/store/useAuthStore'
import { gamesAPI, postsAPI, commentsAPI } from '@/services/apiService'
import { API_CONFIG } from '@/services/config'
import { storeToRefs } from 'pinia'

export default {
  name: 'AppSocial',
  components: {
    UploadPosts,
    ChooseGameView,
    GameView
  },
  setup() {
    // Router setup
    const router = useRouter()
    
    // Get API Base URL
    const API_BASE_URL = API_CONFIG.getBaseUrl()
    
    // Pinia stores
    const gameStore = useGameStore()
    const postStore = usePostStore()
    const commentStore = useCommentStore()
    const profileStore = useProfileStore()
    const authStore = useAuthStore()
    
    // Store refs
    const { games, gameCategories, loading: gamesLoading } = storeToRefs(gameStore)
    const { profiles: userProfiles } = storeToRefs(profileStore)
    const { user, isLoggedIn } = storeToRefs(authStore)
    
    // Local state (not managed by stores)
    const mediaItems = ref([]) // Derived from posts (real-time listener)
    const postComments = ref({}) // Store comments for each post (real-time listener)
    const postsLoading = ref(true)
    const unsubscribe = ref(null) // Store Firestore listener
    const commentsUnsubscribe = ref(null) // Store comments listener
    const error = ref('')
    const hiddenPosts = ref(new Set())
    const canAccessPosts = ref(false)
    const hasPostsAccess = computed(() => canAccessPosts.value && !postsLoading.value)
    
    // Game filtering system
    const selectedGame = ref('')
    const selectedVideoType = ref('')
    
    // View control
    const showLoginPrompt = ref(false)

    // Event Handlers for child components
    const handleGameSelected = async (game) => {
      selectedGame.value = game
      selectedVideoType.value = '' // Reset video type when game changes
      // Load posts for the selected game
      if (game) {
        await loadPostsByGameAndType(game, '')
      }
    }

    const handleVideoTypeSelected = async (type) => {
      selectedVideoType.value = type
      // Load posts with the selected type filter
      if (selectedGame.value) {
        await loadPostsByGameAndType(selectedGame.value, type)
      }
    }

    const handleFiltersCleared = async () => {
      selectedGame.value = ''
      selectedVideoType.value = ''
      // Load all posts when filters are cleared
      await loadAllPosts()
    }

    const handlePostUploaded = async () => {
      // After backend upload, refresh posts from backend so GameView reflects the new post
      try {
        const currentUser = authStore.currentUser
        await loadPostsForUser(currentUser)
      } catch (err) {
        console.error('Error refreshing posts after upload:', err)
      }
    }

    const handleAddComment = (postId, commentText) => {
      addComment(postId, commentText)
    }

    const handleAddReply = (postId, parentCommentId, replyText) => {
      addReply(postId, parentCommentId, replyText)
    }

    const handleLoadCommentReplies = (commentId) => {
      // Load replies for a specific comment
      loadCommentReplies(commentId)
    }

    // Comments function - used by GameView component
    async function addComment(mediaId, commentText) {
      if (!authStore.currentUser) {
        error.value = 'Please log in to add comments'
        return
      }
      
      // Check if post exists
      const post = mediaItems.value.find(item => item.id === mediaId)
      if (!post) {
        error.value = 'Post not found'
        return
      }
      
      try {
        error.value = '' // Clear any previous errors
        
        // Get current user's profile picture
        let currentUserPpUrl = userProfiles.value[authStore.currentUser.uid]?.ppUrl || ''
        
        // If we don't have it cached, try to fetch it
        if (!currentUserPpUrl) {
          try {
            const profile = await profileStore.fetchProfileById(authStore.currentUser.uid)
            currentUserPpUrl = profile?.ppUrl || ''
          } catch (err) {
            console.error('Error fetching current user profile:', err)
          }
        }
        
        const newCommentData = {
          postId: mediaId, // Reference to the post
          userId: authStore.currentUser.uid, 
          userProfileId: authStore.currentUser.uid, // Ensure backend links to UserProfile
          content: commentText,
          ppUrl: currentUserPpUrl, // Include current user's profile picture
          userDisplayName: authStore.currentUser.displayName || authStore.currentUser.email || 'Anonymous'
        }
        
        // Add comment using store
        const newComment = await commentStore.createComment(newCommentData)
        
        // Update local state immediately
        if (newComment) {
          const transformedComment = transformComments([newComment])[0]
          if (!postComments.value[mediaId]) {
            postComments.value[mediaId] = []
          }
          postComments.value[mediaId].push(transformedComment)
        }
      } catch (err) {
        error.value = 'Failed to add comment. Please check your permissions.'
      }
    }

    async function addReply(postId, parentCommentId, replyText) {
      if (!authStore.currentUser) {
        error.value = 'Please log in to add replies'
        return
      }
      
      // Check if post exists
      const post = mediaItems.value.find(item => item.id === postId)
      if (!post) {
        error.value = 'Post not found'
        return
      }
      
      try {
        error.value = '' // Clear any previous errors
        
        // Get current user's profile picture
        let currentUserPpUrl = userProfiles.value[authStore.currentUser.uid]?.ppUrl || ''
        
        // If we don't have it cached, try to fetch it
        if (!currentUserPpUrl) {
          try {
            const profile = await profileStore.fetchProfileById(authStore.currentUser.uid)
            currentUserPpUrl = profile?.ppUrl || ''
          } catch (err) {
            console.error('Error fetching current user profile:', err)
          }
        }
        
        const newReplyData = {
          postId: postId,
          userProfileId: authStore.currentUser.uid, // Ensure backend links to UserProfile
          userId: authStore.currentUser.uid,
          content: replyText,
          parentCommentId: parentCommentId, // Reference to parent comment
          ppUrl: currentUserPpUrl, // Include current user's profile picture
          userDisplayName: authStore.currentUser.displayName || authStore.currentUser.email || 'Anonymous'
        }
        
        // Add reply using store
        const newReply = await commentStore.createComment(newReplyData)
        
        // Update local state immediately
        if (newReply) {
          const transformedReply = transformComments([newReply])[0]
          if (!postComments.value[postId]) {
            postComments.value[postId] = []
          }
          postComments.value[postId].push(transformedReply)
        }
      } catch (err) {
        error.value = 'Failed to add reply. Please check your permissions.'
      }
    }

    async function loadCommentReplies(commentId) {
      try {
        // Fetch replies for this comment
        await commentStore.fetchCommentReplies(commentId)
      } catch (err) {
        error.value = 'Failed to load replies'
      }
    }


    async function addReaction(mediaId, emoji) {
      if (!authStore.currentUser) {
        error.value = 'Please log in to react to posts'
        return
      }
      
      const postIndex = mediaItems.value.findIndex(item => item.id === mediaId)
      const post = mediaItems.value[postIndex]
      
      if (!post) {
        error.value = 'Post not found'
        return
      }
      
      const userId = authStore.currentUser.uid
      
      try {
        error.value = '' // Clear any previous errors
        
        // Use store action to add reaction
        const updatedPost = await postStore.addReactionToPost(mediaId, emoji, userId)
        
        // Update local state immediately
        if (updatedPost && updatedPost.likesCount !== undefined) {
          mediaItems.value[postIndex].likesCount = updatedPost.likesCount
        }
      } catch (err) {
        error.value = 'Failed to update reaction. Please check your permissions.'
      }
    }

    async function hidePost(postId) {
      hiddenPosts.value.add(postId)
      try {
        await postStore.hidePost(postId)
      } catch (err) {
        hiddenPosts.value.delete(postId)
        error.value = 'Failed to hide post'
      }
    }

    async function deletePost(postId) {
      if (!confirm('Are you sure you want to delete this post?')) return
      
      try {
        await postStore.removePost(postId)
        // Refresh posts locally
        mediaItems.value = mediaItems.value.filter(p => p.id !== postId)
      } catch (err) {
        console.error('Error deleting post:', err)
        error.value = 'Failed to delete post'
      }
    }

    // Navigate to user profile
    function navigateToUserProfile(userId) {
      if (!userId) {
        return
      }
      
      // Check if it's the current user - go to their own profile
      if (authStore.currentUser && String(userId) === String(authStore.currentUser.uid)) {
        router.push('/profile')
      } else {
        // Navigate to public profile view
        router.push(`/user/${userId}`)
      }
    }

    // Load user profile data from denormalized post/comment data
    function createProfileFromData(displayName, ppUrl ) {
      // Handle base64 photo URLs from backend
      let profilePpUrl = ppUrl || defaultAvatar
      if (profilePpUrl && profilePpUrl !== defaultAvatar && !profilePpUrl.startsWith('data:') && !profilePpUrl.startsWith('http')) {
        // If it's a base64 string (not already a data URL), wrap it
        profilePpUrl = `data:image/jpeg;base64,${profilePpUrl}`
      }
      
      return {
        ppUrl: profilePpUrl,
        displayName: displayName || 'Anonymous User',
        userBio: ''
      }
    }

    // Load profiles for all visible users from denormalized data in posts and comments
    // This avoids unnecessary API calls and 403 errors since posts contain display names and profile pictures
    async function loadVisibleUserProfiles() {
      const userIds = new Set()
      const usersNeedingApiLookup = new Set() // Users without ppUrl that need API fetch
      
      // Collect user profiles from posts (using denormalized data)
      mediaItems.value.forEach(post => {
        if (post.userId && !userProfiles.value[post.userId]) {
          // Create profile from post data instead of fetching from API
          userProfiles.value[post.userId] = createProfileFromData(
            post.userDisplayName,
            post.ppUrl,
            post.userId
          )
          // If post doesn't have ppUrl, mark for API lookup
          if (!post.ppUrl) {
            usersNeedingApiLookup.add(post.userId)
          }
        }
        if (post.userId) userIds.add(post.userId)
      })
      
      // Collect user profiles from comments (using denormalized data)
      Object.values(postComments.value).forEach(comments => {
        comments.forEach(comment => {
          if (comment.userId && !userProfiles.value[comment.userId]) {
            // Create profile from comment data instead of fetching from API
            userProfiles.value[comment.userId] = createProfileFromData(
              comment.userDisplayName || comment.username,
              comment.ppUrl,
              comment.userId
            )
            // If comment doesn't have ppUrl, mark for API lookup
            if (!comment.ppUrl) {
              usersNeedingApiLookup.add(comment.userId)
            }
          }
          if (comment.userId) userIds.add(comment.userId)
        })
      })
      
      console.log('Profiles loaded from denormalized data for', userIds.size, 'users')
      
      // Fetch missing profiles from API for users without ppUrl
      if (usersNeedingApiLookup.size > 0) {
        console.log('Fetching missing profiles for', usersNeedingApiLookup.size, 'users')
        const fetchPromises = Array.from(usersNeedingApiLookup).map(userId =>
          profileStore.fetchProfileById(userId)
            .then(profile => {
              if (profile && profile.ppUrl) {
                userProfiles.value[userId] = {
                  ppUrl: profile.ppUrl,
                  displayName: profile.displayName || 'Anonymous User',
                  userBio: profile.userBio || ''
                }
                console.log('Fetched missing profile for user:', userId)
              }
            })
            .catch(err => {
              console.error(`Failed to fetch profile for user ${userId}:`, err)
              // Keep the default profile created from denormalized data
            })
        )
        
        // Don't wait for these - let them load in background
        Promise.all(fetchPromises).catch(err => {
          console.error('Error fetching missing profiles:', err)
        })
      }
    }

    // Helper to transform comments to ensure frontend compatibility
    const transformComments = (comments) => {
      if (!comments) return []
      return comments.map(c => {
        // console.log('Raw comment from backend:', c)
        return {
          ...c,
          // Normalize parentCommentId (handle snake_case from backend if present)
          parentCommentId: c.parentCommentId || c.parent_comment_id || null,
          // Frontend expects 'username' but backend DTO sends 'userDisplayName'
          username: c.userDisplayName || c.username || 'Anonymous',
          // Frontend expects 'content' (we fixed GameView to use content)
          // Ensure it's populated
          content: c.content || c.text || '',
          // Ensure ppUrl is set
          ppUrl: c.ppUrl || defaultAvatar
        }
      })
    }

    // Load comments using the comment store and backend API
    async function loadComments() {
      try {
        // For now, we'll load comments for each post individually
        // This can be optimized later with a bulk endpoint
        const commentPromises = mediaItems.value.map(async (post) => {
          try {
            const comments = await commentsAPI.getCommentsByPostId(post.id)
            if (comments && Array.isArray(comments)) {
              postComments.value[post.id] = transformComments(comments)
            } else {
              postComments.value[post.id] = []
            }
          } catch (error) {
            console.error(`Error loading comments for post ${post.id}:`, error)
            postComments.value[post.id] = []
          }
        })
        
        await Promise.all(commentPromises)
        
        // Load user profiles for comments
        await loadVisibleUserProfiles()
      } catch (error) {
        console.error('Error loading comments:', error)
      }
    }

    // Load game categories from PostgreSQL backend
    async function loadGameCategories() {
      try {
        error.value = '' // Clear any previous errors
        console.log('Loading games from backend...')
        
        // Test direct API call first
        try {
          const directApiResult = await gamesAPI.getActiveGames()
          console.log('Direct API call result:', directApiResult)
        } catch (apiError) {
          console.error('Direct API call failed:', apiError)
        }
        
        // Fetch games using store (which should call the backend API)
        await gameStore.fetchGames()
        
        console.log('Games loaded from store:', games.value)
        console.log('Game categories processed:', gameCategories.value)
        
        // Store full game objects for debugging
        window.gameObjectsCache = games.value
      } catch (err) {
        console.error('Error loading games:', err)
        error.value = 'Failed to load games from database. Please check your connection and try again.'
      }
    }

    // Prompt login function
    const promptLogin = () => {
      showLoginPrompt.value = true
      router.push('/login')
    }

    // Load posts for everyone (logged in or not)
    const loadPostsForUser = async (currentUser) => {
        postsLoading.value = true
        canAccessPosts.value = false
        
        try {
            // Load all posts from PostgreSQL API
            const posts = await postsAPI.getAllPosts()
            
            // Transform backend posts to frontend format
            let items = transformPostsToMediaItems(posts)
            
            // Client-side filter to ensure only APPROVED posts are shown
            // (Safeguard in case backend filtering is bypassed or backend is outdated but DTO has status)
            items = items.filter(p => !p.status || p.status === 'APPROVED')
            
            mediaItems.value = items
            
            console.log('Posts loaded from API:', mediaItems.value)
            canAccessPosts.value = true
            postsLoading.value = false
            error.value = '' // Clear any previous errors
            
            // Load comments for all posts
            if (currentUser || mediaItems.value.length > 0) {
              await loadComments()
            }
            
            // Load user profiles for posts
            if (currentUser || mediaItems.value.length > 0) {
              await loadVisibleUserProfiles()
            }
          } catch (err) {
            console.error('Error loading posts via backend:', err)
            canAccessPosts.value = false
            postsLoading.value = false
            error.value = 'Failed to load posts from backend. Please check your connection.'
          }
    }

    // Load all published posts (no game filter)
    const loadAllPosts = async () => {
      postsLoading.value = true
      try {
        const posts = await postsAPI.getAllPosts()
        mediaItems.value = transformPostsToMediaItems(posts)
        await loadComments()
        await loadVisibleUserProfiles()
        error.value = ''
      } catch (err) {
        console.error('Error loading all posts:', err)
        error.value = 'Failed to load posts from backend. Please check your connection.'
      } finally {
        postsLoading.value = false
      }
    }

    // Load posts filtered by game and optionally by type (image/video)
    const loadPostsByGameAndType = async (game, type) => {
      postsLoading.value = true
      try {
        // We load all posts and let GameView handle the filtering client-side
        // This ensures consistency with the transform logic (e.g. mapping null game to 'Other')
        // and avoids double-filtering or mismatches.
        const allPosts = await postsAPI.getAllPosts()
        
        mediaItems.value = transformPostsToMediaItems(allPosts)
        await loadComments()
        await loadVisibleUserProfiles()
        error.value = ''
      } catch (err) {
        console.error('Error loading posts by game and type:', err)
        error.value = 'Failed to load posts from backend. Please check your connection.'
      } finally {
        postsLoading.value = false
      }
    }

    // Transform backend posts to frontend format
    const transformPostsToMediaItems = (posts) => {
      return posts.map((post, index) => {
        let rawUserId = post.userId
        
        // If userId is not directly available, try to extract it from other fields
        if (rawUserId === undefined || rawUserId === null) {
            const directKeys = ['userId', 'uid', 'user_id', 'id']
            for (const key of directKeys) {
                if (post[key] !== undefined && post[key] !== null) {
                    rawUserId = post[key]
                    break
                }
            }
        }
        
        // If rawUserId is found but is an object (e.g. a User object instead of ID), try to extract the ID from it
        if (rawUserId && typeof rawUserId === 'object') {
            rawUserId = rawUserId.id || rawUserId.uid || rawUserId.userId || rawUserId.user_id || null
        }
        
        // Check for nested user/author/creator/account/publisher/owner/member object (Spring Boot / JPA common pattern)
        // Also handle cases where these fields might be the ID directly (primitive)
        if (rawUserId === undefined || rawUserId === null) {
            const nestedKeys = ['user', 'author', 'creator', 'account', 'publisher', 'owner', 'member']
            
            for (const key of nestedKeys) {
                if (post[key] !== undefined && post[key] !== null) {
                    if (typeof post[key] === 'object') {
                        // Check common ID fields in the nested object
                        if (post[key].id !== undefined && post[key].id !== null) {
                            rawUserId = post[key].id
                        } else if (post[key].uid !== undefined && post[key].uid !== null) {
                            rawUserId = post[key].uid
                        } else if (post[key].userId !== undefined && post[key].userId !== null) {
                            rawUserId = post[key].userId
                        }
                    } else {
                        // Primitive value (assuming it's the ID)
                        rawUserId = post[key]
                    }
                    
                    if (rawUserId !== undefined && rawUserId !== null) break
                }
            }
        }
        
        if (rawUserId === undefined || rawUserId === null) {
            if (process.env.NODE_ENV === 'development') {
                console.warn('Could not extract userId for post:', post.id, post.title)
            }
            // Ensure userId is not undefined to avoid issues downstream
            rawUserId = '' 
        }

        if (index === 0) {
             console.log('Extracted UserID for first post:', rawUserId)
        }
        
        // Safely determine if this is an image or video post
        const isImage = post.imageUrl && !post.videoUrl
        const mimeType = isImage ? 'image/jpeg' : 'video/mp4'
        
        // Determine media URL - use imageUrl for images, videoUrl for videos
        let mediaUrl = ''
        if (post.imageUrl) {
          mediaUrl = post.imageUrl
        } else if (post.videoUrl) {
          mediaUrl = post.videoUrl
        }

        // Process URL
        if (mediaUrl) {
          if (mediaUrl.startsWith('http') || mediaUrl.startsWith('data:')) {
             // Already absolute or data URL
          } else if (mediaUrl.startsWith('/') || mediaUrl.startsWith('media/')) {
             // Relative path - prepend API Base URL
             // If it starts with media/, add slash
             const prefix = mediaUrl.startsWith('/') ? '' : '/'
             
             // Check if API_BASE_URL already has /api and the path has /api
             // The backend returns /api/storage/... or media/...
             // API_BASE_URL usually ends with /api.
             // If mediaUrl starts with /api/, we might be duplicating if we just join.
             
             // Simplest safe join:
             // If mediaUrl starts with /api, and API_BASE_URL ends with /api, strip one.
             let baseUrl = API_BASE_URL
             if (baseUrl.endsWith('/api') && mediaUrl.startsWith('/api')) {
                baseUrl = baseUrl.substring(0, baseUrl.length - 4)
             }
             
             mediaUrl = `${baseUrl}${prefix}${mediaUrl}`
          } else {
             // Fallback for base64 without prefix (legacy)
             const typePrefix = isImage ? 'data:image/jpeg;base64,' : 'data:video/mp4;base64,'
             mediaUrl = `${typePrefix}${mediaUrl}`
          }
        }
        
        return {
          // Required fields from PostgreSQL Post table
          id: post.id,
          title: post.title || '',
          content: post.content || post.description || '',
          description: post.content || post.description || '',
          game: post.game || 'Other', // Default to 'Other' if game not specified
          
          // Media fields (may have imageUrl or videoUrl or both)
          imageUrl: post.imageUrl || null,
          videoUrl: post.videoUrl || null,
          url: mediaUrl,
          type: isImage ? 'image' : 'video',
          mimeType: mimeType,
          duration: post.duration || 0,
          
          // User info (denormalized for display)
          userId: rawUserId,
          username: post.userDisplayName || 'Anonymous',
          userDisplayName: post.userDisplayName || 'Anonymous',
          ppUrl: post.ppUrl || defaultAvatar,

          // Engagement metrics
          likesCount: post.likesCount || 0,
          
          // Timestamps
          timestamp: post.createdAt ? new Date(post.createdAt) : new Date(),
          createdAt: post.createdAt,
          updatedAt: post.updatedAt,
          deletedAt: post.deletedAt || null,
          
          // Status fields
          isPublished: post.isPublished !== false,
          isActive: post.isPublished !== false,
          isDeleted: post.isDeleted === true,
          isHidden: post.isHidden === true,
          status: post.status, // Map status field
          isPublic: true
        }
      })
    }

    // Initial data load (no Firebase auth listener)
    onMounted(async () => {
      // User state is handled by authStore
      try {
        // Load game categories from backend
        await loadGameCategories()
        // Load posts for guest or logged-in users via backend API
        await loadPostsForUser(user.value)
      } catch (err) {
        console.error('Error during initial social page load:', err)
      }
    })

    // Cleanup listener on component unmount
    onUnmounted(() => {
      if (unsubscribe.value) {
        unsubscribe.value()
      }
      if (commentsUnsubscribe.value) {
        commentsUnsubscribe.value()
      }
    })

    return {
      // State
      gameCategories,
      gamesLoading,
      isLoggedIn,
      selectedGame,
      selectedVideoType,
      mediaItems,
      postComments,
      userProfiles,
      postsLoading,
      hasPostsAccess,
      error,
      hiddenPosts,
      showLoginPrompt,
      
      // Event handlers
      handleGameSelected,
      handleVideoTypeSelected,
      handleFiltersCleared,
      handlePostUploaded,
      handleAddComment,
      handleAddReply,
      handleLoadCommentReplies,
      
      // Functions
      loadGameCategories,
      addReaction,
      deletePost,
      hidePost,
      navigateToUserProfile,
      promptLogin
    }
  }
}
</script>

<style scoped>
/* Social Container */
.social-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

.social-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(120, 119, 198, 0.2) 0%, transparent 50%);
  pointer-events: none;
  z-index: 1;
}

/* Main Content Layout */
.main-content {
  display: flex;
  flex-direction: column;
  grid-template-columns: 1fr 1.5fr;
  gap: 2rem;
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  position: relative;
  z-index: 2;
  min-height: 100vh;
}

/* Posts Section */
.posts-section {
  background: linear-gradient(45deg, #12f34a 100% , rgb(172, 221, 36) 0%);
  border-radius: 20px;
  padding: 1.5rem;
  box-shadow: 
    0 20px 40px rgba(0, 0, 0, 0.1),
    0 8px 16px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  min-height: calc(100vh - 4rem);
}

.posts-header {
  margin-bottom: 1.5rem;
}

.posts-header h3 {
  font-size: 1.8rem;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 1rem 0;
  font-weight: 700;
}

.guest-prompt {
  background: linear-gradient(135deg, #ff7eb3, #ff758c);
  color: white;
  padding: 1rem;
  border-radius: 15px;
  text-align: center;
  margin-bottom: 1rem;
  box-shadow: 0 4px 15px rgba(255, 117, 140, 0.3);
}

.guest-prompt a {
  color: white;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 2px solid rgba(255, 255, 255, 0.5);
  padding-bottom: 2px;
  transition: all 0.3s ease;
}

.guest-prompt a:hover {
  border-bottom-color: white;
  transform: translateY(-1px);
}

.filter-controls {
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: rgba(103, 126, 234, 0.05);
  border-radius: 15px;
  border: 1px solid rgba(103, 126, 234, 0.1);
}

/* Responsive Design */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
    gap: 1.5rem;
    padding: 1rem;
  }
  
  .posts-section {
    min-height: auto;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 1rem 0.5rem;
    gap: 1rem;
  }
  
  .posts-section {
    padding: 1rem;
    border-radius: 15px;
  }
  
  .posts-header h3 {
    font-size: 1.5rem;
  }
  
  .filter-controls {
    padding: 1rem;
  }
}

@media (max-width: 480px) {
  .main-content {
    padding: 0.5rem;
  }
  
  .posts-section {
    padding: 0.75rem;
    border-radius: 12px;
  }
  
  .posts-header h3 {
    font-size: 1.3rem;
  }
}

/* Scrollbar Styling */
.posts-section::-webkit-scrollbar {
  width: 8px;
}

.posts-section::-webkit-scrollbar-track {
  background: rgba(103, 126, 234, 0.1);
  border-radius: 10px;
}

.posts-section::-webkit-scrollbar-thumb {
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 10px;
}

.posts-section::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(135deg, #5a67d8, #6b46c1);
}
</style>

<style scoped>
.img {
  width:25%;
}
</style>