package com.webapp.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * JSON Security Validator
 * 
 * Validates JSON payloads for security threats:
 * - Prototype pollution via __proto__, constructor, prototype
 * - Dangerous property names
 * - Nested object depth (DoS prevention)
 * - Array size limits (DoS prevention)
 */
@Component
public class JsonSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(JsonSecurityValidator.class);

    @Autowired
    private JavaScriptSecurityValidator jsSecurityValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Security limits
    private static final int MAX_NESTING_DEPTH = 20;
    private static final int MAX_ARRAY_SIZE = 10000;
    private static final int MAX_STRING_LENGTH = 100000;

    // Dangerous property names
    private static final String[] DANGEROUS_PROPERTIES = {
        "__proto__",
        "constructor",
        "prototype",
        "__defineGetter__",
        "__defineSetter__",
        "__lookupGetter__",
        "__lookupSetter__"
    };

    /**
     * Validate JSON string for security threats
     */
    public void validateJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }

        try {
            // Parse JSON
            JsonNode rootNode = objectMapper.readTree(jsonString);

            // Validate the JSON structure
            validateJsonNode(rootNode, 0);

        } catch (Exception e) {
            log.error("Error parsing JSON for validation", e);
            throw new SecurityException("Invalid JSON format");
        }
    }

    /**
     * Validate JSON node recursively
     */
    private void validateJsonNode(JsonNode node, int depth) {
        // Check nesting depth
        if (depth > MAX_NESTING_DEPTH) {
            log.error("JSON nesting depth exceeds maximum: {}", depth);
            throw new SecurityException("JSON nesting too deep (max " + MAX_NESTING_DEPTH + ")");
        }

        if (node.isObject()) {
            // Validate object properties
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                // Check for dangerous property names
                validatePropertyName(fieldName);

                // Check for prototype pollution
                jsSecurityValidator.validatePrototypePollution(fieldName);

                // Recursively validate child nodes
                JsonNode childNode = node.get(fieldName);
                validateJsonNode(childNode, depth + 1);
            }

        } else if (node.isArray()) {
            // Check array size
            if (node.size() > MAX_ARRAY_SIZE) {
                log.error("JSON array size exceeds maximum: {}", node.size());
                throw new SecurityException("JSON array too large (max " + MAX_ARRAY_SIZE + ")");
            }

            // Validate array elements
            for (JsonNode element : node) {
                validateJsonNode(element, depth + 1);
            }

        } else if (node.isTextual()) {
            // Validate string values
            String textValue = node.asText();

            // Check string length
            if (textValue.length() > MAX_STRING_LENGTH) {
                log.error("JSON string length exceeds maximum: {}", textValue.length());
                throw new SecurityException("JSON string too long (max " + MAX_STRING_LENGTH + ")");
            }

            // Check for dangerous JavaScript patterns
            if (jsSecurityValidator.containsDangerousJavaScript(textValue)) {
                log.error("Dangerous JavaScript in JSON value");
                throw new SecurityException("Dangerous JavaScript in JSON");
            }

            // Check for template injection
            if (jsSecurityValidator.containsTemplateInjection(textValue)) {
                log.error("Template injection in JSON value");
                throw new SecurityException("Template injection in JSON");
            }
        }
    }

    /**
     * Validate property name
     */
    private void validatePropertyName(String propertyName) {
        if (propertyName == null) {
            return;
        }

        // Check for dangerous property names
        for (String dangerous : DANGEROUS_PROPERTIES) {
            if (propertyName.equalsIgnoreCase(dangerous)) {
                log.error("Dangerous property name in JSON: {}", propertyName);
                throw new SecurityException("Property name not allowed: " + propertyName);
            }
        }

        // Check for bracket notation attempts
        if (propertyName.contains("[") || propertyName.contains("]")) {
            log.warn("Suspicious property name with brackets: {}", propertyName);
        }
    }

    /**
     * Validate JSON for prototype pollution (quick check)
     */
    public void quickValidatePrototypePollution(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }

        // Quick string-based check
        String lowerJson = jsonString.toLowerCase();

        if (lowerJson.contains("\"__proto__\"") ||
            lowerJson.contains("'__proto__'") ||
            lowerJson.contains("\"constructor\"") ||
            lowerJson.contains("\"prototype\"")) {
            
            log.error("Prototype pollution keys detected in JSON");
            throw new SecurityException("Invalid JSON: prototype pollution detected");
        }
    }

    /**
     * Check if JSON contains dangerous patterns
     */
    public boolean containsDangerousPatterns(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return false;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            return checkNodeForDangerousPatterns(rootNode);
        } catch (Exception e) {
            log.error("Error checking JSON for dangerous patterns", e);
            return true; // Assume dangerous if can't parse
        }
    }

    /**
     * Check node for dangerous patterns recursively
     */
    private boolean checkNodeForDangerousPatterns(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                // Check field name
                for (String dangerous : DANGEROUS_PROPERTIES) {
                    if (fieldName.equalsIgnoreCase(dangerous)) {
                        return true;
                    }
                }

                // Check child nodes
                if (checkNodeForDangerousPatterns(node.get(fieldName))) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                if (checkNodeForDangerousPatterns(element)) {
                    return true;
                }
            }
        } else if (node.isTextual()) {
            String textValue = node.asText();
            if (jsSecurityValidator.containsDangerousJavaScript(textValue)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sanitize JSON by removing dangerous properties
     */
    public String sanitizeJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return jsonString;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode sanitizedNode = sanitizeJsonNode(rootNode);
            return objectMapper.writeValueAsString(sanitizedNode);
        } catch (Exception e) {
            log.error("Error sanitizing JSON", e);
            return jsonString;
        }
    }

    /**
     * Sanitize JSON node by removing dangerous properties
     */
    private JsonNode sanitizeJsonNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<String> fieldNames = node.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();

                // Remove dangerous properties
                for (String dangerous : DANGEROUS_PROPERTIES) {
                    if (fieldName.equalsIgnoreCase(dangerous)) {
                        ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(fieldName);
                        log.warn("Removed dangerous property from JSON: {}", fieldName);
                        continue;
                    }
                }

                // Recursively sanitize child nodes
                JsonNode childNode = node.get(fieldName);
                if (childNode != null) {
                    sanitizeJsonNode(childNode);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                sanitizeJsonNode(element);
            }
        }

        return node;
    }
}
