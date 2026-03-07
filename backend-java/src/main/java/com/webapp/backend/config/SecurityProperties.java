package com.webapp.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for security settings.
 * 
 * These can be overridden in application.properties or application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private RateLimit rateLimit = new RateLimit();
    private LoginAttempt loginAttempt = new LoginAttempt();

    public static class RateLimit {
        private int generalCapacity = 100;
        private int generalRefillTokens = 100;
        private int generalRefillMinutes = 1;
        
        private int authCapacity = 10;
        private int authRefillTokens = 10;
        private int authRefillMinutes = 1;

        // Getters and setters
        public int getGeneralCapacity() { return generalCapacity; }
        public void setGeneralCapacity(int generalCapacity) { this.generalCapacity = generalCapacity; }
        
        public int getGeneralRefillTokens() { return generalRefillTokens; }
        public void setGeneralRefillTokens(int generalRefillTokens) { this.generalRefillTokens = generalRefillTokens; }
        
        public int getGeneralRefillMinutes() { return generalRefillMinutes; }
        public void setGeneralRefillMinutes(int generalRefillMinutes) { this.generalRefillMinutes = generalRefillMinutes; }
        
        public int getAuthCapacity() { return authCapacity; }
        public void setAuthCapacity(int authCapacity) { this.authCapacity = authCapacity; }
        
        public int getAuthRefillTokens() { return authRefillTokens; }
        public void setAuthRefillTokens(int authRefillTokens) { this.authRefillTokens = authRefillTokens; }
        
        public int getAuthRefillMinutes() { return authRefillMinutes; }
        public void setAuthRefillMinutes(int authRefillMinutes) { this.authRefillMinutes = authRefillMinutes; }
    }

    public static class LoginAttempt {
        private int maxAttempts = 5;
        private int blockDurationMinutes = 15;

        // Getters and setters
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        
        public int getBlockDurationMinutes() { return blockDurationMinutes; }
        public void setBlockDurationMinutes(int blockDurationMinutes) { this.blockDurationMinutes = blockDurationMinutes; }
    }

    // Getters and setters
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }
    
    public LoginAttempt getLoginAttempt() { return loginAttempt; }
    public void setLoginAttempt(LoginAttempt loginAttempt) { this.loginAttempt = loginAttempt; }
}
