import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { gamesAPI, postsAPI, commentsAPI, profilesAPI } from '@/services/apiService'

// Games Store
export const useGameStore = defineStore('games', () => {
  const games = ref([])
  const selectedGame = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const gameCount = computed(() => games.value.length)

  const gameCategories = computed(() => {
    const categories = new Set()
    games.value.forEach(game => {
      if (game.name) categories.add(game.name)
    })
    return Array.from(categories).sort()
  })

  const setGames = (newGames) => {
    games.value = newGames
  }

  const setSelectedGame = (game) => {
    selectedGame.value = game
  }

  const setLoading = (loading) => {
    isLoading.value = loading
  }

  const setError = (err) => {
    error.value = err
  }

  const fetchGames = async () => {
    try {
      setLoading(true)
      setError(null)
      const fetchedGames = await gamesAPI.getActiveGames()
      setGames(fetchedGames || [])
      return fetchedGames
    } catch (err) {
      console.error('Error fetching games:', err)
      setError(err.message || 'Failed to fetch games')
      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    games,
    selectedGame,
    isLoading,
    error,
    gameCount,
    gameCategories,
    setGames,
    setSelectedGame,
    setLoading,
    setError,
    fetchGames
  }
})

// Posts Store
export const usePostStore = defineStore('posts', () => {
  const posts = ref([])
  const selectedPost = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const postCount = computed(() => posts.value.length)

  const setPosts = (newPosts) => {
    posts.value = newPosts
  }

  const setSelectedPost = (post) => {
    selectedPost.value = post
  }

  const setLoading = (loading) => {
    isLoading.value = loading
  }

  const setError = (err) => {
    error.value = err
  }

  const addReactionToPost = async (postId, emoji, userId) => {
    try {
      setLoading(true)
      setError(null)
      const updatedPost = await postsAPI.reactToPost(postId, { emoji, userId })
      return updatedPost
    } catch (err) {
      console.error('Error adding reaction to post:', err)
      setError(err.message || 'Failed to add reaction')
      throw err
    } finally {
      setLoading(false)
    }
  }

  const removePost = async (postId) => {
    try {
      setLoading(true)
      setError(null)
      await postsAPI.deletePost(postId)
    } catch (err) {
      console.error('Error removing post:', err)
      setError(err.message || 'Failed to remove post')
      throw err
    } finally {
      setLoading(false)
    }
  }

  const hidePost = async (postId) => {
    try {
      setLoading(true)
      setError(null)
      await postsAPI.updatePost(postId, { isHidden: true })
    } catch (err) {
      console.error('Error hiding post:', err)
      setError(err.message || 'Failed to hide post')
      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    posts,
    selectedPost,
    isLoading,
    error,
    postCount,
    setPosts,
    setSelectedPost,
    setLoading,
    setError,
    addReactionToPost,
    removePost,
    hidePost
  }
})

// Comments Store
export const useCommentStore = defineStore('comments', () => {
  const comments = ref([])
  const selectedComment = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const commentCount = computed(() => comments.value.length)

  const setComments = (newComments) => {
    comments.value = newComments
  }

  const setSelectedComment = (comment) => {
    selectedComment.value = comment
  }

  const setLoading = (loading) => {
    isLoading.value = loading
  }

  const setError = (err) => {
    error.value = err
  }

  const createComment = async (commentData) => {
    try {
      setLoading(true)
      setError(null)
      const newComment = await commentsAPI.createComment(commentData)
      comments.value.push(newComment)
      return newComment
    } catch (err) {
      console.error('Error creating comment:', err)
      setError(err.message || 'Failed to create comment')
      throw err
    } finally {
      setLoading(false)
    }
  }

  const fetchCommentReplies = async (commentId) => {
    try {
      setLoading(true)
      setError(null)
      const replies = await commentsAPI.getCommentReplies(commentId)
      return replies
    } catch (err) {
      console.error('Error fetching comment replies:', err)
      setError(err.message || 'Failed to fetch comment replies')
      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    comments,
    selectedComment,
    isLoading,
    error,
    commentCount,
    setComments,
    setSelectedComment,
    setLoading,
    setError,
    createComment,
    fetchCommentReplies
  }
})

// Profile Store
export const useProfileStore = defineStore('profile', () => {
  const profiles = ref({})
  const profile = ref(null)
  const isLoading = ref(false)
  const error = ref(null)

  const isProfileSet = computed(() => profile.value !== null)

  const setProfile = (newProfile) => {
    profile.value = newProfile
  }

  const setLoading = (loading) => {
    isLoading.value = loading
  }

  const setError = (err) => {
    error.value = err
  }

  const setProfiles = (newProfiles) => {
    profiles.value = newProfiles
  }

  const fetchProfileById = async (userId) => {
    try {
      setLoading(true)
      setError(null)
      const profileData = await profilesAPI.getProfileByUserId(userId)
      profiles.value[userId] = profileData
      return profileData
    } catch (err) {
      console.error('Error fetching profile by ID:', err)
      setError(err.message || 'Failed to fetch profile')
      throw err
    } finally {
      setLoading(false)
    }
  }

  return {
    profiles,
    profile,
    isLoading,
    error,
    isProfileSet,
    setProfile,
    setProfiles,
    setLoading,
    setError,
    fetchProfileById
  }
})
