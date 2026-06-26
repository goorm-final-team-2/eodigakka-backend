package com.eodigakka.global;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eodigakka.global.error.BusinessException;
import com.eodigakka.global.error.ErrorCode;
import com.eodigakka.global.error.GlobalExceptionHandler;
import com.eodigakka.global.response.ApiResponse;
import com.eodigakka.global.security.CustomAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalApiContractTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void successResponseContainsDataAndMessage() throws Exception {
    mockMvc
        .perform(get("/test/success"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.value").value("ok"))
        .andExpect(jsonPath("$.message").value("success"));
  }

  @Test
  void emptySuccessResponseOmitsNullData() throws Exception {
    mockMvc
        .perform(get("/test/success-empty"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"message\":\"success\"}"));
  }

  @Test
  void businessExceptionUsesConfiguredStatusAndErrorCode() throws Exception {
    mockMvc
        .perform(get("/test/business-error"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"))
        .andExpect(jsonPath("$.message").value("요청 권한이 없습니다."))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void invalidRequestBodyReturnsFieldErrors() throws Exception {
    mockMvc
        .perform(
            post("/test/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.errors.name").value("이름은 필수입니다."));
  }

  @Test
  void malformedJsonReturnsBadRequestWithoutInternalDetails() throws Exception {
    mockMvc
        .perform(
            post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("요청값이 올바르지 않습니다."));
  }

  @Test
  void typeMismatchReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/test/type/not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void missingRequestParameterReturnsBadRequest() throws Exception {
    mockMvc
        .perform(get("/test/required"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  @Test
  void missingResourceReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void unexpectedExceptionReturnsGenericServerError() throws Exception {
    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
        .andExpect(content().string(not(containsString("sensitive detail"))));
  }

  @Test
  void authenticationEntryPointWritesUtf8ErrorResponse() throws Exception {
    CustomAuthenticationEntryPoint entryPoint =
        new CustomAuthenticationEntryPoint(
            JsonMapper.builder().addModule(new JavaTimeModule()).build());
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(
        new MockHttpServletRequest(), response, new AuthenticationException("unauthorized") {});

    String body = response.getContentAsString(StandardCharsets.UTF_8);
    JsonNode responseBody = new ObjectMapper().readTree(body);
    org.assertj.core.api.Assertions.assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
    org.assertj.core.api.Assertions.assertThat(responseBody.get("message").asText())
        .isEqualTo("인증이 필요합니다.");
  }

  @RestController
  static class TestController {

    @GetMapping("/test/success")
    ApiResponse<Map<String, String>> success() {
      return ApiResponse.success(Map.of("value", "ok"));
    }

    @GetMapping("/test/success-empty")
    ApiResponse<Void> successEmpty() {
      return ApiResponse.success();
    }

    @GetMapping("/test/business-error")
    void businessError() {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @PostMapping("/test/validate")
    ApiResponse<TestRequest> validate(@Valid @RequestBody TestRequest request) {
      return ApiResponse.success(request);
    }

    @GetMapping("/test/type/{id}")
    ApiResponse<Long> type(@PathVariable Long id) {
      return ApiResponse.success(id);
    }

    @GetMapping("/test/required")
    ApiResponse<String> required(@RequestParam String value) {
      return ApiResponse.success(value);
    }

    @GetMapping("/test/not-found")
    ResponseEntity<Void> notFound() throws NoResourceFoundException {
      throw new NoResourceFoundException(HttpMethod.GET, "/missing");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
      throw new IllegalStateException("sensitive detail");
    }
  }

  record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {}
}
