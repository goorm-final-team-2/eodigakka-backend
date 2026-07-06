package com.eodigakka.global.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Profile("dev")
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DevHttpLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(DevHttpLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ContentCachingRequestWrapper cachingRequest =
        new ContentCachingRequestWrapper(request, Integer.MAX_VALUE);
    ContentCachingResponseWrapper cachingResponse = new ContentCachingResponseWrapper(response);

    try {
      filterChain.doFilter(cachingRequest, cachingResponse);
    } catch (Exception exception) {
      log.error(
          "HTTP exchange failed before a response was completed\n{}",
          exchange(cachingRequest),
          exception);
      throw exception;
    } finally {
      logCompletedExchange(cachingRequest, cachingResponse);
      cachingResponse.copyBodyToResponse();
    }
  }

  private void logCompletedExchange(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
    String exchange = exchange(request, response);
    if (response.getStatus() >= 400) {
      log.warn("HTTP exchange completed with error\n{}", exchange);
      return;
    }
    log.info("HTTP exchange completed\n{}", exchange);
  }

  private String exchange(ContentCachingRequestWrapper request) {
    return """
        request: %s %s%s
        parameters: %s
        requestBody: %s
        """
        .formatted(
            request.getMethod(),
            request.getRequestURI(),
            queryString(request),
            parameters(request),
            body(request.getContentAsByteArray(), charset(request.getCharacterEncoding())));
  }

  private String exchange(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
    return """
        %sresponseStatus: %s
        responseBody: %s
        """
        .formatted(
            exchange(request),
            response.getStatus(),
            body(response.getContentAsByteArray(), charset(response.getCharacterEncoding())));
  }

  private String queryString(HttpServletRequest request) {
    String queryString = request.getQueryString();
    return queryString == null || queryString.isBlank() ? "" : "?" + queryString;
  }

  private String parameters(HttpServletRequest request) {
    if (request.getParameterMap().isEmpty()) {
      return "{}";
    }
    return request.getParameterMap().entrySet().stream()
        .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  private Charset charset(String encoding) {
    if (encoding == null
        || encoding.isBlank()
        || StandardCharsets.ISO_8859_1.name().equals(encoding)) {
      return StandardCharsets.UTF_8;
    }
    return Charset.forName(encoding);
  }

  private String body(byte[] body, Charset charset) {
    return body.length == 0 ? "" : new String(body, charset);
  }
}
