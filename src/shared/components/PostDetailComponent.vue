<template>
  <div class="post-detail-container">
    <div class="post-detail-overlay"></div>
    
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading post...</p>
    </div>
    
    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button @click="$router.push('/social')" class="back-btn">
        ← Back to Social
      </button>
    </div>
    
    <!-- Post Content -->
    <div v-else class="post-content">
      <!-- Post Header -->
      <div class="post-header">
        <button @click="$router.push('/social')" class="back-btn">
          ← Back to Social
        </button>
        
        <div class="post-author">
          <img 
            :src="post.userProfile?.photoUrl || defaultAvatar" 
            :alt="post.username + ' profile'" 
            class="author-photo"
            @error="$event.target.src = defaultAvatar"
          />
          <div class="author-details">
            <span 
              @click="navigateToProfile(post.userId)" 
              class="username-link"
            >
              {{ post.username }}
            </span>
            <span class="post-date">{{ formatDate(post.timestamp) }}</span>
          </div>
        </div>
        
        <div class="post-actions" v-if="isLoggedIn && (isOwnPost)">
          <button
            v-if="isOwnPost"
            @click="deletePost"
            class="delete-btn"
            title="Delete post"
          >
            🗑️ Delete Post
          </button>
        </div>
      </div>
      
      <!-- Post Media and Content -->
      <div class="post-main">
        <h1 class="post-title">{{ post.title }}</h1>
        
        <div class="post-media">
          <video v-if="post.type === 'video'" controls class="media-content">
            <source :src="post.url" :type="post.mimeType">
            Your browser does not support video playback.
          </video>
          <img v-else :src="post.url" :alt="post.title" class="media-content">
        </div>
        
        <div class="post-description">
          <p>{{ post.description }}</p>
        </div>
      </div>
      
      <!-- Post Interactions -->
      <div class="post-interactions">
        <!-- Reactions Section -->
        <div class="reactions-section">
          <h3>Reactions</h3>
          <div class="reactions">
            <button 
              @click="handleReaction('LIKE')"
              class="like-btn"
              :class="{ 'logged-out': !isLoggedIn }"
              title="Like this post"
            >
              <span class="flashy-score">+{{ post.likesCount || 0 }}</span>
            </button>
          </div>
        </div>

        <!-- Comments Section -->
        <div class="comments-section">
          <h3>Comments ({{ comments.length }})</h3>
          
          <!-- Add Comment -->
          <div v-if="isLoggedIn" class="add-comment">
            <div class="comment-input-group">
              <img 
                :src="currentUserProfile?.photoUrl || defaultAvatar" 
                :alt="currentUser?.displayName + ' profile'" 
                class="comment-author-photo"
                @error="$event.target.src = defaultAvatar"
              />
              <input 
                v-model="newComment" 
                placeholder="Add a comment..."
                @keyup.enter="addComment"
                class="comment-input"
              />
              <button @click="addComment" class="comment-submit-btn" :disabled="!newComment.trim()">
                Post
              </button>
            </div>
          </div>
          
          <div v-else class="login-to-comment">
            <button @click="$router.push('/account')" class="comment-login-btn">
              Login to comment
            </button>
          </div>
          
          <!-- Comments List -->
          <div class="comments-list">
            <CommentItem 
              v-for="comment in rootComments" 
              :key="comment.id" 
              :comment="comment" 
              :all-comments="comments" 
              :active-reply-id="activeReplyId" 
              :is-logged-in="isLoggedIn" 
              @navigate-profile="navigateToProfile" 
              @initiate-reply="initiateReply" 
              @cancel-reply="cancelReply" 
              @submit-reply="(text, parentId) => submitReply(parentId, text)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CommentItem from './CommentItem.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'
import { postsAPI, commentsAPI, profilesAPI } from '@/services/apiService'

