package com.webapp.backend.service;

import com.webapp.backend.dto.NotificationsDTO;

import java.util.List;

/**
 * Notification Service Interface
 */
public interface NotificationService {

	List<NotificationsDTO> getNotificationsForUser(Long userId);

	NotificationsDTO markNotificationAsRead(Long id, Long userId);

    void createNotification(com.webapp.backend.model.Notification notification);
}
