package com.webapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * SQL Injection Validator
 * 
 * Validates user inputs to prevent SQL injection attacks by:
 * - Detecting SQL keywords and patterns
 * - Blocking SQL metacharacters
 * - Validating input format
 * - Sanitizing query parameters
 * 
 * Note: This is an additional defense layer. The primary protection
 * comes from using parameterized queries (PreparedStatements/JPA).
 */
@Component
public class SQLInjectionValidator {

    private static final Logger log = LoggerFactory.getLogger(SQLInjectionValidator.class);

    // SQL injection patterns
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
        // SQL keywords
        Pattern.compile(".*\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE|CAST|CONVERT)\\b.*", 
            Pattern.CASE_INSENSITIVE),
        
        // SQL comments
        Pattern.compile(".*(-{2}|/\\*|\\*/|#).*"),
        
        // SQL string concatenation
        Pattern.compile(".*\\|\\|.*"),
        Pattern.compile(".*\\+.*'.*"),
        
        // SQL operators and functions
        Pattern.compile(".*\\b(AND|OR|NOT|XOR|BETWEEN|IN|EXISTS|IS NULL|IS NOT NULL)\\b.*", 
            Pattern.CASE_INSENSITIVE),
        
        // SQL metacharacters
        Pattern.compile(".*[';\"\\\\].*"),
        
        // Hex encoding
        Pattern.compile(".*0x[0-9a-fA-F]+.*"),
        
        // SQL functions
        Pattern.compile(".*\\b(CONCAT|SUBSTRING|ASCII|CHAR|SLEEP|BENCHMARK|WAITFOR|DELAY)\\b.*", 
            Pattern.CASE_INSENSITIVE),
        
        // Stacked queries
        Pattern.compile(".*;\\s*(SELECT|INSERT|UPDATE|DELETE|DROP).*", 
            Pattern.CASE_INSENSITIVE),
        
        // Boolean-based blind SQL injection
        Pattern.compile(".*\\b(TRUE|FALSE)\\b.*=.*\\b(TRUE|FALSE)\\b.*", 
            Pattern.CASE_INSENSITIVE),
        
        // Time-based blind SQL injection
        Pattern.compile(".*\\b(SLEEP|BENCHMARK|WAITFOR|DELAY|PG_SLEEP)\\b.*\\(.*\\).*", 
            Pattern.CASE_INSENSITIVE),
        
        // UNION-based injection
        Pattern.compile(".*\\bUNION\\b.*\\bSELECT\\b.*", 
            Pattern.CASE_INSENSITIVE),
        
