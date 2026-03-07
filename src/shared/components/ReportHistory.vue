<template>
  <div class="report-history-container">
    <div class="report-history-header">
      <h2>📋 My Reports</h2>
      <p class="subtitle">Track the status of your submitted reports</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading your reports...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="reports.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <h3>No Reports Yet</h3>
      <p>You haven't submitted any reports.</p>
      <p class="help-text">Help us keep the community safe by reporting inappropriate content.</p>
    </div>

    <!-- Reports List -->
    <div v-else class="reports-list">
      <div
        v-for="report in reports"
        :key="report.id"
        class="report-card"
        :class="{ 'expanded': expandedReportId === report.id }"
      >
        <!-- Report Header -->
        <div class="report-header" @click="toggleExpanded(report.id)">
          <div class="report-meta">
            <div class="report-info">
              <h3 class="report-title">
                {{ getContentTypeName(report.type) }} - {{ getCategoryName(report.type, report.category) }}
              </h3>
              <p class="report-date">{{ formatDate(report.reportedAt) }}</p>
            </div>
            <div class="report-status">
              <span class="status-badge" :class="report.status">
                {{ formatStatus(report.status) }}
              </span>
            </div>
          </div>
          <div class="expand-icon">
            {{ expandedReportId === report.id ? '▲' : '▼' }}
          </div>
        </div>

        <!-- Report Details (Expanded) -->
        <div v-if="expandedReportId === report.id" class="report-details">
          <div class="detail-row">
            <strong>Report ID:</strong>
            <code>{{ report.id }}</code>
          </div>

          <div class="detail-row">
            <strong>Content Type:</strong>
            <span>{{ getContentTypeName(report.type) }}</span>
          </div>

          <div class="detail-row">
            <strong>Category:</strong>
            <span>{{ getCategoryName(report.type, report.category) }}</span>
          </div>

          <div class="detail-row">
            <strong>Description:</strong>
            <p class="detail-text">{{ report.description }}</p>
          </div>

          <div v-if="report.evidence && report.evidence.length > 0" class="detail-row">
            <strong>Evidence:</strong>
            <ul class="evidence-list">
              <li v-for="(item, index) in report.evidence" :key="index">{{ item }}</li>
            </ul>
          </div>

          <div class="detail-row">
            <strong>Submitted:</strong>
            <span>{{ formatDateTime(report.reportedAt) }}</span>
          </div>

          <div class="detail-row">
            <strong>Status:</strong>
            <span class="status-detail">
              <span class="status-badge" :class="report.status">
                {{ formatStatus(report.status) }}
              </span>
            </span>
          </div>

          <!-- Status-specific information -->
          <div v-if="report.status === 'under_review'" class="status-info info">
            <p>⏳ Your report is being reviewed by our moderation team. This typically takes 24-48 hours.</p>
          </div>

          <div v-else-if="report.status === 'resolved'" class="status-info success">
            <p>✓ Your report has been resolved. Action was taken on the reported content.</p>
            <div v-if="report.resolution" class="resolution-details">
              <strong>Resolution:</strong>
              <p>{{ report.resolution }}</p>
            </div>
          </div>

          <div v-else-if="report.status === 'dismissed'" class="status-info warning">
            <p>ℹ️ Your report was reviewed and dismissed because it didn't meet our community guidelines.</p>
            <div v-if="report.dismissalReason" class="dismissal-details">
              <strong>Reason:</strong>
              <p>{{ report.dismissalReason }}</p>
            </div>
            <button v-if="!report.appealed" class="btn-appeal" @click="openAppeal(report.id)">
              Appeal Decision
            </button>
            <div v-if="report.appealed" class="appeal-badge">
              Appeal Submitted - Under Review
            </div>
          </div>

          <div v-else-if="report.status === 'appealed'" class="status-info info">
            <p>🔄 Your appeal has been submitted and is under review.</p>
          </div>

          <div v-else-if="report.status === 'pending'" class="status-info">
            <p>⏳ Your report is pending review. We'll get to it soon.</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Appeal Modal -->
    <div v-if="showAppealModal" class="appeal-modal-overlay" @click.self="closeAppeal">
      <div class="appeal-modal">
        <div class="modal-header">
          <h3>Appeal Decision</h3>
          <button class="close-btn" @click="closeAppeal">×</button>
        </div>

        <div class="modal-content">
          <p>Why do you think our decision was incorrect? Please provide additional context or evidence.</p>
          <textarea
            v-model="appealForm.description"
            placeholder="Explain your appeal..."
            rows="6"
            maxlength="500"
          ></textarea>
          <span class="char-count">{{ appealForm.description.length }}/500</span>
        </div>

        <div class="modal-footer">
          <button class="btn btn-secondary" @click="closeAppeal">Cancel</button>
          <button
            class="btn btn-primary"
            :disabled="!appealForm.description.trim() || appealSubmitting"
            @click="submitAppeal"
          >
            {{ appealSubmitting ? 'Submitting...' : 'Submit Appeal' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div v-if="toastMessage" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineProps } from 'vue'
import { reportingService } from '@/services/reportingService'

const reports = ref([])
const loading = ref(true)
const expandedReportId = ref(null)
const showAppealModal = ref(false)
const selectedReportForAppeal = ref(null)
const appealSubmitting = ref(false)
const toastMessage = ref(null)
const toastType = ref('success')

const appealForm = ref({
  description: ''
})

const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

onMounted(() => {
  loadReports()
})

async function loadReports() {
  loading.value = true
  try {
    const data = await reportingService.getUserReports(props.userId, 50)
    reports.value = Array.isArray(data) ? data : data.reports || []
  } catch (error) {
    console.error('Error loading reports:', error)
    showToast('Failed to load reports', 'error')
  } finally {
    loading.value = false
  }
}

function toggleExpanded(reportId) {
  expandedReportId.value = expandedReportId.value === reportId ? null : reportId
}

function getContentTypeName(type) {
  const names = {
    POST: '📸 Post',
    COMMENT: '💬 Comment',
    USER: '👤 User Profile',
    MESSAGE: '✉️ Message'
  }
  return names[type] || type
}

function getCategoryName(type, category) {
  return reportingService.getCategories(type)[category] || category
}

function formatDate(dateString) {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 60) {
    return `${diffMins} minute${diffMins !== 1 ? 's' : ''} ago`
  } else if (diffHours < 24) {
    return `${diffHours} hour${diffHours !== 1 ? 's' : ''} ago`
  } else if (diffDays < 7) {
    return `${diffDays} day${diffDays !== 1 ? 's' : ''} ago`
  } else {
    return date.toLocaleDateString()
  }
}

