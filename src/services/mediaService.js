// src/services/mediaService.js
import { storageAPI, postsAPI, profilesAPI } from '@/services/apiService'

const MAX_SIZE_MB = 100
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024

const ALLOWED_POST_TYPES = [
  'image/png',
  'image/jpeg',
  'image/webp',
  'video/mp4'
]

const ALLOWED_PROFILE_TYPES = [
  'image/png',
  'image/jpeg',
  'image/webp'
]

function validateFile(file, { allowedTypes, maxSizeBytes }) {
  if (!file) throw new Error('No file selected')
  if (file.size > maxSizeBytes) {
    throw new Error(`File is larger than ${MAX_SIZE_MB} MB`)
  }
  if (!allowedTypes.includes(file.type)) {
    throw new Error('Unsupported file type')
  }
}

function putToSignedUrl(uploadUrl, file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('PUT', uploadUrl)

    // Content-Type MUST match the value used when signing on the backend
    xhr.setRequestHeader('Content-Type', file.type)

    if (xhr.upload && typeof onProgress === 'function') {
      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
          const percent = Math.round((event.loaded / event.total) * 100)
          onProgress(percent)
        }
      }
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`Upload failed with status ${xhr.status}`))
      }
    }

    xhr.onerror = () => reject(new Error('Network error during upload'))
    xhr.send(file)
  })
}


/**
 * Upload media for a post:
 * 1. Create post
 * 2. Get signed URL
 * 3. PUT file to R2
 * 4. Update post with publicUrl (videoUrl/imageUrl)
 * Returns { postId, postData, publicUrl }
 */
export async function uploadPostMedia({
  file,
  title,
  description,
  game,
  onProgress
}) {
  const isVideo = file.type.startsWith('video/')

  validateFile(file, {
    allowedTypes: ALLOWED_POST_TYPES,
    maxSizeBytes: MAX_SIZE_BYTES
  })

  const postData = {
    title,
    content: description,
    game,
    type: isVideo ? 'video' : 'image',
    mimeType: file.type,
    url: '',
    isPublished: true
  }

  // 1) create post
  const created = await postsAPI.createPost(postData)
  const postId = created.id

  // 2) get signed URL
  const signed = await storageAPI.getSignedUploadUrl({
    fileName: file.name,
    mimeType: file.type,
    fileSize: file.size,
    purpose: isVideo ? 'post-video' : 'post-image'
  })

  // 3) upload to R2
  await putToSignedUrl(signed.uploadUrl, file, onProgress)

  // Construct the proxy URL using the objectKey returned by the backend
  // The backend endpoint is /api/storage/{key} or /api/storage/media/{user}/{file}
  // objectKey already contains the full path (e.g. 'media/123/file.mp4')
  const objectKey = signed.objectKey
  const publicUrl = signed.publicUrl || (objectKey ? `/api/storage/${objectKey}` : '')

  // 4) update post URL
  if (isVideo) {
    await postsAPI.updatePost(postId, { videoUrl: publicUrl })
    postData.videoUrl = publicUrl
  } else {
    await postsAPI.updatePost(postId, { imageUrl: publicUrl })
    postData.imageUrl = publicUrl
  }

  return { postId, postData, publicUrl }
}

/**
 * Upload profile photo:
 * 1. Get signed URL
 * 2. PUT file to R2
 * 3. Update profile with publicUrl
 * Returns publicUrl
 */
export async function uploadProfilePhoto({
  file,
  userId,
  onProgress
}) {
  validateFile(file, {
    allowedTypes: ALLOWED_PROFILE_TYPES,
    maxSizeBytes: MAX_SIZE_BYTES
  })

  const signed = await storageAPI.getSignedUploadUrl({
    fileName: file.name,
    mimeType: file.type,
    fileSize: file.size,
    purpose: 'profile-photo'
  })

  await putToSignedUrl(signed.uploadUrl, file, onProgress)

  const objectKey = signed.objectKey
  const publicUrl = signed.publicUrl || (objectKey ? `/api/storage/${objectKey}` : '')
  
  await profilesAPI.updateProfilePhoto(userId, publicUrl)

  return publicUrl
}