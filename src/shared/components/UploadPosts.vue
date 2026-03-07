<template>  
  <div class="upload-section">
    <h3>📝 Share Your Gaming Clips</h3>
    
    <div v-if="isLoggedIn" class="upload-form">
      <h4>Create New Post</h4>
      <div class="upload-controls">
        <input 
          type="file" 
          @change="handleFileSelect" 
          accept="image/*,video/*" 
          class="file-input"
          id="media-upload"
        />
        <label for="media-upload" class="file-label">
          📎 Choose Media
        </label>
        <!-- File Info Display -->
        <div v-if="showFileInfo" class="file-info">
          <div class="file-details">
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size">(~ {{ (selectedFile.size / 1024 / 1024).toFixed(2) }} MB)</span>
          </div>
          <div class="upload-progress" v-if="uploadLoading">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
            </div>
            <span class="progress-text">{{ Math.round(uploadProgress) }}% uploaded</span>
          </div>
        </div>
        <select v-model="selectedUploadGame" class="media-game" required :disabled="gamesLoading">
          <option value="">{{ gamesLoading ? 'Loading games...' : gameCategories.length === 1 && gameCategories[0] === 'Other' ? 'Games unavailable - using Other' : 'Select from game list' }}</option>
          <option v-for="game in gameCategories" :key="game" :value="game">{{ game }}</option>
        </select>
        
        <div v-if="gameCategories.length === 1 && gameCategories[0] === 'Other' && !gamesLoading" class="upload-games-error">
          <p>⚠️ Could not load games from database. Only "Other" category available.</p>
        </div>
        
        <input 
          v-model="mediaTitle" 
          placeholder="Post title..." 
          class="media-title" 
          required 
        />
        
        <textarea 
          v-model="mediaDescription" 
          placeholder="What's on your mind?" 
          class="media-description"
          rows="4"
        ></textarea>
        
        <button 
          @click="uploadMedia" 
          :disabled="!selectedFile || !mediaTitle || !selectedUploadGame || uploadLoading"
          class="upload-btn"
        >
          {{ uploadLoading ? '⏳ Uploading...' : '🚀 Share Post' }}
        </button>
        
        <div v-if="error" class="alert alert-danger mt-2">{{ error }}</div>
        <div v-if="uploadSuccess" class="alert alert-success mt-2">✅ Post uploaded! It will be visible after moderation.</div>
      </div>
    </div>
  
    <div v-else class="login-prompt">
      <div class="login-card">
        <h4>🔐 Join the Community</h4>
        <p>Share your clips with the world!</p>
        <router-link to="/login" class="login-btn">Sign In</router-link>
        <router-link to="/account" class="signup-btn">Create Account</router-link>
      </div>
    </div>
    <!-- Google Ads - Between Upload and Posts -->
  <GoogleAdsComponent />
  </div>
  
</template>

<script>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/store/useAuthStore'
import { storeToRefs } from 'pinia'
import { uploadPostMedia } from '@/services/mediaService'
import GoogleAdsComponent from '@/shared/components/GoogleAdsComponent.vue'

export default {
  name: 'UploadPosts',
  components: {
    GoogleAdsComponent
  },
  props: {
    gameCategories: {
      type: Array,
      default: () => []
    },
    gamesLoading: {
      type: Boolean,
      default: false
    }
  },
  emits: ['reloadGames', 'createDefaultGames', 'postUploaded'],
  setup(props, { emit }) {
    // State
    const authStore = useAuthStore()
    const { isLoggedIn, currentUser } = storeToRefs(authStore)
    const selectedFile = ref(null)
    const mediaTitle = ref('')
    const mediaDescription = ref('')
    const selectedUploadGame = ref('')
    const error = ref('')
    const uploadSuccess = ref(false)
    const uploadLoading = ref(false)
    const uploadProgress = ref(0)
    const uploadedUrl = ref('')
    const showFileInfo = ref(false)

    // Removed local isLoggedIn check onMounted as it is now handled by the authStore
    onMounted(() => {
    })

    function handleFileSelect(event) {
      const file = event.target.files[0]
      if (file) {
        selectedFile.value = file
        showFileInfo.value = true
        uploadProgress.value = 0
      } else {
        selectedFile.value = null
        showFileInfo.value = false
        uploadProgress.value = 0
      }
    }

    async function uploadMedia() {
      if (!isLoggedIn.value) {
        error.value = 'Please log in to upload media'
        return
      }
      
      if (!selectedFile.value || !mediaTitle.value || !selectedUploadGame.value) {
        error.value = 'Please select a file, enter a title, and choose a game category'
        return
      }

      // Get and verify authentication token before proceeding
      let authToken
      try {
        authToken = currentUser.value?.token
        if (!authToken) {
          // Fallback to local storage if token not in store (though store should have it)
          authToken = localStorage.getItem('jwtToken')
        }
        
        if (!authToken) {
          error.value = 'Authentication failed. Please log in again.'
          return
        }
      } catch (err) {
        console.error('Error getting auth token:', err)
        error.value = 'Failed to authenticate. Please log in again.'
        return
      }

      // Centralized error handling
      const handleError = (message, error) => {
        console.error(message, error);
        let userFriendlyMessage = 'Failed to upload media. Please try again.';
        if (error && error.message) {
            if (error.message.includes('Authentication required')) userFriendlyMessage = 'Please log in again to upload media.';
            else if (error.message.includes('User profile not found')) userFriendlyMessage = 'Account setup incomplete. Please visit your profile page to complete setup.';
            else if (error.message.includes('413')) userFriendlyMessage = 'File is too large. Please choose a smaller file.';
            else userFriendlyMessage = error.message;
        }
        error.value = userFriendlyMessage;
        uploadLoading.value = false;
        uploadProgress.value = 0;
      };

      try {
        error.value = ''; // Clear any previous errors
        uploadLoading.value = true;
        uploadProgress.value = 0;
        const { postId, postData, publicUrl } = await uploadPostMedia({
          file: selectedFile.value,
          title: mediaTitle.value,
          description: mediaDescription.value,
          game: selectedUploadGame.value,
          onProgress: (p) => {
            uploadProgress.value = p
          }
        })

        uploadedUrl.value = publicUrl

        // Finalize UI
        if (uploadProgress.value < 100) {
          uploadProgress.value = 100
        }

        setTimeout(() => {
          selectedFile.value = null;
          showFileInfo.value = false;
          uploadProgress.value = 0;
          mediaTitle.value = '';
          mediaDescription.value = '';
          selectedUploadGame.value = '';
          uploadLoading.value = false;
          uploadSuccess.value = true;

          emit('postUploaded', { id: postId, ...postData });

          setTimeout(() => {
            uploadSuccess.value = false;
          }, 3000);
        }, 500);

      } catch (err) {
        handleError('Error during media upload:', err);
      }
    }

    return {
      isLoggedIn,
      selectedFile,
      mediaTitle,
      mediaDescription,
      selectedUploadGame,
      error,
      uploadSuccess,
      uploadLoading,
      uploadProgress,
      showFileInfo,
      uploadedUrl,
      handleFileSelect,
      uploadMedia
    }
  }
}
</script>

