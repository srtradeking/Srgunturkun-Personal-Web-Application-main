package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * JavaScript Security Validator
 * 
 * Validates and detects dangerous JavaScript patterns:
 * - eval() and Function() constructor
 * - setTimeout/setInterval with strings
 * - document.write()
 * - innerHTML assignments
 * - Dangerous DOM APIs
 * - Prototype pollution
 * - Event handler injection
 */
@Component
public class JavaScriptSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(JavaScriptSecurityValidator.class);

    // Dangerous JavaScript functions
    private static final Pattern[] DANGEROUS_JS_PATTERNS = {
        // eval and Function constructor
        Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bFunction\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("new\\s+Function\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // setTimeout/setInterval with string
        Pattern.compile("setTimeout\\s*\\(\\s*['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("setInterval\\s*\\(\\s*['\"]", Pattern.CASE_INSENSITIVE),
        
        // document.write
        Pattern.compile("document\\.write", Pattern.CASE_INSENSITIVE),
        Pattern.compile("document\\.writeln", Pattern.CASE_INSENSITIVE),
        
        // innerHTML and outerHTML
        Pattern.compile("\\.innerHTML\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.outerHTML\\s*=", Pattern.CASE_INSENSITIVE),
        
        // insertAdjacentHTML
        Pattern.compile("\\.insertAdjacentHTML\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // execScript
        Pattern.compile("\\bexecScript\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // location manipulation
        Pattern.compile("location\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("location\\.href\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("location\\.replace\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("location\\.assign\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // window.open with javascript:
        Pattern.compile("window\\.open\\s*\\(\\s*['\"]javascript:", Pattern.CASE_INSENSITIVE),
        
        // document.cookie access
        Pattern.compile("document\\.cookie", Pattern.CASE_INSENSITIVE),
        
        // Dangerous event handlers
        Pattern.compile("on\\w+\\s*=\\s*['\"]", Pattern.CASE_INSENSITIVE),
        
        // Script tag creation
        Pattern.compile("createElement\\s*\\(\\s*['\"]script['\"]", Pattern.CASE_INSENSITIVE),
        
        // Import/require (for Node.js context)
        Pattern.compile("\\brequire\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bimport\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // Dangerous globals
        Pattern.compile("\\b__proto__\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bconstructor\\s*\\.\\s*prototype", Pattern.CASE_INSENSITIVE),
        
        // with statement
        Pattern.compile("\\bwith\\s*\\(", Pattern.CASE_INSENSITIVE)
    };

    // Prototype pollution patterns
    private static final Pattern[] PROTOTYPE_POLLUTION_PATTERNS = {
        Pattern.compile("__proto__\\s*[\\[\\.]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("constructor\\s*\\.\\s*prototype", Pattern.CASE_INSENSITIVE),
        Pattern.compile("prototype\\s*\\.\\s*__proto__", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Object\\.prototype", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[\\s*['\"]__proto__['\"]\\s*\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[\\s*['\"]constructor['\"]\\s*\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\[\\s*['\"]prototype['\"]\\s*\\]", Pattern.CASE_INSENSITIVE)
    };

    // DOM-based XSS sinks
    private static final String[] DOM_XSS_SINKS = {
        "innerHTML", "outerHTML", "insertAdjacentHTML",
        "document.write", "document.writeln",
        "eval", "setTimeout", "setInterval",
        "Function", "execScript",
        "location", "location.href", "location.replace",
        "window.open", "document.location"
    };

    /**
     * Validate JavaScript code for dangerous patterns
     */
    public void validateJavaScript(String jsCode) {
        if (jsCode == null || jsCode.isEmpty()) {
            return;
        }

        // Check for dangerous patterns
        for (Pattern pattern : DANGEROUS_JS_PATTERNS) {
            if (pattern.matcher(jsCode).find()) {
                log.error("Dangerous JavaScript pattern detected: {}", 
                    sanitizeForLog(jsCode));
                throw new SecurityException("Dangerous JavaScript pattern detected");
            }
        }
    }

    /**
     * Validate for prototype pollution attempts
     */
    public void validatePrototypePollution(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        for (Pattern pattern : PROTOTYPE_POLLUTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.error("Prototype pollution attempt detected: {}", 
                    sanitizeForLog(input));
                throw new SecurityException("Prototype pollution attempt detected");
            }
        }
    }

    /**
     * Validate JSON for prototype pollution
     */
    public void validateJsonForPrototypePollution(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }

        // Check for __proto__, constructor, prototype in JSON keys
        if (json.contains("\"__proto__\"") || 
            json.contains("'__proto__'") ||
            json.contains("\"constructor\"") ||
            json.contains("\"prototype\"")) {
            log.error("Prototype pollution in JSON detected");
            throw new SecurityException("Invalid JSON: prototype pollution keys detected");
        }
    }

    /**
     * Check if string contains dangerous JavaScript
     */
    public boolean containsDangerousJavaScript(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : DANGEROUS_JS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check for DOM-based XSS sinks
     */
    public boolean containsDOMXSSSink(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();
        for (String sink : DOM_XSS_SINKS) {
            if (lowerInput.contains(sink.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate event handler
     */
    public void validateEventHandler(String handler) {
        if (handler == null || handler.isEmpty()) {
            return;
        }

        // Event handlers should not contain dangerous patterns
        if (containsDangerousJavaScript(handler)) {
            log.error("Dangerous JavaScript in event handler: {}", 
                sanitizeForLog(handler));
            throw new SecurityException("Invalid event handler");
        }
    }

    /**
     * Validate URL for javascript: protocol
     */
    public void validateUrlProtocol(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        String lowerUrl = url.toLowerCase().trim();
        
        // Check for dangerous protocols
        if (lowerUrl.startsWith("javascript:") ||
            lowerUrl.startsWith("vbscript:") ||
            lowerUrl.startsWith("data:text/html") ||
            lowerUrl.startsWith("data:application/javascript")) {
            log.error("Dangerous URL protocol detected: {}", sanitizeForLog(url));
            throw new SecurityException("Dangerous URL protocol not allowed");
        }

        // Check for encoded javascript:
        if (lowerUrl.contains("%6a%61%76%61%73%63%72%69%70%74") || // javascript
            lowerUrl.contains("&#106;&#97;&#118;&#97;")) { // java
            log.error("Encoded javascript: protocol detected");
            throw new SecurityException("Encoded dangerous protocol detected");
        }
    }

    /**
     * Sanitize JavaScript string for safe output
     */
    public String sanitizeJavaScriptString(String input) {
        if (input == null) {
            return null;
        }

        // Escape special characters
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("'", "\\'")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("<", "\\x3C")
                   .replace(">", "\\x3E")
                   .replace("&", "\\x26");
    }

    /**
     * Validate object property name (prevent prototype pollution)
     */
    public void validatePropertyName(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return;
        }

        String lowerProperty = propertyName.toLowerCase();

        // Block dangerous property names
        if (lowerProperty.equals("__proto__") ||
            lowerProperty.equals("constructor") ||
            lowerProperty.equals("prototype")) {
            log.error("Dangerous property name: {}", propertyName);
            throw new SecurityException("Property name not allowed: " + propertyName);
        }
    }

    /**
     * Check for template injection
     */
    public boolean containsTemplateInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // Check for Angular/Vue/React template syntax
        return input.contains("{{") || 
               input.contains("}}") ||
               input.contains("${") ||
               input.contains("<%") ||
               input.contains("%>");
    }

    /**
     * Validate for template injection
     */
    public void validateTemplateInjection(String input) {
        if (containsTemplateInjection(input)) {
            log.error("Template injection attempt detected: {}", sanitizeForLog(input));
            throw new SecurityException("Template injection detected");
        }
    }

    /**
     * Check for JSONP callback
     */
    public boolean isJSONPCallback(String callback) {
        if (callback == null || callback.isEmpty()) {
            return false;
        }

        // JSONP callbacks should only contain alphanumeric and dots
        return !callback.matches("^[a-zA-Z0-9._]+$");
    }

    /**
     * Validate JSONP callback name
     */
    public void validateJSONPCallback(String callback) {
        if (callback == null || callback.isEmpty()) {
            return;
        }

        // Only allow safe callback names
        if (!callback.matches("^[a-zA-Z][a-zA-Z0-9._]*$")) {
            log.error("Invalid JSONP callback: {}", callback);
            throw new SecurityException("Invalid JSONP callback name");
        }

        // Check for dangerous patterns
        if (containsDangerousJavaScript(callback)) {
            log.error("Dangerous JavaScript in JSONP callback");
            throw new SecurityException("Invalid JSONP callback");
        }
    }

    /**
     * Sanitize for logging
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }

        if (input.length() > 100) {
            input = input.substring(0, 100) + "...";
        }

        return input.replaceAll("[\\r\\n\\t]", " ")
                   .replaceAll("[\\p{Cntrl}]", "");
    }

    /**
     * Validate Content-Type for JavaScript responses
     */
    public void validateJavaScriptContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return;
        }

        String lowerContentType = contentType.toLowerCase();

        // JavaScript should only be served with proper MIME types
        if (!lowerContentType.contains("application/javascript") &&
            !lowerContentType.contains("text/javascript") &&
            !lowerContentType.contains("application/json")) {
            
            // Check if it looks like JavaScript but has wrong content type
            log.warn("Suspicious content type for JavaScript: {}", contentType);
        }
    }
}