function formatDateTime(dateString) {
  const date = new Date(dateString)
  return date.toLocaleString()
}

function formatStatus(status) {
  const statuses = {
    pending: '⏳ Pending',
    under_review: '👀 Under Review',
    resolved: '✓ Resolved',
    dismissed: 'ℹ️ Dismissed',
    appealed: '🔄 Appealed'
  }
  return statuses[status] || status
}

function openAppeal(reportId) {
  selectedReportForAppeal.value = reportId
  showAppealModal.value = true
}

function closeAppeal() {
  showAppealModal.value = false
  selectedReportForAppeal.value = null
  appealForm.value.description = ''
}

async function submitAppeal() {
  if (!selectedReportForAppeal.value || !appealForm.value.description.trim()) {
    return
  }

  appealSubmitting.value = true
  try {
    await reportingService.appealReport(
      selectedReportForAppeal.value,
      appealForm.value.description
    )
    showToast('Appeal submitted successfully', 'success')
    closeAppeal()
    await loadReports()
  } catch (error) {
    console.error('Error submitting appeal:', error)
    showToast('Failed to submit appeal', 'error')
  } finally {
    appealSubmitting.value = false
  }
}

function showToast(message, type = 'success') {
  toastMessage.value = message
  toastType.value = type
  setTimeout(() => {
    toastMessage.value = null
  }, 4000)
}
</script>

<style scoped>
.report-history-container {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.report-history-header {
  margin-bottom: 30px;
}

.report-history-header h2 {
  margin: 0 0 8px 0;
  font-size: 1.8rem;
  color: #333;
}

.subtitle {
  margin: 0;
  color: #666;
  font-size: 1rem;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: white;
  border-radius: 8px;
  text-align: center;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f0f0f0;
  border-top-color: #dd7724ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}

.empty-state h3 {
  margin: 0 0 8px 0;
  font-size: 1.3rem;
  color: #333;
}

.empty-state p {
  margin: 8px 0;
  color: #666;
}

.help-text {
  font-size: 0.9rem;
  color: #999 !important;
}

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  transition: all 0.3s;
}

.report-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.report-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  user-select: none;
  background: #fafafa;
  border-bottom: 1px solid #eee;
  transition: all 0.2s;
}

.report-header:hover {
  background: #f0f0f0;
}

