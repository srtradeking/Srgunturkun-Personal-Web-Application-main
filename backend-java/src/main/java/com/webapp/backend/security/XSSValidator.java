package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

/**
 * XSS (Cross-Site Scripting) Validator
 * 
 * Validates and sanitizes user inputs to prevent XSS attacks by:
 * - Detecting script tags and event handlers
 * - Blocking JavaScript protocols
 * - Sanitizing HTML entities
 * - Validating input patterns
 */
@Component
public class XSSValidator {

    private static final Logger log = LoggerFactory.getLogger(XSSValidator.class);

    // XSS patterns to detect
    private static final Pattern[] XSS_PATTERNS = {
        // Script tags
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        
        // Event handlers
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on(load|error|click|mouse|focus|blur|change|submit)\\s*=", Pattern.CASE_INSENSITIVE),
        
        // JavaScript protocol
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        
        // iframe and embed tags
        Pattern.compile("<iframe[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<embed[^>]*>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<object[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // Style with expression
        Pattern.compile("style\\s*=.*expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("style\\s*=.*javascript:", Pattern.CASE_INSENSITIVE),
        
        // Meta refresh
        Pattern.compile("<meta[^>]*http-equiv[^>]*refresh", Pattern.CASE_INSENSITIVE),
        
        // Link with javascript
        Pattern.compile("<link[^>]*href\\s*=\\s*['\"]?javascript:", Pattern.CASE_INSENSITIVE),
        
        // Base tag
        Pattern.compile("<base[^>]*>", Pattern.CASE_INSENSITIVE),
        
        // Import
        Pattern.compile("@import", Pattern.CASE_INSENSITIVE),
        
        // Expression
        Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // Eval and similar
        Pattern.compile("\\beval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bexecScript\\s*\\(", Pattern.CASE_INSENSITIVE),
        
        // Document methods
        Pattern.compile("document\\.(write|cookie|domain)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("window\\.(location|open)", Pattern.CASE_INSENSITIVE),
        
        // HTML entities that could be XSS
        Pattern.compile("&#x?[0-9a-f]+;?", Pattern.CASE_INSENSITIVE),
        
        // SVG with script
        Pattern.compile("<svg[^>]*>.*?<script", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
    };

    /**
     * Validate input for XSS patterns
     */
    public void validateInput(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.error("XSS attempt detected: {}", sanitizeForLog(input));
                throw new SecurityException("Invalid input: potential XSS detected");
            }
        }
    }

    /**
     * Sanitize input by encoding HTML entities
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Use Spring's HtmlUtils to escape HTML
        String sanitized = HtmlUtils.htmlEscape(input);

        // Additional sanitization
        sanitized = sanitized.replaceAll("<", "&lt;")
                           .replaceAll(">", "&gt;")
                           .replaceAll("\"", "&quot;")
                           .replaceAll("'", "&#x27;")
                           .replaceAll("/", "&#x2F;");

        return sanitized;
    }

    /**
     * Sanitize HTML content (for rich text)
     * Allows safe HTML tags only
     */
    public String sanitizeHtml(String html) {
        if (html == null) {
            return null;
        }

        // Remove all script tags and content
        String sanitized = html.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        
        // Remove event handlers
        sanitized = sanitized.replaceAll("(?i)\\s*on\\w+\\s*=\\s*['\"][^'\"]*['\"]", "");
        sanitized = sanitized.replaceAll("(?i)\\s*on\\w+\\s*=\\s*[^\\s>]+", "");
        
        // Remove javascript: protocol
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)vbscript:", "");
        
        // Remove style with expression
        sanitized = sanitized.replaceAll("(?i)style\\s*=\\s*['\"][^'\"]*expression[^'\"]*['\"]", "");
        
        // Remove iframe, embed, object
        sanitized = sanitized.replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "");
        sanitized = sanitized.replaceAll("(?i)<embed[^>]*>", "");
        sanitized = sanitized.replaceAll("(?i)<object[^>]*>.*?</object>", "");
        
        return sanitized;
    }

