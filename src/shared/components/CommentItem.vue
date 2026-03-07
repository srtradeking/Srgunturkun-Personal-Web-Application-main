<template>
  <div class="comment-item">
    <!-- Comment Content -->
    <div class="comment-main">
      <div class="comment-author">
        <img 
          :src="authorPhoto" 
          :alt="comment.username + ' profile'" 
          class="author-photo"
          @error="$event.target.src = defaultAvatar"
        />
        <div class="comment-content-wrapper">
          <div class="comment-bubble">
            <span 
              @click="$emit('navigate-profile', comment.userId)" 
              class="comment-user username-link"
              :title="'View ' + comment.username + '\'s profile'"
            >
              {{ comment.username }}
            </span>
            <span class="comment-text">{{ comment.content || comment.text }}</span>
          </div>
          
          <!-- Actions -->
          <div class="comment-actions">
            <div class="reply-action-container">
              <button 
                v-if="isLoggedIn" 
                @click="$emit('initiate-reply', comment.id)" 
                class="action-btn reply-action-btn"
                :class="{ 'active': activeReplyId === comment.id }"
              >
                Reply
              </button>
            </div>
            <span class="comment-date" v-if="comment.timestamp">{{ formatDate(comment.timestamp) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Reply Form -->
    <div v-if="activeReplyId === comment.id" class="reply-form">
      <div class="reply-input-wrapper">
        <input 
          v-model="replyText" 
          placeholder="Write a reply..."
          @keyup.enter="submitReply"
          ref="replyInput"
          class="reply-input"
        />
        <div class="reply-form-actions">
          <button @click="$emit('cancel-reply')" class="cancel-reply-btn">Cancel</button>
          <button @click="submitReply" class="submit-reply-btn" :disabled="!replyText.trim()">Reply</button>
        </div>
      </div>
    </div>

    <!-- Nested Replies Dropdown -->
    <div v-if="children.length > 0" class="replies-dropdown">
      <button @click="toggleExpanded" class="view-replies-btn">
        <span class="dropdown-icon">{{ expanded ? '▼' : '▶' }}</span>
        {{ expanded ? 'Hide' : 'View' }} {{ children.length }} {{ children.length === 1 ? 'reply' : 'replies' }}
      </button>
      
      <div v-if="expanded" class="replies-content">
        <div class="replies-container">
          <div class="replies-list">
            <CommentItem 
              v-for="child in children" 
              :key="child.id" 
              :comment="child"
              :all-comments="allComments"
              :user-profiles="userProfiles"
              :active-reply-id="activeReplyId"
              :is-logged-in="isLoggedIn"
              @navigate-profile="$emit('navigate-profile', $event)"
              @initiate-reply="$emit('initiate-reply', $event)"
              @cancel-reply="$emit('cancel-reply')"
              @submit-reply="(text, parentId) => $emit('submit-reply', text, parentId)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, nextTick, watch } from 'vue'
import defaultAvatar from '@/shared/assets/DefaultAvatar.png'

export default {
  name: 'CommentItem',
  props: {
    comment: {
      type: Object,
      required: true
    },
    allComments: {
      type: Array,
      default: () => []
    },
    userProfiles: {
      type: Object,
      default: () => ({})
    },
    activeReplyId: {
      type: [String, Number],
      default: null
    },
    isLoggedIn: {
      type: Boolean,
      default: false
    }
  },
  emits: ['navigate-profile', 'initiate-reply', 'cancel-reply', 'submit-reply'],
  setup(props, { emit }) {
    const expanded = ref(false)
    const replyText = ref('')
    const replyInput = ref(null)

    // Compute children for this comment
    const children = computed(() => {
      // Find comments whose parentCommentId matches this comment's ID
      // Normalize IDs to strings for comparison just in case
      const myId = String(props.comment.id)
      return props.allComments.filter(c => {
        const pId = c.parentCommentId || c.parent_comment_id
        return pId && String(pId) === myId
      }).sort((a, b) => {
        const tA = new Date(a.timestamp || 0).getTime()
        const tB = new Date(b.timestamp || 0).getTime()
        return tA - tB // Oldest first for replies usually? Or newest? 
        // GameView was showing replies in some order. PostDetail sorted by timestamp.
        // Let's stick to chronological for replies (conversation flow).
      })
    })

    const authorPhoto = computed(() => {
      // Try to find profile photo on the comment object itself (PostDetail style)
      if (props.comment.userProfile?.photoUrl) return props.comment.userProfile.photoUrl
      // Or in the userProfiles map (GameView style)
      if (props.userProfiles && props.comment.userId) {
        return props.userProfiles[props.comment.userId]?.ppUrl || defaultAvatar
      }
      return props.comment.ppUrl || defaultAvatar
    })

    const formatDate = (timestamp) => {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      // Simple format
      return date.toLocaleDateString()
    }

    const toggleExpanded = () => {
      expanded.value = !expanded.value
    }

    const submitReply = () => {
      if (!replyText.value.trim()) return
      emit('submit-reply', replyText.value, props.comment.id)
      replyText.value = ''
    }

    // Focus input when reply becomes active for this comment
    watch(() => props.activeReplyId, (newVal) => {
      if (newVal === props.comment.id) {
        nextTick(() => {
          if (replyInput.value) replyInput.value.focus()
        })
      }
    })

    return {
      expanded,
      children,
      replyText,
      replyInput,
      authorPhoto,
      defaultAvatar,
      formatDate,
      toggleExpanded,
      submitReply
    }
  }
}
</script>

<style scoped>
.comment-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.comment-main {
  display: flex;
  align-items: flex-start;
}

.comment-author {
  display: flex;
  gap: 10px;
  width: 100%;
}

.author-photo {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.comment-content-wrapper {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: calc(100% - 42px);
}

.comment-bubble {
  background-color: #f0f2f5;
  padding: 8px 12px;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.comment-user {
  font-weight: 600;
  font-size: 0.9em;
  color: #050505;
  cursor: pointer;
}

.comment-user:hover {
  text-decoration: underline;
}

.comment-text {
  font-size: 0.95em;
  color: #050505;
  word-break: break-word;
  line-height: 1.4;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  margin-left: 12px;
  font-size: 0.8em;
  color: #65676b;
  align-items: center;
}

.action-btn {
  background: none;
  border: none;
  padding: 0;
  color: #65676b;
  font-weight: 600;
  cursor: pointer;
  font-size: inherit;
}

.action-btn:hover {
  text-decoration: underline;
}

.reply-form {
  margin-left: 42px;
  margin-top: 5px;
}

.reply-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reply-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 18px;
  outline: none;
  font-size: 0.9em;
}

.reply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.cancel-reply-btn {
  background: none;
  border: none;
  color: #65676b;
  font-size: 0.85em;
  cursor: pointer;
}

.submit-reply-btn {
  background-color: #667eea;
  color: white;
  border: none;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.85em;
  cursor: pointer;
}

.submit-reply-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Dropdown Styling */
.replies-dropdown {
  margin-left: 42px;
  margin-top: 8px;
}

.view-replies-btn {
  background: none;
  border: none;
  color: #667eea;
  font-weight: 600;
  cursor: pointer;
  padding: 5px 0;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 0.9em;
}

.view-replies-btn:hover {
  text-decoration: underline;
}

.dropdown-icon {
  font-size: 0.8em;
}

.replies-content {
  margin-top: 0;
  width: 100%;
  animation: slideDown 0.3s ease-out;
}

.replies-container {
  margin-left: 0;
  margin-top: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  border-left: 4px solid #667eea;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .replies-dropdown {
    margin-left: 20px;
  }
  
  .replies-container {
    padding: 10px;
  }
}
</style>