.report-meta {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.report-info {
  flex: 1;
}

.report-title {
  margin: 0 0 4px 0;
  font-size: 1.05rem;
  color: #333;
  font-weight: 600;
}

.report-date {
  margin: 0;
  font-size: 0.85rem;
  color: #999;
}

.report-status {
  flex-shrink: 0;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge.pending {
  background: #e3f2fd;
  color: #1976d2;
}

.status-badge.under_review {
  background: #fff3e0;
  color: #f57c00;
}

.status-badge.resolved {
  background: #e8f5e9;
  color: #388e3c;
}

.status-badge.dismissed {
  background: #f3e5f5;
  color: #7b1fa2;
}

.status-badge.appealed {
  background: #e0f2f1;
  color: #00796b;
}

.expand-icon {
  font-size: 1.2rem;
  color: #999;
  transition: all 0.2s;
  margin-left: 12px;
}

.report-card.expanded .expand-icon {
  color: #dd7724ff;
}

.report-details {
  padding: 20px;
  border-top: 1px solid #eee;
  background: white;
  animation: expandDown 0.3s ease;
}

@keyframes expandDown {
  from {
    opacity: 0;
    max-height: 0;
  }
  to {
    opacity: 1;
    max-height: 1000px;
  }
}

.detail-row {
  margin-bottom: 16px;
}

.detail-row strong {
  display: block;
  color: #333;
  margin-bottom: 4px;
  font-weight: 600;
}

.detail-row span,
.detail-text {
  color: #666;
  line-height: 1.6;
}

.detail-row code {
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: monospace;
  color: #dd7724ff;
}

.evidence-list {
  margin: 4px 0 0 0;
  padding-left: 20px;
}

.evidence-list li {
  color: #666;
  margin-bottom: 4px;
}

.status-info {
  padding: 12px;
  border-radius: 6px;
  margin-top: 16px;
  border-left: 4px solid;
}

.status-info p {
  margin: 0;
  color: inherit;
  font-size: 0.95rem;
}

.status-info.info {
  background: #e3f2fd;
  border-color: #1976d2;
  color: #1976d2;
}

.status-info.success {
  background: #e8f5e9;
  border-color: #388e3c;
  color: #388e3c;
}

.status-info.warning {
  background: #fff3e0;
  border-color: #f57c00;
  color: #f57c00;
}

.resolution-details,
.dismissal-details {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.resolution-details strong,
.dismissal-details strong {
  display: block;
  margin-bottom: 4px;
}

.resolution-details p,
.dismissal-details p {
  margin: 0;
  font-size: 0.9rem;
}

.appeal-badge {
  display: inline-block;
  padding: 6px 12px;
  background: #e0f2f1;
  color: #00796b;
  border-radius: 4px;
  font-size: 0.85rem;
  margin-top: 12px;
}

.btn-appeal {
  padding: 8px 16px;
  margin-top: 12px;
  background: #dd7724ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-appeal:hover {
  background: #c56a1e;
}

/* Appeal Modal */
.appeal-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.appeal-modal {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  padding: 0;
}

.close-btn:hover {
  color: #333;
}

.modal-content {
  padding: 20px;
}

.modal-content p {
  margin: 0 0 12px 0;
  color: #666;
}

.modal-content textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-family: inherit;
  font-size: 0.95rem;
  resize: vertical;
}

.modal-content textarea:focus {
  outline: none;
  border-color: #dd7724ff;
  box-shadow: 0 0 0 3px rgba(221, 119, 36, 0.1);
}

.char-count {
  display: block;
  text-align: right;
  font-size: 0.8rem;
  color: #999;
  margin-top: 4px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #eee;
  background: #fafafa;
  justify-content: flex-end;
}

.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: #e0e0e0;
  color: #333;
}

.btn-secondary:hover {
  background: #d0d0d0;
}

.btn-primary {
  background: #dd7724ff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #c56a1e;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Toast Notification */
.toast {
  position: fixed;
  bottom: 20px;
  right: 20px;
  padding: 16px 20px;
  border-radius: 6px;
  font-size: 0.95rem;
  font-weight: 500;
  animation: slideIn 0.3s ease;
  z-index: 1001;
}

@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast.success {
  background: #4caf50;
  color: white;
}

.toast.error {
  background: #f44336;
  color: white;
}

/* Mobile Responsive */
@media (max-width: 600px) {
  .report-history-container {
    padding: 12px;
  }

  .report-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .report-meta {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .expand-icon {
    margin-left: 0;
    align-self: flex-end;
  }

  .appeal-modal {
    width: 95%;
  }

  .modal-footer {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }

  .toast {
    right: 12px;
    left: 12px;
  }
}
</style>