    /**
     * Validate and sanitize text input (no HTML allowed)
     */
    public String sanitizeText(String text) {
        if (text == null) {
            return null;
        }

        // First validate for XSS
        validateInput(text);

        // Then sanitize
        return sanitizeInput(text);
    }

    /**
     * Validate URL for XSS
     */
    public void validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        String lowerUrl = url.toLowerCase();

        // Check for javascript: protocol
        if (lowerUrl.startsWith("javascript:") || 
            lowerUrl.startsWith("vbscript:") ||
            lowerUrl.startsWith("data:text/html")) {
            log.error("XSS attempt in URL: {}", sanitizeForLog(url));
            throw new SecurityException("Invalid URL: dangerous protocol detected");
        }

        // Check for encoded javascript
        if (lowerUrl.contains("%6a%61%76%61%73%63%72%69%70%74") || // javascript
            lowerUrl.contains("&#106;&#97;&#118;&#97;")) { // java
            log.error("Encoded XSS attempt in URL: {}", sanitizeForLog(url));
            throw new SecurityException("Invalid URL: encoded script detected");
        }

        // Validate for XSS patterns
        validateInput(url);
    }

    /**
     * Check if input contains XSS patterns
     */
    public boolean containsXSS(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Encode for JavaScript context
     */
    public String encodeForJavaScript(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\'':
                    encoded.append("\\'");
                    break;
                case '"':
                    encoded.append("\\\"");
                    break;
                case '\\':
                    encoded.append("\\\\");
                    break;
                case '/':
                    encoded.append("\\/");
                    break;
                case '\n':
                    encoded.append("\\n");
                    break;
                case '\r':
                    encoded.append("\\r");
                    break;
                case '\t':
                    encoded.append("\\t");
                    break;
                case '<':
                    encoded.append("\\x3C");
                    break;
                case '>':
                    encoded.append("\\x3E");
                    break;
                default:
                    if (c < 32 || c > 126) {
                        encoded.append(String.format("\\u%04x", (int) c));
                    } else {
                        encoded.append(c);
                    }
            }
        }
        return encoded.toString();
    }

    /**
     * Encode for HTML attribute context
     */
    public String encodeForHtmlAttribute(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;")
                   .replaceAll("'", "&#x27;")
                   .replaceAll("/", "&#x2F;");
    }

    /**
     * Encode for CSS context
     */
    public String encodeForCSS(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder encoded = new StringBuilder();
        for (char c : input.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || 
                (c >= '0' && c <= '9') || c == '-' || c == '_') {
                encoded.append(c);
            } else {
                encoded.append(String.format("\\%x ", (int) c));
            }
        }
        return encoded.toString();
    }

    /**
     * Sanitize input for logging (prevent log injection)
     */
    private String sanitizeForLog(String input) {
        if (input == null) {
            return "null";
        }

        // Truncate long inputs
        if (input.length() > 100) {
            input = input.substring(0, 100) + "...";
        }

        // Remove newlines and control characters
        return input.replaceAll("[\\r\\n\\t]", " ")
                   .replaceAll("[\\p{Cntrl}]", "");
    }

    /**
     * Validate JSON input for XSS
     */
    public void validateJsonInput(String json) {
        if (json == null || json.isEmpty()) {
            return;
        }

        // Check for script tags in JSON
        if (json.contains("<script") || json.contains("</script>")) {
            log.error("XSS attempt in JSON: script tags detected");
            throw new SecurityException("Invalid JSON: script tags not allowed");
        }

        // Check for event handlers
        if (Pattern.compile("on\\w+\\s*:", Pattern.CASE_INSENSITIVE).matcher(json).find()) {
            log.error("XSS attempt in JSON: event handlers detected");
            throw new SecurityException("Invalid JSON: event handlers not allowed");
        }
    }

    /**
     * Strip all HTML tags
     */
    public String stripHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replaceAll("<[^>]*>", "");
    }

    /**
     * Validate input length to prevent DoS via large inputs
     */
    public void validateInputLength(String input, int maxLength) {
        if (input != null && input.length() > maxLength) {
            throw new IllegalArgumentException(
                String.format("Input too long: %d characters (max %d)", input.length(), maxLength)
            );
        }
    }
}
