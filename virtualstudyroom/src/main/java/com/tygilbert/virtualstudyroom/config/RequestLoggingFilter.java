package com.tygilbert.virtualstudyroom.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = Optional.ofNullable(request.getHeader(CORRELATION_ID_HEADER))
                .filter(value -> !value.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());

        MDC.put(CORRELATION_ID_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.nanoTime();
        boolean requestFailed = false;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            requestFailed = true;
            logger.error(
                    "request_failed method={} path={} status={} durationMs={} remoteAddr={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    elapsedMillis(startTime),
                    request.getRemoteAddr(),
                    exception
            );
            throw exception;
        } finally {
            if (!requestFailed) {
                logCompletion(request, response, startTime);
            }
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    private void logCompletion(HttpServletRequest request, HttpServletResponse response, long startTime) {
        int status = response.getStatus();
        String message = "request_complete method={} path={} status={} durationMs={} remoteAddr={}";
        long durationMs = elapsedMillis(startTime);

        if (status >= 500) {
            logger.error(message, request.getMethod(), request.getRequestURI(), status, durationMs, request.getRemoteAddr());
            return;
        }

        if (status >= 400) {
            logger.warn(message, request.getMethod(), request.getRequestURI(), status, durationMs, request.getRemoteAddr());
            return;
        }

        logger.info(message, request.getMethod(), request.getRequestURI(), status, durationMs, request.getRemoteAddr());
    }

    private long elapsedMillis(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}