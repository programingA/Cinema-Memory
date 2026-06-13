package com.cinemamemory.api.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

public final class InputSanitizer {
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern HTML_MARKERS = Pattern.compile("[<>]");
    private static final String LOCAL_MEDIA_PREFIX = "/uploads/media/";

    private InputSanitizer() {
    }

    public static String requiredPlainText(String value, String fieldName, int maxLength) {
        String sanitized = plainText(value, fieldName, maxLength);
        if (sanitized == null || sanitized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return sanitized;
    }

    public static String optionalPlainText(String value, String fieldName, int maxLength) {
        return plainText(value, fieldName, maxLength);
    }

    public static String optionalMediaUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String sanitized = sanitizeBasic(value, "Media URL", 700);
        if (sanitized.startsWith("/")) {
            int mediaPathIndex = sanitized.indexOf(LOCAL_MEDIA_PREFIX);
            if (mediaPathIndex >= 0) {
                return sanitized.substring(mediaPathIndex);
            }
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media URL");
        }

        try {
            URI uri = new URI(sanitized);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                return sanitized;
            }
        } catch (URISyntaxException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media URL");
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid media URL");
    }

    private static String plainText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String sanitized = sanitizeBasic(value, fieldName, maxLength);
        if (HTML_MARKERS.matcher(sanitized).find()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " cannot include HTML");
        }
        return sanitized;
    }

    private static String sanitizeBasic(String value, String fieldName, int maxLength) {
        String sanitized = CONTROL_CHARS.matcher(value.trim()).replaceAll("");
        if (sanitized.length() > maxLength) {
            throw new ApiException(HttpStatus.BAD_REQUEST, fieldName + " is too long");
        }
        return sanitized;
    }
}