export default {
  name: 'PostDetailComponent',
  components: {
    CommentItem
  },
  props: {
    postId: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()
    
    // State
    const loading = ref(true)
    const error = ref('')
    const post = ref({})
    const comments = ref([])
    const currentUser = ref(null)
    const currentUserProfile = ref({})
    const newComment = ref('')
    
    // Reply State
    const activeReplyId = ref(null)

    let unsubscribeAuth = null
    
    // Computed
    const rootComments = computed(() => {
      return comments.value.filter(c => !c.parentCommentId)
    })

    const isLoggedIn = computed(() => {
      try {
        return !!localStorage.getItem('jwtToken')
      } catch {
        return false
      }
    })
    const isOwnPost = computed(() => {
      if (!post.value) return false
      try {
        const storedId = localStorage.getItem('userId')
        if (!storedId) return false
        const numericId = Number(storedId)
        if (Number.isNaN(numericId)) return false
        return post.value.userId === numericId
      } catch {
        return false
      }
    })
    
    // Methods
    async function loadPost() {
      try {
        loading.value = true
        error.value = ''
        
        // Load post from backend API
        const postData = await postsAPI.getPostById(props.postId)
        
        if (!postData) {
          error.value = 'Post not found.'
          loading.value = false
          return
        }

        // Privacy check
        const isOwner = currentUser.value && currentUser.value.uid === postData.userId
        const isPublic = postData.isPublished && !postData.isHidden && !postData.isDeleted
        
        if (!isPublic && !isOwner) {
          error.value = 'This post is private or unavailable.'
          loading.value = false
          return
        }
        
        post.value = postData
        
        // Load user profile for the post author
        if (postData.userId) {
          try {
            const userProfile = await profilesAPI.getProfileByUserId(postData.userId)
            post.value.userProfile = userProfile
          } catch (err) {
            console.error('Error loading post author profile:', err)
          }
        }
        
        loading.value = false
        
        // Load comments after post is loaded
        await loadComments()
      } catch (err) {
        console.error('Error loading post:', err)
        error.value = 'Failed to load post. Please try again.'
        loading.value = false
      }
    }
    
    async function loadComments() {
      try {
        const postComments = await commentsAPI.getCommentsByPostId(props.postId)
        
        if (Array.isArray(postComments)) {
          // Load user profiles for each comment
          const commentsWithProfiles = await Promise.all(
            postComments.map(async (comment) => {
              // Normalize parentCommentId
              const normalizedComment = {
                ...comment,
                parentCommentId: comment.parentCommentId || comment.parent_comment_id || null
              }
              
              try {
                const userProfile = await profilesAPI.getProfileByUserId(comment.userId)
                return { ...normalizedComment, userProfile }
              } catch (err) {
                console.error(`Error loading profile for comment ${comment.id}:`, err)
                return normalizedComment
              }
            })
          )
          
          // Sort comments by timestamp (newest first)
          commentsWithProfiles.sort((a, b) => {
            const aTime = new Date(a.timestamp || 0).getTime()
            const bTime = new Date(b.timestamp || 0).getTime()
            return bTime - aTime
          })
          
          comments.value = commentsWithProfiles
        } else {
          comments.value = []
        }
      } catch (err) {
        console.error('Error loading comments:', err)
        comments.value = []
      }
    }
    
    async function loadCurrentUserProfile() {
      if (!currentUser.value) return
      
      try {
        const profile = await profilesAPI.getProfileByUserId(currentUser.value.uid)
        currentUserProfile.value = profile || {}
      } catch (err) {
        console.error('Error loading current user profile:', err)
      }
    }
    
    async function handleReaction(emoji) {
      if (!isLoggedIn.value) {
        router.push('/account')
        return
      }
      
      let userId = null
      try {
        const storedId = localStorage.getItem('userId')
        if (storedId && !Number.isNaN(Number(storedId))) {
          userId = Number(storedId)
        }
      } catch {}

      if (userId == null) {
        router.push('/account')
        return
      }

      try {
        const updatedPost = await postsAPI.reactToPost(props.postId, { userId, emoji })
        if (updatedPost && updatedPost.likesCount !== undefined) {
          post.value.likesCount = updatedPost.likesCount
        }
      } catch (err) {
        console.error('Error adding reaction:', err)
        alert('Failed to add reaction. Please try again.')
      }
    }
    
    function navigateToProfile(userId) {
      router.push(`/user/${userId}`)
    }
    
    function formatDate(timestamp) {
      if (!timestamp) return 'Unknown date'
      
      const date = typeof timestamp === 'string' ? new Date(timestamp) : new Date(timestamp)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }
    
    async function addComment() {
      if (!isLoggedIn.value || !newComment.value.trim()) return

      let userId = null
      try {
        const storedId = localStorage.getItem('userId')
        if (storedId && !Number.isNaN(Number(storedId))) {
          userId = Number(storedId)
        }
      } catch {}

      if (userId == null) {
        router.push('/account')
        return
      }

      try {
        const commentData = {
          postId: props.postId,
          userId,
          username: 'Anonymous',
          content: newComment.value.trim(), // API expects 'content' usually, DTO might map text->content or vice versa. AppSocial uses content.
          timestamp: new Date().toISOString()
        }
        
        await commentsAPI.createComment(commentData)
        newComment.value = ''
        
        // Reload comments
        await loadComments()
      } catch (err) {
        console.error('Error adding comment:', err)
        alert('Failed to add comment. Please try again.')
      }
    }
    
    function initiateReply(commentId) {
      if (activeReplyId.value === commentId) {
        activeReplyId.value = null
        return
      }

      activeReplyId.value = commentId
    }

    function cancelReply() {
      activeReplyId.value = null
    }

    async function submitReply(parentCommentId, content) {
      if (!isLoggedIn.value) return
      
      const text = content
      if (!text || !text.trim()) return

      let userId = null
      try {
        const storedId = localStorage.getItem('userId')
        if (storedId && !Number.isNaN(Number(storedId))) {
          userId = Number(storedId)
        }
      } catch {}

      if (userId == null) {
        router.push('/account')
        return
      }

      try {
        const commentData = {
          postId: props.postId,
          userId,
          username: 'Anonymous',
          content: text.trim(),
          parentCommentId: parentCommentId,
          timestamp: new Date().toISOString()
        }
        
        await commentsAPI.createComment(commentData)
        
        activeReplyId.value = null
        
        // Reload comments
        await loadComments()
      } catch (err) {
        console.error('Error adding reply:', err)
        alert('Failed to add reply. Please try again.')
      }
    }

    async function deletePost() {
      if (!isOwnPost.value) return;
      
      if (!confirm('Are you sure you want to delete this post? This action cannot be undone.')) {
        return;
      }
      
      try {
        await postsAPI.deletePost(props.postId);
        router.push('/social');
      } catch (err) {
        console.error('Error deleting post:', err);
        alert('Failed to delete post. Please try again.');
      }
    }

    // Lifecycle
    onMounted(() => {
      loadPost()
    })
    
    return {
      loading,
      error,
      post,
      comments,
      currentUser,
      currentUserProfile,
      newComment,
      isLoggedIn,
      isOwnPost,
      handleReaction,
      addComment,
      deletePost,
      navigateToProfile,
      formatDate,
      defaultAvatar,
      // Reply exports
      activeReplyId,
      rootComments,
      initiateReply,
      cancelReply,
      submitReply
    }
  }
}
</script>

