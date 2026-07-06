package com.eodigakka.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class DevHttpLoggingFilterTest {

  @Test
  void logsRequestAndResponseDetails(CapturedOutput output) throws Exception {
    DevHttpLoggingFilter filter = new DevHttpLoggingFilter();
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/dev/auth/login");
    request.setQueryString("source=swagger");
    request.setParameter("source", "swagger");
    request.setContentType(MediaType.APPLICATION_JSON_VALUE);
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    request.setContent(
        """
        {"socialId":"frontend","nickname":"프론트"}
        """
            .getBytes(StandardCharsets.UTF_8));
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    FilterChain filterChain =
        (servletRequest, servletResponse) -> {
          try (ServletInputStream inputStream = servletRequest.getInputStream()) {
            inputStream.readAllBytes();
          }
          servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
          servletResponse
              .getWriter()
              .write("{\"data\":{\"accessToken\":\"access-token\"},\"message\":\"success\"}");
        };

    filter.doFilter(request, response, filterChain);

    assertThat(response.getContentAsString())
        .isEqualTo("{\"data\":{\"accessToken\":\"access-token\"},\"message\":\"success\"}");
    assertThat(output)
        .contains("HTTP exchange completed")
        .contains("POST /api/dev/auth/login?source=swagger")
        .contains("parameters: {source=swagger}")
        .contains("\"socialId\":\"frontend\"")
        .contains("\"nickname\":\"프론트\"")
        .contains("responseStatus: 200")
        .contains("\"accessToken\":\"access-token\"");
  }

  @Test
  void logsErrorResponseDetails(CapturedOutput output) throws Exception {
    DevHttpLoggingFilter filter = new DevHttpLoggingFilter();
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/appointments/10/places/search");
    request.setQueryString("query=강남역");
    request.setParameter("query", "강남역");
    request.setCharacterEncoding(StandardCharsets.UTF_8.name());
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        (servletRequest, servletResponse) -> {
          servletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
          ((HttpServletResponse) servletResponse).setStatus(502);
          servletResponse
              .getWriter()
              .write(
                  """
                  {"code":"KAKAO_LOCAL_SEARCH_FAILED","message":"카카오 장소 검색에 실패했습니다."}
                  """);
        };

    filter.doFilter(request, response, filterChain);

    assertThat(output)
        .contains("HTTP exchange completed with error")
        .contains("GET /api/appointments/10/places/search?query=강남역")
        .contains("parameters: {query=강남역}")
        .contains("responseStatus: 502")
        .contains("KAKAO_LOCAL_SEARCH_FAILED")
        .contains("카카오 장소 검색에 실패했습니다.");
  }
}
