package com.cinemamemory.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class InputSanitizerTest {
    @Test
    void requiredPlainTextRejectsHtml() {
        assertThatThrownBy(() -> InputSanitizer.requiredPlainText("<img src=x onerror=alert(1)>", "Title", 160))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void optionalMediaUrlCanonicalizesProxiedLocalUploadPath() {
        String mediaUrl = InputSanitizer.optionalMediaUrl("/api/backend/uploads/media/example.png");

        assertThat(mediaUrl).isEqualTo("/uploads/media/example.png");
    }

    @Test
    void optionalMediaUrlRejectsJavascriptUrl() {
        assertThatThrownBy(() -> InputSanitizer.optionalMediaUrl("javascript:alert(1)"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).status())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
