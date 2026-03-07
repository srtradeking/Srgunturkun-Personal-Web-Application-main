package com.webapp.backend.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility for parsing and extracting browser information from User-Agent strings
 */
@NoArgsConstructor
@Slf4j
public class BrowserInfoExtractor {

    /**
     * Parse User-Agent string to extract browser and OS information
     * 
     * @param userAgent the User-Agent header string
     * @return formatted browser info string (e.g., "Chrome 120 on Windows 10")
     */
    public static String extractBrowserInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String browser = extractBrowser(userAgent);
        String os = extractOS(userAgent);

        return String.format("%s on %s", browser, os);
    }

    /**
     * Extract browser name and version from User-Agent
     */
    private static String extractBrowser(String userAgent) {
        // Chrome
        if (userAgent.contains("Chrome")) {
            String version = extractVersion(userAgent, "Chrome");
            return "Chrome " + version;
        }
        // Firefox
        if (userAgent.contains("Firefox")) {
            String version = extractVersion(userAgent, "Firefox");
            return "Firefox " + version;
        }
        // Safari
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            String version = extractVersion(userAgent, "Version");
            return "Safari " + version;
        }
        // Edge
        if (userAgent.contains("Edg")) {
            String version = extractVersion(userAgent, "Edg");
            return "Edge " + version;
        }
        // Opera
        if (userAgent.contains("OPR")) {
            String version = extractVersion(userAgent, "OPR");
            return "Opera " + version;
        }
        // IE
        if (userAgent.contains("Trident")) {
            return "Internet Explorer";
        }

        return "Unknown Browser";
    }

    /**
     * Extract operating system from User-Agent
     */
    private static String extractOS(String userAgent) {
        // Windows
        if (userAgent.contains("Windows NT")) {
            if (userAgent.contains("Windows NT 10.0")) {
                return "Windows 10";
            }
            if (userAgent.contains("Windows NT 6.3")) {
                return "Windows 8.1";
            }
            if (userAgent.contains("Windows NT 6.2")) {
                return "Windows 8";
            }
            if (userAgent.contains("Windows NT 6.1")) {
                return "Windows 7";
            }
        }
        // macOS
        if (userAgent.contains("Macintosh")) {
            if (userAgent.contains("Intel Mac OS X")) {
                String version = extractMacVersion(userAgent);
                return "macOS " + version;
            }
        }
        // Linux
        if (userAgent.contains("Linux")) {
            return "Linux";
        }
        // iOS
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            String version = extractIOSVersion(userAgent);
            return "iOS " + version;
        }
        // Android
        if (userAgent.contains("Android")) {
            String version = extractAndroidVersion(userAgent);
            return "Android " + version;
        }

        return "Unknown OS";
    }

    /**
     * Extract version number from User-Agent
     */
    private static String extractVersion(String userAgent, String identifier) {
        int index = userAgent.indexOf(identifier);
        if (index == -1) {
            return "Unknown";
        }

        int startIndex = index + identifier.length() + 1;
        int endIndex = startIndex;

        while (endIndex < userAgent.length() &&
                (Character.isDigit(userAgent.charAt(endIndex)) || userAgent.charAt(endIndex) == '.')) {
            endIndex++;
        }

        return endIndex > startIndex ? userAgent.substring(startIndex, endIndex) : "Unknown";
    }

    /**
     * Extract macOS version
     */
    private static String extractMacVersion(String userAgent) {
        int index = userAgent.indexOf("Mac OS X");
        if (index == -1) {
            return "Unknown";
        }

        int startIndex = index + "Mac OS X ".length();
        int endIndex = startIndex;

        while (endIndex < userAgent.length() &&
                (Character.isDigit(userAgent.charAt(endIndex)) || userAgent.charAt(endIndex) == '_' || userAgent.charAt(endIndex) == '.')) {
            endIndex++;
        }

        String version = userAgent.substring(startIndex, endIndex).replace("_", ".");
        return !version.isEmpty() ? version : "Unknown";
    }

    /**
     * Extract iOS version
     */
    private static String extractIOSVersion(String userAgent) {
        int index = userAgent.indexOf("OS");
        if (index == -1) {
            return "Unknown";
        }

        int startIndex = index + 3;
        int endIndex = startIndex;

        while (endIndex < userAgent.length() &&
                (Character.isDigit(userAgent.charAt(endIndex)) || userAgent.charAt(endIndex) == '_')) {
            endIndex++;
        }

        String version = userAgent.substring(startIndex, endIndex).replace("_", ".");
        return !version.isEmpty() ? version : "Unknown";
    }

    /**
     * Extract Android version
     */
    private static String extractAndroidVersion(String userAgent) {
        int index = userAgent.indexOf("Android");
        if (index == -1) {
            return "Unknown";
        }

        int startIndex = index + "Android ".length();
        int endIndex = startIndex;

        while (endIndex < userAgent.length() &&
                (Character.isDigit(userAgent.charAt(endIndex)) || userAgent.charAt(endIndex) == '.')) {
            endIndex++;
        }

        return endIndex > startIndex ? userAgent.substring(startIndex, endIndex) : "Unknown";
    }
}
