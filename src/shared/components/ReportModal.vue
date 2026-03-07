<template>
  <div v-if="isOpen" class="report-modal-overlay" @click.self="closeModal">
    <div class="report-modal">
      <!-- Header -->
      <div class="report-modal-header">
        <h2>Report {{ contentTypeName }}</h2>
        <button class="close-btn" @click="closeModal" aria-label="Close">×</button>
      </div>

      <!-- Content -->
      <div class="report-modal-content">
        <!-- Step 1: Select Category -->
        <div v-if="currentStep === 1" class="report-step">
          <h3>What's the issue?</h3>
          <div class="category-options">
            <button
              v-for="(categoryName, categoryKey) in categories"
              :key="categoryKey"
              class="category-option"
              :class="{ 'selected': selectedCategory === categoryKey }"
              @click="selectCategory(categoryKey)"
            >
              <span class="category-icon">📋</span>
              <span class="category-label">{{ categoryName }}</span>
            </button>
          </div>
          <p v-if="selectedCategory" class="category-help">
            {{ getCategoryDescription(selectedCategory) }}
          </p>
        </div>

        <!-- Step 2: Provide Details -->
        <div v-if="currentStep === 2" class="report-step">
          <h3>Provide Details</h3>
          
          <div class="form-group">
            <label for="description">What happened? (required)</label>
            <textarea
              id="description"
              v-model="reportForm.description"
              placeholder="Please describe the issue in detail..."
              rows="6"
              maxlength="1000"
              required
            ></textarea>
            <span class="char-count">{{ reportForm.description.length }}/1000</span>
          </div>

          <div class="form-group">
            <label for="evidence">Evidence (optional)</label>
            <input
              id="evidence"
              v-model="reportForm.evidence"
              type="text"
              placeholder="URLs, usernames, or other references (comma-separated)"
            />
            <p class="help-text">Include any specific evidence like timestamps, user mentions, or links</p>
          </div>
        </div>

        <!-- Step 3: Review & Confirm -->
        <div v-if="currentStep === 3" class="report-step">
          <h3>Review Your Report</h3>
          
          <div class="review-item">
            <strong>Content Type:</strong>
            <span>{{ contentTypeName }}</span>
          </div>

          <div class="review-item">
            <strong>Category:</strong>
            <span>{{ categories[selectedCategory] }}</span>
          </div>

          <div class="review-item">
            <strong>Details:</strong>
            <p class="review-text">{{ reportForm.description }}</p>
          </div>

          <div v-if="reportForm.evidence" class="review-item">
            <strong>Evidence:</strong>
            <p class="review-text">{{ reportForm.evidence }}</p>
          </div>

          <div class="review-notice">
            <p>
              ⚠️ <strong>Important:</strong> False reports may result in account restrictions.
              Make sure your report is accurate and detailed.
            </p>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="isSubmitting" class="loading-state">
          <div class="spinner"></div>
          <p>Submitting your report...</p>
        </div>

        <!-- Error State -->
        <div v-if="error" class="error-message">
          <p>{{ error }}</p>
          <button class="btn-retry" @click="error = null">Try Again</button>
        </div>

        <!-- Success State -->
        <div v-if="reportSubmitted" class="success-state">
          <div class="success-icon">✓</div>
          <h3>Report Submitted</h3>
          <p>Thank you for helping us keep our community safe.</p>
          <p class="report-id">Report ID: <code>{{ reportId }}</code></p>
          <p class="help-text">You can check the status of your report anytime using this ID.</p>
        </div>
      </div>

      <!-- Footer (Buttons) -->
      <div v-if="!reportSubmitted" class="report-modal-footer">
        <button
          v-if="currentStep > 1 && !isSubmitting"
          class="btn btn-secondary"
          @click="previousStep"
        >
          ← Back
        </button>

        <button
          v-if="currentStep < 3 && !isSubmitting"
          class="btn btn-primary"
          :disabled="!canProceed"
          @click="nextStep"
        >
          Continue →
        </button>

        <button
          v-if="currentStep === 3 && !isSubmitting"
          class="btn btn-danger"
          @click="submitReport"
        >
          Submit Report
        </button>

        <button class="btn btn-cancel" @click="closeModal">
          {{ reportSubmitted ? 'Close' : 'Cancel' }}
        </button>
      </div>

      <!-- Success Footer -->
      <div v-if="reportSubmitted" class="report-modal-footer">
        <button class="btn btn-primary" @click="closeModal">
          Done
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, defineProps, defineEmits } from 'vue'
import { reportingService } from '@/services/reportingService'

