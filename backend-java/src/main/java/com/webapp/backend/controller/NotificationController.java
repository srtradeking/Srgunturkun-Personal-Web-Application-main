package com.webapp.backend.controller;

import com.webapp.backend.dto.NotificationsDTO;
import com.webapp.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Notification REST Controller
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "User notification APIs")
@CrossOrigin(origins = "*")
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	@Operation(summary = "Get notifications for current user")
	public ResponseEntity<?> getMyNotifications() {
		try {
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			Long userId = null;
			if (authentication != null && authentication.isAuthenticated()) {
				Object principal = authentication.getPrincipal();
				if (principal instanceof Long) {
					userId = (Long) principal;
				} else if (principal instanceof String) {
					String principalStr = (String) principal;
					if (principalStr.matches("\\d+")) {
						try { userId = Long.parseLong(principalStr); } catch (NumberFormatException ignored) {}
					}
				}
			}
			
			if (userId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("error", "Could not resolve current user from security context"));
			}
			
			List<NotificationsDTO> notifications = notificationService.getNotificationsForUser(userId);
			return ResponseEntity.ok(notifications);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
		}
	}

	@PostMapping("/{id}/read")
	@Operation(summary = "Mark a notification as read")
	public ResponseEntity<?> markAsRead(@PathVariable Long id) {
		try {
			var authentication = SecurityContextHolder.getContext().getAuthentication();
			Long userId = null;
			if (authentication != null && authentication.isAuthenticated()) {
				Object principal = authentication.getPrincipal();
				if (principal instanceof Long) {
					userId = (Long) principal;
				} else if (principal instanceof String) {
					String principalStr = (String) principal;
					if (principalStr.matches("\\d+")) {
						try { userId = Long.parseLong(principalStr); } catch (NumberFormatException ignored) {}
					}
				}
			}
			
			NotificationsDTO updated = notificationService.markNotificationAsRead(id, userId);
			return ResponseEntity.ok(updated);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", e.getMessage(), "type", e.getClass().getSimpleName()));
		}
	}
}