<style scoped>
/* Reply Styles */
.comment-thread {
  margin-bottom: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
}

/* Comment Item styles are now in CommentItem.vue */


.post-detail-container {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

.post-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  opacity: 0.1;
  z-index: -1;
}

.loading-state,
.error-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  margin-top: 100px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.post-content {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e9ecef;
}

.back-btn {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background: #5a6fd8;
}

.post-author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-photo {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.author-details {
  display: flex;
  flex-direction: column;
}

.username-link {
  color: #667eea;
  text-decoration: none;
  font-weight: 600;
  cursor: pointer;
}

.username-link:hover {
  text-decoration: underline;
}

.post-date {
  font-size: 0.8em;
  color: #666;
}

.post-actions {
  display: flex;
  gap: 8px;
}

.delete-btn {
  background: #dc3545;
  color: white;
  border: none;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.2s;
}

.delete-btn:hover {
  background: #c82333;
}

.post-main {
  padding: 20px;
}

.post-title {
  margin: 0 0 20px 0;
  font-size: 2em;
  color: #333;
  line-height: 1.2;
}

.post-media {
  margin-bottom: 20px;
}

.media-content {
  width: 100%;
  max-height: 500px;
  object-fit: contain;
  border-radius: 8px;
}

.post-description {
  margin-bottom: 30px;
}

.post-description p {
  color: #555;
  line-height: 1.6;
  font-size: 1.1em;
  margin: 0;
}

.post-interactions {
  border-top: 1px solid #e9ecef;
}

.reactions-section,
.comments-section {
  padding: 20px;
}

.reactions-section h3,
.comments-section h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.2em;
}

.reactions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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
  background: linear-gradient(135deg, #a8c0ff 0%, #3f2b96 100%);
  box-shadow: 0 4px 15px rgba(63, 43, 150, 0.3);
}

.flashy-score {
  color: white;
  font-weight: 800;
  font-size: 1.2em;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
  letter-spacing: 0.5px;
}

.add-comment {
  margin-bottom: 20px;
}

.comment-input-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.comment-author-photo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.comment-input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #dee2e6;
  border-radius: 20px;
  font-size: 0.95em;
  outline: none;
}

.comment-input:focus {
  border-color: #667eea;
}

.comment-submit-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.comment-submit-btn:hover:not(:disabled) {
  background: #5a6fd8;
}

.comment-submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-to-comment {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.comment-login-btn {
  background: #667eea;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 500;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* Comment Item styles are now in CommentItem.vue */

/* Responsive Design */
@media (max-width: 768px) {
  .post-detail-container {
    padding: 10px;
  }
  
  .post-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .post-title {
    font-size: 1.5em;
  }
  
  .comment-input-group {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
