<template>
  <div class="game-view">
    <!-- Posts Loading State -->
    <div v-if="postsLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading amazing posts...</p>
    </div>

    <!-- Error State -->
    <div v-if="isLoggedIn && !hasPostsAccess && !postsLoading" class="error-state">
      <p>Unable to access posts. Please check your permissions.</p>
      <p v-if="error" class="error-message">{{ error }}</p>
    </div>

    <!-- Posts List -->
    <div v-if="!postsLoading" class="posts-list">
      <!-- No posts message -->
      <div v-if="displayedPosts.length === 0 && selectedGame && selectedVideoType" class="no-posts">
        <p>🎭 No {{ selectedVideoType }} videos found for {{ selectedGame }}.</p>
        <p>Be the first to share some {{ selectedVideoType === 'short' ? 'quick clips' : 'epic gameplay' }}!</p>
      </div>
      
      <!-- Posts -->
      <div v-for="(item, index) in displayedPosts" :key="item.id" class="post-item">
        <!-- Post Header -->
        <div class="post-header">
          <div class="post-actions" v-if="isLoggedIn">
            <!-- Actions removed as per request -->
          </div>
          
          <div class="post-author">
            <div class="author-info">
              <img 
                :src="getUserPhoto(item.userId)" 
                :alt="item.username + ' profile'" 
                class="author-photo"
                @error="$event.target.src = defaultAvatar"
              />
              <div class="author-details">
                <span 
                  @click="$emit('navigateToProfile', item.userId)" 
                  class="username-link"
                  :title="'View ' + item.username + '\'s profile'"
                >
                  {{ item.username }}
                </span>
                <span class="post-date">{{ formatDate(item.timestamp) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Post Content -->
        <div class="post-content">
          <h4 class="post-title">{{ item.title }}</h4>
          
          <div class="post-media">
            <video v-if="item.type === 'video'" controls class="media-content">
              <source :src="item.url" :type="item.mimeType">
              Your browser does not support video playback.
            </video>
            <img v-else :src="item.url" :alt="item.title" class="media-content">
          </div>
          
          <p class="post-description">{{ item.description }}</p>
        </div>

        <!-- Post Interactions -->
        <div class="post-interactions">
          <!-- Reactions Section -->
          <div class="reactions">
            <button 
              @click="handleReaction(item.id, 'LIKE')"
              class="like-btn"
              :class="{ 'logged-out': !isLoggedIn }"
              title="Like this post"
            >
              <span class="flashy-score">+{{ item.likesCount || 0 }}</span>
            </button>
          </div>

          <!-- Comments Section -->
          <div class="comments-section">
            <div class="comments-list">
              <!-- Top Level Comments (Recursive) -->
              <div v-for="comment in getRootComments(item.id)" :key="comment.id" class="comment-thread">
                 <CommentItem 
                   :comment="comment"
                   :all-comments="getComments(item.id)"
                   :user-profiles="userProfiles"
                   :active-reply-id="activeReplyId"
                   :is-logged-in="isLoggedIn"
                   @navigate-profile="handleProfileNavigation"
                   @initiate-reply="initiateReply"
                   @cancel-reply="cancelReply"
                   @submit-reply="(text, parentId) => submitReply(item.id, parentId, text)"
                 />
              </div>
            </div>
            
            <div class="add-comment" v-if="isLoggedIn">
              <input 
                v-model="newComments[item.id]" 
                placeholder="Add a comment..."
                @keyup.enter="addComment(item.id)"
                class="comment-input"
              />
            </div>
            
            <div v-else class="login-to-comment">
              <button @click="$emit('promptLogin')" class="comment-login-btn">
                Login to comment
              </button>
            </div>
          </div>
        </div>
        
        <!-- Guest View Limit Prompt -->
        <div v-if="!isLoggedIn && index >= guestViewLimit - 1" class="guest-limit-prompt">
          <div class="limit-card">
            <h4>🎉 You've seen {{ guestViewLimit }} amazing posts!</h4>
            <p>Join our community to see more, interact, and share your own content.</p>
            <div class="limit-actions">
              <router-link to="/account" class="join-btn">Join Now</router-link>
              <router-link to="/account" class="login-link">Already have an account?</router-link>
            </div>
          </div>
        </div>
        
        <!-- Progressive Login Prompt -->
        <div v-if="!isLoggedIn && shouldShowProgressivePrompt(index)" class="progressive-prompt">
          <p>💡 <router-link to="/account">Create an account</router-link> to like and comment on posts!</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CommentItem from './CommentItem.vue'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import { useAuthStore } from '@/store/useAuthStore'
import { ref, computed } from 'vue'

export default {
  name: 'GameView',
  components: {
    CommentItem
  },
  props: {
    selectedGame: {
      type: String,
      default: ''
    },
    selectedVideoType: {
      type: String,
      default: ''
    },
    posts: {
      type: Array,
      default: () => []
    },
    comments: {
      type: Object,
      default: () => ({})
    },
    userProfiles: {
      type: Object,
      default: () => ({})
    },
    isLoggedIn: {
      type: Boolean,
      default: false
    },
    postsLoading: {
      type: Boolean,
      default: false
    },
    hasPostsAccess: {
      type: Boolean,
      default: false
    },
    error: {
      type: String,
      default: ''
    },
    hiddenPosts: {
      type: Set,
      default: () => new Set()
    }
  },
  emits: [
    'addReaction', 
    'addComment', 
    'addReply',
    'hidePost', 
    'navigateToProfile', 
    'promptLogin'
  ],
  setup(props, { emit }) {
    // Stores
    const authStore = useAuthStore()

    // State
    const newComments = ref({})
    const guestViewLimit = ref(5)
    
    // Reply State
    const activeReplyId = ref(null)

    // ... existing computed ...

    // Computed
    const displayedPosts = computed(() => {
      // Return empty if no posts available (ignoring filter checks for now)
      if (!props.posts) return []
      
      // Filter posts by game and video type
      let filteredPosts = props.posts.filter(post => {
        // 1. Game Filter
        // If selectedGame is 'All Games' (or empty), show all games. Otherwise, match strictly.
        const postGame = post.game || 'Other'
        if (props.selectedGame && props.selectedGame !== 'All Games' && postGame !== props.selectedGame) {
          return false
        }
        
        // 2. Video Type Filter
        // If selectedVideoType is 'All Types' (or empty), show all types.
        if (!props.selectedVideoType || props.selectedVideoType === 'All Types') {
          return true
        }

        // Handle specific type filters ('short' vs 'long')
        // 'short': Images + Videos <= 60s
        // 'long': Videos > 60s
        
        if (props.selectedVideoType === 'short') {
          if (post.type === 'image') return true
          if (post.type === 'video') {
             return (post.duration || 0) <= 60
          }
          return false
        }
        
        if (props.selectedVideoType === 'long') {
          if (post.type === 'image') return false
          if (post.type === 'video') {
             // Treat 0 duration (unknown) as long or filter it out? 
             // Usually better to treat > 60 strictly, or maybe fallback for unknown. 
             // Using logic: duration > 60.
             return (post.duration || 0) > 60
          }
          return false
        }
        
        return true
      })

      // Sort posts (Default by date)
      filteredPosts.sort((a, b) => {
        // Helper to get time in ms from various formats (Firestore Timestamp, JS Date, string)
        const getTime = (t) => {
          if (!t) return 0
          if (t.seconds) return t.seconds * 1000 // Firestore Timestamp
          if (t.getTime && typeof t.getTime === 'function') return t.getTime() // JS Date
          return new Date(t).getTime() // String/Other
        }
        
        const aTime = getTime(a.timestamp)
        const bTime = getTime(b.timestamp)
        return bTime - aTime
      })

      // Limit for guests
      if (props.isLoggedIn) {
        return filteredPosts
      }
      return filteredPosts.slice(0, guestViewLimit.value + 3)
    })

    // Methods
    const getUserPhoto = (userId) => {
      return props.userProfiles[userId]?.ppUrl || defaultAvatar
    }

    const formatDate = (timestamp) => {
      if (!timestamp) return 'Unknown date'
      const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp)
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }

    const getComments = (postId) => {
      return props.comments[postId] || []
    }
    
    const getRootComments = (postId) => {
      const allComments = getComments(postId)
      return allComments.filter(c => !c.parentCommentId)
    }

    const isOwnPost = (post) => {
      if (!authStore.user) return false
      const user = authStore.user
      const currentUserId = String(user.uid || user.id || user.userId)
      return post.userId != null && String(post.userId) === currentUserId
    }

    const shouldShowProgressivePrompt = (index) => {
      if (props.isLoggedIn) return false
      return index === 2 || (index > 2 && (index + 1) % 3 === 0)
    }

    const handleReaction = (postId, emoji) => {
      if (props.isLoggedIn) {
        emit('addReaction', postId, emoji)
      } else {
        emit('promptLogin')
      }
    }

    const handleProfileNavigation = (userId) => {
      if (props.isLoggedIn) {
        emit('navigateToProfile', userId)
      } else {
        emit('promptLogin')
      }
    }

    const addComment = (postId) => {
      if (!props.isLoggedIn || !newComments.value[postId]) return
      
      const commentText = newComments.value[postId].trim()
      if (commentText) {
        emit('addComment', postId, commentText)
        newComments.value[postId] = ''
      }
    }
    
    const initiateReply = (commentId) => {
      activeReplyId.value = commentId
    }

    const cancelReply = () => {
      activeReplyId.value = null
    }

    const submitReply = (postId, parentId, content) => {
      if (!props.isLoggedIn) return
      
      const text = content
      if (!text || !text.trim()) return

      emit('addReply', postId, parentId, text)
      
      activeReplyId.value = null
    }

    return {
      displayedPosts,
      newComments,
      getUserPhoto,
      formatDate,
      getComments,
      getRootComments,
      activeReplyId,
      initiateReply,
      cancelReply,
      submitReply,
      isOwnPost,
      shouldShowProgressivePrompt,
      handleReaction,
      handleProfileNavigation,
      addComment,
      defaultAvatar
    }
  }
}
</script>

<style scoped>
.game-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 40px 20px;
  margin: 20px 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #dd7724ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-state p {
  color: #666;
  font-size: 1.1em;
}