const props = defineProps({
  isOpen: {
    type: Boolean,
    default: false
  },
  contentType: {
    type: String,
    required: true,
    validator: (value) => ['POST', 'COMMENT', 'USER', 'MESSAGE'].includes(value)
  },
  contentId: {
    type: String,
    required: true
  },
  userId: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['close', 'submitted'])

const currentStep = ref(1)
const selectedCategory = ref(null)
const isSubmitting = ref(false)
const error = ref(null)
const reportSubmitted = ref(false)
const reportId = ref(null)

const reportForm = ref({
  description: '',
  evidence: ''
})

const categories = computed(() => {
  return reportingService.getCategories(props.contentType) || {}
})

const contentTypeName = computed(() => {
  const names = {
    POST: 'Post',
    COMMENT: 'Comment',
    USER: 'User Profile',
    MESSAGE: 'Message'
  }
  return names[props.contentType] || 'Content'
})

const canProceed = computed(() => {
  if (currentStep.value === 1) {
    return selectedCategory.value !== null
  }
  if (currentStep.value === 2) {
    return reportForm.value.description.trim().length >= 10
  }
  return true
})

function selectCategory(categoryKey) {
  selectedCategory.value = categoryKey
}

function getCategoryDescription(categoryKey) {
  const descriptions = {
    OFFENSIVE: 'Content that is abusive, threatening, or hateful',
    MISLEADING: 'False or misleading information',
    SPAM: 'Repetitive or unsolicited content',
    NSFW: 'Explicit or adult content not appropriate for all users',
    COPYRIGHT: 'Content that violates copyright or intellectual property',
    HARASSMENT: 'Direct attacks on individuals',
    HATE_SPEECH: 'Content attacking people based on protected characteristics',
    FAKE_ACCOUNT: 'Account that appears to be impersonating someone',
    IMPERSONATION: 'Account falsely claiming to be someone',
    SCAM: 'Account involved in scams or fraud',
    SCAM_ACCOUNT: 'Account involved in scams or fraud',
    OTHER: 'Something else not listed above'
  }
  return descriptions[categoryKey] || 'Please provide details about your report'
}

function nextStep() {
  if (canProceed.value) {
    currentStep.value++
  }
}

function previousStep() {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

async function submitReport() {
  isSubmitting.value = true
  error.value = null

  try {
    const evidenceArray = reportForm.value.evidence
      .split(',')
      .map((e) => e.trim())
      .filter((e) => e.length > 0)

    let response

    switch (props.contentType) {
      case 'POST':
        response = await reportingService.reportPost(
          props.contentId,
          selectedCategory.value,
          reportForm.value.description,
          evidenceArray
        )
        break

      case 'COMMENT':
        response = await reportingService.reportComment(
          props.contentId,
          props.userId,
          selectedCategory.value,
          reportForm.value.description,
          evidenceArray
        )
        break

      case 'USER':
        response = await reportingService.reportUser(
          props.contentId,
          selectedCategory.value,
          reportForm.value.description,
          evidenceArray
        )
        break

      case 'MESSAGE':
        response = await reportingService.reportMessage(
          props.contentId,
          props.userId,
          selectedCategory.value,
          reportForm.value.description,
          evidenceArray
        )
        break

      default:
        throw new Error('Invalid content type')
    }

    reportId.value = response.reportId || response.id
    reportSubmitted.value = true
    emit('submitted', response)
  } catch (err) {
    error.value = err.message || 'Failed to submit report. Please try again.'
  } finally {
    isSubmitting.value = false
  }
}

function closeModal() {
  emit('close')
  // Reset form
  setTimeout(() => {
    currentStep.value = 1
    selectedCategory.value = null
    reportForm.value = {
      description: '',
      evidence: ''
    }
    error.value = null
    reportSubmitted.value = false
    reportId.value = null
  }, 300)
}
</script>

<style scoped>
.report-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.report-modal {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.report-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  background: #f9f9f9;
}

.report-modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #e0e0e0;
  color: #333;
}

.report-modal-content {
  padding: 30px;
  min-height: 300px;
  display: flex;
  flex-direction: column;
}

.report-step h3 {
  margin: 0 0 20px 0;
  font-size: 1.2rem;
  color: #333;
}

.category-options {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-bottom: 20px;
}

.category-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px;
  border: 2px solid #ddd;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.category-option:hover {
  border-color: #dd7724ff;
  background: #fff8f5;
}

