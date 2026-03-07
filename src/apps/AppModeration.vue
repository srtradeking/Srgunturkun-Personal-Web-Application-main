<template>
  <div class="moderation-container">
    <div class="moderation-header">
      <h1>📋 Post Moderation Panel</h1>
      <p class="subtitle">Review and approve or reject pending posts</p>
    </div>

    <!-- Access Denied Message -->
    <div v-if="!isAuthorized" class="access-denied">
      <div class="access-denied-content">
        <span class="icon">🔒</span>
        <h2>Access Denied</h2>
        <p>You must be an Admin or Moderator to access this panel.</p>
        <router-link to="/social" class="back-link">← Back to Social</router-link>
      </div>
    </div>

    <!-- Loading State -->
    <div v-else-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading pending posts...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <span class="icon">⚠️</span>
      <p>{{ error }}</p>
      <button @click="loadPendingPosts" class="retry-btn">Retry</button>
    </div>

    <!-- Empty State -->
    <div v-else-if="pendingPosts.length === 0" class="empty-state">
      <span class="icon">✅</span>
      <h2>All Clear!</h2>
      <p>No pending posts to review at this time.</p>
    </div>

    <!-- Pending Posts List -->
    <div v-else class="posts-list">
      <div class="posts-count">
        <span>{{ pendingPosts.length }} post(s) awaiting review</span>
      </div>

      <div 
        v-for="post in pendingPosts" 
        :key="post.id" 
        class="post-card"
        :class="{ 'processing': processingPosts[post.id] }"
      >
        <!-- Post Header -->
        <div class="post-header">
          <div class="user-info">
            <img 
              :src="post.ppUrl || defaultAvatar" 
              :alt="post.userDisplayName || 'User'" 
              class="avatar"
            />
            <div class="user-details">
              <span class="display-name">{{ post.userDisplayName || 'Unknown User' }}</span>
              <span class="post-date">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <span class="status-badge pending">PENDING</span>
        </div>

        <!-- Post Content -->
        <div class="post-content">
          <h3 v-if="post.title" class="post-title">{{ post.title }}</h3>
          <p v-if="post.content" class="post-text">{{ post.content }}</p>
          
          <!-- Media Preview -->
          <div v-if="post.imageUrl || post.videoUrl" class="media-preview">
            <img 
              v-if="post.type === 'image' && post.imageUrl" 
              :src="post.imageUrl" 
              alt="Post image"
              class="preview-image"
            />
            <video 
              v-else-if="post.type === 'video' && post.videoUrl" 
              :src="post.videoUrl"
              controls
              class="preview-video"
            ></video>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="post-actions">
          <button 
            @click="approvePost(post.id)" 
            class="action-btn approve"
            :disabled="processingPosts[post.id]"
          >
            <span class="btn-icon">✓</span>
            Approve
          </button>
          <button 
            @click="rejectPost(post.id)" 
            class="action-btn reject"
            :disabled="processingPosts[post.id]"
          >
            <span class="btn-icon">✗</span>
            Reject
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { postsAPI } from '@/services/apiService'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'

export default {
  name: 'AppModeration',
  setup() {
    const pendingPosts = ref([])
    const loading = ref(true)
    const error = ref(null)
    const processingPosts = ref({})
    const userRole = ref(null)

    const isAuthorized = computed(() => {
      const role = localStorage.getItem('role') || localStorage.getItem('userRole')
      return role === 'ADMIN' || role === 'MODERATOR'
    })

    const loadPendingPosts = async () => {
      if (!isAuthorized.value) {
        loading.value = false
        return
      }

      loading.value = true
      error.value = null

      try {
        const posts = await postsAPI.getPendingPosts()
        pendingPosts.value = Array.isArray(posts) ? posts : []
      } catch (err) {
        console.error('Failed to load pending posts:', err)
        if (err.status === 403) {
          error.value = 'Access denied. You do not have permission to view pending posts.'
        } else {
          error.value = err.message || 'Failed to load pending posts'
        }
      } finally {
        loading.value = false
      }
    }

    const approvePost = async (postId) => {
      processingPosts.value[postId] = true
      try {
        await postsAPI.updatePostStatus(postId, 'APPROVED')
        pendingPosts.value = pendingPosts.value.filter(p => p.id !== postId)
      } catch (err) {
        console.error('Failed to approve post:', err)
        alert('Failed to approve post: ' + (err.message || 'Unknown error'))
      } finally {
        delete processingPosts.value[postId]
      }
    }

    const rejectPost = async (postId) => {
      processingPosts.value[postId] = true
      try {
        await postsAPI.rejectPost(postId)
        pendingPosts.value = pendingPosts.value.filter(p => p.id !== postId)
      } catch (err) {
        console.error('Failed to reject post:', err)
        alert('Failed to reject post: ' + (err.message || 'Unknown error'))
      } finally {
        delete processingPosts.value[postId]
      }
    }

    const formatDate = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    onMounted(() => {
      loadPendingPosts()
    })

    return {
      pendingPosts,
      loading,
      error,
      processingPosts,
      isAuthorized,
      defaultAvatar,
      loadPendingPosts,
      approvePost,
      rejectPost,
      formatDate
    }
  }
}
</script>