.error-state p {
  color: #dc3545;
  margin: 10px 0;
}

.error-message {
  font-size: 0.9em;
  background: #f8d7da;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #f5c6cb;
}

/* Posts List */
.posts-list {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 25px;
}

.no-posts {
  text-align: center;
  padding: 60px 20px;
  color: #666;
  font-size: 1.2em;
  background: linear-gradient(45deg, #f8f9fa, #e9ecef);
  border-radius: 15px;
  border: 2px dashed #dee2e6;
}

.post-item {
  background: white;
  border-radius: 15px;
  max-width: 700px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.08);
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.3s ease;
}

.post-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px rgba(0,0,0,0.12);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 25px 15px;
  border-bottom: 1px solid #f0f0f0;
}

.post-actions {
  display: flex;
  gap: 10px;
}

.delete-btn, .hide-btn {
  background: #fff5f5;
  border: 1px solid #fed7d7;
  color: #e53e3e;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 1.1em;
}

.delete-btn:hover, .hide-btn:hover {
  background: #fed7d7;
  transform: scale(1.1);
}

.post-author {
  flex: 1;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-photo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #dd7724ff;
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username-link {
  color: #667eea;
  cursor: pointer;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.2s ease;
  padding: 2px 4px;
  border-radius: 3px;
}

.username-link:hover {
  background-color: #f0f3ff;
  color: #5a67d8;
  text-decoration: underline;
  transform: translateY(-1px);
}

.post-date {
  font-size: 0.8em;
  color: #999;
}

.post-content {
  padding: 20px 25px;
}

.post-title {
  color: #333;
  margin-bottom: 15px;
  font-size: 1.3em;
  font-weight: 600;
}

.post-media {
  margin: 15px 0;
  border-radius: 10px;
  overflow: hidden;
}

.media-content {
  width: 40%;
  height: auto;
  border-radius: 10px;
}

.post-description {
  color: #666;
  line-height: 1.6;
  margin: 15px 0 0;
}

.post-interactions {
  padding: 20px 25px;
  border-top: 1px solid #f0f0f0;
  
}

.reactions {
  display: flex;
  margin: 10px 0;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  justify-content: flex-start; /* Align to start */
}

.like-btn {
  background: linear-gradient(135deg, #ff7eb3 0%, #ff758c 100%);
  border: none;
  border-radius: 50px;
  padding: 8px 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  box-shadow: 0 4px 15px rgba(255, 117, 140, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.like-btn:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 6px 20px rgba(255, 117, 140, 0.6);
  filter: brightness(1.1);
}

.like-btn:active {
  transform: translateY(1px) scale(0.95);
}

.like-btn.logged-out {
  opacity: 0.7;
  background: linear-gradient(135deg, #ff7eb3 0%, #ff758c 100%);
  box-shadow: 0 4px 15px rgba(255, 117, 140, 0.4);
}

.flashy-score {
  color: white;
  font-weight: 800;
  font-size: 1.2em;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  letter-spacing: 0.5px;
}

.comments-section {
  margin-top: 15px;
}

.comment-input {
  position: relative;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 0.9em;
}

.comment-input:focus {
  border-color: #007bff;
}

.login-to-comment {
  padding: 10px;
  text-align: center;
}

.comment-login-btn {
  background: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background 0.3s;
}

.comment-login-btn:hover {
  background: #0056b3;
}

/* Guest viewing styles */
.guest-limit-prompt {
  margin: 20px 0;
  text-align: center;
}

.limit-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  max-width: 500px;
  margin: 0 auto;
}

.limit-card h4 {
  margin-bottom: 15px;
  font-size: 1.4em;
}

.limit-card p {
  margin-bottom: 25px;
  opacity: 0.9;
}

.limit-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  align-items: center;
}

.join-btn {
  background: white;
  color: #667eea;
  padding: 12px 25px;
  border-radius: 25px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.join-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.2);
  text-decoration: none;
  color: #667eea;
}

.login-link {
  color: white;
  text-decoration: underline;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.login-link:hover {
  opacity: 1;
  color: white;
  text-decoration: underline;
}

.progressive-prompt {
  background: #f8f9fa;
  border: 2px dashed #dee2e6;
  border-radius: 10px;
  padding: 15px;
  margin: 15px 0;
  text-align: center;
}

.progressive-prompt p {
  margin: 0;
  color: #6c757d;
}

.progressive-prompt a {
  color: #007bff;
  font-weight: 600;
  text-decoration: none;
}

.progressive-prompt a:hover {
  text-decoration: underline;
}
</style>