.category-option.selected {
  border-color: #dd7724ff;
  background: #fff8f5;
  box-shadow: 0 2px 8px rgba(221, 119, 36, 0.2);
}

.category-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.category-label {
  font-weight: 500;
  color: #333;
}

.category-help {
  padding: 12px;
  background: #e8f4f8;
  border-left: 3px solid #0288d1;
  border-radius: 4px;
  font-size: 0.9rem;
  color: #0288d1;
  margin: 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group textarea,
.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: inherit;
  font-size: 1rem;
  transition: all 0.2s;
}

.form-group textarea:focus,
.form-group input:focus {
  outline: none;
  border-color: #dd7724ff;
  box-shadow: 0 0 0 3px rgba(221, 119, 36, 0.1);
}

.char-count {
  display: block;
  text-align: right;
  font-size: 0.85rem;
  color: #999;
  margin-top: 4px;
}

.help-text {
  font-size: 0.85rem;
  color: #666;
  margin: 4px 0 0 0;
}

.review-item {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.review-item strong {
  display: block;
  color: #333;
  margin-bottom: 4px;
}

.review-item span,
.review-text {
  color: #666;
  line-height: 1.5;
}

.review-notice {
  padding: 12px;
  background: #fff3cd;
  border-left: 3px solid #ffc107;
  border-radius: 4px;
  margin-top: 20px;
}

.review-notice p {
  margin: 0;
  color: #856404;
  font-size: 0.9rem;
}

.loading-state,
.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f0f0f0;
  border-top-color: #dd7724ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.success-icon {
  font-size: 3rem;
  color: #4caf50;
  margin-bottom: 16px;
}

.success-state h3 {
  margin: 0 0 8px 0;
  color: #333;
  font-size: 1.3rem;
}

.success-state p {
  margin: 8px 0;
  color: #666;
  font-size: 0.95rem;
}

.report-id {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
  font-family: monospace;
  margin-top: 16px;
}

.report-id code {
  color: #dd7724ff;
  font-weight: 500;
}

.error-message {
  padding: 16px;
  background: #ffebee;
  border-left: 3px solid #f44336;
  border-radius: 4px;
  color: #c62828;
  margin-bottom: 16px;
}

.error-message p {
  margin: 0 0 12px 0;
}

.btn-retry {
  background: #f44336;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
}

.btn-retry:hover {
  background: #d32f2f;
}

.report-modal-footer {
  display: flex;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #eee;
  background: #f9f9f9;
  justify-content: flex-end;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #dd7724ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c56a1e;
  box-shadow: 0 2px 8px rgba(221, 119, 36, 0.3);
}

.btn-secondary {
  background: #e0e0e0;
  color: #333;
}

.btn-secondary:hover:not(:disabled) {
  background: #d0d0d0;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #d32f2f;
  box-shadow: 0 2px 8px rgba(244, 67, 54, 0.3);
}

.btn-cancel {
  background: white;
  color: #333;
  border: 1px solid #ddd;
}

.btn-cancel:hover:not(:disabled) {
  background: #f9f9f9;
  border-color: #999;
}

/* Mobile Responsive */
@media (max-width: 600px) {
  .report-modal {
    width: 95%;
    max-height: 95vh;
  }

  .report-modal-header {
    padding: 16px;
  }

  .report-modal-content {
    padding: 16px;
  }

  .report-modal-footer {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }

  .category-options {
    grid-template-columns: 1fr;
  }
}
</style>