<style scoped>
.moderation-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 100vh;
}

.moderation-header {
  text-align: center;
  margin-bottom: 2rem;
}

.moderation-header h1 {
  font-size: 2rem;
  color: var(--text-primary, #333);
  margin-bottom: 0.5rem;
}

.moderation-header .subtitle {
  color: var(--text-secondary, #666);
  font-size: 1rem;
}

/* Access Denied */
.access-denied {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.access-denied-content {
  text-align: center;
  padding: 3rem;
  background: var(--card-bg, #fff);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.access-denied-content .icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 1rem;
}

.access-denied-content h2 {
  color: var(--text-primary, #333);
  margin-bottom: 0.5rem;
}

.access-denied-content p {
  color: var(--text-secondary, #666);
  margin-bottom: 1.5rem;
}

.back-link {
  color: var(--primary-color, #007bff);
  text-decoration: none;
  font-weight: 500;
}

.back-link:hover {
  text-decoration: underline;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 1rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color, #e0e0e0);
  border-top-color: var(--primary-color, #007bff);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Error State */
.error-state {
  text-align: center;
  padding: 3rem;
  background: #fff5f5;
  border-radius: 12px;
  border: 1px solid #ffcccc;
}

.error-state .icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 1rem;
}

.error-state p {
  color: #cc0000;
  margin-bottom: 1rem;
}

.retry-btn {
  padding: 0.5rem 1.5rem;
  background: var(--primary-color, #007bff);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

.retry-btn:hover {
  opacity: 0.9;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: var(--card-bg, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.empty-state .icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 1rem;
}

.empty-state h2 {
  color: var(--text-primary, #333);
  margin-bottom: 0.5rem;
}

.empty-state p {
  color: var(--text-secondary, #666);
}

/* Posts List */
.posts-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.posts-count {
  padding: 0.75rem 1rem;
  background: var(--info-bg, #e7f3ff);
  border-radius: 8px;
  color: var(--info-text, #0066cc);
  font-weight: 500;
}

/* Post Card */
.post-card {
  background: var(--card-bg, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: opacity 0.3s ease;
}

.post-card.processing {
  opacity: 0.6;
  pointer-events: none;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--border-color, #eee);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.display-name {
  font-weight: 600;
  color: var(--text-primary, #333);
}

.post-date {
  font-size: 0.85rem;
  color: var(--text-secondary, #888);
}

.status-badge {
  padding: 0.35rem 0.75rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.pending {
  background: #fff3cd;
  color: #856404;
}

/* Post Content */
.post-content {
  padding: 1.25rem;
}

.post-title {
  font-size: 1.1rem;
  color: var(--text-primary, #333);
  margin-bottom: 0.5rem;
}

.post-text {
  color: var(--text-secondary, #555);
  line-height: 1.6;
  margin-bottom: 1rem;
}

.media-preview {
  margin-top: 1rem;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.preview-image {
  width: 100%;
  max-height: 400px;
  object-fit: contain;
}

.preview-video {
  width: 100%;
  max-height: 400px;
}

/* Action Buttons */
.post-actions {
  display: flex;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border-top: 1px solid var(--border-color, #eee);
  background: var(--action-bg, #fafafa);
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.approve {
  background: #28a745;
  color: white;
}

.action-btn.approve:hover:not(:disabled) {
  background: #218838;
}

.action-btn.reject {
  background: #dc3545;
  color: white;
}

.action-btn.reject:hover:not(:disabled) {
  background: #c82333;
}

.btn-icon {
  font-size: 1.1rem;
}

/* Dark Mode Support */
:global(.dark-mode) .moderation-container {
  background: var(--bg-dark, #1a1a1a);
}

:global(.dark-mode) .moderation-header h1,
:global(.dark-mode) .access-denied-content h2,
:global(.dark-mode) .empty-state h2,
:global(.dark-mode) .post-title,
:global(.dark-mode) .display-name {
  color: var(--text-light, #f0f0f0);
}

:global(.dark-mode) .moderation-header .subtitle,
:global(.dark-mode) .access-denied-content p,
:global(.dark-mode) .empty-state p,
:global(.dark-mode) .post-text,
:global(.dark-mode) .post-date {
  color: var(--text-muted-dark, #aaa);
}

:global(.dark-mode) .post-card,
:global(.dark-mode) .access-denied-content,
:global(.dark-mode) .empty-state {
  background: var(--card-bg-dark, #2a2a2a);
}

:global(.dark-mode) .post-header,
:global(.dark-mode) .post-actions {
  border-color: var(--border-dark, #444);
}

:global(.dark-mode) .post-actions {
  background: var(--action-bg-dark, #222);
}

/* Responsive */
@media (max-width: 768px) {
  .moderation-container {
    padding: 1rem;
  }

  .moderation-header h1 {
    font-size: 1.5rem;
  }

  .post-actions {
    flex-direction: column;
  }
}
</style>