<style scoped>
.upload-section {
  background: linear-gradient(135deg, rgb(172, 221, 36) 0%, #f39c12 100%);
  border-radius: 15px;
  padding: 25px;
  box-shadow: 0 10px 30px rgba(221, 119, 36, 0.2);
  height: fit-content;
  position: sticky;
  top: 20px;
}

.upload-section h3 {
  color: white;
  margin-bottom: 20px;
  text-align: center;
  font-size: 1.5em;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.upload-form h4 {
  color: white;
  margin-bottom: 15px;
  font-size: 1.2em;
}

.login-prompt {
  text-align: center;
}

.login-card {
  background: rgba(255,255,255,0.95);
  padding: 30px;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.1);
}

.login-card h4 {
  color: #dd7724ff;
  margin-bottom: 15px;
}

.login-card p {
  color: #666;
  margin-bottom: 20px;
}

.login-btn, .signup-btn {
  display: inline-block;
  padding: 12px 20px;
  margin: 5px;
  border-radius: 25px;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s;
}

.login-btn {
  background: #dd7724ff;
  color: white;
}

.signup-btn {
  background: transparent;
  color: #dd7724ff;
  border: 2px solid #dd7724ff;
}

.login-btn:hover, .signup-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(221, 119, 36, 0.3);
  text-decoration: none;
}

.login-btn:hover {
  color: white;
}

.signup-btn:hover {
  background: #dd7724ff;
  color: white;
}

/* Upload Controls */
.upload-controls {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.file-input {
  display: none;
}

.file-label {
  display: inline-block;
  padding: 12px 20px;
  background: rgba(255,255,255,0.9);
  color: #dd7724ff;
  border-radius: 25px;
  cursor: pointer;
  font-weight: 600;
  text-align: center;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.file-label:hover {
  background: white;
  border-color: rgba(255,255,255,0.5);
  transform: translateY(-1px);
}

.file-info {
  background: rgba(255,255,255,0.95);
  border-radius: 10px;
  padding: 15px;
  margin-top: 10px;
  border: 2px solid rgba(255,255,255,0.3);
}

.file-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.file-name {
  font-weight: 600;
  color: #dd7724ff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: #666;
  font-size: 0.9em;
}

.upload-progress {
  margin-top: 10px;
}

.progress-bar {
  background: rgba(0,0,0,0.1);
  border-radius: 10px;
  height: 8px;
  overflow: hidden;
  margin-bottom: 5px;
}

.progress-fill {
  background: linear-gradient(90deg, #dd7724ff, #f39c12);
  height: 100%;
  border-radius: 10px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.85em;
  color: #666;
  text-align: center;
  display: block;
}

.upload-btn {
  background: rgba(255,255,255,0.9);
  color: #dd7724ff;
  border: none;
  padding: 15px 25px;
  border-radius: 25px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 1.1em;
}

.upload-btn:hover:not(:disabled) {
  background: white;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(255,255,255,0.3);
}

.upload-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.media-title, .media-description, .media-game {
  padding: 15px;
  border: 2px solid rgba(255,255,255,0.3);
  border-radius: 10px;
  background: rgba(255,255,255,0.9);
  color: #121212;
  font-size: 1em;
  transition: all 0.3s;
}

.media-title:focus, .media-description:focus, .media-game:focus {
  outline: none;
  border-color: rgba(255,255,255,0.8);
  background: white;
  box-shadow: 0 0 0 3px rgba(255,255,255,0.2);
}

.media-game {
  cursor: pointer;
}

.media-description {
  min-height: 100px;
  resize: vertical;
  font-family: inherit;
}

.upload-games-error {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 12px;
  margin-top: 8px;
}

.upload-games-error p {
  color: #856404;
  margin: 0;
  font-size: 0.85em;
  text-align: center;
}

.alert {
  padding: 10px;
  border-radius: 5px;
  margin-top: 10px;
}

.alert-danger {
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  color: #721c24;
}

.alert-success {
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
  color: #155724;
}
</style>