        // Subquery injection
        Pattern.compile(".*\\(\\s*SELECT\\b.*\\).*", 
            Pattern.CASE_INSENSITIVE)
    };

    // Strict SQL patterns (for high-security fields)
    private static final Pattern[] STRICT_SQL_PATTERNS = {
        Pattern.compile(".*[';\"\\-#/\\\\*].*"),  // Any SQL metacharacter
        Pattern.compile(".*\\b(SELECT|INSERT|UPDATE|DELETE|DROP|UNION|EXEC)\\b.*", 
            Pattern.CASE_INSENSITIVE)
    };

    /**
     * Validate input for SQL injection patterns
     */
    public void validateInput(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.error("SQL injection attempt detected: {}", sanitizeForLog(input));
                throw new SecurityException("Invalid input: potential SQL injection detected");
            }
        }
    }

    /**
     * Validate input with strict rules (for sensitive fields)
     */
    public void validateInputStrict(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        for (Pattern pattern : STRICT_SQL_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.error("SQL injection attempt detected (strict): {}", sanitizeForLog(input));
                throw new SecurityException("Invalid input: contains forbidden characters");
            }
        }
    }

    /**
     * Validate search query parameter
     */
    public void validateSearchQuery(String query) {
        if (query == null || query.isEmpty()) {
            return;
        }

        // Check length
        if (query.length() > 100) {
            throw new IllegalArgumentException("Search query too long (max 100 characters)");
        }

        // Check for SQL injection
        validateInput(query);

        // Additional checks for search queries
        if (query.contains("--") || query.contains("/*") || query.contains("*/")) {
            log.error("SQL comment detected in search query: {}", sanitizeForLog(query));
            throw new SecurityException("Invalid search query: SQL comments not allowed");
        }
    }

    /**
     * Validate numeric input (ID, count, etc.)
     */
    public void validateNumericInput(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        if (!input.matches("^[0-9]+$")) {
            log.warn("Non-numeric input detected: {}", sanitizeForLog(input));
            throw new IllegalArgumentException("Input must be numeric");
        }
    }

    /**
     * Validate alphanumeric input (username, identifier, etc.)
     */
    public void validateAlphanumeric(String input) {
        if (input == null || input.isEmpty()) {
            return;
        }

        if (!input.matches("^[a-zA-Z0-9_-]+$")) {
            log.warn("Non-alphanumeric input detected: {}", sanitizeForLog(input));
            throw new IllegalArgumentException("Input must be alphanumeric");
        }
    }

    /**
     * Validate email format
     */
    public void validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return;
        }

        // Basic email validation
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check for SQL injection in email
        validateInputStrict(email);
    }

    /**
     * Sanitize input for safe usage (escape special characters)
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }

        // Remove SQL comments
        String sanitized = input.replaceAll("--.*", "")
                                .replaceAll("/\\*.*?\\*/", "")
                                .replaceAll("#.*", "");

        // Escape single quotes
        sanitized = sanitized.replace("'", "''");

        // Remove null bytes
        sanitized = sanitized.replace("\0", "");

        // Trim whitespace
        sanitized = sanitized.trim();

        return sanitized;
    }

    /**
     * Check if input contains SQL keywords
     */
    public boolean containsSQLKeywords(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        String upperInput = input.toUpperCase();
        String[] keywords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
            "UNION", "EXEC", "EXECUTE", "DECLARE", "CAST", "CONVERT",
            "TRUNCATE", "GRANT", "REVOKE"
        };

        for (String keyword : keywords) {
            if (upperInput.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if input contains SQL metacharacters
     */
    public boolean containsSQLMetacharacters(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        return input.matches(".*[';\"\\-#/\\\\*].*");
    }

    /**
     * Validate ORDER BY parameter (prevent injection in sorting)
     */
    public void validateOrderByParameter(String orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return;
        }

        // Only allow alphanumeric, underscore, and comma (for multiple columns)
        if (!orderBy.matches("^[a-zA-Z0-9_,\\s]+$")) {
            log.error("Invalid ORDER BY parameter: {}", sanitizeForLog(orderBy));
            throw new SecurityException("Invalid sort parameter");
        }

        // Check for SQL keywords
        if (containsSQLKeywords(orderBy)) {
            log.error("SQL keywords in ORDER BY parameter: {}", sanitizeForLog(orderBy));
            throw new SecurityException("Invalid sort parameter: contains SQL keywords");
        }
    }

    /**
     * Validate LIMIT/OFFSET parameters
     */
    public void validatePaginationParameter(String param) {
        if (param == null || param.isEmpty()) {
            return;
        }

        validateNumericInput(param);

        // Check reasonable limits
        try {
            int value = Integer.parseInt(param);
            if (value < 0 || value > 10000) {
                throw new IllegalArgumentException("Pagination parameter out of range (0-10000)");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid pagination parameter");
        }
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
     * Validate table/column name (for dynamic queries - use with extreme caution)
     */
    public void validateIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }

        // Only allow alphanumeric and underscore
        if (!identifier.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            log.error("Invalid identifier: {}", sanitizeForLog(identifier));
            throw new SecurityException("Invalid identifier format");
        }

        // Check for SQL keywords
        if (containsSQLKeywords(identifier)) {
            log.error("SQL keyword used as identifier: {}", identifier);
            throw new SecurityException("Identifier cannot be a SQL keyword");
        }

        // Check length
        if (identifier.length() > 64) {
            throw new IllegalArgumentException("Identifier too long (max 64 characters)");
        }
    }
}
