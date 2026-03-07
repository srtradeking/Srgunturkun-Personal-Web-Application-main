package com.webapp.backend.service.impl;

import com.webapp.backend.mapper.NotificationMapper;
import com.webapp.backend.dto.NotificationsDTO;
import com.webapp.backend.model.Notification;
import com.webapp.backend.repository.NotificationRepository;
import com.webapp.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationMapper notificationMapper;

	@Override
	@Transactional(readOnly = true)
	public List<NotificationsDTO> getNotificationsForUser(Long userId) {
		if (userId == null) {
			return List.of();
		}
		List<Notification> entities = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
		return notificationMapper.toDtoList(entities);
	}

	@Override
	public NotificationsDTO markNotificationAsRead(Long id, Long userId) {
		Notification notification = notificationRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));

		if (userId != null && !userId.equals(notification.getUserId())) {
			throw new RuntimeException("Cannot modify notification that does not belong to current user");
		}

		if (!Boolean.TRUE.equals(notification.getIsRead())) {
			notification.setIsRead(true);
			notification.setUpdatedAt(LocalDateTime.now());
			notification = notificationRepository.save(notification);
		}

		return notificationMapper.toDto(notification);
	}

	@Override
	@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
	public void createNotification(Notification notification) {
		try {
			notificationRepository.save(notification);
		} catch (Exception e) {
			log.error("Failed to save notification: {}", e.getMessage());
			throw e;
		}
	}
}
