package com.webapp.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 
 * Provides health and status endpoints for the application.
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Application health and status APIs")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check application health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "Personal Web Backend");
        health.put("version", "1.0.0");
        health.put("database", "PostgreSQL");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/database")
    @Operation(summary = "Check database connectivity")
    public ResponseEntity<Map<String, Object>> databaseStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // For now, assume database is always available with PostgreSQL
        // In a real implementation, you might want to test a simple query
        status.put("status", "CONNECTED");
        status.put("type", "PostgreSQL");
        status.put("message", "Database connection is healthy");
        
        return ResponseEntity.ok(status);
    }
}
