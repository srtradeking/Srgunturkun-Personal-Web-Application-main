/**
 * Reporting Service
 * Handles user reports for posts, comments, users, and messages
 * 
 * Features:
 * - Report creation and submission
 * - Report history tracking
 * - Multiple report categories
 * - Report status tracking
 */

import { apiRequest } from './apiService'

const REPORT_CATEGORIES = {
  POST: {
    OFFENSIVE: 'Offensive Content',
    MISLEADING: 'Misleading Information',
    SPAM: 'Spam',
    NSFW: 'NSFW Content',
    COPYRIGHT: 'Copyright Violation',
    OTHER: 'Other'
  },
  COMMENT: {
    HARASSMENT: 'Harassment',
    HATE_SPEECH: 'Hate Speech',
    SPAM: 'Spam',
    OFFENSIVE: 'Offensive Content',
    OTHER: 'Other'
  },
  USER: {
    FAKE_ACCOUNT: 'Fake Account',
    IMPERSONATION: 'Impersonation',
    SCAM: 'Scam Account',
    HARASSMENT: 'Harassment',
    SPAM: 'Spam Account',
    OTHER: 'Other'
  },
  MESSAGE: {
    SPAM: 'Spam',
    SCAM: 'Scam',
    HARASSMENT: 'Harassment',
    OFFENSIVE: 'Offensive Content',
    OTHER: 'Other'
  }
}

const REPORT_STATUS = {
  PENDING: 'pending',
  UNDER_REVIEW: 'under_review',
  RESOLVED: 'resolved',
  DISMISSED: 'dismissed',
  APPEALED: 'appealed'
}

export const reportingService = {
  /**
   * Submit a report for a post/media
   * @param {string} postId - ID of the post to report
   * @param {string} category - Report category
   * @param {string} description - Detailed description of the issue
   * @param {string[]} evidence - URLs or evidence references
   * @returns {Promise} Report response
   */
  async reportPost(postId, category, description, evidence = []) {
    try {
      const response = await apiRequest('/reports/posts', {
        method: 'POST',
        body: JSON.stringify({
          contentId: postId,
          category,
          description,
          evidence
        })
      })
      return response
    } catch (error) {
      console.error('Error reporting post:', error)
      throw error
    }
  },

  /**
   * Submit a report for a comment
   * @param {string} commentId - ID of the comment to report
   * @param {string} postId - ID of parent post
   * @param {string} category - Report category
   * @param {string} description - Detailed description
   * @param {string[]} evidence - Evidence references
   * @returns {Promise} Report response
   */
  async reportComment(commentId, postId, category, description, evidence = []) {
    try {
      const response = await apiRequest('/reports/comments', {
        method: 'POST',
        body: JSON.stringify({
          contentId: commentId,
          category,
          description,
          evidence
        })
      })
      return response
    } catch (error) {
      console.error('Error reporting comment:', error)
      throw error
    }
  },

  /**
   * Submit a report for a user profile
   * @param {string} userId - UID of reported user
   * @param {string} category - Report category
   * @param {string} description - Detailed description
   * @param {string[]} evidence - Evidence references
   * @returns {Promise} Report response
   */
  async reportUser(userId, category, description, evidence = []) {
    try {
      const response = await apiRequest('/reports/users', {
        method: 'POST',
        body: JSON.stringify({
          contentId: userId,
          category,
          description,
          evidence
        })
      })
      return response
    } catch (error) {
      console.error('Error reporting user:', error)
      throw error
    }
  },

  /**
   * Submit a report for a message
   * @param {string} messageId - ID of the message
   * @param {string} senderId - UID of sender
   * @param {string} category - Report category
   * @param {string} description - Detailed description
   * @param {string[]} evidence - Evidence references
   * @returns {Promise} Report response
   */
  async reportMessage(messageId, senderId, category, description, evidence = []) {
    try {
      const response = await apiRequest('/reports/messages', {
        method: 'POST',
        body: JSON.stringify({
          contentId: messageId,
          category,
          description,
          evidence
        })
      })
      return response
    } catch (error) {
      console.error('Error reporting message:', error)
      throw error
    }
  },

  /**
   * Get user's report history
   * @param {number} userId - Backend user ID
   * @param {number} limit - Number of reports to fetch
   * @returns {Promise} User's reports
   */
  async getUserReports(userId, limit = 20) {
    try {
      const response = await apiRequest(`/reports/user/${userId}?limit=${limit}`)
      return response
    } catch (error) {
      console.error('Error fetching user reports:', error)
      throw error
    }
  },

  /**
   * Get report details (for report status checking)
   * @param {string} reportId - ID of the report
   * @returns {Promise} Report details
   */
  async getReportStatus(reportId) {
    try {
      const response = await apiRequest(`/reports/${reportId}`)
      return response
    } catch (error) {
      console.error('Error fetching report status:', error)
      throw error
    }
  },

  /**
   * Appeal a report decision (user appeals if report was dismissed)
   * @param {string} reportId - ID of the report
   * @param {string} appealDescription - Why the user is appealing
   * @returns {Promise} Appeal response
   */
  async appealReport(reportId, appealDescription) {
    try {
      const response = await apiRequest(`/reports/${reportId}/appeal`, {
        method: 'POST',
        body: JSON.stringify({
          appealDescription,
          appealedAt: new Date().toISOString()
        })
      })
      return response
    } catch (error) {
      console.error('Error appealing report:', error)
      throw error
    }
  },

  /**
   * Get report categories for different content types
   * @param {string} contentType - Type of content (POST, COMMENT, USER, MESSAGE)
   * @returns {Object} Categories for that content type
   */
  getCategories(contentType) {
    return REPORT_CATEGORIES[contentType] || {}
  },

  /**
   * Get all report category types
   * @returns {Object} All categories
   */
  getAllCategories() {
    return REPORT_CATEGORIES
  },

  /**
   * Get report status types
   * @returns {Object} Status types
   */
  getStatusTypes() {
    return REPORT_STATUS
  },

  /**
   * Check if user can report (prevent spam)
   * @param {number|string} userId - Backend user ID
   * @param {string} contentId - ID of content being reported
   * @returns {Promise<boolean>} True if user can report
   */
  async canUserReport(userId, contentId) {
    try {
      const response = await apiRequest(`/reports/can-report/${userId}/${contentId}`)
      return response.canReport
    } catch (error) {
      console.warn('Could not verify reporting eligibility:', error)
      return true // Allow if check fails
    }
  }
}
