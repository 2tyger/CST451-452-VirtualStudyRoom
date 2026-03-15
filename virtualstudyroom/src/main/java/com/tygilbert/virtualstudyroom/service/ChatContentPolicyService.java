package com.tygilbert.virtualstudyroom.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@Service
public class ChatContentPolicyService {

    private final int maxLength;

    public ChatContentPolicyService(@Value("${app.chat.max-length:500}") int maxLength) {
        this.maxLength = maxLength;
    }

    public String sanitizeAndValidate(String rawBody) {
        String normalized = rawBody == null ? "" : rawBody;
        normalized = normalized.replace("\r\n", "\n");
        normalized = stripDisallowedControlChars(normalized).trim();

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body is required");
        }

        if (normalized.length() > maxLength) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message body exceeds max length of " + maxLength + " characters"
            );
        }

        return HtmlUtils.htmlEscape(normalized);
    }

    private String stripDisallowedControlChars(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\t') {
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }
}